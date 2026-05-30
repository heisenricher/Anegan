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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*

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
    val view = LocalView.current

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

    val isDark = isSystemInDarkTheme()

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

    val isCurrentServerRunning = if (selectedMode == TransferMode.WEB_PORTAL) isHttpRunning else isFtpRunning

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Local Transfer Hub",
                    onBack = onBack,
                    neonAccent = NeonBlue
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Mode Selector Tab Row
                val modes = remember { listOf("WEB PORTAL 🌐", "FTP SERVER 📂") }
                NovaSegmentedControl(
                    items = modes,
                    selectedIndex = if (selectedMode == TransferMode.WEB_PORTAL) 0 else 1,
                    onIndexSelected = { idx ->
                        selectedMode = if (idx == 0) TransferMode.WEB_PORTAL else TransferMode.FTP_SERVER
                    },
                    neonColor = NeonBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Section 1: Pulsing Signal Animation
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentServerRunning) {
                        NovaPulseRing(
                            neonColor = NeonBlue,
                            baseRadius = 50f,
                            pulseAmplitude = 16f
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(NeonBlue.copy(alpha = 0.15f))
                                .border(BorderStroke(1.5.dp, NeonBlue), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🟢", fontSize = 24.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF5350).copy(alpha = 0.15f))
                                .border(BorderStroke(1.5.dp, Color(0xFFEF5350)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔴", fontSize = 24.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Server configuration and credential card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    neonAccent = NeonBlue,
                    enableGlow = true
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (selectedMode == TransferMode.WEB_PORTAL) {
                            // ── Web Portal Configuration View ──
                            if (isHttpRunning) {
                                Text(
                                    text = "WEB SHARING IS LIVE! 🌐",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 16.sp,
                                    color = NeonBlue
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Open this link in any browser on your computer connected to the same Wi-Fi network:",
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(NeonBlue.copy(alpha = 0.08f))
                                        .border(BorderStroke(1.5.dp, NeonBlue.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                                        .clickable {
                                            NovaHaptics.click(view)
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
                                        fontFamily = JetBrainsMono,
                                        color = NeonBlue,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("📋", fontSize = 18.sp)
                                }
                            } else {
                                Text(
                                    text = "WEB PORTAL STOPPED",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 16.sp,
                                    color = Color(0xFFEF5350)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Start the server to drag & drop files directly from any desktop browser to this phone offline.",
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDark) NovaMidnightBlue.copy(alpha = 0.4f) else NovaCoolGray50.copy(alpha = 0.4f))
                                        .border(BorderStroke(0.5.dp, if (isDark) NovaBorderDark else NovaBorderLight), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "IP Address: $httpIp",
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        fontSize = 13.sp,
                                        fontFamily = JetBrainsMono,
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
                                    text = "FTP SERVER IS LIVE! 📂",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 16.sp,
                                    color = NeonBlue
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Connect using WinSCP, FileZilla, or any native file manager using the credentials below:",
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
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
                                        .background(NeonBlue.copy(alpha = 0.08f))
                                        .border(BorderStroke(1.5.dp, NeonBlue.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                                        .clickable {
                                            NovaHaptics.click(view)
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
                                        Text("FTP SERVER URL", style = NovaTypography.tagMono, color = NeonBlue)
                                        Text(
                                            text = ftpUrl,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = JetBrainsMono,
                                            color = NeonBlue,
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
                                        .background(if (isDark) NovaMidnightBlue.copy(alpha = 0.4f) else NovaCoolGray50.copy(alpha = 0.4f))
                                        .border(BorderStroke(0.5.dp, if (isDark) NovaBorderDark else NovaBorderLight), RoundedCornerShape(14.dp))
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PORT", style = NovaTypography.tagMono, color = NeonBlue)
                                        Text("2121", fontSize = 14.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = if (isDark) NovaFrostWhite else NovaDeepInk)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(if (isDark) NovaBorderDark else NovaBorderLight))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("USERNAME", style = NovaTypography.tagMono, color = NeonBlue)
                                        Text("anonymous", fontSize = 14.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = if (isDark) NovaFrostWhite else NovaDeepInk)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(if (isDark) NovaBorderDark else NovaBorderLight))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("PASSWORD", style = NovaTypography.tagMono, color = NeonBlue)
                                        Text("None (Empty)", fontSize = 14.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = if (isDark) NovaFrostWhite else NovaDeepInk)
                                    }
                                }
                            } else {
                                Text(
                                    text = "FTP SERVER STOPPED",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    fontSize = 16.sp,
                                    color = Color(0xFFEF5350)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Expose all Anegan local files instantly. Connect from external systems using FTP protocols over Port 2121.",
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDark) NovaMidnightBlue.copy(alpha = 0.4f) else NovaCoolGray50.copy(alpha = 0.4f))
                                        .border(BorderStroke(0.5.dp, if (isDark) NovaBorderDark else NovaBorderLight), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "IP Address: $ftpIp",
                                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                        fontSize = 13.sp,
                                        fontFamily = JetBrainsMono,
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
                val label = if (selectedMode == TransferMode.WEB_PORTAL) {
                    if (isHttpRunning) "Stop Web sharing" else "Start Web sharing"
                } else {
                    if (isFtpRunning) "Stop FTP Server" else "Start FTP Server"
                }

                NovaPrimaryButton(
                    text = label,
                    neonColor = if (isCurrentServerRunning) Color(0xFFEF5350) else NeonBlue,
                    onClick = {
                        NovaHaptics.longPress(view)
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 4: Live active progress display panel
                AnimatedVisibility(visible = transferFileName != null) {
                    val name = transferFileName ?: ""
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        neonAccent = NeonBlue,
                        enableGlow = true
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUploadTransfer) "📥 UPLOADING FILE..." else "📤 DOWNLOADING FILE...",
                                    style = NovaTypography.tagMono,
                                    color = NeonBlue
                                )
                                Text(
                                    text = String.format("%.0f%%", transferProgress * 100),
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = name,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = transferProgress,
                                color = NeonBlue,
                                trackColor = NeonBlue.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                        }
                    }
                }

                // Section 5: Directory Location Storage Notification
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    neonAccent = NeonBlue.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📂", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "STORAGE & SANDBOX DIRECTORY",
                                style = NovaTypography.tagMono,
                                color = NeonBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val storageDetails = if (selectedMode == TransferMode.WEB_PORTAL) {
                                "All files uploaded via browser are saved in the Anegan/Received/ local directory."
                            } else {
                                "The FTP Server is sandboxed securely to the Anegan/ folder. You can browse, download, and modify folders like Documents, Images, Video, Audio, and Received!"
                            }
                            Text(
                                text = storageDetails,
                                color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
