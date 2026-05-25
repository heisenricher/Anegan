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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.anegan.core.conversion.NativeDocumentConverter
import com.anegan.core.conversion.ImageConversionOptions
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch

data class BatchFileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val status: String = "Pending", // "Pending", "Converting", "Success", "Failed"
    val progress: Float = 0f
)

@Composable
fun BatchConversionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fileItems by remember { mutableStateOf(listOf<BatchFileItem>()) }
    var selectedFormat by remember { mutableStateOf("JPG") }
    var isProcessing by remember { mutableStateOf(false) }

    val batchPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val list = mutableListOf<BatchFileItem>()
            uris.forEach { uri ->
                var name = "Unknown file"
                var size = 0L
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameIndex != -1) name = it.getString(nameIndex)
                        if (sizeIndex != -1) size = it.getLong(sizeIndex)
                    }
                }
                list.add(BatchFileItem(uri, name, size))
            }
            fileItems = list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
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
                text = "Batch Processing",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Selector Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { batchPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (fileItems.isNotEmpty()) {
                Text("${fileItems.size} Images Selected (Tap to Change)", color = MidnightIndigo, fontSize = 16.sp)
            } else {
                Text("Tap to Select Multiple Images", color = MidnightIndigo, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Target Format", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val formats = listOf("JPG", "PNG", "WEBP", "PDF")
            formats.forEach { fmt ->
                val isSelected = selectedFormat == fmt
                Button(
                    onClick = { selectedFormat = fmt },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) PureWhite else MidnightIndigo
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fmt, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // List of files with conversion progress/status
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fileItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, color = MidnightIndigo, fontSize = 14.sp)
                            val sizeMb = item.size / (1024f * 1024f)
                            Text(String.format("%.2f MB - Status: %s", sizeMb, item.status), color = Color.Gray, fontSize = 11.sp)
                            
                            if (item.status == "Converting") {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MidnightIndigo
                                )
                            }
                        }
                        
                        Box(modifier = Modifier.padding(start = 8.dp)) {
                            when (item.status) {
                                "Success" -> Text("✓", color = Color(0xFF4CAF50), fontSize = 20.sp)
                                "Failed" -> Text("✗", color = Color(0xFFF44336), fontSize = 20.sp)
                                "Converting" -> CircularProgressIndicator(color = MidnightIndigo, modifier = Modifier.size(18.dp))
                                else -> Text("•", color = Color.Gray, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Convert Button
        Button(
            onClick = {
                if (fileItems.isEmpty()) {
                    Toast.makeText(context, "Please select images first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isProcessing = true

                coroutineScope.launch {
                    val updatedItems = fileItems.toMutableList()
                    val imageConverter = NativeImageConverter()
                    val docConverter = NativeDocumentConverter()
                    val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                    fileItems.forEachIndexed { index, item ->
                        updatedItems[index] = item.copy(status = "Converting")
                        fileItems = updatedItems.toList()

                        try {
                            val tempFile = StorageManager.copyUriToTempFile(context, item.uri)
                            if (tempFile == null) {
                                updatedItems[index] = item.copy(status = "Failed")
                                fileItems = updatedItems.toList()
                                return@forEachIndexed
                            }

                            val result = if (selectedFormat == "PDF") {
                                // PDF conversion
                                docConverter.convertToPdf(tempFile)
                            } else {
                                // Image format conversion
                                val options = ImageConversionOptions(
                                    format = selectedFormat,
                                    quality = 85
                                )
                                imageConverter.convertImage(tempFile, options)
                            }

                            if (result.isSuccess) {
                                val outFile = result.getOrThrow()
                                updatedItems[index] = item.copy(status = "Success")
                                historyDao.insertConversion(
                                    ConversionHistoryEntity(
                                        originalFileName = item.name,
                                        outputFileName = outFile.name,
                                        originalFormat = tempFile.extension.uppercase(),
                                        outputFormat = selectedFormat,
                                        status = "SUCCESS",
                                        timestamp = System.currentTimeMillis(),
                                        originalSize = item.size,
                                        outputSize = outFile.length(),
                                        outputPath = outFile.absolutePath
                                    )
                                )
                            } else {
                                updatedItems[index] = item.copy(status = "Failed")
                                historyDao.insertConversion(
                                    ConversionHistoryEntity(
                                        originalFileName = item.name,
                                        outputFileName = "",
                                        originalFormat = tempFile.extension.uppercase(),
                                        outputFormat = selectedFormat,
                                        status = "FAILED",
                                        timestamp = System.currentTimeMillis(),
                                        originalSize = item.size,
                                        outputSize = 0,
                                        outputPath = ""
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            updatedItems[index] = item.copy(status = "Failed")
                        }

                        fileItems = updatedItems.toList()
                    }

                    isProcessing = false
                    Toast.makeText(context, "Batch Conversion Completed!", Toast.LENGTH_LONG).show()
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
                Text("Process Batch", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
