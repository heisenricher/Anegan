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
import com.anegan.core.conversion.FFmpegMediaConverter
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioToolsScreen(
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
    var progress by remember { mutableStateOf(0f) }

    // Trim states
    var trimStart by remember { mutableStateOf("00:00") }
    var trimEnd by remember { mutableStateOf("00:30") }

    val audioPickerLauncher = rememberLauncherForActivityResult(
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

    fun parseTimeToSeconds(timeStr: String): Double? {
        if (timeStr.isBlank()) return null
        val parts = timeStr.trim().split(":")
        return when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> {
                val mins = parts[0].toDoubleOrNull() ?: return null
                val secs = parts[1].toDoubleOrNull() ?: return null
                mins * 60.0 + secs
            }
            3 -> {
                val hrs = parts[0].toDoubleOrNull() ?: return null
                val mins = parts[1].toDoubleOrNull() ?: return null
                val secs = parts[2].toDoubleOrNull() ?: return null
                hrs * 3600.0 + mins * 60.0 + secs
            }
            else -> null
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
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                color = MidnightIndigo,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = "Audio Cutter / Ringtone Maker",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Picker Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { audioPickerLauncher.launch("audio/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 15.sp)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Text("Tap to Select Audio File", color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trim Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column {
                Text("Trim / Ringtone Settings", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = trimStart,
                    onValueChange = { trimStart = it },
                    label = { Text("Start Time (MM:SS or Seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = trimEnd,
                    onValueChange = { trimEnd = it },
                    label = { Text("End Time (MM:SS or Seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                )
            }
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Progress: ${(progress * 100).toInt()}%", color = MidnightIndigo, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MidnightIndigo,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Submit Button
        Button(
            onClick = {
                val uri = selectedUri
                if (uri == null) {
                    Toast.makeText(context, "Please select an audio file first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isProcessing = true
                progress = 0f

                coroutineScope.launch {
                    try {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isProcessing = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val converter = FFmpegMediaConverter()
                        val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                        val start = parseTimeToSeconds(trimStart)
                        val end = parseTimeToSeconds(trimEnd)
                        if (start == null || end == null || start >= end) {
                            isProcessing = false
                            Toast.makeText(context, "Invalid start or end time", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val result = converter.trimAudio(tempFile, start, end) { progress = it }

                        isProcessing = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()

                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = outFile.name,
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = tempFile.extension.uppercase(),
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

                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = "",
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = tempFile.extension.uppercase(),
                                    status = "FAILED",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSize ?: tempFile.length(),
                                    outputSize = 0,
                                    outputPath = ""
                                )
                            )
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
                Text("Cut / Trim Audio", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
