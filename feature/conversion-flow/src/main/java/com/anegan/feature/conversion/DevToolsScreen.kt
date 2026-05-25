/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.DevToolsManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableStateOf(0) } // 0: Hash, 1: Base64, 2: QR Code

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
                text = "Developer Tools",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("Hash", "Base64", "QR Tools")
            tabs.forEachIndexed { index, title ->
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MidnightIndigo else Color.Transparent)
                        .clickable { activeTab = index }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) PureWhite else MidnightIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tab content
        when (activeTab) {
            0 -> HashTabContent(context)
            1 -> Base64TabContent(context)
            2 -> QrTabContent(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashTabContent(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    var isTextMode by remember { mutableStateOf(true) } // Text vs File
    var inputText by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("SHA-256") }
    val algorithms = listOf("MD5", "SHA-1", "SHA-256")

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var resultHash by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
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
            resultHash = ""
        }
    }

    Column {
        // Text / File Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isTextMode) Color(0xFFF1F5F9) else Color.Transparent)
                    .clickable { isTextMode = true; resultHash = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Text Hash", color = MidnightIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isTextMode) Color(0xFFF1F5F9) else Color.Transparent)
                    .clickable { isTextMode = false; resultHash = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("File Hash", color = MidnightIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Algorithm Selection
        Text("Select Algorithm", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            algorithms.forEach { algo ->
                val isSelected = selectedAlgorithm == algo
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAlgorithm = algo; resultHash = "" },
                    label = { Text(algo) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MidnightIndigo,
                        selectedLabelColor = PureWhite,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MidnightIndigo
                    ),
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isTextMode) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it; resultHash = "" },
                label = { Text("Input Text") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightIndigo,
                    unfocusedBorderColor = Color.LightGray
                )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { filePickerLauncher.launch("*/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedFileName != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(selectedFileName!!, color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                        Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Text("Tap to Select Any File", color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isProcessing) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MidnightIndigo)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (resultHash.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated Hash ($selectedAlgorithm)", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            "Copy",
                            color = MidnightIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Hash", resultHash)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Hash copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(resultHash, color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (isTextMode) {
                    if (inputText.isBlank()) {
                        Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    coroutineScope.launch {
                        resultHash = DevToolsManager().generateTextHash(inputText, selectedAlgorithm)
                        isProcessing = false
                    }
                } else {
                    val uri = selectedUri
                    if (uri == null) {
                        Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    coroutineScope.launch {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile != null) {
                            resultHash = DevToolsManager().generateFileHash(tempFile, selectedAlgorithm)
                            
                            // Log hash generation success to conversion history database
                            try {
                                val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                historyDao.insertConversion(
                                    ConversionHistoryEntity(
                                        originalFileName = selectedFileName ?: tempFile.name,
                                        outputFileName = "None (Hash)",
                                        originalFormat = tempFile.extension.uppercase(),
                                        outputFormat = selectedAlgorithm,
                                        status = "SUCCESS",
                                        timestamp = System.currentTimeMillis(),
                                        originalSize = selectedFileSize ?: tempFile.length(),
                                        outputSize = 0L,
                                        outputPath = ""
                                    )
                                )
                            } catch (e: Exception) {
                                // ignore DB failures
                            }
                        } else {
                            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                        }
                        isProcessing = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            Text("Generate Hash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Base64TabContent(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    var isEncodeMode by remember { mutableStateOf(true) } // Encode vs Decode
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isEncodeMode) Color(0xFFF1F5F9) else Color.Transparent)
                    .clickable { isEncodeMode = true; outputText = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Encode Text", color = MidnightIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isEncodeMode) Color(0xFFF1F5F9) else Color.Transparent)
                    .clickable { isEncodeMode = false; outputText = "" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Decode Text", color = MidnightIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it; outputText = "" },
            label = { Text(if (isEncodeMode) "Plain Text" else "Base64 String") },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightIndigo,
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (outputText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isEncodeMode) "Encoded Base64" else "Decoded Text", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            "Copy",
                            color = MidnightIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Base64 Result", outputText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(outputText, color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (inputText.isBlank()) {
                    Toast.makeText(context, "Please enter some input text", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isProcessing = true
                coroutineScope.launch {
                    outputText = if (isEncodeMode) {
                        DevToolsManager().encodeTextBase64(inputText)
                    } else {
                        DevToolsManager().decodeTextBase64(inputText)
                    }
                    isProcessing = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            Text(if (isEncodeMode) "Base64 Encode" else "Base64 Decode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QrTabContent(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    var isGeneratorMode by remember { mutableStateOf(true) } // Generate vs Scan
    var qrText by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var scannedResult by remember { mutableStateOf("") }

    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) selectedFileName = it.getString(nameIndex)
                }
            }
            scannedResult = ""
            
            // Automatically scan picked QR code
            isProcessing = true
            coroutineScope.launch {
                try {
                    val result = DevToolsManager().scanQrCode(context, uri)
                    if (result.isSuccess) {
                        scannedResult = result.getOrThrow()
                        Toast.makeText(context, "QR scanned successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        val ex = result.exceptionOrNull()
                        Toast.makeText(context, "Scan failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGeneratorMode) Color(0xFFF1F5F9) else Color.Transparent)
                    .clickable { isGeneratorMode = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Generate QR", color = MidnightIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isGeneratorMode) Color(0xFFF1F5F9) else Color.Transparent)
                    .clickable { isGeneratorMode = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Scan QR", color = MidnightIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isGeneratorMode) {
            OutlinedTextField(
                value = qrText,
                onValueChange = { qrText = it; qrBitmap = null },
                label = { Text("Enter Text or URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightIndigo,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MidnightIndigo)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            qrBitmap?.let { bmp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val outputDir = StorageManager.getAneganOutputDirectory("Images")
                                        val qrFile = File(outputDir, "Anegan_QR_${System.currentTimeMillis()}.png")
                                        val fos = FileOutputStream(qrFile)
                                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                        fos.flush()
                                        fos.close()
                                        
                                        Toast.makeText(context, "QR saved to Anegan/Images folder!", Toast.LENGTH_LONG).show()
                                        
                                        // Save entry in Room history DB
                                        val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                        historyDao.insertConversion(
                                            ConversionHistoryEntity(
                                                originalFileName = "Text Input",
                                                outputFileName = qrFile.name,
                                                originalFormat = "TXT",
                                                outputFormat = "PNG",
                                                status = "SUCCESS",
                                                timestamp = System.currentTimeMillis(),
                                                originalSize = qrText.toByteArray(Charsets.UTF_8).size.toLong(),
                                                outputSize = qrFile.length(),
                                                outputPath = qrFile.absolutePath
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save QR Code to Gallery", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (qrBitmap == null) {
                Button(
                    onClick = {
                        if (qrText.isBlank()) {
                            Toast.makeText(context, "Please enter some text or URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isProcessing = true
                        coroutineScope.launch {
                            val result = DevToolsManager().generateQrCode(qrText)
                            isProcessing = false
                            if (result.isSuccess) {
                                qrBitmap = result.getOrThrow()
                            } else {
                                Toast.makeText(context, "Failed to generate QR", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
                ) {
                    Text("Generate QR Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Scanner View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { qrPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedFileName != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(selectedFileName!!, color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Scanned successfully!", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Text("Tap to Select QR Image", color = MidnightIndigo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isProcessing) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MidnightIndigo)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (scannedResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Decoded QR Content", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                "Copy",
                                color = MidnightIndigo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("QR Decoded", scannedResult)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(scannedResult, color = MidnightIndigo, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
