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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anegan.core.conversion.StorageManager
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import com.anegan.feature.conversion.worker.ImageBatchConversionWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionFlowScreen(
    categoryName: String,
    onBack: () -> Unit,
    presetParams: Map<String, String>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var quality by remember {
        val qStr = presetParams?.get("quality")
        val qVal = qStr?.toFloatOrNull()?.let { it / 100f } ?: 0.8f
        mutableStateOf(qVal)
    }
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

    // Target format selection (replaces raw target format being equal to screen name)
    var targetFormat by remember {
        mutableStateOf(presetParams?.get("targetFormat") ?: if (categoryName == "Images") "JPG" else categoryName)
    }

    // Resize inputs
    var resizeWidth by remember { mutableStateOf(presetParams?.get("resizeWidth") ?: "") }
    var resizeHeight by remember { mutableStateOf(presetParams?.get("resizeHeight") ?: "") }

    // Rotation inputs
    var rotationValue by remember {
        val rotVal = presetParams?.get("rotation")?.toFloatOrNull() ?: 0f
        mutableStateOf(rotVal)
    }

    // Crop inputs
    var cropX by remember { mutableStateOf(presetParams?.get("cropX") ?: "") }
    var cropY by remember { mutableStateOf(presetParams?.get("cropY") ?: "") }
    var cropW by remember { mutableStateOf(presetParams?.get("cropW") ?: "") }
    var cropH by remember { mutableStateOf(presetParams?.get("cropH") ?: "") }

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
                color = MidnightIndigo,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = "Image Tools & Converter",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Picker UI Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 15.sp)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Text("Tap to Select Image (JPG, PNG, WEBP, HEIC, AVIF)", color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Target Format selection
        Text("Target Format", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val formats = listOf("JPG", "PNG", "WEBP")
            formats.forEach { fmt ->
                val isSelected = targetFormat == fmt
                Button(
                    onClick = { targetFormat = fmt },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) PureWhite else MidnightIndigo
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fmt)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SizeController(
            initialQuality = quality,
            onQualityChanged = { quality = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Edit tools collapse card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Advanced Resize, Rotate & Crop", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(16.dp))

                // Resize
                Text("Resize Dimensions (Optional)", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = resizeWidth,
                        onValueChange = { resizeWidth = it },
                        label = { Text("Width (px)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                    OutlinedTextField(
                        value = resizeHeight,
                        onValueChange = { resizeHeight = it },
                        label = { Text("Height (px)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rotation
                Text("Rotation (Optional)", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rotations = listOf(0f, 90f, 180f, 270f)
                    rotations.forEach { rot ->
                        val isSelected = rotationValue == rot
                        Button(
                            onClick = { rotationValue = rot },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.background,
                                contentColor = if (isSelected) PureWhite else MidnightIndigo
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("${rot.toInt()}°", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Crop
                Text("Crop Box (Optional)", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cropX,
                        onValueChange = { cropX = it },
                        label = { Text("X Offset") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                    OutlinedTextField(
                        value = cropY,
                        onValueChange = { cropY = it },
                        label = { Text("Y Offset") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cropW,
                        onValueChange = { cropW = it },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                    OutlinedTextField(
                        value = cropH,
                        onValueChange = { cropH = it },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MidnightIndigo, focusedLabelColor = MidnightIndigo)
                    )
                }
            }
        }

        if (isConverting) {
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
                    Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@Button
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
                        
                        val targetSize = (selectedFileSize ?: 1024L) * quality
                        val rWidth = resizeWidth.toIntOrNull() ?: -1
                        val rHeight = resizeHeight.toIntOrNull() ?: -1
                        val cX = cropX.toIntOrNull() ?: -1
                        val cY = cropY.toIntOrNull() ?: -1
                        val cW = cropW.toIntOrNull() ?: -1
                        val cH = cropH.toIntOrNull() ?: -1

                        val workRequest = OneTimeWorkRequestBuilder<ImageBatchConversionWorker>()
                            .setInputData(
                                workDataOf(
                                    "isBatch" to false,
                                    "tempFilePath" to tempFile.absolutePath,
                                    "originalFileName" to (selectedFileName ?: tempFile.name),
                                    "originalFileSize" to (selectedFileSize ?: tempFile.length()),
                                    "format" to targetFormat,
                                    "quality" to (quality * 100).toInt(),
                                    "targetSizeBytes" to targetSize.toLong(),
                                    "exactWidth" to rWidth,
                                    "exactHeight" to rHeight,
                                    "rotationDegrees" to rotationValue,
                                    "cropX" to cX,
                                    "cropY" to cY,
                                    "cropW" to cW,
                                    "cropH" to cH
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
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            if (isConverting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Process Image", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
