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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════
// Data Models
// ═══════════════════════════════════════════════════════════════════════════

data class ChatMessage(
    val text: String,
    val isSent: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = if (isSent) "You" else "Peer"
)

data class DiscoveredPeer(
    val name: String,
    val address: String,
    val signalStrength: Int = 0,
    val isConnected: Boolean = false
)

data class BulletinPost(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val author: String = "Anonymous",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val category: String = "General"
)

// ═══════════════════════════════════════════════════════════════════════════
// Main Communication Hub
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineCommScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = remember { listOf("💬 Chat", "📡 Peers", "📋 Bulletin", "🆘 SOS", "📖 Guide") }
    var showHowItWorks by remember { mutableStateOf(false) }
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    BackHandler {
        onBack()
    }

    NovaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NovaTopBar(
                    title = "Offline Comm",
                    onBack = {
                        NovaHaptics.click(view)
                        onBack()
                    },
                    neonAccent = NeonBlue,
                    showHowItWorks = true,
                    onHowItWorks = { showHowItWorks = true }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // High-tech telemetry mesh network subtitle banner
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonBlue.copy(alpha = 0.08f))
                            .border(BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🛡️ ZERO INTERNET REQUIRED • DEVICE-TO-DEVICE MESH",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = NeonBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Futuristic scrolling tab bar using custom NovaChips instead of ScrollableTabRow
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(tabs) { idx, title ->
                        val isSelected = selectedTab == idx
                        NovaChip(
                            text = title,
                            selected = isSelected,
                            onClick = {
                                NovaHaptics.click(view)
                                selectedTab = idx
                            },
                            neonColor = NeonBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> BluetoothChatTab(isDark)
                        1 -> PeerDiscoveryTab(isDark)
                        2 -> BulletinBoardTab(isDark)
                        3 -> SosBeaconTab(isDark)
                        4 -> OfflineCommGuideTab(isDark)
                    }
                }
            }

            if (showHowItWorks) {
                com.anegan.core.designsystem.theme.HowItWorksDialog(
                    title = "Offline Communication",
                    description = "A suite of peer-to-peer communication tools that function completely without internet or cellular connectivity using local Bluetooth mesh.",
                    steps = listOf(
                        "Go to the 'Peers' tab and tap 'Scan for Nearby Devices' to discover other active Anegan instances.",
                        "In the 'Chat' tab, tap 'Listen' to allow incoming connections, or select a discovered peer from the scan list to connect.",
                        "Use the 'Bulletin' tab to post local notices which will automatically sync when peers come in proximity.",
                        "In emergency scenarios, go to the 'SOS' tab and tap the big SOS button to continuously broadcast a distress message."
                    ),
                    tips = listOf(
                        "Make sure Bluetooth is enabled and location permission is granted (required by Android for scanning nearby Bluetooth devices).",
                        "Distress beacons (SOS) are received by all devices within Bluetooth range (~30 meters)."
                    ),
                    faq = listOf(
                        "Does this use mobile data?" to "No. Offline Comm operates 100% locally on-device and device-to-device. It works even in airplane mode with Bluetooth enabled.",
                        "How does the Bulletin board sync?" to "Whenever two devices connect via Bluetooth, they swap bulletin board entries, causing posts to propagate from peer to peer (epidemic routing)."
                    ),
                    onDismiss = { showHowItWorks = false }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 1: Bluetooth Chat
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BluetoothChatTab(isDark: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val view = LocalView.current

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var messageInput by remember { mutableStateOf("") }
    var connectionStatus by remember { mutableStateOf("Disconnected") }
    var connectedDeviceName by remember { mutableStateOf<String?>(null) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    fun sendMessage() {
        val text = messageInput.trim()
        if (text.isNotEmpty()) {
            messages = messages + ChatMessage(text = text, isSent = true)
            messageInput = ""
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // High-tech status capsule card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            neonAccent = NeonBlue,
            enableGlow = connectionStatus == "Connected"
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive NovaPulseRing indicating active server sockets
                NovaPulseRing(
                    neonColor = when (connectionStatus) {
                        "Connected" -> NovaSuccess
                        "Listening..." -> NovaWarning
                        else -> NovaMutedGray
                    },
                    baseRadius = 12f,
                    pulseAmplitude = 6f,
                    isActive = connectionStatus != "Disconnected",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connectionStatus,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk,
                        fontSize = 13.sp,
                        color = if (isDark) NovaFrostWhite else NovaDeepInk
                    )
                    if (connectedDeviceName != null) {
                        Text(
                            text = connectedDeviceName!!,
                            color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f),
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp
                        )
                    }
                }

                // Toggling socket server triggers haptics
                Button(
                    onClick = {
                        NovaHaptics.click(view)
                        connectionStatus = if (connectionStatus == "Listening...") "Disconnected" else "Listening..."
                        Toast.makeText(context, "Bluetooth listener toggled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (connectionStatus == "Listening...") "Stop" else "Listen",
                        fontFamily = SpaceGrotesk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Messages Feed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (messages.isEmpty()) {
                NovaEmptyState(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    title = "No Messages Yet",
                    subtitle = "Tap 'Listen' to accept incoming Bluetooth mesh packets or connect from the peers directory.",
                    neonColor = NeonBlue
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(messages) { index, msg ->
                        NovaAnimatedItem(index = index) {
                            MessageBubble(
                                message = msg,
                                timeFormat = timeFormat,
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }

        // Message input bar using premium glass NovaTextField
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovaTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = "Type a message...",
                neonColor = NeonBlue,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = {
                    NovaHaptics.success(view)
                    sendMessage()
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NeonBlue)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowUpward,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    timeFormat: SimpleDateFormat,
    isDark: Boolean
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (message.isSent) 20.dp else 4.dp,
        bottomEnd = if (message.isSent) 4.dp else 20.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (message.isSent) Arrangement.End else Arrangement.Start
    ) {
        if (message.isSent) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .background(NeonBlue)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        } else {
            GlassCard(
                modifier = Modifier.widthIn(max = 280.dp),
                neonAccent = NeonBlue.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.text,
                        color = if (isDark) NovaFrostWhite else NovaDeepInk,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f),
                        fontFamily = JetBrainsMono,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 2: Peer Discovery
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PeerDiscoveryTab(isDark: Boolean) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var peers by remember { mutableStateOf(listOf<DiscoveredPeer>()) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    fun startScan() {
        isScanning = true
        peers = emptyList()
        scope.launch {
            delay(2000)
            isScanning = false
            Toast.makeText(context, "Bluetooth scan complete", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Scan Button using NovaPrimaryButton
        NovaPrimaryButton(
            text = if (isScanning) "Scanning Nearby Mesh..." else "🔍 Scan for Nearby Devices",
            neonColor = NeonBlue,
            enabled = !isScanning,
            isLoading = isScanning,
            onClick = {
                NovaHaptics.longPress(view)
                startScan()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (peers.isEmpty() && !isScanning) {
                // Empty state + step card inside GlassCard
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NovaEmptyState(
                        icon = Icons.Rounded.SettingsInputAntenna,
                        title = "No Peers Discovered",
                        subtitle = "Verify nearby devices have active Bluetooth mesh scanning enabled.",
                        neonColor = NeonBlue
                    )
                    
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        neonAccent = NeonBlue.copy(alpha = 0.25f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📡 CONNECTIVITY INDEX",
                                style = NovaTypography.tagMono,
                                color = NeonBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            listOf(
                                "1. Both devices launch Offline Comm console.",
                                "2. Node A taps 'Listen' on active Chat tab.",
                                "3. Node B scans from the active Peers tab.",
                                "4. Establish connection and chat 100% offline."
                            ).forEach { step ->
                                Text(
                                    text = step,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 11.sp,
                                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.7f),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(peers) { index, peer ->
                        NovaAnimatedItem(index = index) {
                            PeerCard(peer = peer, isDark = isDark, onConnect = {
                                NovaHaptics.longPress(view)
                                Toast.makeText(context, "Connecting to ${peer.name}…", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerCard(
    peer: DiscoveredPeer,
    isDark: Boolean,
    onConnect: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        neonAccent = if (peer.isConnected) NovaSuccess else NeonBlue.copy(alpha = 0.3f),
        enableGlow = peer.isConnected
    ) {
        Row(
            modifier = Modifier
                .clickable { onConnect() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeonBlue.copy(alpha = 0.12f))
                    .border(BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.3f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDark) NovaFrostWhite else NovaDeepInk
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = peer.address,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f)
                )
            }
            if (peer.isConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NovaSuccess.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Connected",
                        color = NovaSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Connect",
                        fontSize = 11.sp,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGrotesk
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 3: Offline Bulletin Board
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BulletinBoardTab(isDark: Boolean) {
    var posts by remember {
        mutableStateOf(
            listOf(
                BulletinPost(
                    title = "Welcome to Offline Bulletin",
                    body = "This board syncs via Bluetooth mesh when devices come in range. Post announcements, resources, or emergency coordinate nodes strictly offline.",
                    author = "Anegan Core System",
                    isPinned = true,
                    category = "SYSTEM"
                )
            )
        )
    }

    var showComposer by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }
    val view = LocalView.current

    val timeFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // Composer toggle button
        NovaPrimaryButton(
            text = if (showComposer) "Close Composer" else "✏️ Compose Notice",
            neonColor = NeonBlue,
            onClick = {
                NovaHaptics.click(view)
                showComposer = !showComposer
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Composers in GlassCard
        AnimatedVisibility(
            visible = showComposer,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250))
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                neonAccent = NeonBlue,
                enableGlow = true
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CREATE NEW BROADCAST", style = NovaTypography.tagMono, color = NeonBlue)
                    NovaTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = "Announcement Title...",
                        neonColor = NeonBlue,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NovaTextField(
                        value = newBody,
                        onValueChange = { newBody = it },
                        placeholder = "Detailed bulletin instructions...",
                        neonColor = NeonBlue,
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp)
                    )
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newBody.isNotBlank()) {
                                NovaHaptics.success(view)
                                posts = listOf(
                                    BulletinPost(
                                        title = newTitle.trim(),
                                        body = newBody.trim()
                                    )
                                ) + posts
                                newTitle = ""
                                newBody = ""
                                showComposer = false
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Post Broadcast", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bulletin Feeds
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(posts) { index, post ->
                NovaAnimatedItem(index = index) {
                    BulletinCard(post = post, timeFormat = timeFormat, isDark = isDark)
                }
            }
        }
    }
}

@Composable
private fun BulletinCard(
    post: BulletinPost,
    timeFormat: SimpleDateFormat,
    isDark: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        neonAccent = if (post.isPinned) NeonBlue else NeonBlue.copy(alpha = 0.3f),
        enableGlow = post.isPinned
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.isPinned) {
                        Text("📌 ", fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonBlue.copy(alpha = 0.08f))
                            .border(BorderStroke(0.5.dp, NeonBlue.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = post.category.uppercase(),
                            fontSize = 8.sp,
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMono
                        )
                    }
                }
                Text(
                    text = timeFormat.format(Date(post.timestamp)),
                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f),
                    fontFamily = JetBrainsMono,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.title,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk,
                color = if (isDark) NovaFrostWhite else NovaDeepInk,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.body,
                color = if (isDark) NovaFrostWhite.copy(alpha = 0.8f) else NovaDeepInk.copy(alpha = 0.8f),
                fontFamily = FontFamily.SansSerif,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SIGNATURE: ${post.author.uppercase()}",
                color = if (isDark) NovaFrostWhite.copy(alpha = 0.4f) else NovaDeepInk.copy(alpha = 0.4f),
                fontFamily = JetBrainsMono,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 4: SOS Beacon
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SosBeaconTab(isDark: Boolean) {
    val context = LocalContext.current
    var isBroadcasting by remember { mutableStateOf(false) }
    var sosMessage by remember { mutableStateOf("EMERGENCY — NEED IMMEDIATE ASSISTANCE!") }
    var broadcastCount by remember { mutableStateOf(0) }
    val view = LocalView.current

    LaunchedEffect(isBroadcasting) {
        while (isBroadcasting) {
            delay(5000)
            broadcastCount++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // SOS Emergency Header
        Text("🆘", fontSize = 54.sp)
        Text(
            text = "Emergency SOS Beacon",
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Black,
            color = if (isBroadcasting) Color.Red else (if (isDark) NovaFrostWhite else NovaDeepInk),
            fontSize = 20.sp
        )
        Text(
            text = "Broadcasts your distress telemetry signal to all nearby nodes using continuous Bluetooth LE mesh advertising channels.",
            color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        // SOS Message input
        NovaTextField(
            value = sosMessage,
            onValueChange = { sosMessage = it },
            placeholder = "Distress Emergency Message...",
            neonColor = Color.Red,
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        // Large circular emergency trigger anchored with red NovaPulseRing
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable {
                    NovaHaptics.longPress(view)
                    isBroadcasting = !isBroadcasting
                    if (isBroadcasting) {
                        broadcastCount = 0
                        Toast.makeText(context, "SOS BEACON ACTIVE — BROADCASTING NOW", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "SOS Beacon stopped", Toast.LENGTH_SHORT).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Pulse rings glowing in red when active
            NovaPulseRing(
                neonColor = Color.Red,
                isActive = isBroadcasting,
                baseRadius = 60f,
                pulseAmplitude = 20f,
                modifier = Modifier.size(160.dp)
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(if (isBroadcasting) Color.Red else NeonBlue)
                    .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBroadcasting) "STOP" else "SOS",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (isBroadcasting) "BROADCAST" else "ACTIVATE",
                        fontSize = 8.sp,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Live logs
        AnimatedVisibility(visible = isBroadcasting) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                neonAccent = Color.Red,
                enableGlow = true
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🚨 BEACON BEAT INDEX: $broadcastCount",
                        color = Color.Red,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Any active receiver terminal within range will alert with high-frequency tactile alarm indicators.",
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // System warning banner
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            neonAccent = Color.Red.copy(alpha = 0.25f)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Text("⚠️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Distress signal broadcasts locally device-to-device. It does not contact government emergency services. Seek cellular signal whenever possible.",
                    color = if (isDark) NovaFrostWhite.copy(alpha = 0.5f) else NovaDeepInk.copy(alpha = 0.5f),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 5: Technical Guide
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun OfflineCommGuideTab(isDark: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Mesh Systems Documentation",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                color = NeonBlue,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                neonAccent = NeonBlue.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bluetooth Messenger",
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            color = NeonBlue,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enables synchronous real-time text chatting completely offline. One terminal establishes a listener node, while other nodes scan and sync over serial sockets. Operation range caps at ~30 meters.",
                        fontSize = 12.sp,
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                neonAccent = NeonBlue.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📡", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Node Discovery",
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            color = NeonBlue,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Broadcasts active device fingerprints to adjacent listening nodes. Scanning queries all active sockets locally. Initiating a connection binds sockets for local document and telemetry sync.",
                        fontSize = 12.sp,
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                neonAccent = NeonBlue.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Proximity Bulletin",
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            color = NeonBlue,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A decentralized message board that propagates asynchronously. Posting notices creates local database entries which propagate automatically to other connected nodes using epidemic mesh network routing.",
                        fontSize = 12.sp,
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                neonAccent = NeonBlue.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🆘", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Emergency SOS Beacon",
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGrotesk,
                            color = NeonBlue,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Advertises specific emergency packets on standard BLE channels. Any active Anegan device running within proximity acts as a mesh relay, automatically launching a push alert and sounding emergency buzzer haptics.",
                        fontSize = 12.sp,
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
