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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbShareScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🗄️", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "SMB File Sharing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                if (isConnected) "Connected to \\\\$hostAddress\\$shareName"
                                else "Browse network shares offline",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            try { smbClient.disconnect() } catch (_: Exception) {}
                        }
                        onBack()
                    }) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── CONNECTION PANEL ──────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    if (isConnected) "Connected"
                                    else if (isConnecting) "Connecting..."
                                    else "Not Connected",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        AnimatedVisibility(visible = !isConnected) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Host Address
                                OutlinedTextField(
                                    value = hostAddress,
                                    onValueChange = { hostAddress = it.trim() },
                                    label = { Text("IP Address / Hostname") },
                                    placeholder = { Text("192.168.1.100") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                // Share Name
                                OutlinedTextField(
                                    value = shareName,
                                    onValueChange = { shareName = it.trim() },
                                    label = { Text("Share Name") },
                                    placeholder = { Text("SharedFolder") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                // Username
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Username (optional)") },
                                    placeholder = { Text("guest") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                // Password
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password (optional)") },
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    trailingIcon = {
                                        TextButton(onClick = { showPassword = !showPassword }) {
                                            Text(if (showPassword) "🙈" else "👁️", fontSize = 18.sp)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                // Error message
                                connectionError?.let { err ->
                                    Text(
                                        err,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        // Connect / Disconnect button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                        return@Button
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
                                        } catch (e: Exception) {
                                            isConnecting = false
                                            connectionError = e.message ?: "Connection failed"
                                        }
                                    }
                                }
                            },
                            enabled = !isConnecting && !isTransferring,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                if (isConnecting) "Connecting..."
                                else if (isConnected) "⏏ Disconnect"
                                else "🔗 Connect",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── TRANSFER PROGRESS ─────────────────────────────────────
            if (isTransferring) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                transferStatus ?: "Transferring...",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            LinearProgressIndicator(
                                progress = transferProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Text(
                                "${(transferProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        "Remote Share",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                if (remotePathStack.isNotEmpty()) {
                                    TextButton(onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                    }) {
                                        Text("⬆ Up", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Text(
                                "\\\\$hostAddress\\$shareName${if (remotePath.isNotEmpty()) "\\$remotePath" else ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            if (isLoadingRemote) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            } else if (remoteFiles.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "📭 Empty directory",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                ) {
                                    remoteFiles.forEach { file ->
                                        RemoteFileItem(
                                            file = file,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                        isTransferring = false
                                                        transferStatus = null
                                                    }
                                                }
                                            }
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📱", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Local Files (Anegan)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Text(
                                "Tap a file to upload it to the remote share",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                ) {
                                    localFiles.forEach { file ->
                                        LocalFileItem(
                                            file = file,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isTransferring = false
                                                    transferStatus = null
                                                }
                                            }
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📋", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Transfer History",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            transferHistory.take(10).forEach { entry ->
                                Text(
                                    entry,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "📖 How to Connect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            HowToStep("1️⃣", "Ensure your device is on the same Wi-Fi network as the target computer/NAS")
                            HowToStep("2️⃣", "On Windows: Right-click a folder → Properties → Sharing → Share")
                            HowToStep("3️⃣", "Enter the computer's IP address (e.g., 192.168.1.100)")
                            HowToStep("4️⃣", "Enter the share name (the folder name you shared)")
                            HowToStep("5️⃣", "Username & password are optional for guest-accessible shares")
                            HowToStep("💡", "NAS devices: use your NAS IP and share name from your NAS settings")
                        }
                    }
                }
            }

            // Bottom spacer
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun RemoteFileItem(
    file: SmbClient.SmbFile,
    onClick: () -> Unit
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
                if (file.isDirectory) "📁" else getFileIcon(file.name),
                fontSize = 22.sp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    file.name,
                    fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Text(
                        formatFileSize(file.size),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Text(
            if (file.isDirectory) "→" else "⬇",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Divider(
        modifier = Modifier.padding(start = 40.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun LocalFileItem(
    file: File,
    onClick: () -> Unit
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
            Text(getFileIcon(file.name), fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    file.name,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatFileSize(file.length()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Text(
            "⬆",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Divider(
        modifier = Modifier.padding(start = 40.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun HowToStep(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
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
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
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
