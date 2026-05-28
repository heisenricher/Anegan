/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.wifitransfer

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

enum class TransferMode {
    WEB_PORTAL,
    FTP_SERVER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiTransferScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedMode by remember { mutableStateOf(TransferMode.WEB_PORTAL) }

    // HTTP Web Portal Server States
    var isHttpRunning by remember { mutableStateOf(WifiTransferServer.isServerRunning()) }
    var httpUrl by remember { mutableStateOf(WifiTransferServer.getWebUrl()) }
    var httpIp by remember { mutableStateOf(WifiTransferServer.getIpAddress() ?: "No Wi-Fi Connection") }

    // FTP Server States
    var isFtpRunning by remember { mutableStateOf(FtpServer.isServerRunning()) }
    var ftpUrl by remember { mutableStateOf(FtpServer.getFtpUrl()) }
    var ftpIp by remember { mutableStateOf(FtpServer.getIpAddress() ?: "No Wi-Fi Connection") }

    // Active transfer status states
    var transferFileName by remember { mutableStateOf<String?>(null) }
    var transferProgress by remember { mutableStateOf(0.0f) }
    var isUploadTransfer by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMode) {
        // Clear active transfer visualization on tab change to prevent confusion
        transferFileName = null
        
        // Re-read static running statuses
        isHttpRunning = WifiTransferServer.isServerRunning()
        httpUrl = WifiTransferServer.getWebUrl()
        httpIp = WifiTransferServer.getIpAddress() ?: "No Wi-Fi Connection"

        isFtpRunning = FtpServer.isServerRunning()
        ftpUrl = FtpServer.getFtpUrl()
        ftpIp = FtpServer.getIpAddress() ?: "No Wi-Fi Connection"
    }

    LaunchedEffect(Unit) {
        // Register HTTP server hooks
        WifiTransferServer.onServerStatusChanged = { running ->
            isHttpRunning = running
            httpUrl = WifiTransferServer.getWebUrl()
            httpIp = WifiTransferServer.getIpAddress() ?: "No Wi-Fi Connection"
        }
        WifiTransferServer.onTransferProgress = { filename, progress, isUpload ->
            if (selectedMode == TransferMode.WEB_PORTAL) {
                if (progress >= 1.0f) {
                    transferFileName = null
                } else {
                    transferFileName = filename
                    transferProgress = progress
                    isUploadTransfer = isUpload
                }
            }
        }

        // Register FTP server hooks
        FtpServer.onServerStatusChanged = { running ->
            isFtpRunning = running
            ftpUrl = FtpServer.getFtpUrl()
            ftpIp = FtpServer.getIpAddress() ?: "No Wi-Fi Connection"
        }
        FtpServer.onTransferProgress = { filename, progress, isUpload ->
            if (selectedMode == TransferMode.FTP_SERVER) {
                if (progress >= 1.0f) {
                    transferFileName = null
                } else {
                    transferFileName = filename
                    transferProgress = progress
                    isUploadTransfer = isUpload
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    // Micro-animation loops for server pulsing
    val infiniteTransition = rememberInfiniteTransition(label = "SignalTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val isCurrentServerRunning = if (selectedMode == TransferMode.WEB_PORTAL) isHttpRunning else isFtpRunning

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Local Transfer Hub",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                            color = MidnightIndigo
                        )
                        Text("Connect and share files locally offline", color = Color.Gray, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Segmented Mode Selector Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Gray.copy(alpha = 0.08f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransferMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MidnightIndigo else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = mode
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (mode == TransferMode.WEB_PORTAL) "Web Portal 🌐" else "FTP Server 📂",
                            color = if (isSelected) PureWhite else Color.DarkGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section 1: Pulsing Signal Animation
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentServerRunning) Color(0xFF3F51B5).copy(alpha = 0.1f)
                        else Color(0xFFEF5350).copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentServerRunning) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        drawCircle(
                            color = Color(0xFF3F51B5).copy(alpha = pulseAlpha),
                            radius = (size.minDimension / 2) * pulseScale,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF3F51B5),
                            radius = 22.dp.toPx()
                        )
                    }
                    Text("🟢", fontSize = 18.sp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF5350)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔴", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Server configuration and credential card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedMode == TransferMode.WEB_PORTAL) {
                        // ── Web Portal Configuration View ──
                        if (isHttpRunning) {
                            Text(
                                text = "Web sharing is Live! 🌐",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Open this link in any browser on your computer connected to the same Wi-Fi network:",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MidnightIndigo.copy(alpha = 0.05f))
                                    .border(0.5.dp, MidnightIndigo.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Anegan Link", httpUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Link Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = httpUrl,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightIndigo,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("📋", fontSize = 18.sp)
                            }
                        } else {
                            Text(
                                text = "Web Portal Stopped",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start the server to drag & drop files directly from any desktop browser to this phone offline.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.LightGray.copy(alpha = 0.15f))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "IP Address: $httpIp",
                                    color = Color.DarkGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // ── FTP Server Configuration View ──
                        if (isFtpRunning) {
                            Text(
                                text = "FTP Server is Live! 📂",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Connect using WinSCP, FileZilla, or any native file manager using the credentials below:",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // FTP Link Copy Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MidnightIndigo.copy(alpha = 0.05f))
                                    .border(0.5.dp, MidnightIndigo.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Anegan FTP Link", ftpUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "FTP Link Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("FTP SERVER URL", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = ftpUrl,
                                        fontWeight = FontWeight.Bold,
                                        color = MidnightIndigo,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text("📋", fontSize = 18.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // FTP Credentials Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Gray.copy(alpha = 0.05f))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PORT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("2121", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                                }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("USERNAME", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("anonymous", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                                }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PASSWORD", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text("None (Empty)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MidnightIndigo)
                                }
                            }
                        } else {
                            Text(
                                text = "FTP Server Stopped",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Expose all Anegan local files instantly. Connect from external systems using FTP protocols over Port 2121.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.LightGray.copy(alpha = 0.15f))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "IP Address: $ftpIp",
                                    color = Color.DarkGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Server Start / Stop Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedMode == TransferMode.WEB_PORTAL) {
                        if (isHttpRunning) {
                            WifiTransferServer.stopServer()
                        } else {
                            WifiTransferServer.startServer(context, scope)
                        }
                        isHttpRunning = WifiTransferServer.isServerRunning()
                    } else {
                        if (isFtpRunning) {
                            FtpServer.stopServer()
                        } else {
                            FtpServer.startServer(context, scope)
                        }
                        isFtpRunning = FtpServer.isServerRunning()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentServerRunning) Color(0xFFEF5350) else MidnightIndigo,
                    contentColor = PureWhite
                )
            ) {
                val label = if (selectedMode == TransferMode.WEB_PORTAL) {
                    if (isHttpRunning) "Stop Web sharing" else "Start Web sharing"
                } else {
                    if (isFtpRunning) "Stop FTP Server" else "Start FTP Server"
                }
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Live active progress display panel
            AnimatedVisibility(visible = transferFileName != null) {
                val name = transferFileName ?: ""
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isUploadTransfer) "📥 Uploading File..." else "📤 Downloading...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightIndigo
                            )
                            Text(
                                text = String.format("%.0f%%", transferProgress * 100),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightIndigo
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            fontWeight = FontWeight.SemiBold,
                            color = MidnightIndigo,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = transferProgress,
                            color = MidnightIndigo,
                            trackColor = MidnightIndigo.copy(alpha = 0.1f),
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }

            // Section 5: Directory Location Storage Notification
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Storage & Sandbox Directory",
                            fontWeight = FontWeight.Bold,
                            color = MidnightIndigo,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val storageDetails = if (selectedMode == TransferMode.WEB_PORTAL) {
                            "All files uploaded via browser are saved in the Downloads/Anegan/Received/ local directory."
                        } else {
                            "The FTP Server is sandboxed securely to the Downloads/Anegan/ folder. You can browse, download, and modify output folders like Documents, Images, Video, Audio, and Received!"
                        }
                        Text(
                            text = storageDetails,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
