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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.conversion.NativeDocumentConverter
import com.anegan.core.conversion.NativeImageConverter
import com.anegan.core.conversion.ImageConversionOptions
import com.anegan.core.conversion.ExifManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anegan.feature.conversion.worker.MediaConversionWorker

enum class DocumentSuiteTab {
    SAME_TO_SAME,
    ANOTHER_FILE,
    COMPILE,
    DE_COMPILE,
    METADATA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentConversionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableStateOf(DocumentSuiteTab.SAME_TO_SAME) }
    var selectedOp by remember { mutableStateOf("") }

    // Files state
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFileNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFileSizes by remember { mutableStateOf<List<Long>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }

    // Advanced Controllers state
    var compressMode by remember { mutableStateOf(CompressionMode.QUALITY_RESOLUTION) }
    var qualityPercent by remember { mutableStateOf(80) }
    var scalePercent by remember { mutableStateOf(0.8f) }
    var exactWidth by remember { mutableStateOf("") }
    var exactHeight by remember { mutableStateOf("") }
    var targetSizeMb by remember { mutableStateOf("") }

    // EXIF metadata
    var exifTags by remember { mutableStateOf<Map<String, Map<String, String>>?>(null) }

    // Split page inputs
    var splitStartPage by remember { mutableStateOf("1") }
    var splitEndPage by remember { mutableStateOf("5") }

    // File pickers
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
            exifTags = null
            
            // Auto-load metadata if in Metadata tab
            if (activeTab == DocumentSuiteTab.METADATA) {
                isExecuting = true
                coroutineScope.launch {
                    try {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile != null) {
                            val result = ExifManager().readExifMetadata(tempFile)
                            if (result.isSuccess) {
                                exifTags = result.getOrThrow()
                            } else {
                                Toast.makeText(context, "No metadata found in this image.", Toast.LENGTH_SHORT).show()
                            }
                            tempFile.delete()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isExecuting = false
                    }
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

    // Set default operations when tab changes
    LaunchedEffect(activeTab) {
        selectedUris = emptyList()
        selectedFileNames = emptyList()
        selectedFileSizes = emptyList()
        exifTags = null
        selectedOp = when (activeTab) {
            DocumentSuiteTab.SAME_TO_SAME -> "PDF Optimize"
            DocumentSuiteTab.ANOTHER_FILE -> "Image to PDF"
            DocumentSuiteTab.COMPILE -> "Merge PDFs"
            DocumentSuiteTab.DE_COMPILE -> "Split PDF"
            DocumentSuiteTab.METADATA -> "Metadata Viewer"
        }
    }

    // Main execution router
    fun executeOperation() {
        if (selectedUris.isEmpty()) {
            Toast.makeText(context, "Please select input file(s)", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Large videos compress is triggered via Background Worker
        if (selectedOp == "Video Optimize") {
            val uri = selectedUris.first()
            isExecuting = true
            coroutineScope.launch {
                try {
                    val tempFile = StorageManager.copyUriToTempFile(context, uri)
                    if (tempFile == null) {
                        isExecuting = false
                        Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val targetSizeDouble = if (compressMode == CompressionMode.TARGET_SIZE) {
                        targetSizeMb.toDoubleOrNull() ?: 0.0
                    } else {
                        0.0
                    }
                    
                    val calculatedCrf = ((100 - qualityPercent) / 100f * 51).toInt().coerceIn(0, 51)
                    val workRequest = OneTimeWorkRequestBuilder<MediaConversionWorker>()
                        .setInputData(
                            workDataOf(
                                "operation" to "COMPRESS_VIDEO",
                                "tempFilePath" to tempFile.absolutePath,
                                "originalFileName" to (selectedFileNames.firstOrNull() ?: tempFile.name),
                                "originalFileSize" to (selectedFileSizes.firstOrNull() ?: tempFile.length()),
                                "crf" to calculatedCrf,
                                "targetSizeMb" to targetSizeDouble
                            )
                        )
                        .build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                    Toast.makeText(context, "Video compression enqueued in background", Toast.LENGTH_LONG).show()
                    selectedUris = emptyList()
                    selectedFileNames = emptyList()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isExecuting = false
                }
            }
            return
        }

        isExecuting = true
        coroutineScope.launch {
            try {
                val docConverter = NativeDocumentConverter()
                val imageConverter = NativeImageConverter()
                val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                var successFile: File? = null
                var originalFormat = ""
                var outputFormat = ""

                when (selectedOp) {
                    "PDF Optimize" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = "PDF"
                            outputFormat = "PDF"
                            val targetSize = if (compressMode == CompressionMode.TARGET_SIZE) {
                                ((targetSizeMb.toFloatOrNull() ?: 1.0f) * 1024 * 1024).toLong()
                            } else {
                                null
                            }
                            val quality = if (compressMode == CompressionMode.QUALITY_RESOLUTION) qualityPercent else null
                            val res = docConverter.compressPdf(tempFile, targetSize, quality)
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFile.delete()
                        }
                    }
                    "Image Optimize" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = tempFile.extension.uppercase()
                            outputFormat = tempFile.extension.uppercase()
                            
                            val targetSize = if (compressMode == CompressionMode.TARGET_SIZE) {
                                ((targetSizeMb.toFloatOrNull() ?: 1.0f) * 1024 * 1024).toLong()
                            } else {
                                null
                            }
                            val options = ImageConversionOptions(
                                format = tempFile.extension,
                                targetSizeBytes = targetSize,
                                exactWidth = exactWidth.toIntOrNull(),
                                exactHeight = exactHeight.toIntOrNull(),
                                quality = qualityPercent
                            )
                            val res = imageConverter.convertImage(tempFile, options)
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFile.delete()
                        }
                    }
                    "Image to PDF" -> {
                        val tempFiles = selectedUris.mapNotNull { StorageManager.copyUriToTempFile(context, it) }
                        if (tempFiles.isNotEmpty()) {
                            originalFormat = "IMAGES"
                            outputFormat = "PDF"
                            val res = docConverter.imagesToPdf(tempFiles)
                            if (res.isSuccess) {
                                val combinedPdf = res.getOrThrow()
                                // If target size is specified, compress it
                                if (compressMode == CompressionMode.TARGET_SIZE) {
                                    val targetSize = ((targetSizeMb.toFloatOrNull() ?: 1.0f) * 1024 * 1024).toLong()
                                    val compressedRes = docConverter.compressPdf(combinedPdf, targetSize, null)
                                    successFile = compressedRes.getOrDefault(combinedPdf)
                                } else {
                                    successFile = combinedPdf
                                }
                            }
                            tempFiles.forEach { it.delete() }
                        }
                    }
                    "PDF to Image" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = "PDF"
                            outputFormat = "JPG"
                            val res = docConverter.pdfToImages(tempFile, "jpg")
                            if (res.isSuccess) {
                                val files = res.getOrThrow()
                                if (files.isNotEmpty()) {
                                    if (files.size == 1) {
                                        successFile = files.first()
                                    } else {
                                        val zipRes = docConverter.zipFiles(files, "PDF_Images_${System.currentTimeMillis()}")
                                        if (zipRes.isSuccess) {
                                            successFile = zipRes.getOrThrow()
                                            outputFormat = "ZIP"
                                            files.forEach { it.delete() }
                                        } else {
                                            successFile = files.first()
                                        }
                                    }
                                }
                            }
                            tempFile.delete()
                        }
                    }
                    "Text/Docx to PDF" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = tempFile.extension.uppercase()
                            outputFormat = "PDF"
                            val res = if (tempFile.extension.lowercase() == "docx") {
                                docConverter.docxToPdf(tempFile)
                            } else {
                                docConverter.convertToPdf(tempFile)
                            }
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFile.delete()
                        }
                    }
                    "EPUB to PDF" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = "EPUB"
                            outputFormat = "PDF"
                            val res = docConverter.epubToPdf(tempFile)
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFile.delete()
                        }
                    }
                    "Merge PDFs" -> {
                        val tempFiles = selectedUris.mapNotNull { StorageManager.copyUriToTempFile(context, it) }
                        if (tempFiles.size >= 2) {
                            originalFormat = "PDF(s)"
                            outputFormat = "PDF"
                            val res = docConverter.mergePdfs(tempFiles, "Merged_${System.currentTimeMillis()}")
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFiles.forEach { it.delete() }
                        } else {
                            Toast.makeText(context, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Zip Files" -> {
                        val tempFiles = selectedUris.mapNotNull { StorageManager.copyUriToTempFile(context, it) }
                        if (tempFiles.isNotEmpty()) {
                            originalFormat = "FILES"
                            outputFormat = "ZIP"
                            val res = docConverter.zipFiles(tempFiles, "Archive_${System.currentTimeMillis()}")
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFiles.forEach { it.delete() }
                        }
                    }
                    "Split PDF" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = "PDF"
                            outputFormat = "PDF"
                            val start = splitStartPage.toIntOrNull() ?: 1
                            val end = splitEndPage.toIntOrNull() ?: 5
                            val res = docConverter.splitPdf(tempFile, start, end)
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFile.delete()
                        }
                    }
                    "Unzip File" -> {
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = "ZIP"
                            outputFormat = "FOLDER"
                            val res = docConverter.unzipFile(tempFile)
                            if (res.isSuccess) successFile = res.getOrThrow()
                            tempFile.delete()
                        }
                    }
                    "Metadata Viewer" -> {
                        // Stripping EXIF tags action
                        val tempFile = StorageManager.copyUriToTempFile(context, selectedUris.first())
                        if (tempFile != null) {
                            originalFormat = tempFile.extension.uppercase()
                            outputFormat = tempFile.extension.uppercase()
                            val res = ExifManager().stripExifMetadata(tempFile)
                            if (res.isSuccess) {
                                successFile = res.getOrThrow()
                                exifTags = emptyMap()
                            }
                            tempFile.delete()
                        }
                    }
                }

                if (successFile != null) {
                    Toast.makeText(context, "Saved to ${successFile.absolutePath}", Toast.LENGTH_LONG).show()
                    historyDao.insertConversion(
                        ConversionHistoryEntity(
                            originalFileName = selectedFileNames.firstOrNull() ?: "Source",
                            outputFileName = successFile.name,
                            originalFormat = originalFormat,
                            outputFormat = outputFormat,
                            status = "SUCCESS",
                            timestamp = System.currentTimeMillis(),
                            originalSize = selectedFileSizes.sum(),
                            outputSize = successFile.length(),
                            outputPath = successFile.absolutePath
                        )
                    )
                    selectedUris = emptyList()
                    selectedFileNames = emptyList()
                    selectedFileSizes = emptyList()
                } else {
                    Toast.makeText(context, "Operation failed. Please verify files are correct.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isExecuting = false
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
        // Sleek Back Button + Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            ) {
                Text("←", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Document Suite",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Apple-style Tabs Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val tabs = listOf(
                DocumentSuiteTab.SAME_TO_SAME to "Same to Same",
                DocumentSuiteTab.ANOTHER_FILE to "Convert",
                DocumentSuiteTab.COMPILE to "Compile",
                DocumentSuiteTab.DE_COMPILE to "De-compile",
                DocumentSuiteTab.METADATA to "Metadata"
            )
            tabs.forEach { (tab, label) ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PureWhite else Color.Gray,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Operations Chips Row
        val ops = when (activeTab) {
            DocumentSuiteTab.SAME_TO_SAME -> listOf("PDF Optimize", "Image Optimize", "Video Optimize")
            DocumentSuiteTab.ANOTHER_FILE -> listOf("Image to PDF", "PDF to Image", "Text/Docx to PDF", "EPUB to PDF")
            DocumentSuiteTab.COMPILE -> listOf("Merge PDFs", "Zip Files")
            DocumentSuiteTab.DE_COMPILE -> listOf("Split PDF", "Unzip File")
            DocumentSuiteTab.METADATA -> listOf("Metadata Viewer")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ops.forEach { op ->
                val isSelected = selectedOp == op
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedOp = op }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = op,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // File Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val mime = when (selectedOp) {
                        "PDF Optimize", "PDF to Image", "Split PDF" -> "application/pdf"
                        "Image Optimize", "Metadata Viewer" -> "image/*"
                        "Video Optimize" -> "video/*"
                        "Merge PDFs" -> "application/pdf"
                        "EPUB to PDF" -> "application/epub+zip"
                        "Unzip File" -> "application/zip"
                        else -> "*/*"
                    }
                    if (selectedOp in listOf("Merge PDFs", "Zip Files", "Image to PDF")) {
                        multiFilePickerLauncher.launch(mime)
                    } else {
                        singleFilePickerLauncher.launch(mime)
                    }
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedFileNames.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedFileNames.size == 1) "📄 File Selected" else "📁 ${selectedFileNames.size} Files Selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        selectedFileNames.take(3).forEach { name ->
                            Text(name, color = Color.Gray, fontSize = 12.sp)
                        }
                        if (selectedFileNames.size > 3) {
                            Text("... and ${selectedFileNames.size - 3} more", color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val sizeMb = selectedFileSizes.sum() / (1024f * 1024f)
                        Text(String.format("Total size: %.2f MB", sizeMb), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("➕", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val prompt = when (selectedOp) {
                            "Merge PDFs" -> "Select PDFs to Merge"
                            "Zip Files" -> "Select Files to ZIP"
                            "Split PDF" -> "Select PDF to Split"
                            "Unzip File" -> "Select ZIP File"
                            "Metadata Viewer" -> "Select Image to View EXIF Details"
                            else -> "Select Input File"
                        }
                        Text(prompt, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("100% Secure & Offline", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        // Show EXIF Properties Card in Metadata Tab
        if (activeTab == DocumentSuiteTab.METADATA && exifTags != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("EXIF Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (exifTags!!.isEmpty()) {
                        Text("No properties found in this image.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        exifTags!!.forEach { (category, catMap) ->
                            Text(category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 6.dp))
                            catMap.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, color = Color.Gray, fontSize = 13.sp)
                                    Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                            }
                        }
                    }
                }
            }
        }

        // Sizing & Resolution parameters controller
        val supportsSizing = selectedOp in listOf("PDF Optimize", "Image Optimize", "Video Optimize", "Image to PDF")
        if (supportsSizing && selectedUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            AdvancedSizeResolutionController(
                showResolutionFields = (selectedOp == "Image Optimize"),
                onModeChanged = { compressMode = it },
                onQualityChanged = { qualityPercent = it },
                onScaleChanged = { scalePercent = it },
                onResolutionChanged = { w, h -> exactWidth = w; exactHeight = h },
                onTargetSizeChanged = { targetSizeMb = it }
            )
        }

        // Split PDF pages input
        if (selectedOp == "Split PDF" && selectedUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Split Range", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = splitStartPage,
                            onValueChange = { splitStartPage = it },
                            label = { Text("Start Page") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
                        )
                        OutlinedTextField(
                            value = splitEndPage,
                            onValueChange = { splitEndPage = it },
                            label = { Text("End Page") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Process Action button
        Button(
            onClick = { executeOperation() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedUris.isNotEmpty() && !isExecuting,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                val btnText = when (selectedOp) {
                    "Metadata Viewer" -> "Strip EXIF & Save"
                    else -> "Execute $selectedOp"
                }
                Text(btnText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
