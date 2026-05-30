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
import androidx.compose.foundation.isSystemInDarkTheme
import com.anegan.core.conversion.StorageManager
import com.anegan.core.designsystem.theme.*
import com.anegan.feature.conversion.worker.MediaConversionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaConversionScreen(
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val isVideo = categoryName == "Video"
    var selectedFormat by remember { mutableStateOf(if (isVideo) "MP4" else "MP3") }
    var selectedResolution by remember { mutableStateOf("Original") }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var isConverting by remember { mutableStateOf(false) }
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
                    isConverting = false
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

    val mediaPickerLauncher = rememberLauncherForActivityResult(
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

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "$categoryName Settings",
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Media Picker Box wrapped in premium GlassCard
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable {
                            val mimeType = if (isVideo) "video/*" else "audio/*"
                            mediaPickerLauncher.launch(mimeType)
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
                                    imageVector = if (isVideo) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                                    contentDescription = null,
                                    tint = NeonMagenta,
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
                                    imageVector = if (isVideo) Icons.Rounded.VideoLibrary else Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = NeonMagenta.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Tap to Select Media File",
                                    color = NeonMagenta,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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

                // target format selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = if (isVideo) listOf("MP4", "MKV", "AVI") else listOf("MP3", "M4A", "FLAC")
                    formats.forEach { format ->
                        Box(modifier = Modifier.weight(1f)) {
                            NovaChip(
                                text = format,
                                selected = (selectedFormat == format),
                                onClick = { selectedFormat = format },
                                neonColor = NeonMagenta,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (isVideo) {
                    Text(
                        text = "Resolution",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = if (isDark) NovaFrostWhite else NovaDeepInk
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Original", "1080p", "720p").forEach { res ->
                            Box(modifier = Modifier.weight(1f)) {
                                NovaChip(
                                    text = res,
                                    selected = (selectedResolution == res),
                                    onClick = { selectedResolution = res },
                                    neonColor = NeonMagenta,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Progress Indicator
                if (isConverting) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Converting Media...",
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

                Spacer(modifier = Modifier.weight(1f))

                // Convert Button
                val action = if (categoryName == "Audio") "Extract" else "Convert"
                NovaPrimaryButton(
                    text = "$action to $selectedFormat",
                    neonColor = NeonMagenta,
                    onClick = {
                        val uri = selectedUri
                        if (uri == null) {
                            Toast.makeText(context, "Please select a media file first", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@NovaPrimaryButton
                        }

                        isConverting = true
                        progress = 0f
                        coroutineScope.launch {
                            try {
                                val tempFile = withContext(Dispatchers.IO) {
                                    StorageManager.copyUriToTempFile(context, uri)
                                }
                                if (tempFile == null) {
                                    isConverting = false
                                    Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                val operation = if (isVideo) "CONVERT_VIDEO" else "EXTRACT_AUDIO"
                                val targetResolution = if (isVideo) {
                                    when (selectedResolution) {
                                        "1080p" -> "1920:1080"
                                        "720p" -> "1280:720"
                                        else -> null
                                    }
                                } else null

                                val workRequest = OneTimeWorkRequestBuilder<MediaConversionWorker>()
                                    .setInputData(
                                        workDataOf(
                                            "operation" to operation,
                                            "tempFilePath" to tempFile.absolutePath,
                                            "originalFileName" to (selectedFileName ?: tempFile.name),
                                            "originalFileSize" to (selectedFileSize ?: tempFile.length()),
                                            "outputFormat" to selectedFormat,
                                            "targetFormat" to selectedFormat,
                                            "targetResolution" to targetResolution
                                        )
                                    )
                                    .build()
                                
                                WorkManager.getInstance(context).enqueue(workRequest)
                                currentWorkId = workRequest.id
                            } catch (e: Exception) {
                                isConverting = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isConverting,
                    isLoading = isConverting
                )
            }
        }
    }
}
