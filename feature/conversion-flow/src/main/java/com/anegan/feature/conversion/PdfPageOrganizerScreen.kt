/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.graphics.Bitmap
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
import com.anegan.core.conversion.PdfPageManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageOrganizerScreen(
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
    var outputOrganizedFile by remember { mutableStateOf<File?>(null) }

    // Page organizer states
    var tempPdfFile by remember { mutableStateOf<File?>(null) }
    val pageIndices = remember { mutableStateListOf<Int>() }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap>() }

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
            outputOrganizedFile = null
            pageIndices.clear()
            pageThumbnails.clear()
            
            // Automatically parse PDF details and render thumbnails in background
            isProcessing = true
            coroutineScope.launch {
                try {
                    val file = StorageManager.copyUriToTempFile(context, uri)
                    if (file != null) {
                        tempPdfFile = file
                        val countResult = PdfPageManager().getPageCount(file)
                        if (countResult.isSuccess) {
                            val count = countResult.getOrThrow()
                            for (i in 0 until count) {
                                pageIndices.add(i)
                            }
                            
                            // Load thumbnails on IO dispatcher dynamically
                            launch(Dispatchers.IO) {
                                val pageManager = PdfPageManager()
                                for (i in 0 until count) {
                                    val bmpResult = pageManager.renderPageThumbnail(file, i, 160)
                                    if (bmpResult.isSuccess) {
                                        pageThumbnails[i] = bmpResult.getOrThrow()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Failed to parse PDF pages", Toast.LENGTH_SHORT).show()
                        }
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← ",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = "PDF Page Organizer",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.primary
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
                .clickable { pdfPickerLauncher.launch("application/pdf") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(selectedFileName!!, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                Text("Tap to Select PDF Document", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (isProcessing) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        outputOrganizedFile?.let { file ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Document Reorganized!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Saved to Anegan/Documents folder.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(file.name, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (pageIndices.isNotEmpty() && outputOrganizedFile == null) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Arrange Pages Below", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))

            // Display Pages List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pageIndices.forEachIndexed { listIndex, originalIndex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Thumbnail image
                                val bmp = pageThumbnails[originalIndex]
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page Thumbnail",
                                        modifier = Modifier
                                            .size(width = 60.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 60.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text("Page ${listIndex + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Source index: ${originalIndex + 1}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }

                            // Organize Actions
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        if (listIndex > 0) {
                                            val temp = pageIndices[listIndex]
                                            pageIndices[listIndex] = pageIndices[listIndex - 1]
                                            pageIndices[listIndex - 1] = temp
                                        }
                                    },
                                    enabled = listIndex > 0,
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("▲", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        if (listIndex < pageIndices.size - 1) {
                                            val temp = pageIndices[listIndex]
                                            pageIndices[listIndex] = pageIndices[listIndex + 1]
                                            pageIndices[listIndex + 1] = temp
                                        }
                                    },
                                    enabled = listIndex < pageIndices.size - 1,
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("▼", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        pageIndices.removeAt(listIndex)
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val file = tempPdfFile
                    if (file == null) {
                        Toast.makeText(context, "Internal file reference missing", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (pageIndices.isEmpty()) {
                        Toast.makeText(context, "Page list cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isProcessing = true

                    coroutineScope.launch {
                        try {
                            val result = PdfPageManager().reorderOrDeletePages(file, pageIndices.toList())
                            isProcessing = false

                            if (result.isSuccess) {
                                val organizedFile = result.getOrThrow()
                                outputOrganizedFile = organizedFile
                                Toast.makeText(context, "PDF organized successfully!", Toast.LENGTH_SHORT).show()

                                // Log entry in conversion history
                                val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                historyDao.insertConversion(
                                    ConversionHistoryEntity(
                                        originalFileName = selectedFileName ?: file.name,
                                        outputFileName = organizedFile.name,
                                        originalFormat = "PDF",
                                        outputFormat = "PDF",
                                        status = "SUCCESS",
                                        timestamp = System.currentTimeMillis(),
                                        originalSize = selectedFileSize ?: file.length(),
                                        outputSize = organizedFile.length(),
                                        outputPath = organizedFile.absolutePath
                                    )
                                )
                            } else {
                                val ex = result.exceptionOrNull()
                                Toast.makeText(context, "Reorder failed: ${ex?.message}", Toast.LENGTH_LONG).show()
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite)
            ) {
                Text("Reorganize & Save PDF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
