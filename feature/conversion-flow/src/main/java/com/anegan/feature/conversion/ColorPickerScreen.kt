/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableStateOf(0) } // 0: Spectra, 1: Image Analyzer

    // Global selected color
    var selectedColor by remember { mutableStateOf(Color(0xFF7C6FFF)) }
    var savedColorsList by remember { mutableStateOf(getSavedColors(context)) }

    fun updateColor(newColor: Color) {
        selectedColor = newColor
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Go back to dashboard" }
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                    color = MidnightIndigo
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Color Picker",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("Spectra (HSV)", "Image Analyzer")
            tabs.forEachIndexed { index, title ->
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MidnightIndigo else Color.Transparent)
                        .clickable { activeTab = index }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) PureWhite else MidnightIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab content
        when (activeTab) {
            0 -> SpectraTabContent(
                currentColor = selectedColor,
                onColorChange = { updateColor(it) }
            )
            1 -> ImageAnalyzerTabContent(
                onColorSelected = { updateColor(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Color Info Card
        ColorInfoCard(context = context, color = selectedColor, onSave = {
            val r = (selectedColor.red * 255).toInt()
            val g = (selectedColor.green * 255).toInt()
            val b = (selectedColor.blue * 255).toInt()
            val hex = String.format("#%02X%02X%02X", r, g, b)
            saveColor(context, hex)
            savedColorsList = getSavedColors(context)
            Toast.makeText(context, "Color saved to palette!", Toast.LENGTH_SHORT).show()
        })

        Spacer(modifier = Modifier.height(24.dp))

        // Saved Palettes
        SavedPalettesSection(
            context = context,
            savedColors = savedColorsList,
            onColorLoad = { hex ->
                try {
                    val colorInt = android.graphics.Color.parseColor(hex)
                    selectedColor = Color(colorInt)
                } catch (e: Exception) {
                    // Ignore malformed hex colors
                }
            },
            onColorDelete = { hex ->
                deleteColor(context, hex)
                savedColorsList = getSavedColors(context)
            }
        )
    }
}

@Composable
fun SpectraTabContent(
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    // Convert current Color to HSV
    val initialHsv = remember(currentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
        hsv
    }

    var hue by remember(initialHsv[0]) { mutableStateOf(initialHsv[0]) }
    var saturation by remember(initialHsv[1]) { mutableStateOf(initialHsv[1]) }
    var value by remember(initialHsv[2]) { mutableStateOf(initialHsv[2]) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Color Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(currentColor)
                .border(2.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .semantics { contentDescription = "Active color preview panel" }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hue Slider
        Text(
            text = "Hue: ${hue.toInt()}°",
            fontWeight = FontWeight.Bold,
            color = MidnightIndigo,
            fontSize = 14.sp
        )
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                onColorChange(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = MidnightIndigo,
                activeTrackColor = MidnightIndigo
            ),
            modifier = Modifier.semantics { contentDescription = "Hue rotation selector, active value ${hue.toInt()} degrees" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Saturation Slider
        Text(
            text = "Saturation: ${(saturation * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            color = MidnightIndigo,
            fontSize = 14.sp
        )
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                onColorChange(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MidnightIndigo,
                activeTrackColor = MidnightIndigo
            ),
            modifier = Modifier.semantics { contentDescription = "Saturation slider, active value ${(saturation * 100).toInt()} percent" }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Value (Brightness) Slider
        Text(
            text = "Value (Brightness): ${(value * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            color = MidnightIndigo,
            fontSize = 14.sp
        )
        Slider(
            value = value,
            onValueChange = {
                value = it
                onColorChange(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MidnightIndigo,
                activeTrackColor = MidnightIndigo
            ),
            modifier = Modifier.semantics { contentDescription = "Brightness value slider, active value ${(value * 100).toInt()} percent" }
        )
    }
}

@Composable
fun ImageAnalyzerTabContent(
    onColorSelected: (Color) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var indicatorOffset by remember { mutableStateOf(Offset(150f, 150f)) }
    var canvasSize by remember { mutableStateOf(Offset(300f, 300f)) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            loadedBitmap = loadDownscaledBitmap(context, uri, 1024)
            // Reset indicator position to center
            indicatorOffset = Offset(150f, 150f)
        }
    }

    fun samplePixelAt(offset: Offset) {
        val bmp = loadedBitmap ?: return
        val xRatio = offset.x / canvasSize.x
        val yRatio = offset.y / canvasSize.y

        val pixelX = (xRatio * bmp.width).toInt().coerceIn(0, bmp.width - 1)
        val pixelY = (yRatio * bmp.height).toInt().coerceIn(0, bmp.height - 1)

        try {
            val pixelColor = bmp.getPixel(pixelX, pixelY)
            onColorSelected(Color(pixelColor))
        } catch (e: Exception) {
            // Ignore sampling errors
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (loadedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .onGloballyPositioned { coordinates ->
                        canvasSize = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                    }
                    .pointerInput(loadedBitmap) {
                        detectTapGestures { offset ->
                            indicatorOffset = offset
                            samplePixelAt(offset)
                        }
                    }
                    .pointerInput(loadedBitmap) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            indicatorOffset = change.position.coerceIn(canvasSize)
                            samplePixelAt(indicatorOffset)
                        }
                    },
                contentAlignment = Alignment.TopStart
            ) {
                Image(
                    bitmap = loadedBitmap!!.asImageBitmap(),
                    contentDescription = "Analyzed target photograph",
                    modifier = Modifier.fillMaxSize()
                )

                // Drag indicator ring
                val density = LocalDensity.current
                val offsetDp = with(density) {
                    IntOffset(
                        (indicatorOffset.x - 15.dp.toPx()).toInt(),
                        (indicatorOffset.y - 15.dp.toPx()).toInt()
                    )
                }

                Box(
                    modifier = Modifier
                        .offset { offsetDp }
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .border(4.dp, MidnightIndigo.copy(alpha = 0.5f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
            ) {
                Text("Select Another Image", fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { photoPickerLauncher.launch("image/*") }
                    .semantics { contentDescription = "Tap to pick an image from photo library for offline color extraction" },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📷",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap to load photo & sample pixels",
                        color = MidnightIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ColorInfoCard(
    context: Context,
    color: Color,
    onSave: () -> Unit
) {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()

    val hexString = String.format("#%02X%02X%02X", r, g, b)
    val rgbString = "$r, $g, $b"

    val outHsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.RGBToHSL(r, g, b, outHsl)
    val hslString = String.format("%d°, %d%%, %d%%", outHsl[0].toInt(), (outHsl[1] * 100).toInt(), (outHsl[2] * 100).toInt())

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Color Analysis", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // HEX Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hex Code", color = Color.Gray, fontSize = 11.sp)
                    Text(hexString, color = MidnightIndigo, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { copyToClipboard("Hex Code", hexString) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("Copy", color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

            // RGB Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RGB Format", color = Color.Gray, fontSize = 11.sp)
                    Text(rgbString, color = MidnightIndigo, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { copyToClipboard("RGB Format", rgbString) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("Copy", color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

            // HSL Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HSL Format", color = Color.Gray, fontSize = 11.sp)
                    Text(hslString, color = MidnightIndigo, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = { copyToClipboard("HSL Format", hslString) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("Copy", color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
            ) {
                Text("Save Color to Palette", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SavedPalettesSection(
    context: Context,
    savedColors: List<String>,
    onColorLoad: (String) -> Unit,
    onColorDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Saved Palettes", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (savedColors.isEmpty()) {
                Text(
                    text = "No saved colors yet.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    savedColors.forEach { hex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onColorLoad(hex) }
                                    .semantics { contentDescription = "Load saved hex swatch $hex" }
                            ) {
                                val colorInt = try {
                                    android.graphics.Color.parseColor(hex)
                                } catch (e: Exception) {
                                    android.graphics.Color.GRAY
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(colorInt))
                                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = hex,
                                    color = MidnightIndigo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = { onColorDelete(hex) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { contentDescription = "Delete hex swatch $hex from palette" }
                            ) {
                                Text("✕", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Coordinate coercion helper
private fun Offset.coerceIn(bounds: Offset): Offset {
    return Offset(
        x = x.coerceIn(0f, bounds.x),
        y = y.coerceIn(0f, bounds.y)
    )
}

// Local helper to map density-independent offsets
private fun Offset.coerceIn(width: Float, height: Float): Offset {
    return Offset(
        x = x.coerceIn(0f, width),
        y = y.coerceIn(0f, height)
    )
}

// Image Loader Helper
fun loadDownscaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        var scale = 1
        while (options.outWidth / scale / 2 >= maxDim && options.outHeight / scale / 2 >= maxDim) {
            scale *= 2
        }

        val options2 = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        val inputStream2 = context.contentResolver.openInputStream(uri)
        val bmp = android.graphics.BitmapFactory.decodeStream(inputStream2, null, options2)
        inputStream2?.close()
        bmp
    } catch (e: Exception) {
        null
    }
}

// SharedPreferences Palette Storage Helpers
fun getSavedColors(context: Context): List<String> {
    val prefs = context.getSharedPreferences("anegan_palettes", Context.MODE_PRIVATE)
    val json = prefs.getString("saved_colors", "[]") ?: "[]"
    if (json == "[]" || json.isBlank()) return emptyList()
    return json.removePrefix("[").removeSuffix("]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotEmpty() }
}

fun saveColor(context: Context, hex: String) {
    val current = getSavedColors(context).toMutableList()
    if (!current.contains(hex)) {
        current.add(hex)
        val prefs = context.getSharedPreferences("anegan_palettes", Context.MODE_PRIVATE)
        val json = "[" + current.joinToString(",") { "\"$it\"" } + "]"
        prefs.edit().putString("saved_colors", json).apply()
    }
}

fun deleteColor(context: Context, hex: String) {
    val current = getSavedColors(context).toMutableList()
    current.remove(hex)
    val prefs = context.getSharedPreferences("anegan_palettes", Context.MODE_PRIVATE)
    val json = "[" + current.joinToString(",") { "\"$it\"" } + "]"
    prefs.edit().putString("saved_colors", json).apply()
}
