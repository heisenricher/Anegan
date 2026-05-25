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
import com.anegan.core.conversion.NativeDocumentConverter
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var activeTab by remember { mutableStateOf("Split") }
    var isProcessing by remember { mutableStateOf(false) }

    // Split states
    var startPage by remember { mutableStateOf("1") }
    var endPage by remember { mutableStateOf("2") }

    // Compress states
    var dpiValue by remember { mutableStateOf(150f) }

    // Encrypt states
    var passwordValue by remember { mutableStateOf("") }

    // PDF to Images states
    var imageFormat by remember { mutableStateOf("JPG") }

    // Images to PDF states
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

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
        }
    }

    val imageListPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages = uris
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
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                color = MidnightIndigo,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = "PDF Tools",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Picker Box (only visible when not in Images -> PDF mode)
        if (activeTab != "Images → PDF") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { pdfPickerLauncher.launch("application/pdf") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedFileName != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(selectedFileName!!, color = MidnightIndigo, fontSize = 15.sp)
                        val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                        Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Text("Tap to Select PDF File", color = MidnightIndigo)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Tools Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Split", "Compress", "Encrypt", "To Images", "Images → PDF")
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface)
                        .clickable { activeTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) PureWhite else MidnightIndigo,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            when (activeTab) {
                "Split" -> {
                    Column {
                        Text("Split PDF Settings", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = startPage,
                            onValueChange = { startPage = it },
                            label = { Text("Start Page (1-indexed)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = endPage,
                            onValueChange = { endPage = it },
                            label = { Text("End Page (1-indexed)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                        )
                    }
                }
                "Compress" -> {
                    Column {
                        Text("Compress PDF Settings", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Resolution (DPI): ${dpiValue.toInt()} DPI", fontSize = 14.sp, color = MidnightIndigo)
                        Slider(
                            value = dpiValue,
                            onValueChange = { dpiValue = it },
                            valueRange = 72f..300f,
                            steps = 3, // 72, 148, 224, 300 etc.
                            colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo)
                        )
                    }
                }
                "Encrypt" -> {
                    Column {
                        Text("Password Protect PDF", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = passwordValue,
                            onValueChange = { passwordValue = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                        )
                    }
                }
                "To Images" -> {
                    Column {
                        Text("PDF to Images Converter", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Image Format", fontSize = 14.sp, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val formats = listOf("JPG", "PNG")
                            formats.forEach { fmt ->
                                val isSelected = imageFormat == fmt
                                Button(
                                    onClick = { imageFormat = fmt },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.background,
                                        contentColor = if (isSelected) PureWhite else MidnightIndigo
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(fmt)
                                }
                            }
                        }
                    }
                }
                "Images → PDF" -> {
                    Column {
                        Text("Images to PDF Converter", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { imageListPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImages.isNotEmpty()) {
                                Text("${selectedImages.size} Images Selected (Tap to Change)", color = MidnightIndigo, fontSize = 15.sp)
                            } else {
                                Text("Tap to Select Images", color = MidnightIndigo, fontSize = 15.sp)
                            }
                        }
                    }
                }
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

        Spacer(modifier = Modifier.height(40.dp))

        // Submit Button
        Button(
            onClick = {
                if (activeTab != "Images → PDF" && selectedUri == null) {
                    Toast.makeText(context, "Please select a PDF file first", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (activeTab == "Images → PDF" && selectedImages.isEmpty()) {
                    Toast.makeText(context, "Please select images first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isProcessing = true

                coroutineScope.launch {
                    try {
                        val converter = NativeDocumentConverter()
                        val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                        if (activeTab == "Images → PDF") {
                            val tempFiles = selectedImages.mapNotNull { uri ->
                                StorageManager.copyUriToTempFile(context, uri)
                            }
                            if (tempFiles.isEmpty()) {
                                isProcessing = false
                                Toast.makeText(context, "Failed to resolve images", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val result = converter.imagesToPdf(tempFiles)
                            isProcessing = false

                            if (result.isSuccess) {
                                val outFile = result.getOrThrow()
                                Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                                historyDao.insertConversion(
                                    ConversionHistoryEntity(
                                        originalFileName = "${tempFiles.size} images",
                                        outputFileName = outFile.name,
                                        originalFormat = "IMAGES",
                                        outputFormat = "PDF",
                                        status = "SUCCESS",
                                        timestamp = System.currentTimeMillis(),
                                        originalSize = tempFiles.fold(0L) { acc, f -> acc + f.length() },
                                        outputSize = outFile.length(),
                                        outputPath = outFile.absolutePath
                                    )
                                )
                                selectedImages = emptyList()
                            } else {
                                val ex = result.exceptionOrNull()
                                Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }

                        val uri = selectedUri!!
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isProcessing = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        when (activeTab) {
                            "Split" -> {
                                val start = startPage.toIntOrNull()
                                val end = endPage.toIntOrNull()
                                if (start == null || end == null || start <= 0 || end < start) {
                                    isProcessing = false
                                    Toast.makeText(context, "Invalid page range", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                val result = converter.splitPdf(tempFile, start, end)
                                isProcessing = false

                                if (result.isSuccess) {
                                    val outFile = result.getOrThrow()
                                    Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                                    historyDao.insertConversion(
                                        ConversionHistoryEntity(
                                            originalFileName = selectedFileName ?: tempFile.name,
                                            outputFileName = outFile.name,
                                            originalFormat = "PDF",
                                            outputFormat = "PDF",
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
                                }
                            }
                            "Compress" -> {
                                val result = converter.compressPdf(tempFile, dpiValue.toInt())
                                isProcessing = false

                                if (result.isSuccess) {
                                    val outFile = result.getOrThrow()
                                    Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                                    historyDao.insertConversion(
                                        ConversionHistoryEntity(
                                            originalFileName = selectedFileName ?: tempFile.name,
                                            outputFileName = outFile.name,
                                            originalFormat = "PDF",
                                            outputFormat = "PDF",
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
                                }
                            }
                            "Encrypt" -> {
                                if (passwordValue.isBlank()) {
                                    isProcessing = false
                                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                val result = converter.encryptPdf(tempFile, passwordValue)
                                isProcessing = false

                                if (result.isSuccess) {
                                    val outFile = result.getOrThrow()
                                    Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                                    historyDao.insertConversion(
                                        ConversionHistoryEntity(
                                            originalFileName = selectedFileName ?: tempFile.name,
                                            outputFileName = outFile.name,
                                            originalFormat = "PDF",
                                            outputFormat = "PDF",
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
                                }
                            }
                            "To Images" -> {
                                val result = converter.pdfToImages(tempFile, imageFormat.lowercase())
                                isProcessing = false

                                if (result.isSuccess) {
                                    val outFiles = result.getOrThrow()
                                    if (outFiles.isNotEmpty()) {
                                        Toast.makeText(context, "Exported ${outFiles.size} images to ${outFiles.first().parent}", Toast.LENGTH_LONG).show()
                                        historyDao.insertConversion(
                                            ConversionHistoryEntity(
                                                originalFileName = selectedFileName ?: tempFile.name,
                                                outputFileName = "${tempFile.nameWithoutExtension}_pages",
                                                originalFormat = "PDF",
                                                outputFormat = imageFormat,
                                                status = "SUCCESS",
                                                timestamp = System.currentTimeMillis(),
                                                originalSize = selectedFileSize ?: tempFile.length(),
                                                outputSize = outFiles.fold(0L) { acc, file -> acc + file.length() },
                                                outputPath = outFiles.first().absolutePath
                                            )
                                        )
                                    } else {
                                        Toast.makeText(context, "No pages converted", Toast.LENGTH_LONG).show()
                                    }
                                    selectedUri = null
                                    selectedFileName = null
                                    selectedFileSize = null
                                } else {
                                    val ex = result.exceptionOrNull()
                                    Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
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
            if (isProcessing) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text(if (activeTab == "Images → PDF") "Combine to PDF" else "Process PDF", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
