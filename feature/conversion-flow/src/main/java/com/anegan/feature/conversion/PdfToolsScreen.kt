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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.anegan.core.conversion.StorageManager
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anegan.feature.conversion.worker.DocumentConversionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    onBack: () -> Unit,
    presetParams: Map<String, String>? = null,
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
    var currentWorkId by remember { mutableStateOf<UUID?>(null) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required to show progress", Toast.LENGTH_SHORT).show()
        }
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(currentWorkId) {
        val id = currentWorkId ?: return@DisposableEffect onDispose {}
        val liveData = WorkManager.getInstance(context).getWorkInfoByIdLiveData(id)
        val observer = androidx.lifecycle.Observer<WorkInfo> { workInfo ->
            if (workInfo != null) {
                val progressVal = workInfo.progress.getInt("progress", -1)
                if (progressVal >= 0) {
                    progress = progressVal / 100f
                }
                
                if (workInfo.state.isFinished) {
                    isProcessing = false
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val outName = workInfo.outputData.getString("outputFileName") ?: "file"
                        Toast.makeText(context, "Saved to $outName", Toast.LENGTH_LONG).show()
                        selectedUri = null
                        selectedFileName = null
                        selectedFileSize = null
                    } else {
                        val err = workInfo.outputData.getString("error") ?: "Failed"
                        Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
                    }
                    currentWorkId = null
                }
            }
        }
        liveData.observe(lifecycleOwner, observer)
        onDispose {
            liveData.removeObserver(observer)
        }
    }

    var activeTab by remember { mutableStateOf(presetParams?.get("tab") ?: "Split") }


    // Split states
    var startPage by remember { mutableStateOf(presetParams?.get("startPage") ?: "1") }
    var endPage by remember { mutableStateOf(presetParams?.get("endPage") ?: "2") }

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
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Go back to dashboard" }
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                    color = MidnightIndigo
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
                    .semantics { contentDescription = if (selectedFileName != null) "Selected PDF: $selectedFileName" else "Select a PDF file from device storage" }
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
                        .semantics { contentDescription = "Switch to $tab mode" }
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
                            colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo),
                            modifier = Modifier.semantics { contentDescription = "Resolution slider, active value ${dpiValue.toInt()} DPI" }
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
                                .semantics { contentDescription = if (selectedImages.isNotEmpty()) "${selectedImages.size} images selected. Tap to change selection" else "Select images to combine into PDF" }
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
                if (activeTab != "Images → PDF" && selectedUri == null) {
                    Toast.makeText(context, "Please select a PDF file first", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (activeTab == "Images → PDF" && selectedImages.isEmpty()) {
                    Toast.makeText(context, "Please select images first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@Button
                }

                // If doing Encrypt, validate password first
                if (activeTab == "Encrypt" && passwordValue.isBlank()) {
                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_LONG).show()
                    return@Button
                }

                // If doing Split, validate page range first
                if (activeTab == "Split") {
                    val start = startPage.toIntOrNull()
                    val end = endPage.toIntOrNull()
                    if (start == null || end == null || start <= 0 || end < start) {
                        Toast.makeText(context, "Invalid page range", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                }

                isProcessing = true
                progress = 0f

                coroutineScope.launch {
                    try {
                        if (activeTab == "Images → PDF") {
                            val tempFiles = withContext(Dispatchers.IO) {
                                selectedImages.mapNotNull { uri ->
                                    StorageManager.copyUriToTempFile(context, uri)
                                }
                            }
                            if (tempFiles.isEmpty()) {
                                isProcessing = false
                                Toast.makeText(context, "Failed to resolve images", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            val tempPaths = tempFiles.joinToString(",") { it.absolutePath }
                            val totalSize = tempFiles.sumOf { it.length() }
                            
                            val workRequest = OneTimeWorkRequestBuilder<DocumentConversionWorker>()
                                .setInputData(
                                    workDataOf(
                                        "operation" to "IMAGES_TO_PDF",
                                        "tempFilePaths" to tempPaths,
                                        "originalFileName" to "${tempFiles.size} images",
                                        "originalFileSize" to totalSize
                                    )
                                )
                                .build()
                            
                            WorkManager.getInstance(context).enqueue(workRequest)
                            currentWorkId = workRequest.id
                            selectedImages = emptyList()
                        } else {
                            val uri = selectedUri!!
                            val tempFile = withContext(Dispatchers.IO) {
                                StorageManager.copyUriToTempFile(context, uri)
                            }
                            if (tempFile == null) {
                                isProcessing = false
                                Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val operation = when (activeTab) {
                                "Split" -> "SPLIT_PDF"
                                "Compress" -> "COMPRESS_PDF"
                                "Encrypt" -> "ENCRYPT_PDF"
                                "To Images" -> "PDF_TO_IMAGES"
                                else -> throw Exception("Unknown active tab")
                            }

                            val start = startPage.toIntOrNull() ?: 1
                            val end = endPage.toIntOrNull() ?: 1
                            
                            val workRequest = OneTimeWorkRequestBuilder<DocumentConversionWorker>()
                                .setInputData(
                                    workDataOf(
                                        "operation" to operation,
                                        "tempFilePath" to tempFile.absolutePath,
                                        "originalFileName" to (selectedFileName ?: tempFile.name),
                                        "originalFileSize" to (selectedFileSize ?: tempFile.length()),
                                        "startPage" to start,
                                        "endPage" to end,
                                        "dpi" to dpiValue.toInt(),
                                        "password" to passwordValue,
                                        "format" to imageFormat.lowercase()
                                    )
                                )
                                .build()
                            
                            WorkManager.getInstance(context).enqueue(workRequest)
                            currentWorkId = workRequest.id
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
