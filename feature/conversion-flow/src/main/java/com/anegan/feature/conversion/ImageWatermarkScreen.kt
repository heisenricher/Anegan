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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.ImageWatermarkManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.conversion.WatermarkPosition
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageWatermarkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var outputWatermarkedFile by remember { mutableStateOf<File?>(null) }

    // Watermark custom configs
    var watermarkText by remember { mutableStateOf("Anegan Watermark") }
    var selectedPosition by remember { mutableStateOf(WatermarkPosition.TILED) }
    var sizePercent by remember { mutableStateOf(0.05f) } // 5% of width
    var opacityPercent by remember { mutableStateOf(0.4f) } // 40% opacity
    var selectedColorHex by remember { mutableStateOf("#FFFFFF") }

    val colorOptions = listOf(
        "#FFFFFF" to "White",
        "#000000" to "Black",
        "#FF0000" to "Red",
        "#FFD700" to "Gold",
        "#0F172A" to "Midnight",
        "#C0C0C0" to "Silver"
    )

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
            outputWatermarkedFile = null
        }
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
            Text(
                text = "← ",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = "Image Watermark Editor",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Picker Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                Text("Tap to Select Image", color = MidnightIndigo, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MidnightIndigo)
            }
        }

        outputWatermarkedFile?.let { file ->
            val bmp = remember(file) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Watermark Applied Successfully!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightIndigo,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        bmp?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Watermarked Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Saved to: Anegan/Images/${file.name}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (selectedUri != null && outputWatermarkedFile == null) {
            Spacer(modifier = Modifier.height(24.dp))

            // Configuration Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Watermark Configurations",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightIndigo,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Input
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("Watermark Text") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MidnightIndigo,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Position Selector
                    Text("Select Position", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val positions = listOf(WatermarkPosition.TILED to "Tiled", WatermarkPosition.CENTER to "Center", WatermarkPosition.BOTTOM_RIGHT to "Bottom R")
                        positions.forEach { (pos, label) ->
                            val isSelected = selectedPosition == pos
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPosition = pos },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MidnightIndigo,
                                    selectedLabelColor = PureWhite,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = MidnightIndigo
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Size Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Size Scale", color = Color.Gray, fontSize = 14.sp)
                        Text(String.format("%d%%", (sizePercent * 100).toInt()), color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Slider(
                        value = sizePercent,
                        onValueChange = { sizePercent = it },
                        valueRange = 0.02f..0.15f,
                        colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opacity Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Opacity", color = Color.Gray, fontSize = 14.sp)
                        Text(String.format("%d%%", (opacityPercent * 100).toInt()), color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Slider(
                        value = opacityPercent,
                        onValueChange = { opacityPercent = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Palette
                    Text("Select Color", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorOptions.forEach { (hex, name) ->
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { selectedColorHex = hex }
                                    .background(if (isSelected) Color.Gray.copy(alpha = 0.4f) else Color.Transparent)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val uri = selectedUri
                    if (uri == null) {
                        Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (watermarkText.isBlank()) {
                        Toast.makeText(context, "Please enter watermark text", Toast.LENGTH_SHORT).show()
                        return@Button
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

                            val result = ImageWatermarkManager().addTextWatermark(
                                input = tempFile,
                                text = watermarkText,
                                position = selectedPosition,
                                colorHex = selectedColorHex,
                                opacityPercent = opacityPercent,
                                sizePercent = sizePercent
                            )
                            isProcessing = false

                            if (result.isSuccess) {
                                val file = result.getOrThrow()
                                outputWatermarkedFile = file
                                Toast.makeText(context, "Watermark Applied!", Toast.LENGTH_SHORT).show()

                                // Save entry in Room database conversion history log
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
                            }
                        } catch (e: Exception) {
                            isProcessing = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
            ) {
                Text("Apply & Save Watermark", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
