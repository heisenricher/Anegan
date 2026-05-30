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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.anegan.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableStateOf(0) } // 0: Spectra, 1: Image Analyzer

    // Global selected color
    var selectedColor by remember { mutableStateOf(Color(0xFF7C6FFF)) }
    var savedColorsList by remember { mutableStateOf(getSavedColors(context)) }

    fun updateColor(newColor: Color) {
        selectedColor = newColor
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Color Picker",
                    onBack = {
                        NovaHaptics.click(view)
                        onBack()
                    },
                    neonAccent = NeonMagenta
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Tab Selector Row
                val tabs = listOf("Spectra (HSV)", "Image Analyzer")
                NovaSegmentedControl(
                    items = tabs,
                    selectedIndex = activeTab,
                    onIndexSelected = { index ->
                        activeTab = index
                    },
                    neonColor = NeonMagenta
                )

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

                // Color Info Card inside premium GlassCard
                ColorInfoCard(
                    context = context,
                    color = selectedColor,
                    onSave = {
                        val r = (selectedColor.red * 255).toInt()
                        val g = (selectedColor.green * 255).toInt()
                        val b = (selectedColor.blue * 255).toInt()
                        val hex = String.format(java.util.Locale.ROOT, "#%02X%02X%02X", r, g, b)
                        saveColor(context, hex)
                        savedColorsList = getSavedColors(context)
                        Toast.makeText(context, "Color saved to palette!", Toast.LENGTH_SHORT).show()
                        NovaHaptics.success(view)
                    }
                )

                // Saved Palettes inside GlassCard
                SavedPalettesSection(
                    context = context,
                    savedColors = savedColorsList,
                    onColorLoad = { hex ->
                        try {
                            val colorInt = android.graphics.Color.parseColor(hex)
                            selectedColor = Color(colorInt)
                            NovaHaptics.click(view)
                        } catch (e: Exception) {
                            // Ignore malformed hex colors
                        }
                    },
                    onColorDelete = { hex ->
                        deleteColor(context, hex)
                        savedColorsList = getSavedColors(context)
                        NovaHaptics.click(view)
                    }
                )
            }
        }
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

    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Color Preview Box wrapped in beautiful neon-glowing GlassCard
        GlassCard(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            neonAccent = currentColor,
            enableGlow = true
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentColor)
                    .semantics { contentDescription = "Active color preview panel" }
            )
        }

        // Hue Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hue",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                    fontSize = 13.sp
                )
                Text(
                    text = "${hue.toInt()}°",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta,
                    fontSize = 13.sp
                )
            }
            NovaSlider(
                value = hue,
                onValueChange = {
                    hue = it
                    onColorChange(Color.hsv(hue, saturation, value))
                },
                valueRange = 0f..360f,
                neonColor = NeonMagenta
            )
        }

        // Saturation Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saturation",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                    fontSize = 13.sp
                )
                Text(
                    text = "${(saturation * 100).toInt()}%",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta,
                    fontSize = 13.sp
                )
            }
            NovaSlider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    onColorChange(Color.hsv(hue, saturation, value))
                },
                valueRange = 0f..1f,
                neonColor = NeonMagenta
            )
        }

        // Value (Brightness) Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Value (Brightness)",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                    fontSize = 13.sp
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta,
                    fontSize = 13.sp
                )
            }
            NovaSlider(
                value = value,
                onValueChange = {
                    value = it
                    onColorChange(Color.hsv(hue, saturation, value))
                },
                valueRange = 0f..1f,
                neonColor = NeonMagenta
            )
        }
    }
}

@Composable
fun ImageAnalyzerTabContent(
    onColorSelected: (Color) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
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
            NovaHaptics.click(view)
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

    val isDark = isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxWidth()) {
        if (loadedBitmap != null) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    neonAccent = NeonMagenta,
                    enableGlow = true
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                canvasSize = Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                            }
                            .pointerInput(loadedBitmap) {
                                detectTapGestures { offset ->
                                    indicatorOffset = offset
                                    samplePixelAt(offset)
                                    NovaHaptics.click(view)
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
                                .border(4.dp, NeonMagenta.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }

                NovaSecondaryButton(
                    text = "Select Another Image",
                    neonColor = NeonMagenta,
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { photoPickerLauncher.launch("image/*") },
                neonAccent = NeonMagenta,
                enableGlow = false
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = NeonMagenta.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Tap to load photo & sample pixels",
                            color = NeonMagenta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
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
    val view = LocalView.current
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()

    val hexString = String.format(java.util.Locale.ROOT, "#%02X%02X%02X", r, g, b)
    val rgbString = "$r, $g, $b"

    val outHsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.RGBToHSL(r, g, b, outHsl)
    val hslString = String.format(java.util.Locale.ROOT, "%d°, %d%%, %d%%", outHsl[0].toInt(), (outHsl[1] * 100).toInt(), (outHsl[2] * 100).toInt())

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        neonAccent = NeonMagenta,
        enableGlow = true
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Color Analysis",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // HEX Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hex Code", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        text = hexString,
                        color = NeonMagenta,
                        fontFamily = JetBrainsMono,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                NovaSecondaryButton(
                    text = "Copy",
                    neonColor = NeonMagenta,
                    onClick = {
                        copyToClipboard("Hex Code", hexString)
                        NovaHaptics.success(view)
                    }
                )
            }

            Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f))

            // RGB Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RGB Format", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        text = rgbString,
                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                        fontFamily = JetBrainsMono,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                NovaSecondaryButton(
                    text = "Copy",
                    neonColor = NeonMagenta,
                    onClick = {
                        copyToClipboard("RGB Format", rgbString)
                        NovaHaptics.success(view)
                    }
                )
            }

            Divider(color = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f))

            // HSL Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HSL Format", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        text = hslString,
                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                        fontFamily = JetBrainsMono,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                NovaSecondaryButton(
                    text = "Copy",
                    neonColor = NeonMagenta,
                    onClick = {
                        copyToClipboard("HSL Format", hslString)
                        NovaHaptics.success(view)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NovaPrimaryButton(
                text = "Save Color to Palette",
                neonColor = NeonMagenta,
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            )
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
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        neonAccent = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Saved Palettes",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (savedColors.isEmpty()) {
                Text(
                    text = "No saved colors yet.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        .border(1.dp, if (isDark) NovaBorderDark.copy(alpha = 0.3f) else NovaBorderLight.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = hex,
                                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    onColorDelete(hex)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { contentDescription = "Delete hex swatch $hex from palette" }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete swatch",
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(20.dp)
                                )
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
