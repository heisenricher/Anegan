/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.smbshare

import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbShareScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    // Connection state
    var hostAddress by remember { mutableStateOf("") }
    var shareName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Remote browser state
    var remoteFiles by remember { mutableStateOf<List<SmbClient.SmbFile>>(emptyList()) }
    var remotePath by remember { mutableStateOf("") }
    var remotePathStack by remember { mutableStateOf(listOf<String>()) }
    var isLoadingRemote by remember { mutableStateOf(false) }

    // Local browser state
    var localFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Transfer state
    var transferStatus by remember { mutableStateOf<String?>(null) }
    var transferProgress by remember { mutableStateOf(0f) }
    var isTransferring by remember { mutableStateOf(false) }

    // Transfer history
    var transferHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    // SMB client instance
    val smbClient = remember { SmbClient() }

    // Load local files
    val baseDir = remember {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Anegan"
        ).apply { if (!exists()) mkdirs() }
    }

    fun loadLocalFiles() {
        localFiles = baseDir.listFiles()
            ?.filter { it.isFile && !it.name.startsWith(".") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    LaunchedEffect(Unit) { loadLocalFiles() }

    // Animated pulse for connection indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val isDark = isSystemInDarkTheme()

    BackHandler {
        scope.launch {
            try { smbClient.disconnect() } catch (_: Exception) {}
        }
        onBack()
    }

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "SMB File Sharing",
                    onBack = {
                        NovaHaptics.click(view)
                        scope.launch {
                            try { smbClient.disconnect() } catch (_: Exception) {}
                        }
                        onBack()
                    },
                    neonAccent = NeonBlue
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // ── CONNECTION PANEL ──────────────────────────────────────
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonBlue,
                        enableGlow = true
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Animated connection indicator
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isConnected) Color(0xFF10B981).copy(alpha = pulseAlpha)
                                                else if (isConnecting) Color(0xFFF59E0B).copy(alpha = pulseAlpha)
                                                else Color(0xFF6B7280)
                                            )
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (isConnected) "Connected to \\\\$hostAddress\\$shareName"
                                        else if (isConnecting) "Connecting..."
                                        else "Offline Network Shares",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGrotesk,
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            AnimatedVisibility(visible = !isConnected) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Host Address
                                    Text("IP ADDRESS / HOSTNAME", style = NovaTypography.tagMono, color = NeonBlue)
                                    NovaTextField(
                                        value = hostAddress,
                                        onValueChange = { hostAddress = it.trim() },
                                        placeholder = "192.168.1.100",
                                        neonColor = NeonBlue,
                                        leadingIcon = Icons.Rounded.Dns,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Uri,
                                            imeAction = ImeAction.Next
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Share Name
                                    Text("SHARE NAME", style = NovaTypography.tagMono, color = NeonBlue)
                                    NovaTextField(
                                        value = shareName,
                                        onValueChange = { shareName = it.trim() },
                                        placeholder = "SharedFolder",
                                        neonColor = NeonBlue,
                                        leadingIcon = Icons.Rounded.FolderOpen,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Username
                                    Text("USERNAME (OPTIONAL)", style = NovaTypography.tagMono, color = NeonBlue)
                                    NovaTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        placeholder = "guest",
                                        neonColor = NeonBlue,
                                        leadingIcon = Icons.Rounded.Person,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Password
                                    Text("PASSWORD (OPTIONAL)", style = NovaTypography.tagMono, color = NeonBlue)
                                    NovaTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        placeholder = "Password",
                                        neonColor = NeonBlue,
                                        leadingIcon = Icons.Rounded.Lock,
                                        trailingIcon = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        onTrailingClick = { showPassword = !showPassword },
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Error message
                                    connectionError?.let { err ->
                                        Text(
                                            err,
                                            color = Color(0xFFEF5350),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = JetBrainsMono,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Connect / Disconnect button
                            NovaPrimaryButton(
                                text = if (isConnecting) "Connecting..."
                                else if (isConnected) "Disconnect Share"
                                else "Connect to Share",
                                neonColor = if (isConnected) Color(0xFFEF5350) else NeonBlue,
                                enabled = !isConnecting && !isTransferring,
                                onClick = {
                                    NovaHaptics.longPress(view)
                                    if (isConnected) {
                                        scope.launch {
                                            try { smbClient.disconnect() } catch (_: Exception) {}
                                            isConnected = false
                                            remoteFiles = emptyList()
                                            remotePath = ""
                                            remotePathStack = emptyList()
                                        }
                                    } else {
                                        if (hostAddress.isBlank() || shareName.isBlank()) {
                                            connectionError = "IP Address and Share Name are required"
                                            return@NovaPrimaryButton
                                        }
                                        isConnecting = true
                                        connectionError = null
                                        scope.launch {
                                            try {
                                                smbClient.connect(hostAddress, shareName, username, password)
                                                isConnected = true
                                                isConnecting = false
                                                // Load root directory
                                                isLoadingRemote = true
                                                remoteFiles = smbClient.listFiles("")
                                                remotePath = ""
                                                remotePathStack = emptyList()
                                                isLoadingRemote = false
                                                NovaHaptics.success(view)
                                            } catch (e: Exception) {
                                                isConnecting = false
                                                connectionError = e.message ?: "Connection failed"
                                                NovaHaptics.reject(view)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ── TRANSFER PROGRESS ─────────────────────────────────────
                if (isTransferring) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonBlue,
                            enableGlow = true
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = transferStatus ?: "Transferring...",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                    fontSize = 14.sp
                                )
                                LinearProgressIndicator(
                                    progress = transferProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = NeonBlue,
                                    trackColor = NeonBlue.copy(alpha = 0.12f),
                                )
                                Text(
                                    text = "${(transferProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMono,
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── REMOTE FILE BROWSER ───────────────────────────────────
                if (isConnected) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonBlue
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📡", fontSize = 20.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Remote Directory",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SpaceGrotesk,
                                            color = NeonBlue,
                                            fontSize = 16.sp
                                        )
                                    }
                                    if (remotePathStack.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                NovaHaptics.click(view)
                                                scope.launch {
                                                    val newStack = remotePathStack.dropLast(1)
                                                    val newPath = newStack.joinToString("\\")
                                                    isLoadingRemote = true
                                                    try {
                                                        remoteFiles = smbClient.listFiles(newPath)
                                                        remotePath = newPath
                                                        remotePathStack = newStack
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isLoadingRemote = false
                                                }
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text("⬆ Up Directory", fontWeight = FontWeight.Bold, color = NeonBlue, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = "\\\\$hostAddress\\$shareName${if (remotePath.isNotEmpty()) "\\$remotePath" else ""}",
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMono,
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )

                                if (isLoadingRemote) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(32.dp))
                                    }
                                } else if (remoteFiles.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "📭 Empty Remote Directory",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 280.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        remoteFiles.forEach { file ->
                                            RemoteFileItem(
                                                file = file,
                                                onClick = {
                                                    NovaHaptics.longPress(view)
                                                    if (file.isDirectory) {
                                                        scope.launch {
                                                            val newPath = if (remotePath.isEmpty()) file.name
                                                            else "$remotePath\\${file.name}"
                                                            isLoadingRemote = true
                                                            try {
                                                                remoteFiles = smbClient.listFiles(newPath)
                                                                remotePathStack = remotePathStack + file.name
                                                                remotePath = newPath
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                            isLoadingRemote = false
                                                        }
                                                    } else {
                                                        // Download file
                                                        scope.launch {
                                                            isTransferring = true
                                                            transferStatus = "⬇ Downloading ${file.name}..."
                                                            transferProgress = 0f
                                                            try {
                                                                val remoteFilePath = if (remotePath.isEmpty()) file.name
                                                                else "$remotePath\\${file.name}"
                                                                val localFile = File(baseDir, sanitizeFileName(file.name))
                                                                smbClient.downloadFile(
                                                                    remotePath = remoteFilePath,
                                                                    localFile = localFile,
                                                                    onProgress = { transferProgress = it }
                                                                )
                                                                transferHistory = listOf("⬇ ${file.name} → local") + transferHistory
                                                                loadLocalFiles()
                                                                Toast.makeText(context, "Downloaded: ${file.name}", Toast.LENGTH_SHORT).show()
                                                                NovaHaptics.success(view)
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                                NovaHaptics.reject(view)
                                                            }
                                                            isTransferring = false
                                                            transferStatus = null
                                                        }
                                                    }
                                                },
                                                isDark = isDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── LOCAL FILES (for upload) ──────────────────────────────
                if (isConnected) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonBlue
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📱", fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Local Files (Anegan)",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGrotesk,
                                        color = NeonBlue,
                                        fontSize = 16.sp
                                    )
                                }

                                Text(
                                    "Tap a local document to upload to active directory",
                                    fontSize = 12.sp,
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 10.dp, top = 4.dp)
                                )

                                if (localFiles.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "📭 No local files in Anegan folder",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 250.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        localFiles.forEach { file ->
                                            LocalFileItem(
                                                file = file,
                                                onClick = {
                                                    NovaHaptics.longPress(view)
                                                    scope.launch {
                                                        isTransferring = true
                                                        transferStatus = "⬆ Uploading ${file.name}..."
                                                        transferProgress = 0f
                                                        try {
                                                            val remoteFilePath = if (remotePath.isEmpty()) file.name
                                                            else "$remotePath\\${file.name}"
                                                            smbClient.uploadFile(
                                                                localFile = file,
                                                                remotePath = remoteFilePath,
                                                                onProgress = { transferProgress = it }
                                                            )
                                                            transferHistory = listOf("⬆ ${file.name} → remote") + transferHistory
                                                            // Refresh remote listing
                                                            remoteFiles = smbClient.listFiles(remotePath)
                                                            Toast.makeText(context, "Uploaded: ${file.name}", Toast.LENGTH_SHORT).show()
                                                            NovaHaptics.success(view)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            NovaHaptics.reject(view)
                                                        }
                                                        isTransferring = false
                                                        transferStatus = null
                                                    }
                                                },
                                                isDark = isDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── TRANSFER HISTORY ──────────────────────────────────────
                if (transferHistory.isNotEmpty()) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonBlue.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📋", fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Transfer Logs",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGrotesk,
                                        color = NeonBlue,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                transferHistory.take(10).forEach { entry ->
                                    Text(
                                        text = entry,
                                        fontSize = 12.sp,
                                        fontFamily = JetBrainsMono,
                                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.8f) else NovaDeepInk.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── HOW TO CONNECT GUIDE ──────────────────────────────────
                if (!isConnected) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            neonAccent = NeonBlue.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📖 Local Network Configuration Guide",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    color = NeonBlue,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                HowToStep("1️⃣", "Ensure this device is connected to the same Wi-Fi network as host PC/NAS.")
                                HowToStep("2️⃣", "On Windows: Right-click target folder → Properties → Sharing → Share.")
                                HowToStep("3️⃣", "Lookup and enter the host computer's IP address (e.g. 192.168.1.100).")
                                HowToStep("4️⃣", "Enter the shared folder name exactly as shared on target computer.")
                                HowToStep("5️⃣", "Username and password can be left blank for anonymous guest shares.")
                                HowToStep("💡", "NAS drives: Refer to NAS settings console to verify IP and shares.")
                            }
                        }
                    }
                }

                // Bottom spacer
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun RemoteFileItem(
    file: SmbClient.SmbFile,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (file.isDirectory) "📁" else getFileIcon(file.name),
                fontSize = 22.sp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = file.name,
                    fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMono,
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Text(
            text = if (file.isDirectory) "→" else "⬇",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeonBlue
        )
    }
    Divider(
        modifier = Modifier.padding(start = 40.dp),
        thickness = 0.5.dp,
        color = if (isDark) NovaBorderDark else NovaBorderLight
    )
}

@Composable
private fun LocalFileItem(
    file: File,
    onClick: () -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = getFileIcon(file.name), fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = file.name,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(file.length()),
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMono,
                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f)
                )
            }
        }
        Text(
            text = "⬆",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeonBlue
        )
    }
    Divider(
        modifier = Modifier.padding(start = 40.dp),
        thickness = 0.5.dp,
        color = if (isDark) NovaBorderDark else NovaBorderLight
    )
}

@Composable
private fun HowToStep(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isSystemInDarkTheme()) NovaFrostWhite.copy(alpha = 0.75f) else NovaDeepInk.copy(alpha = 0.75f),
            lineHeight = 18.sp
        )
    }
}

private fun getFileIcon(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> "📕"
        "doc", "docx" -> "📘"
        "xls", "xlsx" -> "📗"
        "ppt", "pptx" -> "📙"
        "txt", "md", "log" -> "📄"
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "🖼️"
        "mp4", "mkv", "avi", "mov", "wmv" -> "🎬"
        "mp3", "m4a", "flac", "wav", "ogg", "aac" -> "🎵"
        "zip", "rar", "7z", "tar", "gz" -> "📦"
        "apk" -> "📲"
        "json", "xml", "csv" -> "📊"
        "html", "htm", "css", "js" -> "🌐"
        "py", "kt", "java", "cpp", "c", "rs" -> "💻"
        else -> "📄"
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

private fun sanitizeFileName(name: String): String {
    return name
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace("..", "_")
        .replace(Regex("[\\r\\n\\u0000]"), "")
        .trim()
        .ifBlank { "downloaded_file_${System.currentTimeMillis()}" }
}
