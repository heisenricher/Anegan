/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.documentreader

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.anegan.core.designsystem.theme.*
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    filePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember { File(filePath) }
    
    var imageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    var scale by remember { mutableStateOf(1f) }
    var translationX by remember { mutableStateOf(0f) }
    var translationY by remember { mutableStateOf(0f) }

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val bitmap = decodeSampledBitmapFromFile(filePath, 2048, 2048)
                    withContext(Dispatchers.Main) {
                        imageBitmap = bitmap
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Image file not found", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onBack()
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = file.name,
                    onBack = onBack,
                    neonAccent = NeonCyan
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF0D1117)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = NeonCyan)
                } else {
                    imageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = file.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
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
                        )
                    }
                    
                    // Floating Zoom Controls
                    GlassCard(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(NovaTokens.Spacing.md),
                        neonAccent = NeonCyan
                    ) {
                        Row(
                            modifier = Modifier.padding(NovaTokens.Spacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scale = (scale + 0.5f).coerceAtMost(5f) }) {
                                Text("+", style = NovaTypography.headlineLarge, color = NeonCyan)
                            }
                            IconButton(onClick = { scale = (scale - 0.5f).coerceAtLeast(1f) }) {
                                Text("-", style = NovaTypography.headlineLarge, color = NeonCyan)
                            }
                            IconButton(onClick = { 
                                scale = 1f
                                translationX = 0f
                                translationY = 0f
                            }) {
                                Text("Reset", style = NovaTypography.tagMono.copy(fontWeight = FontWeight.Bold), color = NeonCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(filePath, options)
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(filePath, options)
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
