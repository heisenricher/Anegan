/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.compose.foundation.isSystemInDarkTheme
import com.anegan.core.conversion.StorageManager
import com.anegan.core.designsystem.theme.*
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
    val view = LocalView.current
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
                        NovaHaptics.success(view)
                    } else {
                        val err = workInfo.outputData.getString("error") ?: "Failed"
                        Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
                        NovaHaptics.reject(view)
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
            NovaHaptics.click(view)
        }
    }

    val imageListPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages = uris
        if (uris.isNotEmpty()) {
            NovaHaptics.click(view)
        }
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "PDF Tools",
                    onBack = {
                        NovaHaptics.click(view)
                        onBack()
                    },
                    neonAccent = NeonCyan
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
                // Frosted PDF File Picker Box (only visible when not in Images -> PDF mode)
                if (activeTab != "Images → PDF") {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable {
                                NovaHaptics.click(view)
                                pdfPickerLauncher.launch("application/pdf")
                            },
                        neonAccent = NeonCyan,
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
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = null,
                                        tint = NeonCyan,
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
                                        color = NeonCyan,
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
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = null,
                                        tint = NeonCyan.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Tap to Select PDF File",
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Segmented Switcher for 5 tabs
                val tabs = listOf("Split", "Compress", "Encrypt", "To Images", "Images → PDF")
                val activeIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
                
                NovaSegmentedControl(
                    items = tabs,
                    selectedIndex = activeIndex,
                    onIndexSelected = { index ->
                        activeTab = tabs[index]
                    },
                    neonColor = NeonCyan
                )

                // Tool Config Form Container
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    neonAccent = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        when (activeTab) {
                            "Split" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Split PDF Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonCyan
                                    )
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Start Page (1-indexed)", fontSize = 11.sp, color = Color.Gray)
                                        NovaTextField(
                                            value = startPage,
                                            onValueChange = { startPage = it },
                                            placeholder = "e.g. 1",
                                            neonColor = NeonCyan,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("End Page (1-indexed)", fontSize = 11.sp, color = Color.Gray)
                                        NovaTextField(
                                            value = endPage,
                                            onValueChange = { endPage = it },
                                            placeholder = "e.g. 2",
                                            neonColor = NeonCyan,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                }
                            }
                            "Compress" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Compress PDF Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonCyan
                                    )

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Resolution (DPI)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                                            )
                                            Text(
                                                text = "${dpiValue.toInt()} DPI",
                                                fontFamily = JetBrainsMono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NeonCyan
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        NovaSlider(
                                            value = dpiValue,
                                            onValueChange = { dpiValue = it },
                                            valueRange = 72f..300f,
                                            steps = 3,
                                            neonColor = NeonCyan
                                        )
                                    }
                                }
                            }
                            "Encrypt" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Password Protect PDF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonCyan
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Enter PDF Password", fontSize = 11.sp, color = Color.Gray)
                                        NovaTextField(
                                            value = passwordValue,
                                            onValueChange = { passwordValue = it },
                                            placeholder = "Password...",
                                            neonColor = NeonCyan
                                        )
                                    }
                                }
                            }
                            "To Images" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "PDF to Images Converter",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonCyan
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Image Format", color = if (isDark) NovaFrostWhite else NovaDeepInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val formats = listOf("JPG", "PNG")
                                            formats.forEach { fmt ->
                                                val isSelected = imageFormat == fmt
                                                Box(modifier = Modifier.weight(1f)) {
                                                    NovaChip(
                                                        text = fmt,
                                                        selected = isSelected,
                                                        onClick = { imageFormat = fmt },
                                                        neonColor = NeonCyan,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Images → PDF" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Images to PDF Converter",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonCyan
                                    )

                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clickable {
                                                NovaHaptics.click(view)
                                                imageListPickerLauncher.launch("image/*")
                                            },
                                        neonAccent = NeonCyan,
                                        enableGlow = selectedImages.isNotEmpty()
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
                                                    tint = NeonCyan.copy(alpha = if (selectedImages.isNotEmpty()) 0.8f else 0.5f),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                if (selectedImages.isNotEmpty()) {
                                                    Text(
                                                        text = "${selectedImages.size} Images Selected (Tap to Change)",
                                                        color = NeonCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Tap to Select Images",
                                                        color = NeonCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Progress Indicator
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Processing PDF...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                NovaPrimaryButton(
                    text = if (activeTab == "Images → PDF") "Combine to PDF" else "Process PDF",
                    neonColor = NeonCyan,
                    onClick = {
                        if (activeTab != "Images → PDF" && selectedUri == null) {
                            Toast.makeText(context, "Please select a PDF file first", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }
                        if (activeTab == "Images → PDF" && selectedImages.isEmpty()) {
                            Toast.makeText(context, "Please select images first", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@NovaPrimaryButton
                        }

                        // If doing Encrypt, validate password first
                        if (activeTab == "Encrypt" && passwordValue.isBlank()) {
                            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_LONG).show()
                            return@NovaPrimaryButton
                        }

                        // If doing Split, validate page range first
                        if (activeTab == "Split") {
                            val start = startPage.toIntOrNull()
                            val end = endPage.toIntOrNull()
                            if (start == null || end == null || start <= 0 || end < start) {
                                Toast.makeText(context, "Invalid page range", Toast.LENGTH_LONG).show()
                                return@NovaPrimaryButton
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
                    isLoading = isProcessing
                )
            }
        }
    }
}
