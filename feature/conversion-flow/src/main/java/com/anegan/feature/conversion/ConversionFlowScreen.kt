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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.NativeImageConverter
import com.anegan.core.conversion.ImageConversionOptions
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionFlowScreen(
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var quality by remember { mutableStateOf(0.8f) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    var isConverting by remember { mutableStateOf(false) }

    // Target format selection (replaces raw target format being equal to screen name)
    var targetFormat by remember { mutableStateOf(if (categoryName == "Images") "JPG" else categoryName) }

    // Resize inputs
    var resizeWidth by remember { mutableStateOf("") }
    var resizeHeight by remember { mutableStateOf("") }

    // Rotation inputs
    var rotationValue by remember { mutableStateOf(0f) }

    // Crop inputs
    var cropX by remember { mutableStateOf("") }
    var cropY by remember { mutableStateOf("") }
    var cropW by remember { mutableStateOf("") }
    var cropH by remember { mutableStateOf("") }

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
                text = "Image Tools & Converter",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Picker UI Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 15.sp)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Text("Tap to Select Image (JPG, PNG, WEBP, HEIC, AVIF)", color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Target Format selection
        Text("Target Format", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val formats = listOf("JPG", "PNG", "WEBP")
            formats.forEach { fmt ->
                val isSelected = targetFormat == fmt
                Button(
                    onClick = { targetFormat = fmt },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) PureWhite else MidnightIndigo
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fmt)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SizeController(
            initialQuality = quality,
            onQualityChanged = { quality = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Edit tools collapse card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Advanced Resize, Rotate & Crop", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(16.dp))

                // Resize
                Text("Resize Dimensions (Optional)", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = resizeWidth,
                        onValueChange = { resizeWidth = it },
                        label = { Text("Width (px)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                    OutlinedTextField(
                        value = resizeHeight,
                        onValueChange = { resizeHeight = it },
                        label = { Text("Height (px)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rotation
                Text("Rotation (Optional)", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rotations = listOf(0f, 90f, 180f, 270f)
                    rotations.forEach { rot ->
                        val isSelected = rotationValue == rot
                        Button(
                            onClick = { rotationValue = rot },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.background,
                                contentColor = if (isSelected) PureWhite else MidnightIndigo
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("${rot.toInt()}°", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Crop
                Text("Crop Box (Optional)", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cropX,
                        onValueChange = { cropX = it },
                        label = { Text("X Offset") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                    OutlinedTextField(
                        value = cropY,
                        onValueChange = { cropY = it },
                        label = { Text("Y Offset") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cropW,
                        onValueChange = { cropW = it },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                    OutlinedTextField(
                        value = cropH,
                        onValueChange = { cropH = it },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                }
            }
        }

        if (isConverting) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Submit Button
        Button(
            onClick = {
                val uri = selectedUri
                if (uri == null) {
                    Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isConverting = true
                coroutineScope.launch {
                    try {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isConverting = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        
                        val targetSize = (selectedFileSize ?: 1024L) * quality
                        
                        // Parse optional crop/rotate/resize options
                        val rWidth = resizeWidth.toIntOrNull()
                        val rHeight = resizeHeight.toIntOrNull()
                        
                        val cX = cropX.toIntOrNull()
                        val cY = cropY.toIntOrNull()
                        val cW = cropW.toIntOrNull()
                        val cH = cropH.toIntOrNull()
                        
                        val cRect = if (cX != null && cY != null && cW != null && cH != null) {
                            android.graphics.Rect(cX, cY, cX + cW, cY + cH)
                        } else {
                            null
                        }

                        val options = ImageConversionOptions(
                            format = targetFormat,
                            quality = (quality * 100).toInt(),
                            targetSizeBytes = targetSize.toLong(),
                            exactWidth = rWidth,
                            exactHeight = rHeight,
                            rotationDegrees = rotationValue,
                            cropRect = cRect
                        )
                        
                        val converter = NativeImageConverter()
                        val result = converter.convertImage(tempFile, options)
                        
                        isConverting = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                            
                            val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = outFile.name,
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = targetFormat,
                                    status = "SUCCESS",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSize ?: tempFile.length(),
                                    outputSize = outFile.length(),
                                    outputPath = outFile.absolutePath
                                )
                            )
                            selectedUri = null
                            selectedFileName = null
                            selectedFileSize = null
                        } else {
                            val ex = result.exceptionOrNull()
                            Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                            
                            val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = "",
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = targetFormat,
                                    status = "FAILED",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSize ?: tempFile.length(),
                                    outputSize = 0,
                                    outputPath = ""
                                )
                            )
                        }
                    } catch (e: Exception) {
                        isConverting = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isConverting,
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            if (isConverting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Process Image", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
