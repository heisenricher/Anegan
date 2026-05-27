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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.ExifManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun ExifScreen(
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
    var exifTags by remember { mutableStateOf<Map<String, String>?>(null) }

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
            exifTags = null
            
            isProcessing = true
            coroutineScope.launch {
                try {
                    val tempFile = StorageManager.copyUriToTempFile(context, uri)
                    if (tempFile != null) {
                        val result = ExifManager().readExifMetadata(tempFile)
                        if (result.isSuccess) {
                            exifTags = result.getOrThrow()
                        } else {
                            Toast.makeText(context, "No EXIF metadata found", Toast.LENGTH_SHORT).show()
                        }
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
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
        // Sleek Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            ) {
                Text("←", fontSize = 20.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "EXIF Metadata",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { imagePickerLauncher.launch("image/*") },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedFileName != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🖼️ Image Selected", fontWeight = FontWeight.Bold, color = MidnightIndigo, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(selectedFileName!!, color = Color.Gray, fontSize = 13.sp)
                        val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                        Text(String.format("%.2f MB", sizeMb), color = MidnightIndigo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap to Select Image", color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Read details like camera model, date, location", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MidnightIndigo)
            }
        }

        exifTags?.let { tags ->
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sleek EXIF Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Metadata Properties",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightIndigo,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (tags.isEmpty()) {
                        Text(
                            text = "No metadata tags found in this image.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    } else {
                        tags.forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = value,
                                    color = MidnightIndigo,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        }
                    }
                }
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val uri = selectedUri
                        if (uri == null) return@Button
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val tempFile = StorageManager.copyUriToTempFile(context, uri)
                                if (tempFile == null) {
                                    isProcessing = false
                                    Toast.makeText(context, "Failed to copy file", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val result = ExifManager().stripExifMetadata(tempFile)
                                isProcessing = false
                                if (result.isSuccess) {
                                    val strippedFile = result.getOrThrow()
                                    Toast.makeText(context, "EXIF Metadata Stripped Successfully!", Toast.LENGTH_LONG).show()
                                    
                                    val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                    historyDao.insertConversion(
                                        ConversionHistoryEntity(
                                            originalFileName = selectedFileName ?: tempFile.name,
                                            outputFileName = strippedFile.name,
                                            originalFormat = tempFile.extension.uppercase(),
                                            outputFormat = strippedFile.extension.uppercase(),
                                            status = "SUCCESS",
                                            timestamp = System.currentTimeMillis(),
                                            originalSize = selectedFileSize ?: tempFile.length(),
                                            outputSize = strippedFile.length(),
                                            outputPath = strippedFile.absolutePath
                                        )
                                    )
                                    exifTags = emptyMap()
                                    selectedUri = null
                                    selectedFileName = null
                                    selectedFileSize = null
                                } else {
                                    val ex = result.exceptionOrNull()
                                    Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                                }
                                tempFile.delete()
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
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
                ) {
                    Text("Strip Metadata & Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
