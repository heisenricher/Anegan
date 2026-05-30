/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.filemanager

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ─────────────────────────────────────────────────────────
//  Data model
// ─────────────────────────────────────────────────────────

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String
)

enum class SortOrder { NAME, SIZE, DATE, TYPE }
enum class ViewMode { LIST, GRID }

// ─────────────────────────────────────────────────────────
//  Helpers
// ─────────────────────────────────────────────────────────

fun File.toFileItem(): FileItem = FileItem(
    name = name,
    path = absolutePath,
    isDirectory = isDirectory,
    size = if (isDirectory) 0L else length(),
    lastModified = lastModified(),
    extension = if (isDirectory) "" else extension.lowercase(Locale.getDefault())
)

fun Long.toHumanReadable(): String {
    if (this < 1024) return "$this B"
    val kb = this / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}

fun Long.toDateString(): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))

fun fileEmoji(item: FileItem): String = when {
    item.isDirectory -> "📁"
    item.extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> "🖼️"
    item.extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp") -> "🎬"
    item.extension in listOf("mp3", "m4a", "flac", "wav", "aac", "ogg") -> "🎵"
    item.extension in listOf("pdf") -> "📕"
    item.extension in listOf("apk") -> "📦"
    item.extension in listOf("zip", "rar", "7z", "tar", "gz") -> "🗜️"
    item.extension in listOf("doc", "docx", "odt") -> "📝"
    item.extension in listOf("xls", "xlsx", "csv") -> "📊"
    item.extension in listOf("ppt", "pptx") -> "📊"
    item.extension in listOf("txt", "md", "log") -> "📄"
    item.extension in listOf("kt", "java", "py", "js", "ts", "json", "xml", "html", "css") -> "💻"
    else -> "📄"
}

fun listDirectory(dir: File): List<FileItem> = try {
    dir.listFiles()
        ?.filter { !it.name.startsWith(".") }
        ?.map { it.toFileItem() }
        ?: emptyList()
} catch (e: SecurityException) {
    emptyList()
}

fun sortFiles(files: List<FileItem>, order: SortOrder): List<FileItem> {
    val (dirs, nonDirs) = files.partition { it.isDirectory }
    val sortedDirs = when (order) {
        SortOrder.NAME -> dirs.sortedBy { it.name.lowercase() }
        SortOrder.SIZE -> dirs.sortedBy { it.name.lowercase() }
        SortOrder.DATE -> dirs.sortedByDescending { it.lastModified }
        SortOrder.TYPE -> dirs.sortedBy { it.name.lowercase() }
    }
    val sortedFiles = when (order) {
        SortOrder.NAME -> nonDirs.sortedBy { it.name.lowercase() }
        SortOrder.SIZE -> nonDirs.sortedByDescending { it.size }
        SortOrder.DATE -> nonDirs.sortedByDescending { it.lastModified }
        SortOrder.TYPE -> nonDirs.sortedWith(compareBy({ it.extension }, { it.name.lowercase() }))
    }
    return sortedDirs + sortedFiles
}

fun storageStats(): Triple<Long, Long, Long> {
    val stat = StatFs(Environment.getExternalStorageDirectory().path)
    val total = stat.totalBytes
    val free = stat.availableBytes
    val used = total - free
    return Triple(total, used, free)
}

suspend fun zipFiles(files: List<FileItem>, destPath: String): Boolean = withContext(Dispatchers.IO) {
    try {
        ZipOutputStream(FileOutputStream(destPath)).use { zos ->
            files.forEach { fileItem ->
                val file = File(fileItem.path)
                if (file.exists() && file.isFile) {
                    zos.putNextEntry(ZipEntry(file.name))
                    FileInputStream(file).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

suspend fun unzipFile(zipPath: String, destDir: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val destFolder = File(destDir)
        if (!destFolder.exists()) destFolder.mkdirs()
        ZipInputStream(FileInputStream(zipPath)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destFolder, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

// ─────────────────────────────────────────────────────────
//  Quick-access locations
// ─────────────────────────────────────────────────────────

data class QuickLocation(val label: String, val path: String)

fun quickLocations(): List<QuickLocation> {
    val root = Environment.getExternalStorageDirectory().absolutePath
    return listOf(
        QuickLocation("Storage", root),
        QuickLocation("Downloads", "$root/Download"),
        QuickLocation("Documents", "$root/Documents"),
        QuickLocation("Images", "$root/DCIM"),
        QuickLocation("Videos", "$root/Movies")
    )
}

// ─────────────────────────────────────────────────────────
//  Main Screen
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()

    // Navigation stack
    val rootDir = remember { Environment.getExternalStorageDirectory() }
    val navigationStack = remember { mutableStateListOf(rootDir) }
    val currentDir by remember { derivedStateOf { navigationStack.last() } }

    // Files state
    var allFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // UI state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var sortOrder by remember { mutableStateOf(SortOrder.NAME) }

    // Selection state
    val selectedPaths = remember { mutableStateListOf<String>() }
    val isSelectionMode by remember { derivedStateOf { selectedPaths.isNotEmpty() } }

    // Bottom-sheet state
    var showSortSheet by remember { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<FileItem?>(null) }

    // Dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var renameValue by remember { mutableStateOf("") }

    // Storage stats
    val (storageTotal, storageUsed, _) = remember { storageStats() }

    val primaryAccent = NeonCyan // Cyan accent for Documents / File manager

    // Displayed / filtered + sorted file list
    val displayedFiles by remember {
        derivedStateOf {
            val filtered = if (searchQuery.isBlank()) allFiles
            else allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
            sortFiles(filtered, sortOrder)
        }
    }

    // Load directory contents
    fun loadDirectory(dir: File) {
        scope.launch {
            isLoading = true
            val files = withContext(Dispatchers.IO) { listDirectory(dir) }
            allFiles = files
            isLoading = false
        }
    }

    LaunchedEffect(currentDir) {
        selectedPaths.clear()
        searchQuery = ""
        loadDirectory(currentDir)
    }

    // ── Breadcrumb path list ──────────────────────────────────────
    val breadcrumbs by remember {
        derivedStateOf {
            navigationStack.map { dir ->
                if (dir == rootDir) "Root" else dir.name
            }
        }
    }

    // ── Delete selected or target ─────────────────────────────────
    fun performDelete(paths: List<String>) {
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                paths.all { path ->
                    runCatching { File(path).deleteRecursively() }.getOrDefault(false)
                }
            }
            selectedPaths.clear()
            loadDirectory(currentDir)
            NovaHaptics.warning(view)
            snackbarHostState.showSnackbar(if (success) "Deleted successfully" else "Some files could not be deleted")
        }
    }

    // ── Rename ────────────────────────────────────────────────────
    fun performRename(target: FileItem, newName: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val src = File(target.path)
                    val dest = File(src.parent ?: "", newName)
                    src.renameTo(dest)
                }.getOrDefault(false)
            }
            loadDirectory(currentDir)
            NovaHaptics.success(view)
            snackbarHostState.showSnackbar(if (result) "Renamed successfully" else "Rename failed")
        }
    }

    // ── Zip selected files ────────────────────────────────────────
    fun performZip(paths: List<String>) {
        scope.launch {
            val items = allFiles.filter { it.path in paths && !it.isDirectory }
            val zipName = "archive_${System.currentTimeMillis()}.zip"
            val destPath = "${currentDir.absolutePath}/$zipName"
            val ok = zipFiles(items, destPath)
            selectedPaths.clear()
            loadDirectory(currentDir)
            NovaHaptics.success(view)
            snackbarHostState.showSnackbar(if (ok) "Archive created: $zipName" else "Zip compression failed")
        }
    }

    // ── Unzip ──────────────────────────────────────────────────────
    fun performUnzip(zipItem: FileItem) {
        scope.launch {
            val destDir = "${currentDir.absolutePath}/${zipItem.name.removeSuffix(".zip")}"
            val ok = unzipFile(zipItem.path, destDir)
            loadDirectory(currentDir)
            NovaHaptics.success(view)
            snackbarHostState.showSnackbar(if (ok) "Extracted to $destDir" else "Zip extraction failed")
        }
    }

    BackHandler {
        when {
            isSearchActive -> { isSearchActive = false; searchQuery = "" }
            isSelectionMode -> selectedPaths.clear()
            navigationStack.size > 1 -> navigationStack.removeLast()
            else -> onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                NovaTopBar(
                    title = if (isSelectionMode) "${selectedPaths.size} SELECTED" else "File System",
                    onBack = {
                        when {
                            isSearchActive -> { isSearchActive = false; searchQuery = "" }
                            isSelectionMode -> selectedPaths.clear()
                            navigationStack.size > 1 -> navigationStack.removeLast()
                            else -> onBack()
                        }
                    },
                    neonAccent = primaryAccent,
                    actions = {
                        if (!isSearchActive && !isSelectionMode) {
                            IconButton(onClick = { 
                                NovaHaptics.click(view)
                                isSearchActive = true 
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = primaryAccent)
                            }
                            IconButton(onClick = { 
                                NovaHaptics.click(view)
                                viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST 
                            }) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = "Toggle View Mode",
                                    tint = primaryAccent
                                )
                            }
                            IconButton(onClick = { 
                                NovaHaptics.click(view)
                                showSortSheet = true 
                            }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort Files", tint = primaryAccent)
                            }
                        }
                        if (isSelectionMode) {
                            IconButton(onClick = {
                                NovaHaptics.click(view)
                                if (selectedPaths.size == allFiles.size) selectedPaths.clear()
                                else { selectedPaths.clear(); selectedPaths.addAll(allFiles.map { it.path }) }
                            }) {
                                Icon(
                                    imageVector = if (selectedPaths.size == allFiles.size)
                                        Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Select All",
                                    tint = primaryAccent
                                )
                            }
                        }
                    }
                )

                // Search Overlay Row
                if (isSearchActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xxs)
                    ) {
                        NovaTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search files in folder...",
                            leadingIcon = Icons.Default.Search,
                            trailingIcon = Icons.Default.Close,
                            onTrailingClick = {
                                isSearchActive = false
                                searchQuery = ""
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            neonColor = primaryAccent
                        )
                    }
                }

                // Breadcrumb Path Navigation Bar
                BreadcrumbBar(
                    crumbs = breadcrumbs,
                    onCrumbClick = { index ->
                        NovaHaptics.click(view)
                        while (navigationStack.size > index + 1) navigationStack.removeLast()
                    },
                    neonColor = primaryAccent
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                BatchActionBar(
                    onDelete = { 
                        NovaHaptics.warning(view)
                        showDeleteDialog = true 
                    },
                    onZip = { performZip(selectedPaths.toList()) },
                    onShare = {
                        NovaHaptics.click(view)
                        Toast.makeText(context, "Sharing selected archives...", Toast.LENGTH_SHORT).show()
                    },
                    neonColor = primaryAccent
                )
            }
        }
    ) { innerPadding ->
        NovaBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
            ) {
                // Storage summary card
                StorageSummaryCard(total = storageTotal, used = storageUsed, neonColor = primaryAccent)

                // Quick access locations
                QuickAccessChips(
                    currentPath = currentDir.absolutePath,
                    onLocationSelected = { path ->
                        NovaHaptics.click(view)
                        val dir = File(path)
                        if (dir.exists()) {
                            navigationStack.clear()
                            navigationStack.add(rootDir)
                            if (dir != rootDir) navigationStack.add(dir)
                        }
                    },
                    neonColor = primaryAccent
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (isLoading) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryAccent)
                    }
                } else if (displayedFiles.isEmpty()) {
                    EmptyFolderState(primaryAccent)
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (viewMode == ViewMode.LIST) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xxs),
                                contentPadding = PaddingValues(bottom = NovaTokens.Spacing.xl)
                            ) {
                                items(displayedFiles, key = { it.path }) { item ->
                                    val isSelected = selectedPaths.contains(item.path)
                                    FileListRow(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            when {
                                                isSelectionMode -> {
                                                    NovaHaptics.click(view)
                                                    if (isSelected) selectedPaths.remove(item.path)
                                                    else selectedPaths.add(item.path)
                                                }
                                                item.isDirectory -> {
                                                    NovaHaptics.click(view)
                                                    navigationStack.add(File(item.path))
                                                }
                                                else -> {
                                                    NovaHaptics.click(view)
                                                    onOpenFile(item.path)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            NovaHaptics.recording(view) // deep haptic snap
                                            actionTarget = item
                                            showActionSheet = true
                                        },
                                        neonColor = primaryAccent
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs),
                                contentPadding = PaddingValues(bottom = NovaTokens.Spacing.xl)
                            ) {
                                items(displayedFiles, key = { it.path }) { item ->
                                    val isSelected = selectedPaths.contains(item.path)
                                    FileGridItem(
                                        item = item,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            when {
                                                isSelectionMode -> {
                                                    NovaHaptics.click(view)
                                                    if (isSelected) selectedPaths.remove(item.path)
                                                    else selectedPaths.add(item.path)
                                                }
                                                item.isDirectory -> {
                                                    NovaHaptics.click(view)
                                                    navigationStack.add(File(item.path))
                                                }
                                                else -> {
                                                    NovaHaptics.click(view)
                                                    onOpenFile(item.path)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            NovaHaptics.recording(view)
                                            actionTarget = item
                                            showActionSheet = true
                                        },
                                        neonColor = primaryAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Sort bottom sheet ─────────────────────────────────────────
    if (showSortSheet) {
        SortBottomSheet(
            currentOrder = sortOrder,
            onOrderSelected = { sortOrder = it; showSortSheet = false },
            onDismiss = { showSortSheet = false },
            neonColor = primaryAccent
        )
    }

    // ── File action bottom sheet ───────────────────────────────────
    if (showActionSheet) {
        actionTarget?.let { target ->
            FileActionSheet(
                item = target,
                onDismiss = { showActionSheet = false; actionTarget = null },
                onRename = {
                    renameTarget = target
                    renameValue = target.name
                    showRenameDialog = true
                    showActionSheet = false
                },
                onDelete = {
                    showActionSheet = false
                    selectedPaths.clear()
                    selectedPaths.add(target.path)
                    showDeleteDialog = true
                },
                onZip = {
                    showActionSheet = false
                    performZip(listOf(target.path))
                },
                onUnzip = {
                    showActionSheet = false
                    if (target.extension == "zip") performUnzip(target)
                },
                onShare = {
                    showActionSheet = false
                    Toast.makeText(context, "Share: using system share sheets...", Toast.LENGTH_SHORT).show()
                },
                neonColor = primaryAccent
            )
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; selectedPaths.clear() },
            title = { 
                Text(
                    text = "Purge Assets?", 
                    style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = NovaError)
                ) 
            },
            text = {
                Text(
                    text = if (selectedPaths.size == 1) "Permanently delete \"${File(selectedPaths.first()).name}\"? This bypasses safety caches and cannot be undone."
                    else "Permanently purge these ${selectedPaths.size} items from storage? This cannot be undone.",
                    style = NovaTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                NovaPrimaryButton(
                    text = "Confirm Purge",
                    neonColor = NovaError,
                    onClick = {
                        performDelete(selectedPaths.toList())
                        showDeleteDialog = false
                    }
                )
            },
            dismissButton = {
                NovaSecondaryButton(
                    text = "Cancel",
                    neonColor = primaryAccent,
                    onClick = { 
                        showDeleteDialog = false
                        selectedPaths.clear() 
                    }
                )
            },
            containerColor = if (isDark) NovaMidnightBlue else Color.White,
            shape = RoundedCornerShape(NovaTokens.Radius.lg),
            modifier = Modifier.border(1.dp, NovaError.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.lg))
        )
    }

    // ── Rename dialog ─────────────────────────────────────────────
    if (showRenameDialog) {
        renameTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { 
                    Text(
                        text = "Modify File Identifier", 
                        style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = primaryAccent)
                    ) 
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Assign a new unique identity name to the item on storage.",
                            style = NovaTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        NovaTextField(
                            value = renameValue,
                            onValueChange = { renameValue = it },
                            placeholder = "Enter identifier...",
                            singleLine = true,
                            neonColor = primaryAccent
                        )
                    }
                },
                confirmButton = {
                    NovaPrimaryButton(
                        text = "Apply Rename",
                        neonColor = primaryAccent,
                        onClick = {
                            if (renameValue.isNotBlank()) {
                                performRename(target, renameValue.trim())
                            }
                            showRenameDialog = false
                        }
                    )
                },
                dismissButton = {
                    NovaSecondaryButton(
                        text = "Cancel",
                        neonColor = primaryAccent,
                        onClick = { showRenameDialog = false }
                    )
                },
                containerColor = if (isDark) NovaMidnightBlue else Color.White,
                shape = RoundedCornerShape(NovaTokens.Radius.lg),
                modifier = Modifier.border(1.dp, primaryAccent.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.lg))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Breadcrumb bar
// ─────────────────────────────────────────────────────────

@Composable
private fun BreadcrumbBar(
    crumbs: List<String>, 
    onCrumbClick: (Int) -> Unit,
    neonColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            NovaChip(
                text = crumb,
                selected = isLast,
                onClick = { if (!isLast) onCrumbClick(index) },
                neonColor = neonColor
            )
            if (!isLast) {
                Text(
                    text = "›",
                    color = neonColor.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Storage Summary Card
// ─────────────────────────────────────────────────────────

@Composable
private fun StorageSummaryCard(
    total: Long, 
    used: Long,
    neonColor: Color
) {
    val free = total - used
    val fraction = if (total > 0) used.toFloat() / total.toFloat() else 0f
    
    GlassCard(
        neonAccent = neonColor,
        enableGlow = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovaTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Mainframe Disk", 
                        style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = neonColor)
                    )
                    Text(
                        text = "${used.toHumanReadable()} / ${total.toHumanReadable()} (${(fraction * 100).toInt()}% USED)",
                        style = NovaTypography.tagMono.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = neonColor,
                    trackColor = neonColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = free.toHumanReadable(),
                    style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = neonColor, fontSize = 13.sp)
                )
                Text(
                    text = "FREE SPACE",
                    style = NovaTypography.tagMono.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Quick access chips
// ─────────────────────────────────────────────────────────

@Composable
private fun QuickAccessChips(
    currentPath: String,
    onLocationSelected: (String) -> Unit,
    neonColor: Color
) {
    val locations = remember { quickLocations() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = NovaTokens.Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        locations.forEach { loc ->
            val isSelected = currentPath == loc.path
            NovaChip(
                text = loc.label,
                selected = isSelected,
                onClick = { onLocationSelected(loc.path) },
                neonColor = neonColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  File list row
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    neonColor: Color
) {
    val isDark = isSystemInDarkTheme()
    val borderStrokeColor = if (isSelected) neonColor else if (isDark) NovaBorderDark.copy(alpha = 0.12f) else NovaBorderLight.copy(alpha = 0.12f)
    val containerBgColor = if (isSelected) neonColor.copy(alpha = 0.08f) else (if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovaTokens.Radius.md))
            .background(containerBgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(NovaTokens.Radius.md)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = NovaTokens.Spacing.sm, vertical = NovaTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = neonColor, uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Cyber Icon block
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                .background(neonColor.copy(alpha = 0.07f))
                .border(1.dp, neonColor.copy(alpha = 0.15f), RoundedCornerShape(NovaTokens.Radius.sm)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = fileEmoji(item), fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (item.isDirectory) "Folder · ${item.lastModified.toDateString()}"
                else "${item.size.toHumanReadable()} · ${item.lastModified.toDateString()}",
                style = NovaTypography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        Icon(
            imageVector = if (item.isDirectory) Icons.Rounded.ChevronRight else Icons.Rounded.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────
//  File grid item
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    neonColor: Color
) {
    val isDark = isSystemInDarkTheme()
    val borderStrokeColor = if (isSelected) neonColor else if (isDark) NovaBorderDark.copy(alpha = 0.12f) else NovaBorderLight.copy(alpha = 0.12f)
    val containerBgColor = if (isSelected) neonColor.copy(alpha = 0.08f) else (if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NovaTokens.Radius.md))
            .background(containerBgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(NovaTokens.Radius.md)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(NovaTokens.Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = neonColor),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(NovaTokens.Radius.md))
                .background(neonColor.copy(alpha = 0.07f))
                .border(1.dp, neonColor.copy(alpha = 0.15f), RoundedCornerShape(NovaTokens.Radius.md)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = fileEmoji(item), fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.name,
            style = NovaTypography.bodySmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = if (item.isDirectory) "Folder" else item.size.toHumanReadable(),
            style = NovaTypography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────────────────

@Composable
private fun EmptyFolderState(neonColor: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Folder Empty",
                style = NovaTypography.tagMono.copy(fontSize = 14.sp, color = neonColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No files or indices found in directory.",
                style = NovaTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Batch action bottom bar
// ─────────────────────────────────────────────────────────

@Composable
private fun BatchActionBar(
    onDelete: () -> Unit,
    onZip: () -> Unit,
    onShare: () -> Unit,
    neonColor: Color
) {
    val isDark = isSystemInDarkTheme()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(NovaTokens.Spacing.md),
        shape = RoundedCornerShape(NovaTokens.Radius.lg),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovaTokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Purge selected", tint = NovaError)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share selected", tint = neonColor)
            }
            NovaPrimaryButton(
                text = "🗜️ Create Zip",
                neonColor = neonColor,
                onClick = onZip,
                modifier = Modifier.height(36.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Sort bottom sheet
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    currentOrder: SortOrder,
    onOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
    neonColor: Color
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = if (isDark) NovaMidnightBlue else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovaTokens.Spacing.lg, vertical = NovaTokens.Spacing.xs)
        ) {
            NovaSectionHeader(
                title = "Sort System Directory",
                neonColor = neonColor
            )
            
            Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))

            SortOrder.entries.forEach { order ->
                val label = when (order) {
                    SortOrder.NAME -> "Name (A–Z alphabet)"
                    SortOrder.SIZE -> "Size (Largest bytes first)"
                    SortOrder.DATE -> "Date modified (Chronological)"
                    SortOrder.TYPE -> "File extension grouping"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            NovaHaptics.click(view)
                            onOrderSelected(order) 
                        }
                        .padding(vertical = NovaTokens.Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label, 
                        modifier = Modifier.weight(1f), 
                        style = NovaTypography.headlineSmall.copy(
                            fontWeight = if (order == currentOrder) FontWeight.Bold else FontWeight.Medium,
                            color = if (order == currentOrder) neonColor else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (order == currentOrder) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(neonColor)
                        )
                    }
                }
                if (order != SortOrder.entries.last()) Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.1f) else NovaBorderLight.copy(alpha = 0.1f))
            }
            Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
        }
    }
}

// ─────────────────────────────────────────────────────────
//  File action bottom sheet
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileActionSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onZip: () -> Unit,
    onUnzip: () -> Unit,
    onShare: () -> Unit,
    neonColor: Color
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current

    data class Action(val emoji: String, val label: String, val action: () -> Unit, val destructive: Boolean = false)

    val actions = buildList {
        add(Action("✏️", "Modify Identifier (Rename)", onRename))
        if (!item.isDirectory) add(Action("📤", "External Broadcast (Share)", onShare))
        if (!item.isDirectory) add(Action("🗜️", "Compress to ZIP (Zip)", onZip))
        if (item.extension == "zip") add(Action("📂", "Extract Archive here", onUnzip))
        add(Action("🗑️", "Purge from Storage (Delete)", onDelete, destructive = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = if (isDark) NovaMidnightBlue else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovaTokens.Spacing.lg, vertical = NovaTokens.Spacing.xs)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                        .background(neonColor.copy(alpha = 0.07f))
                        .border(1.dp, neonColor.copy(alpha = 0.15f), RoundedCornerShape(NovaTokens.Radius.sm)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fileEmoji(item), fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.name,
                        style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.isDirectory) {
                        Text(
                            text = item.size.toHumanReadable(),
                            style = NovaTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.15f) else NovaBorderLight.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            NovaHaptics.click(view)
                            action.action()
                            onDismiss()
                        }
                        .padding(vertical = NovaTokens.Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(action.emoji, fontSize = 20.sp, modifier = Modifier.width(36.dp))
                    Text(
                        text = action.label,
                        style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                        color = if (action.destructive) NovaError
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (action != actions.last()) Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.1f) else NovaBorderLight.copy(alpha = 0.1f), modifier = Modifier.padding(start = 36.dp))
            }
            Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
        }
    }
}
