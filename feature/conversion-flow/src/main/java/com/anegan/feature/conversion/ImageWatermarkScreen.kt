/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.anegan.core.conversion.ImageEditorManager
import com.anegan.core.conversion.ImageWatermarkManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.conversion.WatermarkPosition
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageWatermarkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var outputEditedFile by remember { mutableStateOf<File?>(null) }
    
    // Editor Modes: 0: Crop, 1: Rotate/Flip, 2: Adjust, 3: Filters, 4: Watermark
    var activeEditorMode by remember { mutableStateOf(4) } // Default to watermark

    // 1. Watermark custom configs
    var watermarkText by remember { mutableStateOf("Anegan Watermark") }
    var selectedPosition by remember { mutableStateOf(WatermarkPosition.TILED) }
    var sizePercent by remember { mutableStateOf(0.05f) }
    var opacityPercent by remember { mutableStateOf(0.4f) }
    var selectedColorHex by remember { mutableStateOf("#FFFFFF") }

    val colorOptions = listOf(
        "#FFFFFF" to "White",
        "#000000" to "Black",
        "#FF0000" to "Red",
        "#FFD700" to "Gold",
        "#0F172A" to "Midnight",
        "#C0C0C0" to "Silver"
    )

    // 2. Crop Configs (Left, Top, Right, Bottom percentages)
    var cropLeft by remember { mutableStateOf(0.0f) }
    var cropTop by remember { mutableStateOf(0.0f) }
    var cropRight by remember { mutableStateOf(1.0f) }
    var cropBottom by remember { mutableStateOf(1.0f) }

    // 3. Rotate & Flip Configs
    var rotationDegrees by remember { mutableStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }

    // 4. Adjustments Configs
    var brightnessVal by remember { mutableStateOf(0f) } // -1 to 1
    var contrastVal by remember { mutableStateOf(0f) }   // -1 to 1
    var saturationVal by remember { mutableStateOf(0f) } // -1 to 1

    // 5. Filters Configs
    var selectedFilter by remember { mutableStateOf("None") }
    val filtersList = listOf("None", "Grayscale", "Sepia", "Invert", "Vintage", "Cool", "Warm")

    val imagePickerLauncher = rememberLauncherForActivityResult(
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
            outputEditedFile = null
            NovaHaptics.click(view)
        }
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Image Editor",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frosted Image File Picker Box
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable {
                            NovaHaptics.click(view)
                            imagePickerLauncher.launch("image/*")
                        },
                    neonAccent = NeonMagenta,
                    enableGlow = selectedFileName != null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedFileName != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = selectedFileName!!,
                                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                                Text(
                                    text = String.format(java.util.Locale.ROOT, "%.2f MB", sizeMb),
                                    color = NeonMagenta,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = NeonMagenta.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Tap to Select Image",
                                    color = NeonMagenta,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonMagenta)
                    }
                }

                // Success Box
                outputEditedFile?.let { file ->
                    val bmp = remember(file) {
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonMagenta,
                        enableGlow = true
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Image Saved Successfully!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeonMagenta
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) NovaMidnightBlue.copy(alpha = 0.5f) else NovaCoolGray100.copy(alpha = 0.5f))
                                    .border(1.dp, if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                bmp?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Edited Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Text(
                                text = "Saved to: Anegan/Images/${file.name}",
                                color = Color.Gray,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (selectedUri != null && outputEditedFile == null) {
                    // Editor Mode Selector Row
                    val modes = listOf("Crop", "Rotate", "Adjust", "Filters", "Watermark")
                    
                    NovaSegmentedControl(
                        items = modes,
                        selectedIndex = activeEditorMode,
                        onIndexSelected = { index ->
                            activeEditorMode = index
                        },
                        neonColor = NeonMagenta
                    )

                    // Configuration Form Card based on active Mode
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (activeEditorMode) {
                                0 -> { // Crop Mode
                                    Text("Crop Image Margins", fontWeight = FontWeight.Bold, color = NeonMagenta, fontSize = 15.sp)

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Left Crop", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text("${(cropLeft * 100).toInt()}%", fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = cropLeft,
                                            onValueChange = { cropLeft = it.coerceAtMost(cropRight - 0.1f) },
                                            valueRange = 0f..0.9f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Right Crop", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text("${(cropRight * 100).toInt()}%", fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = cropRight,
                                            onValueChange = { cropRight = it.coerceAtLeast(cropLeft + 0.1f) },
                                            valueRange = 0.1f..1.0f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Top Crop", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text("${(cropTop * 100).toInt()}%", fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = cropTop,
                                            onValueChange = { cropTop = it.coerceAtMost(cropBottom - 0.1f) },
                                            valueRange = 0f..0.9f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Bottom Crop", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text("${(cropBottom * 100).toInt()}%", fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = cropBottom,
                                            onValueChange = { cropBottom = it.coerceAtLeast(cropTop + 0.1f) },
                                            valueRange = 0.1f..1.0f,
                                            neonColor = NeonMagenta
                                        )
                                    }
                                }
                                1 -> { // Rotate/Flip Mode
                                    Text("Rotation & Orientation", fontWeight = FontWeight.Bold, color = NeonMagenta, fontSize = 15.sp)

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Angle", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(0f, 90f, 180f, 270f).forEach { deg ->
                                                val isSel = rotationDegrees == deg
                                                Box(modifier = Modifier.weight(1f)) {
                                                    NovaChip(
                                                        text = "${deg.toInt()}°",
                                                        selected = isSel,
                                                        onClick = { rotationDegrees = deg },
                                                        neonColor = NeonMagenta,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Flip Horizontally", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        NovaSwitch(
                                            checked = flipHorizontal,
                                            onCheckedChange = { flipHorizontal = it },
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Flip Vertically", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        NovaSwitch(
                                            checked = flipVertical,
                                            onCheckedChange = { flipVertical = it },
                                            neonColor = NeonMagenta
                                        )
                                    }
                                }
                                2 -> { // Adjust Mode
                                    Text("Brightness, Contrast & Saturation", fontWeight = FontWeight.Bold, color = NeonMagenta, fontSize = 15.sp)

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Brightness", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text(String.format(java.util.Locale.ROOT, "%.1f", brightnessVal), fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = brightnessVal,
                                            onValueChange = { brightnessVal = it },
                                            valueRange = -0.5f..0.5f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Contrast", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text(String.format(java.util.Locale.ROOT, "%.1f", contrastVal), fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = contrastVal,
                                            onValueChange = { contrastVal = it },
                                            valueRange = -0.5f..0.5f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Saturation", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                                            Text(String.format(java.util.Locale.ROOT, "%.1f", saturationVal), fontFamily = JetBrainsMono, fontSize = 12.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                                        }
                                        NovaSlider(
                                            value = saturationVal,
                                            onValueChange = { saturationVal = it },
                                            valueRange = -0.5f..0.5f,
                                            neonColor = NeonMagenta
                                        )
                                    }
                                }
                                3 -> { // Filters Mode
                                    Text("Photo Filters", fontWeight = FontWeight.Bold, color = NeonMagenta, fontSize = 15.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                filtersList.take(4).forEach { filterName ->
                                                    val isSel = selectedFilter == filterName
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        NovaChip(
                                                            text = filterName,
                                                            selected = isSel,
                                                            onClick = { selectedFilter = filterName },
                                                            neonColor = NeonMagenta,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                filtersList.drop(4).forEach { filterName ->
                                                    val isSel = selectedFilter == filterName
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        NovaChip(
                                                            text = filterName,
                                                            selected = isSel,
                                                            onClick = { selectedFilter = filterName },
                                                            neonColor = NeonMagenta,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                                repeat(4 - filtersList.drop(4).size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                                4 -> { // Watermark Mode
                                    Text("Watermark Settings", fontWeight = FontWeight.Bold, color = NeonMagenta, fontSize = 15.sp)

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Watermark Text",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        NovaTextField(
                                            value = watermarkText,
                                            onValueChange = { watermarkText = it },
                                            placeholder = "Enter watermark text...",
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Select Position", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val positions = listOf(WatermarkPosition.TILED to "Tiled", WatermarkPosition.CENTER to "Center", WatermarkPosition.BOTTOM_RIGHT to "Bottom R")
                                            positions.forEach { (pos, label) ->
                                                val isSelected = selectedPosition == pos
                                                Box(modifier = Modifier.weight(1f)) {
                                                    NovaChip(
                                                        text = label,
                                                        selected = isSelected,
                                                        onClick = { selectedPosition = pos },
                                                        neonColor = NeonMagenta,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Size Scale", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(String.format(java.util.Locale.ROOT, "%d%%", (sizePercent * 100).toInt()), fontFamily = JetBrainsMono, color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        NovaSlider(
                                            value = sizePercent,
                                            onValueChange = { sizePercent = it },
                                            valueRange = 0.02f..0.15f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Opacity", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(String.format(java.util.Locale.ROOT, "%d%%", (opacityPercent * 100).toInt()), fontFamily = JetBrainsMono, color = NeonMagenta, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        NovaSlider(
                                            value = opacityPercent,
                                            onValueChange = { opacityPercent = it },
                                            valueRange = 0.1f..1.0f,
                                            neonColor = NeonMagenta
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Select Color", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            colorOptions.forEach { (hex, name) ->
                                                val isSelected = selectedColorHex == hex
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                                        .clickable {
                                                            selectedColorHex = hex
                                                            NovaHaptics.click(view)
                                                        }
                                                        .border(
                                                            width = if (isSelected) 3.dp else 1.dp,
                                                            color = if (isSelected) NeonMagenta else if (isDark) NovaBorderDark.copy(alpha = 0.3f) else NovaBorderLight.copy(alpha = 0.3f),
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    NovaPrimaryButton(
                        text = when (activeEditorMode) {
                            0 -> "Crop & Save Image"
                            1 -> "Apply Rotation & Save"
                            2 -> "Apply Adjustments & Save"
                            3 -> "Apply Filter & Save"
                            4 -> "Apply Watermark & Save"
                            else -> "Save Changes"
                        },
                        neonColor = NeonMagenta,
                        onClick = {
                            val uri = selectedUri
                            if (uri == null) {
                                Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                                return@NovaPrimaryButton
                            }

                            isProcessing = true
                            coroutineScope.launch {
                                try {
                                    val tempFile = StorageManager.copyUriToTempFile(context, uri)
                                    if (tempFile == null) {
                                        isProcessing = false
                                        Toast.makeText(context, "Failed to copy file", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val result = when (activeEditorMode) {
                                        0 -> ImageEditorManager().cropImage(tempFile, cropLeft, cropTop, cropRight, cropBottom)
                                        1 -> ImageEditorManager().rotateAndFlipImage(tempFile, rotationDegrees, flipHorizontal, flipVertical)
                                        2 -> ImageEditorManager().adjustImage(tempFile, brightnessVal, contrastVal, saturationVal)
                                        3 -> ImageEditorManager().applyFilter(tempFile, selectedFilter)
                                        4 -> ImageWatermarkManager().addTextWatermark(
                                            input = tempFile,
                                            text = watermarkText,
                                            position = selectedPosition,
                                            colorHex = selectedColorHex,
                                            opacityPercent = opacityPercent,
                                            sizePercent = sizePercent
                                        )
                                        else -> Result.failure(Exception("Unknown mode"))
                                    }
                                    
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        val file = result.getOrThrow()
                                        outputEditedFile = file
                                        Toast.makeText(context, "Changes Applied!", Toast.LENGTH_SHORT).show()
                                        NovaHaptics.success(view)

                                        // Log conversion history entry in database
                                        val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                        historyDao.insertConversion(
                                            ConversionHistoryEntity(
                                                originalFileName = selectedFileName ?: tempFile.name,
                                                outputFileName = file.name,
                                                originalFormat = tempFile.extension.uppercase(),
                                                outputFormat = file.extension.uppercase(),
                                                status = "SUCCESS",
                                                timestamp = System.currentTimeMillis(),
                                                originalSize = selectedFileSize ?: tempFile.length(),
                                                outputSize = file.length(),
                                                outputPath = file.absolutePath
                                            )
                                        )
                                    } else {
                                        val ex = result.exceptionOrNull()
                                        Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                                        NovaHaptics.reject(view)
                                    }
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    NovaHaptics.reject(view)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isProcessing,
                        isLoading = isProcessing
                    )
                }
            }
        }
    }
}
