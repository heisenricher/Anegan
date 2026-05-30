/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.documentreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.BookmarkEntity
import com.anegan.core.database.ReadingProgressEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.Delete
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class SearchMatch(
    val pageIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val snippet: String,
    val matchedText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val prefs = remember { context.getSharedPreferences("anegan_doc_reader", Context.MODE_PRIVATE) }
    val pathHash = filePath.hashCode().toString()

    // Persistent reader settings
    var isNightMode by remember { mutableStateOf(prefs.getBoolean("pref_pdf_night_$pathHash", false)) }
    var isBookmarked by remember { mutableStateOf(prefs.getBoolean("pref_pdf_starred_$pathHash", false)) }
    var currentPage by remember { mutableStateOf(1) }
    
    // PdfRenderer structures
    var pageCount by remember { mutableStateOf(0) }
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Gesture / zoom states
    var scale by remember { mutableStateOf(1f) }
    var translationX by remember { mutableStateOf(0f) }
    var translationY by remember { mutableStateOf(0f) }

    // Go-to-page dialog
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var goToPageInput by remember { mutableStateOf("") }

    // Search settings
    var showSearchPanel by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf("Contains") } // "Contains", "Whole Word", "Regex"
    var isCaseSensitive by remember { mutableStateOf(false) }
    var isIndexing by remember { mutableStateOf(false) }
    val extractedPages = remember { mutableStateListOf<String>() }

    // Bookmark list & sheet states
    val bookmarks = remember { mutableStateListOf<BookmarkEntity>() }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    // Custom high-performance inverted night mode filter run on GPU
    val nightModeColorFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f, // Red
                    0f, -1f, 0f, 0f, 255f, // Green
                    0f, 0f, -1f, 0f, 255f, // Blue
                    0f, 0f, 0f, 1f, 0f     // Alpha
                )
            )
        )
    }

    // Initialize Native Renderer & Load Position & Extract PDF text for searching
    LaunchedEffect(filePath) {
        // Load bookmarks
        scope.launch(Dispatchers.IO) {
            try {
                val db = DatabaseProvider.getDatabase(context)
                val list = db.bookmarkDao().getBookmarksForFile(filePath)
                withContext(Dispatchers.Main) {
                    bookmarks.clear()
                    bookmarks.addAll(list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load PDF using native renderer
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    fileDescriptor = pfd
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    
                    // Load last read position
                    val lastSavedPage = prefs.getInt("pref_pdf_page_$pathHash", 0)
                    if (lastSavedPage in 0 until pageCount) {
                        withContext(Dispatchers.Main) {
                            listState.scrollToItem(lastSavedPage)
                            currentPage = lastSavedPage + 1
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        }

        // Index text for searching
        scope.launch(Dispatchers.IO) {
            try {
                isIndexing = true
                val file = File(filePath)
                if (file.exists()) {
                    PDDocument.load(file).use { document ->
                        val stripper = PDFTextStripper()
                        for (i in 0 until document.numberOfPages) {
                            stripper.startPage = i + 1
                            stripper.endPage = i + 1
                            val text = stripper.getText(document) ?: ""
                            extractedPages.add(text)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isIndexing = false
            }
        }
    }

    // Cleanup Native descriptors on close
    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Auto-save position when page changes
    LaunchedEffect(listState.firstVisibleItemIndex, pageCount) {
        val index = listState.firstVisibleItemIndex
        if (index >= 0 && index < pageCount) {
            currentPage = index + 1
            prefs.edit().putInt("pref_pdf_page_$pathHash", index).apply()

            // Save to ReadingProgressEntity in database
            scope.launch(Dispatchers.IO) {
                try {
                    val db = DatabaseProvider.getDatabase(context)
                    db.readingProgressDao().upsert(
                        ReadingProgressEntity(
                            filePath = filePath,
                            fileName = File(filePath).name,
                            fileType = "pdf",
                            currentPage = currentPage,
                            totalPages = pageCount,
                            lastReadAt = System.currentTimeMillis(),
                            percentComplete = if (pageCount > 0) currentPage.toFloat() / pageCount.toFloat() else 0f
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Search matches logic
    val searchMatches = remember(searchQuery, searchMode, isCaseSensitive, extractedPages.size) {
        if (searchQuery.isBlank() || extractedPages.isEmpty()) {
            emptyList<SearchMatch>()
        } else {
            val matches = mutableListOf<SearchMatch>()
            val flags = if (isCaseSensitive) 0 else Pattern.CASE_INSENSITIVE
            extractedPages.forEachIndexed { pageIndex, pageText ->
                try {
                    val pattern = when (searchMode) {
                        "Regex" -> Pattern.compile(searchQuery, flags)
                        "Whole Word" -> Pattern.compile("\\b${Pattern.quote(searchQuery)}\\b", flags)
                        else -> Pattern.compile(Pattern.quote(searchQuery), flags)
                    }
                    val matcher = pattern.matcher(pageText)
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        val snippetStart = maxOf(0, start - 25)
                        val snippetEnd = minOf(pageText.length, end + 25)
                        var snippet = pageText.substring(snippetStart, snippetEnd).replace('\n', ' ')
                        if (snippetStart > 0) snippet = "...$snippet"
                        if (snippetEnd < pageText.length) snippet = "$snippet..."
                        matches.add(
                            SearchMatch(
                                pageIndex = pageIndex,
                                startChar = start,
                                endChar = end,
                                snippet = snippet,
                                matchedText = matcher.group()
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore regex syntax errors
                }
            }
            matches
        }
    }

    var currentMatchIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(searchMatches) {
        currentMatchIndex = if (searchMatches.isNotEmpty()) 0 else -1
    }

    LaunchedEffect(currentMatchIndex) {
        if (currentMatchIndex in searchMatches.indices) {
            val match = searchMatches[currentMatchIndex]
            listState.animateScrollToItem(match.pageIndex)
        }
    }

    // Back Action Handler
    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = File(filePath).name,
                    onBack = onBack,
                    neonAccent = NeonCyan,
                    actions = {
                        // Search toggle
                        IconButton(onClick = { showSearchPanel = !showSearchPanel }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = NeonCyan
                            )
                        }
                        // Star/Bookmark toggle
                        IconButton(
                            onClick = {
                                isBookmarked = !isBookmarked
                                prefs.edit().putBoolean("pref_pdf_starred_$pathHash", isBookmarked).apply()
                                Toast.makeText(context, if (isBookmarked) "Starred document" else "Unstarred", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "Star",
                                tint = NeonCyan
                            )
                        }
                        // Page Bookmark Toggle
                        val isPageBookmarked = remember(currentPage, bookmarks.size) {
                            bookmarks.any { it.pageNumber == currentPage }
                        }
                        IconButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val db = DatabaseProvider.getDatabase(context)
                                        val existing = bookmarks.firstOrNull { it.pageNumber == currentPage }
                                        if (existing != null) {
                                            db.bookmarkDao().delete(existing)
                                            withContext(Dispatchers.Main) {
                                                bookmarks.remove(existing)
                                                Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val newBookmark = BookmarkEntity(
                                                filePath = filePath,
                                                pageNumber = currentPage,
                                                label = "Page $currentPage"
                                            )
                                            db.bookmarkDao().upsert(newBookmark)
                                            val updatedList = db.bookmarkDao().getBookmarksForFile(filePath)
                                            withContext(Dispatchers.Main) {
                                                bookmarks.clear()
                                                bookmarks.addAll(updatedList)
                                                Toast.makeText(context, "Page $currentPage bookmarked", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPageBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Toggle bookmark for page $currentPage",
                                tint = NeonCyan
                            )
                        }
                        // Bookmarks list button
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmarks,
                                contentDescription = "Show all bookmarks",
                                tint = NeonCyan
                            )
                        }
                        // Night Mode Toggle
                        IconButton(
                            onClick = {
                                isNightMode = !isNightMode
                                prefs.edit().putBoolean("pref_pdf_night_$pathHash", isNightMode).apply()
                            }
                        ) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Night Mode",
                                tint = NeonCyan
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    if (showSearchPanel) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xs),
                            neonAccent = NeonCyan
                        ) {
                            Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        NovaTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = "Search text...",
                                            neonColor = NeonCyan
                                        )
                                    }
                                    IconButton(
                                        enabled = searchMatches.isNotEmpty(),
                                        onClick = {
                                            if (searchMatches.isNotEmpty()) {
                                                currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                            }
                                        }
                                    ) {
                                        Text("▲", fontSize = 14.sp, color = if (searchMatches.isNotEmpty()) NeonCyan else Color.Gray)
                                    }
                                    IconButton(
                                        enabled = searchMatches.isNotEmpty(),
                                        onClick = {
                                            if (searchMatches.isNotEmpty()) {
                                                currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                                            }
                                        }
                                    ) {
                                        Text("▼", fontSize = 14.sp, color = if (searchMatches.isNotEmpty()) NeonCyan else Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xxs)) {
                                        listOf("Contains", "Whole Word", "Regex").forEach { mode ->
                                            val isSel = searchMode == mode
                                            NovaChip(
                                                text = mode,
                                                selected = isSel,
                                                onClick = { searchMode = mode },
                                                neonColor = NeonCyan
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isCaseSensitive,
                                            onCheckedChange = { isCaseSensitive = it },
                                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                        )
                                        Text(
                                            text = "Match Case",
                                            style = NovaTypography.bodySmall,
                                            color = NeonCyan
                                        )
                                    }
                                }
                                if (searchMatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                    Text(
                                        text = "Match ${currentMatchIndex + 1} of ${searchMatches.size} on page ${searchMatches[currentMatchIndex].pageIndex + 1}",
                                        style = NovaTypography.tagMono,
                                        color = NeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = searchMatches[currentMatchIndex].snippet,
                                        style = NovaTypography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else if (searchQuery.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                    Text("No matches found", style = NovaTypography.tagMono, color = NovaError)
                                }
                            }
                        }
                    }
                    
                    // Floating control card
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(NovaTokens.Spacing.md),
                        neonAccent = NeonCyan
                    ) {
                        Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page $currentPage of $pageCount",
                                    style = NovaTypography.dataSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan,
                                    modifier = Modifier.clickable {
                                        goToPageInput = currentPage.toString()
                                        showGoToPageDialog = true
                                    }
                                )
                                if (isIndexing) {
                                    Text("Indexing text...", style = NovaTypography.tagMono, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                                // Reset zoom button
                                if (scale != 1f) {
                                    NovaChip(
                                        text = "Reset Zoom",
                                        selected = true,
                                        onClick = {
                                            scale = 1f
                                            translationX = 0f
                                            translationY = 0f
                                        },
                                        neonColor = NeonCyan
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                            // Slider to quickly jump pages
                            NovaSlider(
                                value = currentPage.toFloat(),
                                onValueChange = { pageVal ->
                                    currentPage = pageVal.toInt()
                                    scope.launch {
                                        listState.scrollToItem(currentPage - 1)
                                    }
                                },
                                valueRange = 1f..maxOf(1f, pageCount.toFloat()),
                                neonColor = NeonCyan
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (isNightMode) Color(0xFF0D1117) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                translationX += pan.x * scale
                                translationY += pan.y * scale
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = translationX,
                            translationY = translationY
                        )
                ) {
                    if (pageCount > 0) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(pageCount) { index ->
                                var bitmap by remember { mutableStateOf<Bitmap?>(pageBitmaps[index]) }
                                
                                // Async render pages dynamically to preserve memory
                                LaunchedEffect(index) {
                                    if (bitmap == null) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                pdfRenderer?.let { renderer ->
                                                    val page = renderer.openPage(index)
                                                    // Render at crisp 150DPI
                                                    val width = (page.width * 1.5).toInt()
                                                    val height = (page.height * 1.5).toInt()
                                                    val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                                    page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                    page.close()
                                                    
                                                    pageBitmaps[index] = pageBitmap
                                                    bitmap = pageBitmap
                                                }
                                            } catch (e: Exception) {
                                                // Ignore
                                            }
                                        }
                                    }
                                }

                                // Display page card with shadows
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.707f),
                                    neonAccent = Color.Transparent
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap!!.asImageBitmap(),
                                                contentDescription = "Page ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                colorFilter = if (isNightMode) nightModeColorFilter else null
                                            )
                                        } else {
                                            CircularProgressIndicator(color = NeonCyan)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }

                // Floating Zoom controls
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = NovaTokens.Spacing.md, bottom = 120.dp),
                    neonAccent = NeonCyan
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }) {
                            Text("+", style = NovaTypography.headlineLarge, color = NeonCyan)
                        }
                        IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(1f) }) {
                            Text("-", style = NovaTypography.headlineLarge, color = NeonCyan)
                        }
                        IconButton(onClick = { 
                            scale = 1f
                            translationX = 0f
                            translationY = 0f
                        }) {
                            Text("↺", style = NovaTypography.headlineMedium, color = NeonCyan)
                        }
                    }
                }
            }
        }
    }

    // Go-to-page Dialog
    if (showGoToPageDialog) {
        AlertDialog(
            onDismissRequest = { showGoToPageDialog = false },
            title = { Text("Go to Page", style = MaterialTheme.typography.titleLarge, color = NeonCyan) },
            text = {
                Column {
                    Text("Enter page number (1 to $pageCount):", style = NovaTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                    NovaTextField(
                        value = goToPageInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                goToPageInput = input
                            }
                        },
                        placeholder = "Page number...",
                        neonColor = NeonCyan,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Go
                        )
                    )
                }
            },
            confirmButton = {
                NovaPrimaryButton(
                    text = "Go",
                    neonColor = NeonCyan,
                    onClick = {
                        val p = goToPageInput.toIntOrNull()
                        if (p != null && p in 1..pageCount) {
                            scope.launch {
                                listState.scrollToItem(p - 1)
                            }
                            showGoToPageDialog = false
                        } else {
                            Toast.makeText(context, "Invalid page number", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showGoToPageDialog = false }) {
                    Text("Cancel", color = NeonCyan, style = NovaTypography.labelLarge)
                }
            }
        )
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovaTokens.Spacing.xl, vertical = NovaTokens.Spacing.xs)
            ) {
                Text(
                    text = "Bookmarks",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = NovaTokens.Spacing.md)
                )

                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No bookmarks added yet for this file",
                            style = NovaTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs)
                    ) {
                        items(bookmarks.size) { idx ->
                            val bm = bookmarks[idx]
                            NovaAnimatedItem(index = idx) {
                                GlassCard(
                                    neonAccent = NeonCyan,
                                    onClick = {
                                        scope.launch {
                                            listState.scrollToItem(bm.pageNumber - 1)
                                        }
                                        showBookmarksSheet = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(NovaTokens.Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Bookmark,
                                                tint = NeonCyan,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(NovaTokens.Spacing.md))
                                            Text(
                                                text = bm.label,
                                                style = NovaTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        val db = DatabaseProvider.getDatabase(context)
                                                        db.bookmarkDao().delete(bm)
                                                        withContext(Dispatchers.Main) {
                                                            bookmarks.remove(bm)
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                tint = NovaError,
                                                contentDescription = "Delete Bookmark"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
