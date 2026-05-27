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
import com.anegan.feature.conversion.worker.MediaConversionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoToolsScreen(
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

    var activeTab by remember { mutableStateOf(presetParams?.get("tab") ?: "Trim") }
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


    // Trim states
    var trimStart by remember { mutableStateOf(presetParams?.get("trimStart") ?: "00:00") }
    var trimEnd by remember { mutableStateOf(presetParams?.get("trimEnd") ?: "00:10") }

    // Compress states
    var crfValue by remember {
        val crfVal = presetParams?.get("crf")?.toFloatOrNull() ?: 28f
        mutableStateOf(crfVal)
    }
    var selectedResolution by remember { mutableStateOf(presetParams?.get("resolution") ?: "Original") }

    // Speed states
    var speedFactor by remember {
        val spVal = presetParams?.get("speed")?.toFloatOrNull() ?: 1.0f
        mutableStateOf(spVal)
    }

    // GIF states
    var gifStart by remember { mutableStateOf(presetParams?.get("gifStart") ?: "0.0") }
    var gifDuration by remember { mutableStateOf(presetParams?.get("gifDuration") ?: "5.0") }
    var gifFps by remember { mutableStateOf(presetParams?.get("gifFps") ?: "10") }
    var gifWidth by remember { mutableStateOf(presetParams?.get("gifWidth") ?: "480") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
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
                text = "Video Tools",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
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
                .semantics { contentDescription = if (selectedFileName != null) "Selected video: $selectedFileName" else "Select a video file from device storage" }
                .clickable { videoPickerLauncher.launch("video/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 15.sp)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Text("Tap to Select Video File", color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tools Tabs (Row of Custom Pills)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Trim", "Compress", "Speed", "To GIF")
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
                        fontSize = 13.sp
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
                "Trim" -> {
                    Column {
                        Text("Trim Settings", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = trimStart,
                            onValueChange = { trimStart = it },
                            label = { Text("Start Time (MM:SS or Seconds)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MidnightIndigo,
                                focusedLabelColor = MidnightIndigo
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = trimEnd,
                            onValueChange = { trimEnd = it },
                            label = { Text("End Time (MM:SS or Seconds)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MidnightIndigo,
                                focusedLabelColor = MidnightIndigo
                            )
                        )
                    }
                }
                "Compress" -> {
                    Column {
                        Text("Compression Settings", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Quality (CRF): ${crfValue.toInt()} (Lower is better quality)", fontSize = 14.sp, color = MidnightIndigo)
                        Slider(
                            value = crfValue,
                            onValueChange = { crfValue = it },
                            valueRange = 18f..35f,
                            colors = SliderDefaults.colors(
                                thumbColor = MidnightIndigo,
                                activeTrackColor = MidnightIndigo
                            ),
                            modifier = Modifier.semantics { contentDescription = "Video quality compression slider, active CRF value ${crfValue.toInt()} (lower is higher quality)" }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Resolution Preset", fontSize = 14.sp, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val resolutions = listOf("Original", "1080p", "720p", "480p")
                            resolutions.forEach { res ->
                                val isResSelected = selectedResolution == res
                                Button(
                                    onClick = { selectedResolution = res },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isResSelected) MidnightIndigo else MaterialTheme.colorScheme.background,
                                        contentColor = if (isResSelected) PureWhite else MidnightIndigo
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(res, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                "Speed" -> {
                    Column {
                        Text("Video Speed Control", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(String.format("Speed Factor: %.2fx", speedFactor), fontSize = 14.sp, color = MidnightIndigo)
                        Slider(
                            value = speedFactor,
                            onValueChange = { speedFactor = it },
                            valueRange = 0.25f..4.0f,
                            steps = 14, // 0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, etc.
                            colors = SliderDefaults.colors(
                                thumbColor = MidnightIndigo,
                                activeTrackColor = MidnightIndigo
                            ),
                            modifier = Modifier.semantics { contentDescription = "Video speed factor slider, active speed is ${String.format("%.2f", speedFactor)} times" }
                        )
                    }
                }
                "To GIF" -> {
                    Column {
                        Text("Video to GIF Maker", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = gifStart,
                            onValueChange = { gifStart = it },
                            label = { Text("Start Time (Seconds)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = gifDuration,
                            onValueChange = { gifDuration = it },
                            label = { Text("Duration (Seconds)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = gifFps,
                                onValueChange = { gifFps = it },
                                label = { Text("FPS (e.g. 10)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                            )
                            OutlinedTextField(
                                value = gifWidth,
                                onValueChange = { gifWidth = it },
                                label = { Text("Width (px)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                            )
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
                val uri = selectedUri
                if (uri == null) {
                    Toast.makeText(context, "Please select a video file first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@Button
                }

                // If doing Trim, validate time before launching background worker
                if (activeTab == "Trim") {
                    val start = parseTimeToSeconds(trimStart)
                    val end = parseTimeToSeconds(trimEnd)
                    if (start == null || end == null || start >= end) {
                        Toast.makeText(context, "Invalid start or end time", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                }

                isProcessing = true
                progress = 0f

                coroutineScope.launch {
                    try {
                        val tempFile = withContext(Dispatchers.IO) {
                            StorageManager.copyUriToTempFile(context, uri)
                        }
                        if (tempFile == null) {
                            isProcessing = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val operation = when (activeTab) {
                            "Trim" -> "TRIM_VIDEO"
                            "Compress" -> "COMPRESS_VIDEO"
                            "Speed" -> "SPEED_VIDEO"
                            "To GIF" -> "VIDEO_TO_GIF"
                            else -> throw Exception("Unknown active tab")
                        }

                        val resParam = when (selectedResolution) {
                            "1080p" -> "1920:1080"
                            "720p" -> "1280:720"
                            "480p" -> "854:480"
                            else -> null
                        }

                        val startSec = when (activeTab) {
                            "Trim" -> parseTimeToSeconds(trimStart) ?: 0.0
                            "To GIF" -> gifStart.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val endSec = when (activeTab) {
                            "Trim" -> parseTimeToSeconds(trimEnd) ?: 0.0
                            else -> 0.0
                        }

                        val duration = gifDuration.toDoubleOrNull() ?: 5.0
                        val fps = gifFps.toIntOrNull() ?: 10
                        val width = gifWidth.toIntOrNull() ?: 480

                        val workRequest = OneTimeWorkRequestBuilder<MediaConversionWorker>()
                            .setInputData(
                                workDataOf(
                                    "operation" to operation,
                                    "tempFilePath" to tempFile.absolutePath,
                                    "originalFileName" to (selectedFileName ?: tempFile.name),
                                    "originalFileSize" to (selectedFileSize ?: tempFile.length()),
                                    "startTime" to startSec,
                                    "endTime" to endSec,
                                    "crf" to crfValue.toInt(),
                                    "resolution" to resParam,
                                    "speedFactor" to speedFactor,
                                    "duration" to duration,
                                    "fps" to fps,
                                    "width" to width
                                )
                            )
                            .build()

                        WorkManager.getInstance(context).enqueue(workRequest)
                        currentWorkId = workRequest.id
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
                Text("Process Video", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
