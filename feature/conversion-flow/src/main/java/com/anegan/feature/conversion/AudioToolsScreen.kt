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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import java.util.Locale
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
fun AudioToolsScreen(
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

    // Trim states
    var trimStart by remember { mutableStateOf(presetParams?.get("trimStart") ?: "00:00") }
    var trimEnd by remember { mutableStateOf(presetParams?.get("trimEnd") ?: "00:30") }

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

    BackHandler {
        onBack()
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Audio Cutter",
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
                // Interactive File Picker inside premium GlassCard
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            NovaHaptics.click(view)
                            audioPickerLauncher.launch("audio/*")
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
                                    imageVector = Icons.Rounded.MusicNote,
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
                                    text = String.format(Locale.ROOT, "%.2f MB", sizeMb),
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
                                    imageVector = Icons.Rounded.AudioFile,
                                    contentDescription = null,
                                    tint = NeonMagenta.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Tap to Select Audio File",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonMagenta,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Configuration Panel
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    neonAccent = NeonMagenta.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "TRIM / RINGTONE SETTINGS",
                            style = NovaTypography.tagMono,
                            color = NeonMagenta
                        )
                        
                        Text(
                            text = "START TIME",
                            style = NovaTypography.tagMono,
                            color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f)
                        )
                        NovaTextField(
                            value = trimStart,
                            onValueChange = { trimStart = it },
                            placeholder = "00:00",
                            neonColor = NeonMagenta,
                            leadingIcon = Icons.Rounded.PlayArrow,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = "END TIME",
                            style = NovaTypography.tagMono,
                            color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f)
                        )
                        NovaTextField(
                            value = trimEnd,
                            onValueChange = { trimEnd = it },
                            placeholder = "00:30",
                            neonColor = NeonMagenta,
                            leadingIcon = Icons.Rounded.Stop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Active Downloader progress
                AnimatedVisibility(visible = isProcessing) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonMagenta,
                        enableGlow = true
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Processing Audio...",
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isDark) NovaFrostWhite else NovaDeepInk
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeonMagenta
                                )
                            }
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonMagenta,
                                trackColor = NeonMagenta.copy(alpha = 0.12f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action primary button
                NovaPrimaryButton(
                    text = "Cut / Trim Audio",
                    neonColor = NeonMagenta,
                    enabled = !isProcessing,
                    isLoading = isProcessing,
                    onClick = {
                        val uri = selectedUri
                        if (uri == null) {
                            NovaHaptics.reject(view)
                            Toast.makeText(context, "Please select an audio file first", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@NovaPrimaryButton
                        }

                        val start = parseTimeToSeconds(trimStart)
                        val end = parseTimeToSeconds(trimEnd)
                        if (start == null || end == null || start >= end) {
                            NovaHaptics.reject(view)
                            Toast.makeText(context, "Invalid start or end time", Toast.LENGTH_LONG).show()
                            return@NovaPrimaryButton
                        }

                        isProcessing = true
                        progress = 0f
                        NovaHaptics.longPress(view)

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

                                val workRequest = OneTimeWorkRequestBuilder<MediaConversionWorker>()
                                    .setInputData(
                                        workDataOf(
                                            "operation" to "TRIM_AUDIO",
                                            "tempFilePath" to tempFile.absolutePath,
                                            "originalFileName" to (selectedFileName ?: tempFile.name),
                                            "originalFileSize" to (selectedFileSize ?: tempFile.length()),
                                            "startTime" to start,
                                            "endTime" to end
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
