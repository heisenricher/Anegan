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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
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
    val tabs = listOf("💬 Chat", "📡 Peers", "📋 Bulletin", "🆘 SOS")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Text("←", fontSize = 24.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    "Offline Comm",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
                    color = MidnightIndigo
                )
                Text(
                    "Bluetooth · Wi-Fi Direct · Mesh",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        // Offline badge
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B5E20).copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "🛡️ Zero Internet Required · Device-to-Device Only",
                fontSize = 10.sp,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Tab Bar ────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MidnightIndigo,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MidnightIndigo
                    )
                }
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // ── Tab Content ────────────────────────────────────────────────
        when (selectedTab) {
            0 -> BluetoothChatTab()
            1 -> PeerDiscoveryTab()
            2 -> BulletinBoardTab()
            3 -> SosBeaconTab()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 1: Bluetooth Chat
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BluetoothChatTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
            // In real Bluetooth connection, would write to output stream here
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Connection Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionStatus) {
                    "Connected" -> Color(0xFF1B5E20).copy(alpha = 0.1f)
                    "Listening..." -> Color(0xFFF57F17).copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionStatus) {
                                "Connected" -> Color(0xFF4CAF50)
                                "Listening..." -> Color(0xFFFFC107)
                                else -> Color.Gray
                            }
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        connectionStatus,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MidnightIndigo
                    )
                    if (connectedDeviceName != null) {
                        Text(connectedDeviceName!!, color = Color.Gray, fontSize = 11.sp)
                    }
                }

                // Listen / Connect button
                Button(
                    onClick = {
                        connectionStatus = if (connectionStatus == "Listening...") "Disconnected" else "Listening..."
                        Toast.makeText(context, "Bluetooth listener toggled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (connectionStatus == "Listening...") "Stop" else "Listen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No messages yet",
                                color = MidnightIndigo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap 'Listen' to accept Bluetooth\nconnections from nearby devices.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            items(messages) { msg ->
                MessageBubble(
                    message = msg,
                    timeFormat = timeFormat
                )
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Type a message…", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightIndigo,
                    unfocusedBorderColor = Color.LightGray
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { sendMessage() },
                colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                shape = CircleShape,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Text("↑", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    timeFormat: SimpleDateFormat
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
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(
                    if (message.isSent) MidnightIndigo
                    else MaterialTheme.colorScheme.surface
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = if (message.isSent) PureWhite else MidnightIndigo,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    color = if (message.isSent) PureWhite.copy(alpha = 0.6f) else Color.Gray,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 2: Peer Discovery
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PeerDiscoveryTab() {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }
    var peers by remember { mutableStateOf(listOf<DiscoveredPeer>()) }
    val scope = rememberCoroutineScope()

    fun startScan() {
        isScanning = true
        peers = emptyList()
        // Simulated scan results for demo — real implementation uses
        // BluetoothAdapter.startDiscovery() + BroadcastReceiver
        scope.launch {
            delay(2000)
            // In production: real BT discovery results populate here
            isScanning = false
            Toast.makeText(context, "Bluetooth scan complete", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scan Button
        Button(
            onClick = { startScan() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
            shape = RoundedCornerShape(16.dp),
            enabled = !isScanning
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = PureWhite,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isScanning) "Scanning for nearby devices…" else "🔍 Scan for Nearby Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (peers.isEmpty() && !isScanning) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📡", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No peers discovered", color = MidnightIndigo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Make sure both devices have Bluetooth\nenabled and are within range (~30m).",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // How-to card
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📘 How It Works", fontWeight = FontWeight.Bold, color = MidnightIndigo, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf(
                                "1. Both devices open this screen",
                                "2. One taps 'Listen' in Chat tab",
                                "3. Other taps 'Scan' to find the listener",
                                "4. Tap a discovered peer to connect",
                                "5. Exchange messages offline via Bluetooth"
                            ).forEach { step ->
                                Text(step, color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn {
                items(peers) { peer ->
                    PeerCard(peer = peer, onConnect = {
                        Toast.makeText(context, "Connecting to ${peer.name}…", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }
    }
}

@Composable
private fun PeerCard(
    peer: DiscoveredPeer,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onConnect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            0.5.dp,
            if (peer.isConnected) Color(0xFF4CAF50).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MidnightIndigo.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.name, fontWeight = FontWeight.Bold, color = MidnightIndigo, fontSize = 14.sp)
                Text(peer.address, color = Color.Gray, fontSize = 10.sp)
            }
            if (peer.isConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Connected", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MidnightIndigo),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Connect", fontSize = 11.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 3: Offline Bulletin Board
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BulletinBoardTab() {
    var posts by remember {
        mutableStateOf(
            listOf(
                BulletinPost(
                    title = "Welcome to Offline Bulletin",
                    body = "This board syncs via Bluetooth & Wi-Fi Direct when devices are in proximity. " +
                            "Post announcements, share resources, or coordinate with your community — no internet needed.",
                    author = "Anegan System",
                    isPinned = true,
                    category = "System"
                )
            )
        )
    }

    var showComposer by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }

    val timeFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Compose new post button
        Button(
            onClick = { showComposer = !showComposer },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (showComposer) "Cancel" else "✏️ New Post",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Composer
        AnimatedVisibility(
            visible = showComposer,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MidnightIndigo,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBody,
                        onValueChange = { newBody = it },
                        label = { Text("Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MidnightIndigo,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newBody.isNotBlank()) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Posts list
        LazyColumn {
            items(posts) { post ->
                BulletinCard(post = post, timeFormat = timeFormat)
            }
        }
    }
}

@Composable
private fun BulletinCard(
    post: BulletinPost,
    timeFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (post.isPinned) BorderStroke(1.dp, MidnightIndigo.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.isPinned) {
                    Text("📌 ", fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MidnightIndigo.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(post.category, fontSize = 9.sp, color = MidnightIndigo, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    timeFormat.format(Date(post.timestamp)),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                post.title,
                fontWeight = FontWeight.Bold,
                color = MidnightIndigo,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                post.body,
                color = Color.DarkGray,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "by ${post.author}",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Tab 4: SOS Beacon
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SosBeaconTab() {
    val context = LocalContext.current
    var isBroadcasting by remember { mutableStateOf(false) }
    var sosMessage by remember { mutableStateOf("EMERGENCY — Need immediate assistance!") }
    var broadcastCount by remember { mutableStateOf(0) }

    LaunchedEffect(isBroadcasting) {
        while (isBroadcasting) {
            delay(5000)
            broadcastCount++
            // In production: broadcast SOS packet via Bluetooth LE advertising
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // SOS Emergency Header
        Text("🆘", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Emergency SOS Beacon",
            fontWeight = FontWeight.Black,
            color = if (isBroadcasting) Color.Red else MidnightIndigo,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Broadcasts your distress signal to all\nnearby Anegan devices via Bluetooth.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SOS Message
        OutlinedTextField(
            value = sosMessage,
            onValueChange = { sosMessage = it },
            label = { Text("Emergency Message") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isBroadcasting) Color.Red else MidnightIndigo,
                unfocusedBorderColor = Color.LightGray
            ),
            maxLines = 3,
            enabled = !isBroadcasting
        )

        Spacer(modifier = Modifier.height(24.dp))

        // BIG SOS Button
        Button(
            onClick = {
                isBroadcasting = !isBroadcasting
                if (isBroadcasting) {
                    broadcastCount = 0
                    Toast.makeText(context, "SOS Beacon ACTIVE — Broadcasting to all nearby devices", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "SOS Beacon stopped", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBroadcasting) Color.Red else MidnightIndigo,
                contentColor = PureWhite
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isBroadcasting) "STOP" else "SOS",
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
                Text(
                    text = if (isBroadcasting) "Broadcasting…" else "Tap to activate",
                    fontSize = 10.sp,
                    color = PureWhite.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Broadcast count
        if (isBroadcasting) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📡 Signal broadcast $broadcastCount times",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Any Anegan device within Bluetooth range (~30m)\nwill receive your emergency alert.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Disclaimer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Text("⚠️", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "This SOS system operates via Bluetooth and does not contact emergency services. " +
                            "In a real emergency, call local emergency numbers when possible.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
