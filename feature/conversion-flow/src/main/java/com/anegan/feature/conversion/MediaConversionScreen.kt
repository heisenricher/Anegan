package com.anegan.feature.conversion

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun MediaConversionScreen(
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isVideo = categoryName == "Video"
    var selectedFormat by remember { mutableStateOf(if (isVideo) "MP4" else "MP3") }
    var selectedResolution by remember { mutableStateOf("Original") }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var isConverting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

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
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$categoryName Settings",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // File Picker Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    val mimeType = if (isVideo) "video/*" else "audio/*"
                    mediaPickerLauncher.launch(mimeType)
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 16.sp)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = MidnightIndigo, fontSize = 12.sp)
                }
            } else {
                Text("Tap to Select Media File", color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Target Format", style = MaterialTheme.typography.titleLarge, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val formats = if (isVideo) listOf("MP4", "MKV", "AVI") else listOf("MP3", "M4A", "FLAC")
            formats.forEach { format ->
                Button(
                    onClick = { selectedFormat = format },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFormat == format) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedFormat == format) PureWhite else MidnightIndigo
                    )
                ) {
                    Text(format)
                }
            }
        }

        if (isVideo) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Resolution", style = MaterialTheme.typography.titleLarge, color = MidnightIndigo)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Original", "1080p", "720p").forEach { res ->
                    Button(
                        onClick = { selectedResolution = res },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedResolution == res) MidnightIndigo else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedResolution == res) PureWhite else MidnightIndigo
                        )
                    ) {
                        Text(res)
                    }
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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val uri = selectedUri
                if (uri == null) {
                    Toast.makeText(context, "Please select a media file first", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isConverting = true
                progress = 0f
                coroutineScope.launch {
                    try {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isConverting = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val converter = com.anegan.core.conversion.FFmpegMediaConverter()
                        val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                        val result = if (isVideo) {
                            val resParam = when (selectedResolution) {
                                "1080p" -> "1920:1080"
                                "720p" -> "1280:720"
                                else -> null
                            }
                            val options = com.anegan.core.conversion.VideoConversionOptions(
                                outputFormat = selectedFormat.lowercase(),
                                targetResolution = resParam
                            )
                            converter.convertVideo(tempFile, options) { p ->
                                progress = p
                            }
                        } else {
                            val options = com.anegan.core.conversion.AudioExtractionOptions(
                                targetFormat = selectedFormat.lowercase()
                            )
                            converter.extractAudio(tempFile, options) { p ->
                                progress = p
                            }
                        }

                        isConverting = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = outFile.name,
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = selectedFormat,
                                    status = "SUCCESS",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSize ?: tempFile.length(),
                                    outputSize = outFile.length(),
                                    outputPath = outFile.absolutePath
                                )
                            )
                            selectedUri = null
                            selectedFileName = null
                            selectedFileSize = null
                        } else {
                            val ex = result.exceptionOrNull()
                            Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = "",
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = selectedFormat,
                                    status = "FAILED",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSize ?: tempFile.length(),
                                    outputSize = 0,
                                    outputPath = ""
                                )
                            )
                        }
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
            val action = if (categoryName == "Audio") "Extract" else "Convert"
            if (isConverting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("$action to $selectedFormat", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
