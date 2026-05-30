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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.app.ActivityManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.anegan.core.conversion.DevToolsManager
import com.anegan.core.conversion.StorageManager
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import com.anegan.core.designsystem.theme.PureWhite
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

import com.anegan.core.designsystem.theme.NovaBackground
import com.anegan.core.designsystem.theme.NovaTopBar
import com.anegan.core.designsystem.theme.NovaSegmentedControl
import com.anegan.core.designsystem.theme.GlassCard
import com.anegan.core.designsystem.theme.NovaTextField
import com.anegan.core.designsystem.theme.NovaPrimaryButton
import com.anegan.core.designsystem.theme.NovaSecondaryButton
import com.anegan.core.designsystem.theme.NovaSwitch
import com.anegan.core.designsystem.theme.NovaSlider
import com.anegan.core.designsystem.theme.NovaChip
import com.anegan.core.designsystem.theme.NovaHaptics
import com.anegan.core.designsystem.theme.NovaTypography
import com.anegan.core.designsystem.theme.NeonGold
import com.anegan.core.designsystem.theme.NovaDeepSpace
import com.anegan.core.designsystem.theme.NovaTokens
import com.anegan.core.designsystem.theme.SpaceGrotesk
import com.anegan.core.designsystem.theme.JetBrainsMono
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    var activeTab by remember { mutableStateOf(0) } // 0: Hash, 1: Base64, 2: QR Tools, 3: Dev Utilities

    NovaBackground(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                NovaTopBar(
                    title = "Developer Tools",
                    onBack = onBack,
                    neonAccent = NeonGold
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                NovaSegmentedControl(
                    items = listOf("Hash", "Base64", "QR Tools", "Dev Utils"),
                    selectedIndex = activeTab,
                    onIndexSelected = { 
                        activeTab = it 
                        NovaHaptics.swipeSnap(view)
                    },
                    neonColor = NeonGold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tab content
                when (activeTab) {
                    0 -> HashTabContent(context)
                    1 -> Base64TabContent(context)
                    2 -> QrTabContent(context)
                    3 -> DevUtilsTabContent(context)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
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

    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Text / File Mode Toggle
        NovaSegmentedControl(
            items = listOf("Text Hash", "File Hash"),
            selectedIndex = if (isTextMode) 0 else 1,
            onIndexSelected = { 
                isTextMode = (it == 0)
                resultHash = "" 
                NovaHaptics.swipeSnap(view)
            },
            neonColor = NeonGold,
            modifier = Modifier.fillMaxWidth()
        )

        // Algorithm Selection
        Column {
            Text(
                text = "SELECT ALGORITHM",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = SpaceGrotesk
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                algorithms.forEach { algo ->
                    val isSelected = selectedAlgorithm == algo
                    NovaChip(
                        text = algo,
                        selected = isSelected,
                        onClick = { 
                            selectedAlgorithm = algo
                            resultHash = "" 
                        },
                        neonColor = NeonGold
                    )
                }
            }
        }

        if (isTextMode) {
            NovaTextField(
                value = inputText,
                onValueChange = { 
                    inputText = it
                    resultHash = "" 
                },
                placeholder = "Enter text to hash...",
                singleLine = false,
                neonColor = NeonGold,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            GlassCard(
                neonAccent = NeonGold,
                onClick = { 
                    filePickerLauncher.launch("*/*") 
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedFileName != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, 
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = selectedFileName!!, 
                                color = NeonGold, 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMono
                            )
                            val sizeMb = (selectedFileSize ?: 0L) / (1024f * 1024f)
                            Text(
                                text = String.format("%.2f MB", sizeMb), 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMono
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📁", fontSize = 24.sp)
                            Text(
                                "Tap to Select Any File", 
                                color = NeonGold, 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }

        if (resultHash.isNotEmpty()) {
            GlassCard(
                neonAccent = NeonGold,
                enableGlow = true
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERATED HASH ($selectedAlgorithm)", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = "COPY",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = SpaceGrotesk,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Hash", resultHash)
                                    clipboard.setPrimaryClip(clip)
                                    NovaHaptics.success(view)
                                    Toast.makeText(context, "Hash copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resultHash, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        style = NovaTypography.codeMono,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        NovaPrimaryButton(
            text = "Generate Hash",
            neonColor = NeonGold,
            onClick = {
                if (isTextMode) {
                    if (inputText.isBlank()) {
                        NovaHaptics.warning(view)
                        Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show()
                        return@NovaPrimaryButton
                    }
                    isProcessing = true
                    coroutineScope.launch {
                        resultHash = DevToolsManager().generateTextHash(inputText, selectedAlgorithm)
                        isProcessing = false
                        NovaHaptics.confirm(view)
                    }
                } else {
                    val uri = selectedUri
                    if (uri == null) {
                        NovaHaptics.warning(view)
                        Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                        return@NovaPrimaryButton
                    }
                    isProcessing = true
                    coroutineScope.launch {
                        val tempFile = StorageManager.copyUriToTempFile(context, uri)
                        if (tempFile != null) {
                            resultHash = DevToolsManager().generateFileHash(tempFile, selectedAlgorithm)
                            NovaHaptics.confirm(view)
                            
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
                            NovaHaptics.warning(view)
                            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
                        }
                        isProcessing = false
                    }
                }
            },
            isLoading = isProcessing,
            modifier = Modifier.fillMaxWidth()
        )
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

    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NovaSegmentedControl(
            items = listOf("Encode Text", "Decode Text"),
            selectedIndex = if (isEncodeMode) 0 else 1,
            onIndexSelected = { 
                isEncodeMode = (it == 0)
                outputText = "" 
                NovaHaptics.swipeSnap(view)
            },
            neonColor = NeonGold,
            modifier = Modifier.fillMaxWidth()
        )

        NovaTextField(
            value = inputText,
            onValueChange = { 
                inputText = it
                outputText = "" 
            },
            placeholder = if (isEncodeMode) "Enter plain text to encode..." else "Enter Base64 string to decode...",
            singleLine = false,
            neonColor = NeonGold,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )

        if (outputText.isNotEmpty()) {
            GlassCard(
                neonAccent = NeonGold,
                enableGlow = true
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEncodeMode) "ENCODED BASE64" else "DECODED TEXT", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            text = "COPY",
                            color = NeonGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = SpaceGrotesk,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Base64 Result", outputText)
                                    clipboard.setPrimaryClip(clip)
                                    NovaHaptics.success(view)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = outputText, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        style = NovaTypography.codeMono,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        NovaPrimaryButton(
            text = if (isEncodeMode) "Base64 Encode" else "Base64 Decode",
            neonColor = NeonGold,
            onClick = {
                if (inputText.isBlank()) {
                    NovaHaptics.warning(view)
                    Toast.makeText(context, "Please enter some input text", Toast.LENGTH_SHORT).show()
                    return@NovaPrimaryButton
                }
                isProcessing = true
                coroutineScope.launch {
                    outputText = if (isEncodeMode) {
                        DevToolsManager().encodeTextBase64(inputText)
                    } else {
                        DevToolsManager().decodeTextBase64(inputText)
                    }
                    isProcessing = false
                    NovaHaptics.confirm(view)
                }
            },
            isLoading = isProcessing,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrTabContent(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    var isGeneratorMode by remember { mutableStateOf(true) } // Generate vs Scan
    var isCameraScannerActive by remember { mutableStateOf(false) }

    // Advanced Generator Options
    var qrInputMode by remember { mutableStateOf(0) } // 0: Text, 1: URL, 2: WiFi, 3: Contact, 4: Email, 5: SMS
    
    // Inputs
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("https://") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") } // WPA, WEP, nopass
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }
    var emailRecipient by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var smsNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }

    val fgColors = listOf(
        "Black" to android.graphics.Color.BLACK,
        "Dark Slate" to android.graphics.Color.rgb(30, 41, 59),
        "Navy Blue" to android.graphics.Color.rgb(29, 78, 216),
        "Forest Green" to android.graphics.Color.rgb(22, 163, 74),
        "Royal Purple" to android.graphics.Color.rgb(124, 58, 237),
        "Burgundy" to android.graphics.Color.rgb(159, 18, 57)
    )
    val bgColors = listOf(
        "White" to android.graphics.Color.WHITE,
        "Light Gray" to android.graphics.Color.rgb(248, 250, 252),
        "Soft Cream" to android.graphics.Color.rgb(255, 253, 245),
        "Ice Blue" to android.graphics.Color.rgb(240, 249, 255)
    )
    var selectedFgColorIndex by remember { mutableStateOf(0) }
    var selectedBgColorIndex by remember { mutableStateOf(0) }

    val sizes = listOf(
        "Small (256)" to 256,
        "Medium (512)" to 512,
        "Large (1024)" to 1024
    )
    var selectedSizeIndex by remember { mutableStateOf(1) } // Medium default

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

    if (isCameraScannerActive) {
        CameraQrScanner(
            onQrScanned = { result ->
                scannedResult = result
                isCameraScannerActive = false
                Toast.makeText(context, "QR scanned successfully!", Toast.LENGTH_SHORT).show()
            },
            onClose = {
                isCameraScannerActive = false
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NovaSegmentedControl(
            items = listOf("Generate QR", "Scan QR"),
            selectedIndex = if (isGeneratorMode) 0 else 1,
            onIndexSelected = { 
                isCameraScannerActive = false
                isGeneratorMode = (it == 0)
                NovaHaptics.swipeSnap(view)
            },
            neonColor = NeonGold,
            modifier = Modifier.fillMaxWidth()
        )

        if (isGeneratorMode) {
            // Input mode selector
            Column {
                Text(
                    text = "SELECT CONTENT TYPE", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = SpaceGrotesk
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modes = listOf("Text", "URL", "WiFi", "Contact", "Email", "SMS")
                    items(modes.size) { index ->
                        val isSelected = qrInputMode == index
                        NovaChip(
                            text = modes[index],
                            selected = isSelected,
                            onClick = { 
                                qrInputMode = index
                                qrBitmap = null 
                            },
                            neonColor = NeonGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content Input fields based on mode
            when (qrInputMode) {
                0 -> {
                    NovaTextField(
                        value = textInput,
                        onValueChange = { textInput = it; qrBitmap = null },
                        placeholder = "Enter raw text to encode...",
                        singleLine = false,
                        neonColor = NeonGold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                1 -> {
                    NovaTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it; qrBitmap = null },
                        placeholder = "Enter URL (e.g. https://...)",
                        neonColor = NeonGold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NovaTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it; qrBitmap = null },
                            placeholder = "Network Name (SSID)",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it; qrBitmap = null },
                            placeholder = "Password",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "SECURITY TYPE", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = SpaceGrotesk
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val securities = listOf("WPA", "WEP", "nopass")
                            securities.forEach { sec ->
                                val isSecSelected = wifiSecurity == sec
                                NovaChip(
                                    text = if (sec == "nopass") "None" else sec,
                                    selected = isSecSelected,
                                    onClick = { wifiSecurity = sec; qrBitmap = null },
                                    neonColor = NeonGold
                                )
                            }
                        }
                    }
                }
                3 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NovaTextField(
                            value = contactName,
                            onValueChange = { contactName = it; qrBitmap = null },
                            placeholder = "Full Name",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it; qrBitmap = null },
                            placeholder = "Phone Number",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it; qrBitmap = null },
                            placeholder = "Email Address",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = contactOrg,
                            onValueChange = { contactOrg = it; qrBitmap = null },
                            placeholder = "Organization/Company (Optional)",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                4 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NovaTextField(
                            value = emailRecipient,
                            onValueChange = { emailRecipient = it; qrBitmap = null },
                            placeholder = "Recipient Email",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = emailSubject,
                            onValueChange = { emailSubject = it; qrBitmap = null },
                            placeholder = "Subject",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = emailBody,
                            onValueChange = { emailBody = it; qrBitmap = null },
                            placeholder = "Email Body",
                            singleLine = false,
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                }
                5 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NovaTextField(
                            value = smsNumber,
                            onValueChange = { smsNumber = it; qrBitmap = null },
                            placeholder = "Phone Number",
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NovaTextField(
                            value = smsMessage,
                            onValueChange = { smsMessage = it; qrBitmap = null },
                            placeholder = "Message",
                            singleLine = false,
                            neonColor = NeonGold,
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Color customization & size in a frosted card
            GlassCard(neonAccent = NeonGold) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CUSTOMIZE QR STYLE", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = SpaceGrotesk
                    )

                    // Foreground color
                    Column {
                        Text(
                            text = "Foreground Pattern Color", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                            fontSize = 11.sp,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(fgColors.size) { index ->
                                val colorInfo = fgColors[index]
                                val isSelected = selectedFgColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(colorInfo.second))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) NeonGold else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { 
                                            selectedFgColorIndex = index
                                            qrBitmap = null 
                                            NovaHaptics.click(view)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text(
                                            text = "✓", 
                                            color = if (colorInfo.second == android.graphics.Color.WHITE) Color.Black else PureWhite, 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Background color
                    Column {
                        Text(
                            text = "Background Color", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                            fontSize = 11.sp,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(bgColors.size) { index ->
                                val colorInfo = bgColors[index]
                                val isSelected = selectedBgColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(colorInfo.second))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) NeonGold else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { 
                                            selectedBgColorIndex = index
                                            qrBitmap = null 
                                            NovaHaptics.click(view)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text(
                                            text = "✓", 
                                            color = if (colorInfo.second == android.graphics.Color.WHITE) Color.Black else PureWhite, 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Size selector
                    Column {
                        Text(
                            text = "QR Code Output Size", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                            fontSize = 11.sp,
                            fontFamily = SpaceGrotesk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            sizes.forEachIndexed { index, sizeInfo ->
                                val isSelected = selectedSizeIndex == index
                                NovaChip(
                                    text = sizeInfo.first,
                                    selected = isSelected,
                                    onClick = { 
                                        selectedSizeIndex = index
                                        qrBitmap = null 
                                    },
                                    neonColor = NeonGold
                                )
                            }
                        }
                    }
                }
            }

            if (qrBitmap != null) {
                GlassCard(
                    neonAccent = NeonGold,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )

                        NovaPrimaryButton(
                            text = "Save QR Code to Gallery",
                            neonColor = NeonGold,
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val outputDir = StorageManager.getAneganOutputDirectory("QR Codes")
                                        val qrFile = File(outputDir, "Anegan_QR_${System.currentTimeMillis()}.png")
                                        val fos = FileOutputStream(qrFile)
                                        qrBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                        fos.flush()
                                        fos.close()
                                        
                                        NovaHaptics.success(view)
                                        Toast.makeText(context, "QR saved to Anegan/QR Codes folder!", Toast.LENGTH_LONG).show()
                                        
                                        // Save entry in Room history DB
                                        val finalQrText = when (qrInputMode) {
                                            0 -> textInput
                                            1 -> if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) urlInput else "https://$urlInput"
                                            2 -> "WIFI:S:$wifiSsid;T:${if (wifiSecurity == "nopass") "nopass" else wifiSecurity};P:$wifiPassword;;"
                                            3 -> "BEGIN:VCARD..."
                                            4 -> "mailto:$emailRecipient..."
                                            5 -> "smsto:$smsNumber..."
                                            else -> ""
                                        }
                                        val historyDao = DatabaseProvider.getDatabase(context).historyDao()
                                        historyDao.insertConversion(
                                            ConversionHistoryEntity(
                                                originalFileName = when (qrInputMode) {
                                                    0 -> "Text Input"
                                                    1 -> "URL Input"
                                                    2 -> "WiFi Settings"
                                                    3 -> "vCard Contact"
                                                    4 -> "Email Info"
                                                    5 -> "SMS Message"
                                                    else -> "QR Code"
                                                },
                                                outputFileName = qrFile.name,
                                                originalFormat = "TXT",
                                                outputFormat = "PNG",
                                                status = "SUCCESS",
                                                timestamp = System.currentTimeMillis(),
                                                originalSize = finalQrText.toByteArray(Charsets.UTF_8).size.toLong(),
                                                outputSize = qrFile.length(),
                                                outputPath = qrFile.absolutePath
                                            )
                                        )
                                    } catch (e: Exception) {
                                        NovaHaptics.warning(view)
                                        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (qrBitmap == null) {
                NovaPrimaryButton(
                    text = "Generate QR Code",
                    neonColor = NeonGold,
                    onClick = {
                        val finalQrText = when (qrInputMode) {
                            0 -> textInput
                            1 -> {
                                if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) {
                                    urlInput
                                } else {
                                    "https://$urlInput"
                                }
                            }
                            2 -> {
                                "WIFI:S:$wifiSsid;T:${if (wifiSecurity == "nopass") "nopass" else wifiSecurity};P:$wifiPassword;;"
                            }
                            3 -> {
                                """
                                    BEGIN:VCARD
                                    VERSION:3.0
                                    N:$contactName;;;
                                    FN:$contactName
                                    TEL:$contactPhone
                                    EMAIL:$contactEmail
                                    ORG:$contactOrg
                                    END:VCARD
                                """.trimIndent()
                            }
                            4 -> {
                                "mailto:$emailRecipient?subject=${java.net.URLEncoder.encode(emailSubject, "UTF-8")}&body=${java.net.URLEncoder.encode(emailBody, "UTF-8")}"
                            }
                            5 -> {
                                "smsto:$smsNumber:$smsMessage"
                            }
                            else -> ""
                        }

                        if (finalQrText.isBlank() || 
                            (qrInputMode == 2 && wifiSsid.isBlank()) || 
                            (qrInputMode == 3 && contactName.isBlank()) || 
                            (qrInputMode == 4 && emailRecipient.isBlank()) || 
                            (qrInputMode == 5 && smsNumber.isBlank())
                        ) {
                            NovaHaptics.warning(view)
                            Toast.makeText(context, "Please complete required inputs", Toast.LENGTH_SHORT).show()
                            return@NovaPrimaryButton
                        }

                        isProcessing = true
                        coroutineScope.launch {
                            val result = DevToolsManager().generateQrCode(
                                text = finalQrText,
                                size = sizes[selectedSizeIndex].second,
                                fgColor = fgColors[selectedFgColorIndex].second,
                                bgColor = bgColors[selectedBgColorIndex].second
                            )
                            isProcessing = false
                            if (result.isSuccess) {
                                qrBitmap = result.getOrThrow()
                                NovaHaptics.confirm(view)
                            } else {
                                NovaHaptics.warning(view)
                                Toast.makeText(context, "Failed to generate QR", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    isLoading = isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Scanner View selection
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Option 1: Live camera scan
                GlassCard(
                    neonAccent = NeonGold,
                    onClick = { 
                        isCameraScannerActive = true 
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📷", fontSize = 32.sp)
                        Text(
                            "Start Camera Scanner", 
                            color = NeonGold, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk
                        )
                        Text(
                            "Scan any QR code using your camera", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Option 2: Image scan
                GlassCard(
                    neonAccent = NeonGold,
                    onClick = { 
                        qrPickerLauncher.launch("image/*") 
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🖼️", fontSize = 32.sp)
                        Text(
                            text = if (selectedFileName != null) selectedFileName!! else "Select QR Image from Files",
                            color = NeonGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Scan a QR code from a saved picture/screenshot", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (scannedResult.isNotEmpty()) {
                GlassCard(
                    neonAccent = NeonGold,
                    enableGlow = true
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DECODED QR CONTENT", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk
                            )
                            Text(
                                "COPY",
                                color = NeonGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = SpaceGrotesk,
                                modifier = Modifier
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("QR Decoded", scannedResult)
                                        clipboard.setPrimaryClip(clip)
                                        NovaHaptics.success(view)
                                        Toast.makeText(context, "Copied content to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scannedResult, 
                            color = MaterialTheme.colorScheme.onSurface, 
                            style = NovaTypography.codeMono,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraQrScanner(
    onQrScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )
    
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = CameraPreview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        val scanner = BarcodeScanning.getClient()
                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val barcode = barcodes.firstOrNull()
                                                if (barcode != null && !barcode.rawValue.isNullOrEmpty()) {
                                                    onQrScanned(barcode.rawValue!!)
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
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
                            Toast.makeText(context, "Camera bind failed", Toast.LENGTH_SHORT).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Draw Target Finder Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Target Frame
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Transparent)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                
                // Description overlay
                Text(
                    text = "Align QR Code inside the frame",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera permission is required to scan QR codes",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
        ) {
            Text("✕", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun DevUtilsTabContent(context: Context) {
    val utilities = listOf(
        "JSON Utility",
        "URL Encoder",
        "UUID Generator",
        "JWT Decoder",
        "Device Info",
        "Text Counter",
        "Timestamp Conv",
        "Cron Helper",
        "Base Converter"
    )

    var selectedUtilIndex by remember { mutableStateOf(0) }

    val view = LocalView.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Frosted selector container
        GlassCard(neonAccent = NeonGold) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1 (0 to 2)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..2) {
                        val name = utilities[i]
                        val isSelected = selectedUtilIndex == i
                        val bg = if (isSelected) NeonGold.copy(alpha = 0.15f) else Color.Transparent
                        val border = if (isSelected) NeonGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        val textCol = if (isSelected) NeonGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { 
                                    selectedUtilIndex = i 
                                    NovaHaptics.swipeSnap(view)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name, 
                                color = textCol, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 10.sp,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
                // Row 2 (3 to 5)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 3..5) {
                        val name = utilities[i]
                        val isSelected = selectedUtilIndex == i
                        val bg = if (isSelected) NeonGold.copy(alpha = 0.15f) else Color.Transparent
                        val border = if (isSelected) NeonGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        val textCol = if (isSelected) NeonGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { 
                                    selectedUtilIndex = i 
                                    NovaHaptics.swipeSnap(view)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name, 
                                color = textCol, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 10.sp,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
                // Row 3 (6 to 8)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 6..8) {
                        val name = utilities[i]
                        val isSelected = selectedUtilIndex == i
                        val bg = if (isSelected) NeonGold.copy(alpha = 0.15f) else Color.Transparent
                        val border = if (isSelected) NeonGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        val textCol = if (isSelected) NeonGold else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { 
                                    selectedUtilIndex = i 
                                    NovaHaptics.swipeSnap(view)
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name, 
                                color = textCol, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 10.sp,
                                fontFamily = SpaceGrotesk
                            )
                        }
                    }
                }
            }
        }

        // Render selected utility in a glowing frosted GlassCard
        GlassCard(
            neonAccent = NeonGold,
            enableGlow = true
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = utilities[selectedUtilIndex].uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = NeonGold,
                    fontFamily = SpaceGrotesk
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (selectedUtilIndex) {
                    0 -> JsonFormatterSection(context)
                    1 -> UrlEncoderSection(context)
                    2 -> UuidGeneratorSection(context)
                    3 -> JwtDecoderSection(context)
                    4 -> DeviceInfoSection(context)
                    5 -> TextCounterSection(context)
                    6 -> TimestampConverterSection(context)
                    7 -> CronHelperSection(context)
                    8 -> NumberBaseConverterSection(context)
                }
            }
        }
    }
}

// ==========================================
// UTILITY SECTIONS IMPLEMENTATIONS
// ==========================================

@Composable
fun JsonFormatterSection(context: Context) {
    val view = LocalView.current
    var inputJson by remember { mutableStateOf("") }
    var outputJson by remember { mutableStateOf("") }

    fun formatJson(json: String): String {
        return try {
            val trimmed = json.trim()
            if (trimmed.startsWith("{")) {
                org.json.JSONObject(trimmed).toString(4)
            } else if (trimmed.startsWith("[")) {
                org.json.JSONArray(trimmed).toString(4)
            } else {
                "Invalid JSON: Must start with '{' or '['"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun minifyJson(json: String): String {
        return try {
            val trimmed = json.trim()
            if (trimmed.startsWith("{")) {
                org.json.JSONObject(trimmed).toString()
            } else if (trimmed.startsWith("[")) {
                org.json.JSONArray(trimmed).toString()
            } else {
                "Invalid JSON: Must start with '{' or '['"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NovaTextField(
            value = inputJson,
            onValueChange = { inputJson = it },
            placeholder = "Paste raw JSON here...",
            singleLine = false,
            neonColor = NeonGold,
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NovaSecondaryButton(
                text = "Format",
                neonColor = NeonGold,
                onClick = { 
                    outputJson = formatJson(inputJson)
                    NovaHaptics.confirm(view)
                },
                modifier = Modifier.weight(1f)
            )
            NovaPrimaryButton(
                text = "Minify",
                neonColor = NeonGold,
                onClick = { 
                    outputJson = minifyJson(inputJson)
                    NovaHaptics.confirm(view)
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (outputJson.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESULT", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "COPY RESULT",
                    color = NeonGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = SpaceGrotesk,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("JSON Output", outputJson))
                        NovaHaptics.success(view)
                        Toast.makeText(context, "Copied JSON!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(NovaTokens.Radius.md))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)), RoundedCornerShape(NovaTokens.Radius.md))
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = outputJson, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    style = NovaTypography.codeMono,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun UrlEncoderSection(context: Context) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var parsedInfo by remember { mutableStateOf<Map<String, String>?>(null) }

    fun parseUrl(urlStr: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val url = java.net.URL(urlStr)
            result["Protocol"] = url.protocol
            result["Host"] = url.host
            result["Path"] = url.path
            result["Query"] = url.query ?: "None"
        } catch (e: Exception) {
            result["Error"] = "Not a parseable absolute URL"
        }
        return result
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter URL or Text") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    outputText = try {
                        URLEncoder.encode(inputText, "UTF-8")
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                    parsedInfo = null
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Encode", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    outputText = try {
                        URLDecoder.decode(inputText, "UTF-8")
                    } catch (e: Exception) {
                        "Error: ${e.message}"
                    }
                    parsedInfo = null
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Decode", fontSize = 12.sp)
            }
            Button(
                onClick = {
                    parsedInfo = parseUrl(inputText)
                    outputText = ""
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Parse", fontSize = 12.sp)
            }
        }

        if (outputText.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Result:", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "Copy",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("URL Output", outputText))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            OutlinedTextField(
                value = outputText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        parsedInfo?.let { info ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                info.forEach { (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(k, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(v, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun UuidGeneratorSection(context: Context) {
    var generatedUuid by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var bulkQuantityText by remember { mutableStateOf("5") }
    var bulkList by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(generatedUuid, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Regen",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { generatedUuid = UUID.randomUUID().toString() }
                )
                Text(
                    "Copy",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("UUID", generatedUuid))
                        Toast.makeText(context, "Copied single UUID!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Divider(color = Color.LightGray.copy(alpha = 0.5f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = bulkQuantityText,
                onValueChange = { bulkQuantityText = it },
                label = { Text("Bulk Count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(100.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val count = bulkQuantityText.toIntOrNull() ?: 5
                    val list = mutableListOf<String>()
                    val safeCount = count.coerceIn(1, 50)
                    for (i in 0 until safeCount) {
                        list.add(UUID.randomUUID().toString())
                    }
                    bulkList = list
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Bulk Generate", fontSize = 12.sp)
            }
        }

        if (bulkList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bulk List:", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "Copy All",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val joined = bulkList.joinToString("\n")
                        clipboard.setPrimaryClip(ClipData.newPlainText("UUID List", joined))
                        Toast.makeText(context, "Copied all UUIDs!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            OutlinedTextField(
                value = bulkList.joinToString("\n"),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun JwtDecoderSection(context: Context) {
    var inputJwt by remember { mutableStateOf("") }
    var decodedHeader by remember { mutableStateOf("") }
    var decodedPayload by remember { mutableStateOf("") }
    var expInfo by remember { mutableStateOf("") }

    fun decodeAndFormatJwt(jwt: String) {
        val parts = jwt.trim().split(".")
        if (parts.size < 2) {
            decodedHeader = "Invalid format: JWT must contain 3 parts separated by dots"
            decodedPayload = ""
            expInfo = ""
            return
        }
        try {
            val headerStr = String(android.util.Base64.decode(parts[0], android.util.Base64.DEFAULT), Charsets.UTF_8)
            val payloadStr = String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT), Charsets.UTF_8)
            
            decodedHeader = org.json.JSONObject(headerStr).toString(4)
            val payloadJson = org.json.JSONObject(payloadStr)
            decodedPayload = payloadJson.toString(4)

            if (payloadJson.has("exp")) {
                val expSec = payloadJson.getLong("exp")
                val expMs = expSec * 1000
                val date = Date(expMs)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val now = System.currentTimeMillis()
                if (now > expMs) {
                    expInfo = "Expired on: ${sdf.format(date)}"
                } else {
                    expInfo = "Expires on: ${sdf.format(date)} (Active)"
                }
            } else {
                expInfo = "No 'exp' claim found"
            }
        } catch (e: Exception) {
            decodedHeader = "Error: ${e.message}"
            decodedPayload = ""
            expInfo = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = inputJwt,
            onValueChange = { inputJwt = it },
            label = { Text("Paste JWT") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { decodeAndFormatJwt(inputJwt) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Decode JWT", fontWeight = FontWeight.Bold)
        }

        if (expInfo.isNotEmpty()) {
            Text(expInfo, color = if (expInfo.contains("Expired")) Color.Red else Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (decodedHeader.isNotEmpty()) {
            Text("Header", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = decodedHeader,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (decodedPayload.isNotEmpty()) {
            Text("Payload", color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = decodedPayload,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun DeviceInfoSection(context: Context) {
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager.getMemoryInfo(memInfo)
    val totalRam = String.format("%.2f GB", memInfo.totalMem / (1024f * 1024f * 1024f))
    val availRam = String.format("%.2f GB", memInfo.availMem / (1024f * 1024f * 1024f))

    val dm = context.resources.displayMetrics
    val resolution = "${dm.widthPixels} x ${dm.heightPixels} (${dm.densityDpi} dpi)"

    val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
    val batteryPct = "${bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensorCount = sensorManager.getSensorList(Sensor.TYPE_ALL).size.toString()

    val details = listOf(
        "Device Name" to Build.DEVICE,
        "Model" to Build.MODEL,
        "Manufacturer" to Build.MANUFACTURER,
        "Android Version" to Build.VERSION.RELEASE,
        "SDK Level" to Build.VERSION.SDK_INT.toString(),
        "Total RAM" to totalRam,
        "Available RAM" to availRam,
        "Screen Resolution" to resolution,
        "Battery Level" to batteryPct,
        "Hardware Sensors" to "$sensorCount active sensors"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        details.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
            Divider(color = Color.LightGray.copy(alpha = 0.2f))
        }
    }
}

@Composable
fun TextCounterSection(context: Context) {
    var inputText by remember { mutableStateOf("") }

    val charCount = inputText.length
    val wordCount = inputText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val sentenceCount = inputText.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }.size
    val lineCount = if (inputText.isEmpty()) 0 else inputText.split("\n").size
    val readTime = (wordCount / 200) + 1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter text to count") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Characters", color = Color.Gray, fontSize = 10.sp)
                Text(charCount.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Words", color = Color.Gray, fontSize = 10.sp)
                Text(wordCount.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sentences", color = Color.Gray, fontSize = 10.sp)
                Text(sentenceCount.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lines", color = Color.Gray, fontSize = 10.sp)
                Text(lineCount.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Read Time", color = Color.Gray, fontSize = 10.sp)
                Text("~$readTime min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TimestampConverterSection(context: Context) {
    var timestampStr by remember { mutableStateOf(System.currentTimeMillis().toString()) }
    var convertedDate by remember { mutableStateOf("") }
    var convertedTimestamp by remember { mutableStateOf("") }
    var dateStrInput by remember { mutableStateOf("") }

    fun convertTimestampToDate(ts: String) {
        try {
            val numeric = ts.trim().toLong()
            // Support both seconds and milliseconds
            val actualMs = if (ts.trim().length <= 10) numeric * 1000 else numeric
            val date = Date(actualMs)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())
            convertedDate = sdf.format(date)
        } catch (e: Exception) {
            convertedDate = "Error: ${e.message}"
        }
    }

    fun convertDateToTimestamp(dateString: String) {
        try {
            // Support standard format
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(dateString.trim())
            convertedTimestamp = date?.time?.toString() ?: "Error parsing date"
        } catch (e: Exception) {
            convertedTimestamp = "Error: Use format yyyy-MM-dd HH:mm:ss"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Timestamp to Date
        OutlinedTextField(
            value = timestampStr,
            onValueChange = { timestampStr = it },
            label = { Text("Epoch Timestamp (sec or ms)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { convertTimestampToDate(timestampStr) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Convert to Date-Time")
        }

        if (convertedDate.isNotEmpty()) {
            Text(convertedDate, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Divider(color = Color.LightGray.copy(alpha = 0.5f))

        // Date to Timestamp
        OutlinedTextField(
            value = dateStrInput,
            onValueChange = { dateStrInput = it },
            label = { Text("Date (yyyy-MM-dd HH:mm:ss)") },
            placeholder = { Text("e.g., 2026-05-28 17:00:00") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { convertDateToTimestamp(dateStrInput) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Convert to Timestamp")
        }

        if (convertedTimestamp.isNotEmpty()) {
            Text(convertedTimestamp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CronHelperSection(context: Context) {
    var inputCron by remember { mutableStateOf("*/5 * * * *") }
    var explanationText by remember { mutableStateOf("") }

    val cronExamples = listOf(
        "*/5 * * * *" to "Every 5 minutes",
        "0 * * * *" to "Every hour",
        "0 0 * * *" to "Every day at midnight",
        "0 0 * * 0" to "Every Sunday at midnight",
        "0 9 * * 1-5" to "Every weekday at 9:00 AM"
    )

    fun explainCron(cron: String) {
        val parts = cron.trim().split(Regex("\\s+"))
        if (parts.size != 5) {
            explanationText = "Invalid cron: standard cron must have exactly 5 fields (Minute Hour DayOfMonth Month DayOfWeek)"
            return
        }
        explanationText = try {
            val min = parts[0]
            val hr = parts[1]
            val dom = parts[2]
            val mon = parts[3]
            val dow = parts[4]

            val descMin = if (min == "*") "every minute" else "minute $min"
            val descHr = if (hr == "*") "every hour" else "hour $hr"
            val descDom = if (dom == "*") "every day of the month" else "day $dom of the month"
            val descMon = if (mon == "*") "every month" else "month $mon"
            val descDow = if (dow == "*") "every day of the week" else "day $dow of the week"

            "Runs at: $descMin, $descHr, $descDom, $descMon, $descDow"
        } catch (e: Exception) {
            "Error explaining cron: ${e.message}"
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = inputCron,
            onValueChange = { inputCron = it },
            label = { Text("Cron Expression") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { explainCron(inputCron) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Explain Expression")
        }

        if (explanationText.isNotEmpty()) {
            Text(explanationText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Divider(color = Color.LightGray.copy(alpha = 0.5f))

        Text("Examples (Tap to copy/use):", color = Color.Gray, fontSize = 11.sp)
        cronExamples.forEach { (exp, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        inputCron = exp
                        explainCron(exp)
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(exp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun NumberBaseConverterSection(context: Context) {
    var inputValue by remember { mutableStateOf("42") }
    var selectedFromBaseIndex by remember { mutableStateOf(2) } // Decimal default
    var outputValues by remember { mutableStateOf<Map<String, String>?>(null) }

    val bases = listOf("Binary (2)", "Octal (8)", "Decimal (10)", "Hex (16)")
    val basesRadix = listOf(2, 8, 10, 16)

    fun convertNumber(input: String, fromRadix: Int) {
        try {
            val num = input.trim().toLong(fromRadix)
            outputValues = mapOf(
                "Binary" to java.lang.Long.toBinaryString(num),
                "Octal" to java.lang.Long.toOctalString(num),
                "Decimal" to num.toString(),
                "Hexadecimal" to java.lang.Long.toHexString(num).uppercase()
            )
        } catch (e: Exception) {
            outputValues = mapOf("Error" to "Invalid input for the selected source base.")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Input Value") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("Source Base", color = Color.Gray, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bases.forEachIndexed { idx, name ->
                val isSelected = selectedFromBaseIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                        .clickable { selectedFromBaseIndex = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name.split(" ")[0],
                        color = if (isSelected) PureWhite else MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = { convertNumber(inputValue, basesRadix[selectedFromBaseIndex]) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = PureWhite),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Convert Base")
        }

        outputValues?.let { outs ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                outs.forEach { (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(k, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(v, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
