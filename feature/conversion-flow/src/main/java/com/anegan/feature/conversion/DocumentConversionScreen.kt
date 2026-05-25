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
fun DocumentConversionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTool by remember { mutableStateOf("Convert to PDF") }

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFileNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFileSizes by remember { mutableStateOf<List<Long>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var pdfPassword by remember { mutableStateOf("") }

    val singleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUris = listOf(uri)
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex != -1) selectedFileNames = listOf(it.getString(nameIndex))
                    if (sizeIndex != -1) selectedFileSizes = listOf(it.getLong(sizeIndex))
                }
            }
        }
    }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            val names = mutableListOf<String>()
            val sizes = mutableListOf<Long>()
            uris.forEach { uri ->
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameIndex != -1) names.add(it.getString(nameIndex))
                        if (sizeIndex != -1) sizes.add(it.getLong(sizeIndex))
                    }
                }
            }
            selectedFileNames = names
            selectedFileSizes = sizes
        }
    }

    LaunchedEffect(selectedTool) {
        selectedUris = emptyList()
        selectedFileNames = emptyList()
        selectedFileSizes = emptyList()
    }

    fun executeDocumentTool(password: String = "") {
        if (selectedUris.isEmpty()) {
            Toast.makeText(context, "Please select input file(s)", Toast.LENGTH_SHORT).show()
            return
        }
        isExecuting = true
        coroutineScope.launch {
            try {
                val converter = com.anegan.core.conversion.NativeDocumentConverter()
                val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                when (selectedTool) {
                    "Convert to PDF" -> {
                        val uri = selectedUris.first()
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isExecuting = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val result = converter.convertToPdf(tempFile)
                        isExecuting = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileNames.first(),
                                    outputFileName = outFile.name,
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = "PDF",
                                    status = "SUCCESS",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSizes.first(),
                                    outputSize = outFile.length(),
                                    outputPath = outFile.absolutePath
                                )
                            )
                            selectedUris = emptyList()
                            selectedFileNames = emptyList()
                        } else {
                            val ex = result.exceptionOrNull()
                            Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileNames.first(),
                                    outputFileName = "",
                                    originalFormat = tempFile.extension.uppercase(),
                                    outputFormat = "PDF",
                                    status = "FAILED",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSizes.first(),
                                    outputSize = 0,
                                    outputPath = ""
                                )
                            )
                        }
                    }
                    "Merge" -> {
                        val tempFiles = selectedUris.mapNotNull { uri ->
                            StorageManager.copyUriToTempFile(context, uri)
                        }
                        if (tempFiles.size < 2) {
                            isExecuting = false
                            Toast.makeText(context, "Failed to resolve files (minimum 2 files needed)", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val result = converter.mergePdfs(tempFiles, "Merged_${System.currentTimeMillis()}")
                        isExecuting = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileNames.joinToString(", "),
                                    outputFileName = outFile.name,
                                    originalFormat = "PDF(s)",
                                    outputFormat = "PDF",
                                    status = "SUCCESS",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSizes.sum(),
                                    outputSize = outFile.length(),
                                    outputPath = outFile.absolutePath
                                )
                            )
                            selectedUris = emptyList()
                            selectedFileNames = emptyList()
                        } else {
                            val ex = result.exceptionOrNull()
                            Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileNames.joinToString(", "),
                                    outputFileName = "",
                                    originalFormat = "PDF(s)",
                                    outputFormat = "PDF",
                                    status = "FAILED",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSizes.sum(),
                                    outputSize = 0,
                                    outputPath = ""
                                )
                            )
                        }
                    }
                    "Unlock" -> {
                        val uri = selectedUris.first()
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile == null) {
                            isExecuting = false
                            Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val result = converter.removeProtection(tempFile, password)
                        isExecuting = false
                        if (result.isSuccess) {
                            val outFile = result.getOrThrow()
                            Toast.makeText(context, "Unlocked PDF saved to ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileNames.first(),
                                    outputFileName = outFile.name,
                                    originalFormat = "PDF",
                                    outputFormat = "PDF",
                                    status = "SUCCESS",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSizes.first(),
                                    outputSize = outFile.length(),
                                    outputPath = outFile.absolutePath
                                )
                            )
                            selectedUris = emptyList()
                            selectedFileNames = emptyList()
                        } else {
                            val ex = result.exceptionOrNull()
                            Toast.makeText(context, "Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                            historyDao.insertConversion(
                                ConversionHistoryEntity(
                                    originalFileName = selectedFileNames.first(),
                                    outputFileName = "",
                                    originalFormat = "PDF",
                                    outputFormat = "PDF",
                                    status = "FAILED",
                                    timestamp = System.currentTimeMillis(),
                                    originalSize = selectedFileSizes.first(),
                                    outputSize = 0,
                                    outputPath = ""
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                isExecuting = false
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showPasswordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Password Required") },
            text = {
                Column {
                    Text("Enter password to unlock PDF:")
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pdfPassword,
                        onValueChange = { pdfPassword = it },
                        placeholder = { Text("Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showPasswordDialog = false
                    executeDocumentTool(pdfPassword)
                    pdfPassword = ""
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showPasswordDialog = false
                    pdfPassword = ""
                }) {
                    Text("Cancel")
                }
            }
        )
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
                text = "Document Tools",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Document Picker Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    if (selectedTool == "Merge") {
                        multiFilePickerLauncher.launch("application/pdf")
                    } else if (selectedTool == "Unlock") {
                        singleFilePickerLauncher.launch("application/pdf")
                    } else {
                        // Supports Text documents and images
                        singleFilePickerLauncher.launch("*/*")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileNames.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    selectedFileNames.take(3).forEach { name ->
                        Text(name, color = MidnightIndigo, fontSize = 14.sp)
                    }
                    if (selectedFileNames.size > 3) {
                        Text("... and ${selectedFileNames.size - 3} more", color = MidnightIndigo, fontSize = 12.sp)
                    }
                }
            } else {
                val prompt = when (selectedTool) {
                    "Merge" -> "Tap to Select PDFs to Merge"
                    "Unlock" -> "Tap to Select Password-Protected PDF"
                    else -> "Tap to Select Document (DOCX, TXT, Images)"
                }
                Text(prompt, color = MidnightIndigo)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Select Tool", style = MaterialTheme.typography.titleLarge, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Convert to PDF", "Merge", "Unlock").forEach { tool ->
                Button(
                    onClick = { selectedTool = tool },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTool == tool) MidnightIndigo else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedTool == tool) PureWhite else MidnightIndigo
                    )
                ) {
                    Text(tool)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (selectedTool == "Unlock") {
                    showPasswordDialog = true
                } else {
                    executeDocumentTool()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isExecuting,
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Execute $selectedTool", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
