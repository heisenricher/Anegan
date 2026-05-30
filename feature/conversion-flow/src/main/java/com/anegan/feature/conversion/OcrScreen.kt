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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.anegan.core.conversion.NativeOcrEngine
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var extractedText by remember { mutableStateOf("") }
    
    // OCR Features State
    var selectedLanguage by remember { mutableStateOf("latin") }
    var detectTables by remember { mutableStateOf(false) }
    var isLiveScanMode by remember { mutableStateOf(false) }
    
    // Dropdown state
    var showLangDropdown by remember { mutableStateOf(false) }
    val languages = remember {
        listOf(
            "latin" to "Latin (English, Spanish, etc.)",
            "devanagari" to "Devanagari (Hindi, Sanskrit)",
            "tamil" to "Tamil",
            "telugu" to "Telugu",
            "arabic" to "Arabic",
            "chinese" to "Chinese",
            "japanese" to "Japanese",
            "korean" to "Korean"
        )
    }

    // Camera permission checking
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission required for Live Scan", Toast.LENGTH_SHORT).show()
            isLiveScanMode = false
        }
    }

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
            extractedText = ""
        }
    }

    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Local OCR Scanner",
                    onBack = onBack,
                    neonAccent = NeonCyan
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Options Row (Language Select + Table Detect)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Selector Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        GlassCard(
                            onClick = { showLangDropdown = true },
                            neonAccent = NeonCyan.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = languages.firstOrNull { it.first == selectedLanguage }?.second ?: "Select Language",
                                    fontSize = 13.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Language", tint = NeonCyan)
                            }
                        }

                        DropdownMenu(
                            expanded = showLangDropdown,
                            onDismissRequest = { showLangDropdown = false },
                            modifier = Modifier
                                .background(if (isDark) NovaMidnightBlue else NovaPureWhite)
                                .fillMaxWidth(0.6f)
                        ) {
                            languages.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 13.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk) },
                                    onClick = {
                                        selectedLanguage = code
                                        showLangDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Table Structure Detection Switch Card
                    GlassCard(
                        modifier = Modifier.weight(0.9f),
                        neonAccent = NeonCyan.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Detect Tables", fontSize = 12.sp, color = if (isDark) NovaFrostWhite else NovaDeepInk, fontWeight = FontWeight.Bold)
                            NovaSwitch(
                                checked = detectTables,
                                onCheckedChange = { detectTables = it },
                                neonColor = NeonCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Switch Tabs (Static vs Live Scan)
                val modes = remember { listOf("IMAGE FILE", "LIVE SCANNER") }
                NovaSegmentedControl(
                    items = modes,
                    selectedIndex = if (isLiveScanMode) 1 else 0,
                    onIndexSelected = { idx ->
                        if (idx == 1) {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                isLiveScanMode = true
                            }
                        } else {
                            isLiveScanMode = false
                        }
                    },
                    neonColor = NeonCyan,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Input Content (Static picker or Live camera View)
                if (isLiveScanMode && hasCameraPermission) {
                    val infiniteTransition = rememberInfiniteTransition(label = "laser_scanner")
                    val scanlineFraction by infiniteTransition.animateFloat(
                        initialValue = 0.05f,
                        targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_offset"
                    )

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        neonAccent = NeonCyan,
                        enableGlow = true
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraPreview(
                                modifier = Modifier.fillMaxSize(),
                                onTextDetected = { text ->
                                    extractedText = text
                                },
                                language = selectedLanguage,
                                detectTables = detectTables
                            )

                            // Futuristic Laser Scanner Line Overlay
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val yOffset = (maxHeight.value * scanlineFraction).dp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = yOffset)
                                        .height(3.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    NeonCyan.copy(alpha = 0.2f),
                                                    NeonCyan,
                                                    NeonCyan.copy(alpha = 0.2f)
                                                )
                                            )
                                        )
                                        .neonGlow(NeonCyan, alpha = 0.6f, radiusMultiplier = 0.4f)
                                )
                            }

                            // Overlay HUD instruction
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(12.dp)
                                    .background(NovaMidnightBlue.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "POINT CAMERA AT TEXT TO EXTRACT",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                } else {
                    // Static File Picker Box
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonCyan.copy(alpha = 0.3f),
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedFileName != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                    Text(selectedFileName!!, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                                    Text(String.format("%.2f MB", sizeMb), color = Color.Gray, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tap to change image", color = NeonCyan.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "TAP TO SELECT DOCUMENT IMAGE",
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Extracted Text Display HUD
                if (extractedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Extracted Text Panel
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonCyan,
                        enableGlow = true
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (detectTables) "RECONSTRUCTED TABLE" else "RECOGNIZED OCR TEXT",
                                    fontSize = 12.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Extracted OCR Text", extractedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.12f))
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Simple editable output text area or plain selectable text inside scrollable container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) NovaMidnightBlue.copy(alpha = 0.4f) else NovaCoolGray50.copy(alpha = 0.4f))
                                    .border(BorderStroke(0.5.dp, if (isDark) NovaBorderDark else NovaBorderLight), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = extractedText,
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Button (Only needed/used for static images)
                if (!isLiveScanMode) {
                    NovaPrimaryButton(
                        text = "Extract Text",
                        neonColor = NeonCyan,
                        enabled = !isProcessing && selectedUri != null,
                        isLoading = isProcessing,
                        onClick = {
                            val uri = selectedUri
                            if (uri == null) {
                                Toast.makeText(context, "Please select an image file first", Toast.LENGTH_SHORT).show()
                                return@NovaPrimaryButton
                            }

                            isProcessing = true
                            extractedText = ""

                            coroutineScope.launch {
                                try {
                                    val tempFile = StorageManager.copyUriToTempFile(context, uri)
                                    if (tempFile == null) {
                                        isProcessing = false
                                        Toast.makeText(context, "Failed to resolve file", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val ocrEngine = NativeOcrEngine()
                                    val historyDao = DatabaseProvider.getDatabase(context).historyDao()

                                    val result = ocrEngine.extractTextFromImage(context, uri, selectedLanguage, detectTables)
                                    isProcessing = false

                                    if (result.isSuccess) {
                                        val text = result.getOrThrow()
                                        if (text.isBlank()) {
                                            Toast.makeText(context, "No text recognized in the image", Toast.LENGTH_LONG).show()
                                        } else {
                                            extractedText = text
                                            // Save output as a .txt file
                                            val txtFile = File(StorageManager.getAneganOutputDirectory("Documents"), "${tempFile.nameWithoutExtension}_ocr.txt")
                                            txtFile.writeText(text)

                                            Toast.makeText(context, "OCR Completed! Saved to ${txtFile.name}", Toast.LENGTH_LONG).show()

                                            historyDao.insertConversion(
                                                ConversionHistoryEntity(
                                                    originalFileName = selectedFileName ?: tempFile.name,
                                                    outputFileName = txtFile.name,
                                                    originalFormat = tempFile.extension.uppercase(),
                                                    outputFormat = "TXT",
                                                    status = "SUCCESS",
                                                    timestamp = System.currentTimeMillis(),
                                                    originalSize = selectedFileSize ?: tempFile.length(),
                                                    outputSize = txtFile.length(),
                                                    outputPath = txtFile.absolutePath
                                                )
                                            )
                                        }
                                    } else {
                                        val ex = result.exceptionOrNull()
                                        Toast.makeText(context, "OCR Failed: ${ex?.message}", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onTextDetected: (String) -> Unit,
    language: String,
    detectTables: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val ocrEngine = remember { NativeOcrEngine() }
    val coroutineScope = rememberCoroutineScope()
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                var lastAnalysisTime = 0L
                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastAnalysisTime >= 500) { // Throttle to 500ms
                        lastAnalysisTime = currentTime
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            coroutineScope.launch {
                                val result = ocrEngine.extractTextFromInputImage(image, language, detectTables)
                                if (result.isSuccess) {
                                    val text = result.getOrThrow()
                                    if (text.isNotBlank()) {
                                        onTextDetected(text)
                                    }
                                }
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    // Fail-safe bind catch
                }
            }, ContextCompat.getMainExecutor(context))
            
            previewView
        },
        modifier = modifier
    )
}
