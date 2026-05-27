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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.PdfPageManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ReaderMode {
    READ,
    ORGANIZE,
    ANNOTATE
}

data class DrawPoint(val position: Offset, val color: Color, val brushSize: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderEditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    var tempPdfFile by remember { mutableStateOf<File?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var readerMode by remember { mutableStateOf(ReaderMode.READ) }
    var isInDarkReadingMode by remember { mutableStateOf(false) }

    // Page indexes and rendered bitmap cache
    val pageIndices = remember { mutableStateListOf<Int>() }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap>() }

    // Annotation Dialog target
    var annotatePageIndex by remember { mutableStateOf<Int?>(null) }
    var showAnnotationDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isInDarkReadingMode) Color(0xFF121212) else MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Sleek Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isInDarkReadingMode) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface)
                        .border(
                            width = 0.5.dp,
                            color = if (isInDarkReadingMode) Color(0xFF2E2E2E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Text("←", fontSize = 20.sp, color = if (isInDarkReadingMode) Color.White else MidnightIndigo, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "PDF Reader & Editor",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp),
                    color = if (isInDarkReadingMode) Color.White else MidnightIndigo
                )
            }

            if (selectedFileName != null) {
                // Invert Reading Dark Mode Toggle
                IconButton(
                    onClick = { isInDarkReadingMode = !isInDarkReadingMode },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isInDarkReadingMode) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                ) {
                    Text(
                        text = if (isInDarkReadingMode) "☀️" else "🌙",
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedFileName == null) {
            // File Selector view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pdfPickerLauncher.launch("application/pdf") },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📕", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select PDF Document", color = MidnightIndigo, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Read, annotate, sign, and edit pages offline.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Mode Select Segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isInDarkReadingMode) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface)
                    .border(
                        width = 0.5.dp,
                        color = if (isInDarkReadingMode) Color(0xFF2E2E2E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                val modes = listOf(
                    ReaderMode.READ to "Read",
                    ReaderMode.ORGANIZE to "Organize",
                )
                modes.forEach { (mode, label) ->
                    val isSelected = readerMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MidnightIndigo else Color.Transparent)
                            .clickable { readerMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else (if (isInDarkReadingMode) Color.Gray else MidnightIndigo)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MidnightIndigo)
                }
            } else {
                when (readerMode) {
                    ReaderMode.READ -> {
                        // Scrolling Render View
                        Box(modifier = Modifier.weight(1f)) {
                            PdfScrollReader(
                                file = tempPdfFile!!,
                                pageIndices = pageIndices,
                                pageThumbnails = pageThumbnails,
                                isDarkReading = isInDarkReadingMode,
                                onAnnotate = { index ->
                                    annotatePageIndex = index
                                    showAnnotationDialog = true
                                }
                            )
                        }
                    }
                    ReaderMode.ORGANIZE -> {
                        // Arrange pages grid
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.weight(1f)) {
                                PdfPagesGrid(
                                    pageIndices = pageIndices,
                                    pageThumbnails = pageThumbnails,
                                    isDarkReading = isInDarkReadingMode,
                                    onRotate = { idx ->
                                        // Rotate 90 deg locally in background
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
                                        pageIndices.removeAt(idx)
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { saveOrganizedPdf() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = Color.White)
                            ) {
                                Text("Save Reorganized PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    // Annotations Finger Drawing & Sign Dialog
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
            }
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
    onAnnotate: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val loadedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkReading) Color(0xFF1E1E1E) else Color.White),
                border = BorderStroke(
                    0.5.dp,
                    if (isDarkReading) Color(0xFF2E2E2E) else Color.LightGray.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${listIdx + 1}",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkReading) Color.White else MidnightIndigo,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Annotate / Sign ✍️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MidnightIndigo,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDarkReading) Color(0xFF2A2A2E) else Color(0xFFF2F2F7))
                                .clickable { onAnnotate(originalIndex) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
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
                                CircularProgressIndicator(color = MidnightIndigo, strokeWidth = 2.dp)
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
    onDelete: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(pageIndices.toList()) { listIndex, originalIndex ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkReading) Color(0xFF1E1E1E) else Color.White),
                border = BorderStroke(
                    0.5.dp,
                    if (isDarkReading) Color(0xFF2E2E2E) else Color.LightGray.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Page ${listIndex + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkReading) Color.White else MidnightIndigo
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Up Arrow to Reorder
                            Text(
                                text = "▲",
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable(enabled = listIndex > 0) {
                                        val temp = pageIndices[listIndex]
                                        pageIndices[listIndex] = pageIndices[listIndex - 1]
                                        pageIndices[listIndex - 1] = temp
                                    }
                                    .padding(4.dp),
                                color = if (listIndex > 0) MidnightIndigo else Color.LightGray
                            )
                            // Down Arrow to Reorder
                            Text(
                                text = "▼",
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .clickable(enabled = listIndex < pageIndices.size - 1) {
                                        val temp = pageIndices[listIndex]
                                        pageIndices[listIndex] = pageIndices[listIndex + 1]
                                        pageIndices[listIndex + 1] = temp
                                    }
                                    .padding(4.dp),
                                color = if (listIndex < pageIndices.size - 1) MidnightIndigo else Color.LightGray
                            )
                        }
                    }

                    // Page Thumbnail Image
                    Box(
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = pageThumbnails[originalIndex]
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page Thumb",
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
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onRotate(originalIndex) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkReading) Color(0xFF2C2C2E) else Color(0xFFF1F5F9),
                                contentColor = MidnightIndigo
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text("Rotate 🔄", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onDelete(listIndex) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEAEA),
                                contentColor = Color.Red
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text("Delete 🗑️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Helper graphics layer modifier for Compose transformation API support
private fun Modifier.graphicsLayer(
    scaleX: Float,
    scaleY: Float,
    translationX: Float,
    translationY: Float
): Modifier = this.then(
    Modifier.pointerInput(Unit) {}
        .graphicsLayer(
            scaleX = scaleX,
            scaleY = scaleY,
            translationX = translationX,
            translationY = translationY
        )
)

// ─────────────────────────────────────────────────────────
//  Fullscreen Canvas Drawing & Annotation Dialog
// ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDrawAnnotationDialog(
    file: File,
    pageIndex: Int,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scaleFactor by remember { mutableStateOf(1f) }

    // Touch drawing points
    val drawPoints = remember { mutableStateListOf<DrawPoint>() }
    var brushColor by remember { mutableStateOf(Color.Blue) }
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
                TopAppBar(
                    title = { Text("Sign / Draw Page", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Red) }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val baseBmp = pageBitmap ?: return@TextButton
                                // Flatten drawing paths + text directly onto the annotation layer bitmap
                                // Create transparent overlay of the exact page size
                                val annotationBmp = Bitmap.createBitmap(baseBmp.width, baseBmp.height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(annotationBmp)
                                
                                // Draw user paths
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
                                        color = android.graphics.Color.BLUE
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
                            Text("Save", fontWeight = FontWeight.Bold, color = MidnightIndigo)
                        }
                    }
                )
            },
            bottomBar = {
                // Drawing options tool drawer (Apple-style)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colors
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val colors = listOf(Color.Blue, Color.Black, Color.Red)
                                colors.forEach { col ->
                                    val isSelected = brushColor == col
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) MidnightIndigo else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { brushColor = col }
                                    )
                                }
                            }
                            
                            // Add Text annotation action
                            Button(
                                onClick = { showTextInput = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F7), contentColor = MidnightIndigo)
                            ) {
                                Text("✍️ Add Text", fontSize = 12.sp)
                            }

                            // Clear Canvas
                            Button(
                                onClick = { drawPoints.clear(); textAnnotation = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEAEA), contentColor = Color.Red)
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Brush Size Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Brush Size: ${brushSize.toInt()}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Slider(
                                value = brushSize,
                                onValueChange = { brushSize = it },
                                valueRange = 2f..24f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo)
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
                    .background(Color(0xFFE2E8F0)),
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Map coordinates relative to the canvas rendering
                                    // Canvas display is scaled to 1080 width base to easily store coordinates
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

                            // Render text overlay if position is active
                            if (textAnnotation.isNotBlank()) {
                                val tx = textPosition.x * (size.width / 1080f)
                                val ty = textPosition.y * (size.height / (1080f * aspectRatio))
                                
                                drawCircle(color = Color.Blue, radius = 4f, center = Offset(tx, ty))
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
                                    .background(Color.Blue.copy(alpha = 0.15f))
                                    .border(0.5.dp, Color.Blue, RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Text(textAnnotation, color = Color.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(color = MidnightIndigo)
                }
            }
        }
    }

    // Text Annotation Input Dialog
    if (showTextInput) {
        AlertDialog(
            onDismissRequest = { showTextInput = false },
            title = { Text("Add Text Overlay") },
            text = {
                OutlinedTextField(
                    value = textAnnotation,
                    onValueChange = { textAnnotation = it },
                    placeholder = { Text("Type text/signature here…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showTextInput = false }) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        )
    }
}
