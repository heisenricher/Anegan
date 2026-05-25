package com.anegan.feature.conversion

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.NativeImageConverter
import com.anegan.core.conversion.ImageConversionOptions
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun ConversionFlowScreen(
    categoryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var quality by remember { mutableStateOf(0.8f) }
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    var isConverting by remember { mutableStateOf(false) }

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
    ) {
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
                text = "$categoryName Conversion",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // File Picker UI Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selectedFileName!!, color = MidnightIndigo, fontSize = 16.sp)
                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                    Text(String.format("%.2f MB", sizeMb), color = MidnightIndigo, fontSize = 12.sp)
                }
            } else {
                Text("Tap to Select Image File", color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SizeController(
            initialQuality = quality,
            onQualityChanged = { quality = it }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val uri = selectedUri
                if (uri == null) {
                    Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isConverting = true
                coroutineScope.launch {
                    try {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isConverting = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        
                        val targetFormat = categoryName.uppercase() // e.g. "PNG", "WEBP", "JPEG"
                        val targetSize = (selectedFileSize ?: 1024L) * quality
                        
                        val options = ImageConversionOptions(
                            format = targetFormat,
                            quality = (quality * 100).toInt(),
                            targetSizeBytes = targetSize.toLong()
                        )
                        
                        val converter = NativeImageConverter()
                        val result = converter.convertImage(tempFile, options)
                        
                        isConverting = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                            
                            val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = outFile.name,
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = targetFormat,
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
                            
                            val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileName ?: tempFile.name,
                                    outputFileName = "",
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = targetFormat,
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
            if (isConverting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Convert File", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
