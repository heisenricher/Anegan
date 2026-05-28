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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import com.anegan.core.designsystem.theme.LuminousGlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    // Initialize Native Renderer & Load Position
    LaunchedEffect(filePath) {
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
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val index = listState.firstVisibleItemIndex
        if (index < pageCount) {
            currentPage = index + 1
            prefs.edit().putInt("pref_pdf_page_$pathHash", index).apply()
        }
    }

    // Back Action Handler
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = File(filePath).name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightIndigo,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Star/Bookmark toggle
                    IconButton(
                        onClick = {
                            isBookmarked = !isBookmarked
                            prefs.edit().putBoolean("pref_pdf_starred_$pathHash", isBookmarked).apply()
                            Toast.makeText(context, if (isBookmarked) "Starred document" else "Unstarred", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(if (isBookmarked) "⭐" else "☆", fontSize = 20.sp)
                    }
                    // Night Mode Toggle
                    IconButton(
                        onClick = {
                            isNightMode = !isNightMode
                            prefs.edit().putBoolean("pref_pdf_night_$pathHash", isNightMode).apply()
                        }
                    ) {
                        Text(if (isNightMode) "☀️" else "🌙", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Floating control card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page $currentPage of $pageCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo
                        )
                        // Reset zoom button
                        if (scale != 1f) {
                            Text(
                                text = "Reset Zoom",
                                fontSize = 11.sp,
                                color = MidnightIndigo,
                                modifier = Modifier
                                    .clickable {
                                        scale = 1f
                                        translationX = 0f
                                        translationY = 0f
                                    }
                                    .background(LuminousGlow, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Slider to quickly jump pages
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { pageVal ->
                            currentPage = pageVal.toInt()
                            scope.launch {
                                listState.scrollToItem(currentPage - 1)
                            }
                        },
                        valueRange = 1f..maxOf(1f, pageCount.toFloat()),
                        colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isNightMode) Color(0xFF121212) else Color(0xFFF1F5F9))
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
                ),
            contentAlignment = Alignment.Center
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
                                            // Render at crisp 150DPI (scaling from screen density)
                                            val width = (page.width * 1.5).toInt()
                                            val height = (page.height * 1.5).toInt()
                                            val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                            page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            page.close()
                                            
                                            pageBitmaps[index] = pageBitmap
                                            bitmap = pageBitmap
                                        }
                                    } catch (e: Exception) {
                                        // Ignore page render errors
                                    }
                                }
                            }
                        }

                        // Display page card with shadows
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.707f), // standard standard A4 aspect ratio
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                    CircularProgressIndicator(color = MidnightIndigo)
                                }
                            }
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = MidnightIndigo)
            }
        }
    }
}
