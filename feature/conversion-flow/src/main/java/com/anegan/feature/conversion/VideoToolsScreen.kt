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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
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
    val view = LocalView.current
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

    // Trim states
    var trimStart by remember { mutableStateOf(presetParams?.get("trimStart") ?: "00:00") }
    var trimEnd by remember { mutableStateOf(presetParams?.get("trimEnd") ?: "00:10") }

    // Compress states
    var crfValue by remember {
        val crfVal = presetParams?.get("crf")?.toFloatOrNull() ?: 28f
        mutableStateOf(crfVal)
    }
    var selectedResolution by remember { mutableStateOf(presetParams?.get("resolution") ?: "Original") }
    var compressWithTargetSize by remember { mutableStateOf(false) }
    var targetVideoSizeMb by remember { mutableStateOf("") }

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
            NovaHaptics.click(view)
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

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Video Tools",
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Glass File Picker Box
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            NovaHaptics.click(view)
                            videoPickerLauncher.launch("video/*")
                        },
                    neonAccent = NeonMagenta,
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
                                    imageVector = Icons.Rounded.VideoFile,
                                    contentDescription = null,
                                    tint = NeonMagenta,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = selectedFileName!!,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                                Text(
                                    text = String.format(java.util.Locale.ROOT, "%.2f MB", sizeMb),
                                    color = NeonMagenta,
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
                                    imageVector = Icons.Rounded.VideoLibrary,
                                    contentDescription = null,
                                    tint = NeonMagenta.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Tap to Select Video File",
                                    color = NeonMagenta,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Segmented Tool Switcher
                val tabs = listOf("Trim", "Compress", "Speed", "To GIF")
                val activeIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
                
                NovaSegmentedControl(
                    items = tabs,
                    selectedIndex = activeIndex,
                    onIndexSelected = { index ->
                        activeTab = tabs[index]
                    },
                    neonColor = NeonMagenta
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
                            "Trim" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Trim Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonMagenta
                                    )
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Start Time",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        NovaTextField(
                                            value = trimStart,
                                            onValueChange = { trimStart = it },
                                            placeholder = "MM:SS or Seconds (e.g. 00:00)",
                                            neonColor = NeonMagenta
                                        )
                                    }
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "End Time",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        NovaTextField(
                                            value = trimEnd,
                                            onValueChange = { trimEnd = it },
                                            placeholder = "MM:SS or Seconds (e.g. 00:10)",
                                            neonColor = NeonMagenta
                                        )
                                    }
                                }
                            }
                            "Compress" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Compression Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonMagenta
                                    )

                                    val compressModes = listOf("Quality Mode", "Target Size Mode")
                                    val cIndex = if (!compressWithTargetSize) 0 else 1
                                    
                                    NovaSegmentedControl(
                                        items = compressModes,
                                        selectedIndex = cIndex,
                                        onIndexSelected = { index ->
                                            compressWithTargetSize = (index == 1)
                                        },
                                        neonColor = NeonMagenta
                                    )

                                    if (!compressWithTargetSize) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Compression Level (CRF)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) NovaFrostWhite else NovaDeepInk
                                                )
                                                Text(
                                                    text = "${crfValue.toInt()}",
                                                    fontFamily = JetBrainsMono,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = NeonMagenta
                                                )
                                            }
                                            Text(
                                                text = "Lower CRF is better quality but larger size",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            NovaSlider(
                                                value = crfValue,
                                                onValueChange = { crfValue = it },
                                                valueRange = 18f..35f,
                                                neonColor = NeonMagenta
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Target Output Size (MB)",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            NovaTextField(
                                                value = targetVideoSizeMb,
                                                onValueChange = { targetVideoSizeMb = it },
                                                placeholder = "e.g. 5.50",
                                                neonColor = NeonMagenta,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Resolution Preset",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val resolutions = listOf("Original", "1080p", "720p", "480p")
                                            resolutions.forEach { res ->
                                                val isResSelected = selectedResolution == res
                                                Box(modifier = Modifier.weight(1f)) {
                                                    NovaChip(
                                                        text = res,
                                                        selected = isResSelected,
                                                        onClick = { selectedResolution = res },
                                                        neonColor = NeonMagenta,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Speed" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Video Speed Control",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonMagenta
                                    )

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Speed Factor",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                                            )
                                            Text(
                                                text = String.format(java.util.Locale.ROOT, "%.2fx", speedFactor),
                                                fontFamily = JetBrainsMono,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NeonMagenta
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        NovaSlider(
                                            value = speedFactor,
                                            onValueChange = { speedFactor = it },
                                            valueRange = 0.25f..4.0f,
                                            steps = 14,
                                            neonColor = NeonMagenta
                                        )
                                    }
                                }
                            }
                            "To GIF" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Video to GIF Maker",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NeonMagenta
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Start Time (Seconds)",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        NovaTextField(
                                            value = gifStart,
                                            onValueChange = { gifStart = it },
                                            placeholder = "e.g. 0.0",
                                            neonColor = NeonMagenta,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Duration (Seconds)",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        NovaTextField(
                                            value = gifDuration,
                                            onValueChange = { gifDuration = it },
                                            placeholder = "e.g. 5.0",
                                            neonColor = NeonMagenta,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "FPS",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            NovaTextField(
                                                value = gifFps,
                                                onValueChange = { gifFps = it },
                                                placeholder = "e.g. 10",
                                                neonColor = NeonMagenta,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Width (px)",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            NovaTextField(
                                                value = gifWidth,
                                                onValueChange = { gifWidth = it },
                                                placeholder = "e.g. 480",
                                                neonColor = NeonMagenta,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Progress indicator
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Processing Video...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NeonMagenta
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonMagenta,
                            trackColor = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                NovaPrimaryButton(
                    text = "Process Video",
                    neonColor = NeonMagenta,
                    onClick = {
                        val uri = selectedUri
                        if (uri == null) {
                            Toast.makeText(context, "Please select a video file first", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@NovaPrimaryButton
                        }

                        // If doing Trim, validate time before launching background worker
                        if (activeTab == "Trim") {
                            val start = parseTimeToSeconds(trimStart)
                            val end = parseTimeToSeconds(trimEnd)
                            if (start == null || end == null || start >= end) {
                                Toast.makeText(context, "Invalid start or end time", Toast.LENGTH_LONG).show()
                                return@NovaPrimaryButton
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

                                val targetSize = if (compressWithTargetSize) (targetVideoSizeMb.toDoubleOrNull() ?: 0.0) else 0.0
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
                                            "width" to width,
                                            "targetSizeMb" to targetSize
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
                    isLoading = isProcessing
                )
            }
        }
    }
}
