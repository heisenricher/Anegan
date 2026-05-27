/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.filemanager

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
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
        QuickLocation("Internal Storage", root),
        QuickLocation("Downloads", "$root/Download"),
        QuickLocation("Documents", "$root/Documents"),
        QuickLocation("Images", "$root/DCIM"),
        QuickLocation("Videos", "$root/Movies"),
        QuickLocation("APKs", "$root/Download")
    )
}

// ─────────────────────────────────────────────────────────
//  Main Screen
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

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
                if (dir == rootDir) "Home" else dir.name
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
            snackbarHostState.showSnackbar(if (result) "Renamed to $newName" else "Rename failed")
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
            snackbarHostState.showSnackbar(if (ok) "Zipped → $zipName" else "Zip failed")
        }
    }

    // ── Unzip ──────────────────────────────────────────────────────
    fun performUnzip(zipItem: FileItem) {
        scope.launch {
            val destDir = "${currentDir.absolutePath}/${zipItem.name.removeSuffix(".zip")}"
            val ok = unzipFile(zipItem.path, destDir)
            loadDirectory(currentDir)
            snackbarHostState.showSnackbar(if (ok) "Extracted to $destDir" else "Extraction failed")
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  Scaffold
    // ═════════════════════════════════════════════════════════════
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search files…") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        } else {
                            Text(
                                text = if (isSelectionMode) "${selectedPaths.size} selected" else "File Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                isSearchActive -> { isSearchActive = false; searchQuery = "" }
                                isSelectionMode -> selectedPaths.clear()
                                navigationStack.size > 1 -> navigationStack.removeLast()
                                else -> onBack()
                            }
                        }) {
                             Icon(
                                 imageVector = if (isSelectionMode || isSearchActive)
                                     Icons.Default.Close else Icons.Default.ArrowBack,
                                 contentDescription = "Back"
                             )
                        }
                    },
                    actions = {
                        if (!isSearchActive && !isSelectionMode) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST }) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = "Toggle view"
                                )
                            }
                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                        }
                        if (isSelectionMode) {
                            IconButton(onClick = {
                                if (selectedPaths.size == allFiles.size) selectedPaths.clear()
                                else { selectedPaths.clear(); selectedPaths.addAll(allFiles.map { it.path }) }
                            }) {
                                Icon(
                                    imageVector = if (selectedPaths.size == allFiles.size)
                                        Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Select all"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                // Breadcrumb bar
                BreadcrumbBar(
                    crumbs = breadcrumbs,
                    onCrumbClick = { index ->
                        while (navigationStack.size > index + 1) navigationStack.removeLast()
                    }
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
                    onDelete = { showDeleteDialog = true },
                    onZip = { performZip(selectedPaths.toList()) },
                    onShare = {
                        snackbarHostState.let { scope.launch { it.showSnackbar("Share: use system intent in host Activity") } }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage summary card
            StorageSummaryCard(total = storageTotal, used = storageUsed)

            // Quick access chips
            QuickAccessChips { path ->
                val dir = File(path)
                if (dir.exists()) {
                    navigationStack.clear()
                    navigationStack.add(rootDir)
                    if (dir != rootDir) navigationStack.add(dir)
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MidnightIndigo)
                }
            } else if (displayedFiles.isEmpty()) {
                EmptyFolderState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                        if (isSelected) selectedPaths.remove(item.path)
                                        else selectedPaths.add(item.path)
                                    }
                                    item.isDirectory -> navigationStack.add(File(item.path))
                                    item.extension == "zip" -> performUnzip(item)
                                    else -> { /* open with system */ }
                                }
                            },
                            onLongClick = {
                                actionTarget = item
                                showActionSheet = true
                            }
                        )
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
            onDismiss = { showSortSheet = false }
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
                    scope.launch { snackbarHostState.showSnackbar("Share: use system intent in host Activity") }
                }
            )
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; selectedPaths.clear() },
            title = { Text("Delete files?") },
            text = {
                Text(
                    if (selectedPaths.size == 1) "Delete \"${File(selectedPaths.first()).name}\"? This cannot be undone."
                    else "Delete ${selectedPaths.size} items? This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        performDelete(selectedPaths.toList())
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; selectedPaths.clear() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Rename dialog ─────────────────────────────────────────────
    if (showRenameDialog) {
        renameTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename") },
                text = {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        label = { Text("New name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameValue.isNotBlank()) {
                                performRename(target, renameValue.trim())
                            }
                            showRenameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo)
                    ) { Text("Rename", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Breadcrumb bar
// ─────────────────────────────────────────────────────────

@Composable
private fun BreadcrumbBar(crumbs: List<String>, onCrumbClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, crumb ->
            if (index > 0) {
                Text(
                    text = " › ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            val isLast = index == crumbs.lastIndex
            Text(
                text = crumb,
                fontSize = 12.sp,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isLast) MidnightIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(enabled = !isLast) { onCrumbClick(index) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Storage Summary Card
// ─────────────────────────────────────────────────────────

@Composable
private fun StorageSummaryCard(total: Long, used: Long) {
    val free = total - used
    val fraction = if (total > 0) used.toFloat() / total.toFloat() else 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Storage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${used.toHumanReadable()} / ${total.toHumanReadable()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MidnightIndigo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${free.toHumanReadable()} free",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Quick access chips
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAccessChips(onLocationSelected: (String) -> Unit) {
    val locations = remember { quickLocations() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        locations.forEach { loc ->
            FilterChip(
                selected = false,
                onClick = { onLocationSelected(loc.path) },
                label = { Text(loc.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedContainerColor = MidnightIndigo,
                    selectedLabelColor = Color.White
                )
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
    onLongClick: () -> Unit
) {
    val bgColor = if (isSelected) MidnightIndigo.copy(alpha = 0.08f)
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = MidnightIndigo),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = fileEmoji(item), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (item.isDirectory) item.lastModified.toDateString()
                else "${item.size.toHumanReadable()}  ·  ${item.lastModified.toDateString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Empty state
// ─────────────────────────────────────────────────────────

@Composable
private fun EmptyFolderState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📂", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "This folder is empty",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
        }
        IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share", tint = MidnightIndigo)
        }
        TextButton(
            onClick = onZip,
            colors = ButtonDefaults.textButtonColors(contentColor = MidnightIndigo)
        ) {
            Text("🗜️ Zip")
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
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SortOrder.entries.forEach { order ->
                val label = when (order) {
                    SortOrder.NAME -> "Name (A–Z)"
                    SortOrder.SIZE -> "Size (largest first)"
                    SortOrder.DATE -> "Date modified"
                    SortOrder.TYPE -> "File type"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOrderSelected(order) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (order == currentOrder) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MidnightIndigo)
                        )
                    }
                }
                if (order != SortOrder.entries.last()) Divider()
            }
            Spacer(modifier = Modifier.height(24.dp))
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
    onShare: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    data class Action(val emoji: String, val label: String, val action: () -> Unit, val destructive: Boolean = false)

    val actions = buildList {
        add(Action("✏️", "Rename", onRename))
        if (!item.isDirectory) add(Action("📤", "Share", onShare))
        if (!item.isDirectory) add(Action("🗜️", "Zip", onZip))
        if (item.extension == "zip") add(Action("📂", "Unzip here", onUnzip))
        add(Action("🗑️", "Delete", onDelete, destructive = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fileEmoji(item), fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.isDirectory) {
                        Text(
                            item.size.toHumanReadable(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            action.action()
                            onDismiss()
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(action.emoji, fontSize = 20.sp, modifier = Modifier.width(36.dp))
                    Text(
                        action.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (action.destructive) Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (action != actions.last()) Divider(modifier = Modifier.padding(start = 36.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
