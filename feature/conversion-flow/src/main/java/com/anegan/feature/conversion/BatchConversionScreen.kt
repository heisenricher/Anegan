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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anegan.core.conversion.StorageManager
import com.anegan.core.designsystem.theme.*
import com.anegan.feature.conversion.worker.ImageBatchConversionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class BatchFileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val status: String = "Pending", // "Pending", "Converting", "Success", "Failed"
    val progress: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchConversionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var fileItems by remember { mutableStateOf(listOf<BatchFileItem>()) }
    var selectedFormat by remember { mutableStateOf("JPG") }
    var isProcessing by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf(0f) }
    var currentWorkId by remember { mutableStateOf<UUID?>(null) }

    var compressionMode by remember { mutableStateOf(CompressionMode.QUALITY_RESOLUTION) }
    var quality by remember { mutableStateOf(80) }
    var targetSizeMb by remember { mutableStateOf("1.0") }

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
                    batchProgress = progressVal / 100f
                }
                
                if (workInfo.state.isFinished) {
                    isProcessing = false
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val successCount = workInfo.outputData.getInt("successCount", 0)
                        val totalCount = workInfo.outputData.getInt("totalCount", 0)
                        Toast.makeText(context, "Successfully converted $successCount/$totalCount files!", Toast.LENGTH_LONG).show()
                        fileItems = fileItems.map { it.copy(status = "Success") }
                        NovaHaptics.success(view)
                    } else {
                        val err = workInfo.outputData.getString("error") ?: "Failed"
                        Toast.makeText(context, "Batch Failed: $err", Toast.LENGTH_LONG).show()
                        fileItems = fileItems.map { it.copy(status = "Failed") }
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
            NovaHaptics.click(view)
        }
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Batch Processing",
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
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive File Picker inside premium GlassCard
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable {
                            NovaHaptics.click(view)
                            batchPickerLauncher.launch("image/*")
                        },
                    neonAccent = NeonMagenta,
                    enableGlow = fileItems.isNotEmpty()
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
                                tint = NeonMagenta.copy(alpha = if (fileItems.isNotEmpty()) 0.8f else 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            if (fileItems.isNotEmpty()) {
                                Text(
                                    text = "${fileItems.size} Images Selected (Tap to Change)",
                                    color = NeonMagenta,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    text = "Tap to Select Multiple Images",
                                    color = NeonMagenta,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Target Format",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = if (isDark) NovaFrostWhite else NovaDeepInk
                )

                // Row of custom chips for format selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("JPG", "PNG", "WEBP", "PDF")
                    formats.forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        Box(modifier = Modifier.weight(1f)) {
                            NovaChip(
                                text = fmt,
                                selected = isSelected,
                                onClick = { selectedFormat = fmt },
                                neonColor = NeonMagenta,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (selectedFormat != "PDF") {
                    AdvancedSizeResolutionController(
                        initialMode = compressionMode,
                        initialQuality = quality,
                        initialTargetSizeMb = targetSizeMb,
                        onModeChanged = { compressionMode = it },
                        onQualityChanged = { quality = it },
                        onScaleChanged = {},
                        onResolutionChanged = { _, _ -> },
                        onTargetSizeChanged = { targetSizeMb = it },
                        neonAccent = NeonMagenta
                    )
                }

                // Staggered list of images inside Thin GlassCard rows
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(fileItems) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeMb = item.size / (1024f * 1024f)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.ROOT, "%.2f MB", sizeMb),
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Status: ${item.status}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (item.status) {
                                                "Success" -> Color(0xFF4CAF50)
                                                "Failed" -> Color(0xFFF44336)
                                                "Converting" -> NeonMagenta
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                    
                                    if (item.status == "Converting") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = NeonMagenta,
                                            trackColor = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                                
                                Box(modifier = Modifier.padding(start = 8.dp)) {
                                    when (item.status) {
                                        "Success" -> Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                                        "Failed" -> Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFF44336), modifier = Modifier.size(22.dp))
                                        "Converting" -> CircularProgressIndicator(color = NeonMagenta, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        else -> Icon(Icons.Rounded.HourglassEmpty, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Batch progress linear indicator
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Converting Batch...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                            Text(
                                text = "${(batchProgress * 100).toInt()}%",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonMagenta
                            )
                        }
                        LinearProgressIndicator(
                            progress = batchProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonMagenta,
                            trackColor = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f)
                        )
                    }
                }

                // Convert Button
                NovaPrimaryButton(
                    text = "Process Batch",
                    neonColor = NeonMagenta,
                    onClick = {
                        if (fileItems.isEmpty()) {
                            Toast.makeText(context, "Please select images first", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@NovaPrimaryButton
                        }

                        val targetSizeBytes = if (selectedFormat != "PDF" && compressionMode == CompressionMode.TARGET_SIZE) {
                            val sizeVal = targetSizeMb.toDoubleOrNull()
                            if (sizeVal == null || sizeVal <= 0) {
                                Toast.makeText(context, "Please enter a valid target size (MB)", Toast.LENGTH_SHORT).show()
                                return@NovaPrimaryButton
                            }
                            (sizeVal * 1024 * 1024).toLong()
                        } else {
                            -1L
                        }

                        isProcessing = true
                        batchProgress = 0f
                        fileItems = fileItems.map { it.copy(status = "Converting") }

                        coroutineScope.launch {
                            try {
                                val tempFiles = withContext(Dispatchers.IO) {
                                    fileItems.mapNotNull { item ->
                                        StorageManager.copyUriToTempFile(context, item.uri)
                                    }
                                }
                                if (tempFiles.isEmpty()) {
                                    isProcessing = false
                                    Toast.makeText(context, "Failed to resolve any files", Toast.LENGTH_SHORT).show()
                                    fileItems = fileItems.map { it.copy(status = "Failed") }
                                    return@launch
                                }

                                val tempPaths = tempFiles.joinToString(",") { it.absolutePath }
                                val originalNames = fileItems.joinToString(",") { it.name }
                                val originalSizes = fileItems.joinToString(",") { it.size.toString() }

                                val workRequest = OneTimeWorkRequestBuilder<ImageBatchConversionWorker>()
                                    .setInputData(
                                        workDataOf(
                                            "isBatch" to true,
                                            "tempFilePaths" to tempPaths,
                                            "originalFileNames" to originalNames,
                                            "originalFileSizes" to originalSizes,
                                            "format" to selectedFormat,
                                            "quality" to quality,
                                            "targetSizeBytes" to targetSizeBytes
                                        )
                                    )
                                    .build()

                                WorkManager.getInstance(context).enqueue(workRequest)
                                currentWorkId = workRequest.id
                            } catch (e: Exception) {
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                fileItems = fileItems.map { it.copy(status = "Failed") }
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
