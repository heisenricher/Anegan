/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.PdfPageManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ReaderMode {
    READ,
    ORGANIZE
}

data class DrawPoint(val position: Offset, val color: Color, val brushSize: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderEditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var readerMode by remember { mutableStateOf(ReaderMode.READ) }
    var isInDarkReadingMode by remember { mutableStateOf(isDark) }

    // Page indexes and rendered bitmap cache
    val pageIndices = remember { mutableStateListOf<Int>() }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap>() }

    // Annotation Dialog target
    var annotatePageIndex by remember { mutableStateOf<Int?>(null) }
    var showAnnotationDialog by remember { mutableStateOf(false) }

    val primaryAccent = NeonCyan // Cyan theme for Documents

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex != -1) selectedFileName = it.getString(nameIndex)
                    if (sizeIndex != -1) selectedFileSize = it.getLong(sizeIndex)
                }
            }
            pageIndices.clear()
            pageThumbnails.clear()
            readerMode = ReaderMode.READ
            
            isProcessing = true
            coroutineScope.launch {
                try {
                    val file = StorageManager.copyUriToTempFile(context, uri)
                    if (file != null) {
                        tempPdfFile = file
                        val countResult = PdfPageManager().getPageCount(file)
                        if (countResult.isSuccess) {
                            val count = countResult.getOrThrow()
                            for (i in 0 until count) {
                                pageIndices.add(i)
                            }
                            
                            // Load thumbnails dynamically
                            launch(Dispatchers.IO) {
                                val pageManager = PdfPageManager()
                                for (i in 0 until count) {
                                    val bmpResult = pageManager.renderPageThumbnail(file, i, 220)
                                    if (bmpResult.isSuccess) {
                                        pageThumbnails[i] = bmpResult.getOrThrow()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Failed to load PDF pages", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Save changes for Organize mode (Reorder, Delete, Rotate)
    fun saveOrganizedPdf() {
        val file = tempPdfFile ?: return
        if (pageIndices.isEmpty()) {
            Toast.makeText(context, "Document cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessing = true
        coroutineScope.launch {
            try {
                val result = PdfPageManager().reorderOrDeletePages(file, pageIndices.toList())
                isProcessing = false
                if (result.isSuccess) {
                    val savedFile = result.getOrThrow()
                    NovaHaptics.success(view)
                    Toast.makeText(context, "PDF saved to Anegan/Documents successfully!", Toast.LENGTH_LONG).show()
                    
                    // Log to DB
                    val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                    historyDao.insertConversion(
                        ConversionHistoryEntity(
                            originalFileName = selectedFileName ?: file.name,
                            outputFileName = savedFile.name,
                            originalFormat = "PDF",
                            outputFormat = "PDF",
                            status = "SUCCESS",
                            timestamp = System.currentTimeMillis(),
                            originalSize = selectedFileSize ?: file.length(),
                            outputSize = savedFile.length(),
                            outputPath = savedFile.absolutePath
                        )
                    )
                    selectedUri = null
                    selectedFileName = null
                    selectedFileSize = null
                    pageIndices.clear()
                    pageThumbnails.clear()
                } else {
                    val ex = result.exceptionOrNull()
                    Toast.makeText(context, "Save failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isProcessing = false
                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    BackHandler {
        if (selectedFileName != null) {
            selectedFileName = null
            selectedUri = null
            pageIndices.clear()
            pageThumbnails.clear()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Document Reader",
                onBack = {
                    if (selectedFileName != null) {
                        selectedFileName = null
                        selectedUri = null
                        pageIndices.clear()
                        pageThumbnails.clear()
                    } else {
                        onBack()
                    }
                },
                neonAccent = primaryAccent,
                actions = {
                    if (selectedFileName != null) {
                        IconButton(
                            onClick = { 
                                NovaHaptics.click(view)
                                isInDarkReadingMode = !isInDarkReadingMode 
                            }
                        ) {
                            Text(
                                text = if (isInDarkReadingMode) "☀️" else "🌙",
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NovaBackground {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedFileName == null) {
                    // File Selector view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            neonAccent = primaryAccent,
                            enableGlow = true,
                            onClick = { 
                                NovaHaptics.click(view)
                                pdfPickerLauncher.launch("application/pdf") 
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(NovaTokens.Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📕", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))
                                Text(
                                    text = "Mount PDF Document", 
                                    style = NovaTypography.headlineLarge.copy(color = primaryAccent, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                                Text(
                                    text = "Read, annotate, sign, and organize pages offline", 
                                    style = NovaTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Futuristic Mode Select Segment
                    NovaSegmentedControl(
                        items = listOf("Read Document", "Organize Pages"),
                        selectedIndex = if (readerMode == ReaderMode.READ) 0 else 1,
                        onIndexSelected = { 
                            readerMode = if (it == 0) ReaderMode.READ else ReaderMode.ORGANIZE 
                        },
                        neonColor = primaryAccent,
                        modifier = Modifier.padding(vertical = NovaTokens.Spacing.xxs)
                    )

                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))

                    if (isProcessing) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = primaryAccent)
                        }
                    } else {
                        when (readerMode) {
                            ReaderMode.READ -> {
                                Box(modifier = Modifier.weight(1f)) {
                                    PdfScrollReader(
                                        file = tempPdfFile!!,
                                        pageIndices = pageIndices,
                                        pageThumbnails = pageThumbnails,
                                        isDarkReading = isInDarkReadingMode,
                                        onAnnotate = { index ->
                                            NovaHaptics.recording(view)
                                            annotatePageIndex = index
                                            showAnnotationDialog = true
                                        },
                                        neonColor = primaryAccent
                                    )
                                }
                            }
                            ReaderMode.ORGANIZE -> {
                                Column(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PdfPagesGrid(
                                            pageIndices = pageIndices,
                                            pageThumbnails = pageThumbnails,
                                            isDarkReading = isInDarkReadingMode,
                                            onRotate = { idx ->
                                                NovaHaptics.click(view)
                                                isProcessing = true
                                                coroutineScope.launch {
                                                    val res = PdfPageManager().rotatePage(tempPdfFile!!, idx, 90)
                                                    isProcessing = false
                                                    if (res.isSuccess) {
                                                        tempPdfFile = res.getOrThrow()
                                                        // Reload thumbnail
                                                        val reBmp = PdfPageManager().renderPageThumbnail(tempPdfFile!!, idx, 220)
                                                        if (reBmp.isSuccess) {
                                                            pageThumbnails[idx] = reBmp.getOrThrow()
                                                        }
                                                        Toast.makeText(context, "Page rotated", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onDelete = { idx ->
                                                NovaHaptics.warning(view)
                                                pageIndices.removeAt(idx)
                                            },
                                            neonColor = primaryAccent
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))
                                    
                                    NovaPrimaryButton(
                                        text = "Save Reorganized Document",
                                        neonColor = primaryAccent,
                                        onClick = { saveOrganizedPdf() },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Annotations Drawing & Sign Dialog
    if (showAnnotationDialog && annotatePageIndex != null && tempPdfFile != null) {
        PdfDrawAnnotationDialog(
            file = tempPdfFile!!,
            pageIndex = annotatePageIndex!!,
            onDismiss = { showAnnotationDialog = false; annotatePageIndex = null },
            onSave = { annotatedBitmap ->
                showAnnotationDialog = false
                isProcessing = true
                coroutineScope.launch {
                    val res = PdfPageManager().savePageAnnotation(tempPdfFile!!, annotatePageIndex!!, annotatedBitmap)
                    isProcessing = false
                    annotatePageIndex = null
                    if (res.isSuccess) {
                        tempPdfFile = res.getOrThrow()
                        NovaHaptics.success(view)
                        Toast.makeText(context, "Page annotated successfully!", Toast.LENGTH_SHORT).show()
                        
                        // Reload thumbnail
                        val reBmp = PdfPageManager().renderPageThumbnail(tempPdfFile!!, annotatePageIndex ?: 0, 220)
                        if (reBmp.isSuccess) {
                            pageThumbnails[annotatePageIndex ?: 0] = reBmp.getOrThrow()
                        }
                    } else {
                        Toast.makeText(context, "Failed to save annotations", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            neonColor = primaryAccent
        )
    }
}

// ─────────────────────────────────────────────────────────
//  Lazy Scroll Reader Component
// ─────────────────────────────────────────────────────────
@Composable
fun PdfScrollReader(
    file: File,
    pageIndices: List<Int>,
    pageThumbnails: Map<Int, Bitmap>,
    isDarkReading: Boolean,
    onAnnotate: (Int) -> Unit,
    neonColor: Color
) {
    val loadedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.md),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        itemsIndexed(pageIndices) { listIdx, originalIndex ->
            // Dynamic high-res rendering on scroll
            LaunchedEffect(originalIndex) {
                if (!loadedBitmaps.containsKey(originalIndex)) {
                    withContext(Dispatchers.IO) {
                        val manager = PdfPageManager()
                        val res = manager.renderPageThumbnail(file, originalIndex, 900) // high-res width
                        if (res.isSuccess) {
                            loadedBitmaps[originalIndex] = res.getOrThrow()
                        }
                    }
                }
            }

            GlassCard(
                neonAccent = if (isDarkReading) neonColor else Color.Transparent,
                enableGlow = isDarkReading
            ) {
                Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAGE ${listIdx + 1}",
                            style = NovaTypography.tagMono.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkReading) neonColor else MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                                .background(if (isDarkReading) neonColor.copy(alpha = 0.15f) else neonColor.copy(alpha = 0.07f))
                                .border(1.dp, neonColor.copy(alpha = 0.25f), RoundedCornerShape(NovaTokens.Radius.sm))
                                .clickable { onAnnotate(originalIndex) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Sign / Draw ✍️",
                                style = NovaTypography.tagMono.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = neonColor
                                )
                            )
                        }
                    }

                    // Pinch-to-zoom container
                    var scale by remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkReading) Color.Black else Color(0xFFF8FAFC))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    offset = if (scale > 1f) offset + pan else Offset.Zero
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = loadedBitmaps[originalIndex] ?: pageThumbnails[originalIndex]
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page Render",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                colorFilter = if (isDarkReading) ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            -1f, 0f, 0f, 0f, 255f,
                                            0f, -1f, 0f, 0f, 255f,
                                            0f, 0f, -1f, 0f, 255f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                ) else null
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = neonColor, strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Pages Organize Grid View
// ─────────────────────────────────────────────────────────
@Composable
fun PdfPagesGrid(
    pageIndices: MutableList<Int>,
    pageThumbnails: Map<Int, Bitmap>,
    isDarkReading: Boolean,
    onRotate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    neonColor: Color
) {
    val isDark = isSystemInDarkTheme()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(pageIndices.toList()) { listIndex, originalIndex ->
            GlassCard(
                neonAccent = if (isDarkReading) neonColor else Color.Transparent,
                enableGlow = false
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(NovaTokens.Spacing.sm),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAGE ${listIndex + 1}",
                            style = NovaTypography.tagMono.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkReading) neonColor else MaterialTheme.colorScheme.primary
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Up Arrow to Reorder
                            Text(
                                text = "▲",
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable(enabled = listIndex > 0) {
                                        val temp = pageIndices[listIndex]
                                        pageIndices[listIndex] = pageIndices[listIndex - 1]
                                        pageIndices[listIndex - 1] = temp
                                    }
                                    .padding(4.dp),
                                color = if (listIndex > 0) neonColor else (if (isDark) NovaBorderDark else NovaBorderLight)
                            )
                            // Down Arrow to Reorder
                            Text(
                                text = "▼",
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable(enabled = listIndex < pageIndices.size - 1) {
                                        val temp = pageIndices[listIndex]
                                        pageIndices[listIndex] = pageIndices[listIndex + 1]
                                        pageIndices[listIndex + 1] = temp
                                    }
                                    .padding(4.dp),
                                color = if (listIndex < pageIndices.size - 1) neonColor else (if (isDark) NovaBorderDark else NovaBorderLight)
                            )
                        }
                    }

                    // Page Thumbnail Image
                    Box(
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                            .background(if (isDark) Color.Black.copy(alpha = 0.3f) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = pageThumbnails[originalIndex]
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                colorFilter = if (isDarkReading) ColorFilter.colorMatrix(
                                    ColorMatrix(
                                        floatArrayOf(
                                            -1f, 0f, 0f, 0f, 255f,
                                            0f, -1f, 0f, 0f, 255f,
                                            0f, 0f, -1f, 0f, 255f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                ) else null
                            )
                        } else {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = neonColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                                .background(if (isDarkReading) neonColor.copy(alpha = 0.15f) else neonColor.copy(alpha = 0.08f))
                                .border(1.dp, neonColor.copy(alpha = 0.3f), RoundedCornerShape(NovaTokens.Radius.sm))
                                .clickable { onRotate(originalIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rotate 🔄", 
                                style = NovaTypography.tagMono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = neonColor)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                                .background(NovaError.copy(alpha = 0.08f))
                                .border(1.dp, NovaError.copy(alpha = 0.3f), RoundedCornerShape(NovaTokens.Radius.sm))
                                .clickable { onDelete(listIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete 🗑️", 
                                style = NovaTypography.tagMono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NovaError)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  Fullscreen Canvas Drawing & Annotation Dialog
// ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDrawAnnotationDialog(
    file: File,
    pageIndex: Int,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit,
    neonColor: Color
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Touch drawing points
    val drawPoints = remember { mutableStateListOf<DrawPoint>() }
    var brushColor by remember { mutableStateOf(neonColor) }
    var brushSize by remember { mutableStateOf(6f) }

    // Text annotation state
    var textAnnotation by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var textPosition by remember { mutableStateOf(Offset(100f, 100f)) }

    // Load higher resolution page
    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            val manager = PdfPageManager()
            val res = manager.renderPageThumbnail(file, pageIndex, 1080) // 1080px high-res width
            if (res.isSuccess) {
                pageBitmap = res.getOrThrow()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = "SIGN & ANNOTATE PAGE",
                    onBack = onDismiss,
                    neonAccent = neonColor,
                    actions = {
                        TextButton(
                            onClick = {
                                val baseBmp = pageBitmap ?: return@TextButton
                                // Flatten drawing paths + text directly onto the annotation layer bitmap
                                val annotationBmp = Bitmap.createBitmap(baseBmp.width, baseBmp.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(annotationBmp)
                                
                                val paint = Paint().apply {
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                }
                                
                                drawPoints.forEach { pt ->
                                    paint.color = android.graphics.Color.argb(
                                        (pt.color.alpha * 255).toInt(),
                                        (pt.color.red * 255).toInt(),
                                        (pt.color.green * 255).toInt(),
                                        (pt.color.blue * 255).toInt()
                                    )
                                    paint.strokeWidth = pt.brushSize * (baseBmp.width.toFloat() / 1080f) // scale stroke relative to base width
                                    canvas.drawPoint(
                                        pt.position.x * (baseBmp.width.toFloat() / 1080f),
                                        pt.position.y * (baseBmp.height.toFloat() / (1080f * (baseBmp.height.toFloat() / baseBmp.width.toFloat()))),
                                        paint
                                    )
                                }
                                
                                // Draw Text Overlay if any
                                if (textAnnotation.isNotBlank()) {
                                    val textPaint = Paint().apply {
                                        color = android.graphics.Color.argb(255, (neonColor.red * 255).toInt(), (neonColor.green * 255).toInt(), (neonColor.blue * 255).toInt())
                                        textSize = 42f * (baseBmp.width.toFloat() / 1080f)
                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        isAntiAlias = true
                                    }
                                    canvas.drawText(
                                        textAnnotation,
                                        textPosition.x * (baseBmp.width.toFloat() / 1080f),
                                        textPosition.y * (baseBmp.height.toFloat() / (1080f * (baseBmp.height.toFloat() / baseBmp.width.toFloat()))),
                                        textPaint
                                    )
                                }
                                
                                onSave(annotationBmp)
                            },
                            enabled = pageBitmap != null
                        ) {
                            Text(
                                text = "SAVE", 
                                style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold, color = neonColor)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                // Futuristic bottom tool selector drawer
                GlassCard(
                    neonAccent = neonColor,
                    enableGlow = false,
                    cornerRadius = 0.dp
                ) {
                    Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colors
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val colors = listOf(neonColor, NovaError, if (isDark) NovaFrostWhite else NovaDeepInk)
                                colors.forEach { col ->
                                    val isSelected = brushColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) (if (isDark) Color.White else Color.Black) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { 
                                                NovaHaptics.click(view)
                                                brushColor = col 
                                            }
                                    )
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Add Text annotation action
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                                        .background(neonColor.copy(alpha = 0.08f))
                                        .border(1.dp, neonColor.copy(alpha = 0.3f), RoundedCornerShape(NovaTokens.Radius.sm))
                                        .clickable { 
                                            NovaHaptics.click(view)
                                            showTextInput = true 
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "✍️ Write Text", 
                                        style = NovaTypography.tagMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = neonColor)
                                    )
                                }

                                // Clear Canvas
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(NovaTokens.Radius.sm))
                                        .background(NovaError.copy(alpha = 0.08f))
                                        .border(1.dp, NovaError.copy(alpha = 0.3f), RoundedCornerShape(NovaTokens.Radius.sm))
                                        .clickable { 
                                            NovaHaptics.warning(view)
                                            drawPoints.clear()
                                            textAnnotation = "" 
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Clear All", 
                                        style = NovaTypography.tagMono.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NovaError)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.sm))

                        // Brush Size Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Brush Vector Size: ${brushSize.toInt()}", 
                                style = NovaTypography.tagMono.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            NovaSlider(
                                value = brushSize,
                                onValueChange = { brushSize = it },
                                valueRange = 2f..24f,
                                neonColor = neonColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                val base = pageBitmap
                if (base != null) {
                    val aspectRatio = base.height.toFloat() / base.width.toFloat()
                    val drawWidth = 340.dp // matches standard dialog width
                    val drawHeight = (340.dp * aspectRatio)

                    Box(
                        modifier = Modifier
                            .width(drawWidth)
                            .height(drawHeight)
                            .clip(RoundedCornerShape(NovaTokens.Radius.md))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val scaleX = 1080f / size.width.toFloat()
                                    val scaleY = (1080f * aspectRatio) / size.height.toFloat()
                                    
                                    val localPoint = change.position
                                    drawPoints.add(
                                        DrawPoint(
                                            position = Offset(localPoint.x * scaleX, localPoint.y * scaleY),
                                            color = brushColor,
                                            brushSize = brushSize
                                        )
                                    )
                                }
                            }
                            .border(1.dp, neonColor.copy(alpha = 0.3f), RoundedCornerShape(NovaTokens.Radius.md))
                    ) {
                        // Render underlying PDF Page
                        Image(
                            bitmap = base.asImageBitmap(),
                            contentDescription = "Sign Sheet",
                            modifier = Modifier.fillMaxSize()
                        )

                        // Render Annotation Drawing Paths
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawPoints.forEach { pt ->
                                val x = pt.position.x * (size.width / 1080f)
                                val y = pt.position.y * (size.height / (1080f * aspectRatio))
                                drawCircle(
                                    color = pt.color,
                                    radius = pt.brushSize / 2f,
                                    center = Offset(x, y)
                                )
                            }

                            // Render text overlay pointer if active
                            if (textAnnotation.isNotBlank()) {
                                val tx = textPosition.x * (size.width / 1080f)
                                val ty = textPosition.y * (size.height / (1080f * aspectRatio))
                                drawCircle(color = neonColor, radius = 4f, center = Offset(tx, ty))
                            }
                        }

                        // Text position draggable box if active
                        if (textAnnotation.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (textPosition.x * (340f / 1080f)).dp,
                                        y = (textPosition.y * ((340f * aspectRatio) / (1080f * aspectRatio))).dp
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val scaleX = 1080f / 340f
                                            textPosition = Offset(
                                                textPosition.x + dragAmount.x * scaleX,
                                                textPosition.y + dragAmount.y * scaleX
                                            )
                                        }
                                    }
                                    .background(neonColor.copy(alpha = 0.15f))
                                    .border(1.dp, neonColor, RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = textAnnotation, 
                                    color = neonColor, 
                                    style = NovaTypography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(color = neonColor)
                }
            }
        }
    }

    // Text Annotation Input Dialog
    if (showTextInput) {
        AlertDialog(
            onDismissRequest = { showTextInput = false },
            title = { 
                Text(
                    text = "Add Signature Text", 
                    style = NovaTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = neonColor)
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Type text below, then drag it onto the desired coordinate position on the page canvas.",
                        style = NovaTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NovaTextField(
                        value = textAnnotation,
                        onValueChange = { textAnnotation = it },
                        placeholder = "Type signature text...",
                        singleLine = true,
                        neonColor = neonColor
                    )
                }
            },
            confirmButton = {
                NovaPrimaryButton(
                    text = "Apply Text",
                    neonColor = neonColor,
                    onClick = { showTextInput = false }
                )
            },
            containerColor = if (isDark) NovaMidnightBlue else Color.White,
            shape = RoundedCornerShape(NovaTokens.Radius.lg),
            modifier = Modifier.border(1.dp, neonColor.copy(alpha = 0.2f), RoundedCornerShape(NovaTokens.Radius.lg))
        )
    }
}
