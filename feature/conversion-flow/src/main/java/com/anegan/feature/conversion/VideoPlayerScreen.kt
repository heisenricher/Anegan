/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUri: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()

    var activeVideoUri by remember { mutableStateOf(videoUri) }
    var localVideos by remember { mutableStateOf(listOf<File>()) }
    var isLoadingVideos by remember { mutableStateOf(false) }

    // Scan for local videos if no videoUri is provided
    LaunchedEffect(activeVideoUri) {
        if (activeVideoUri.isNullOrBlank()) {
            isLoadingVideos = true
            withContext(Dispatchers.IO) {
                val folders = listOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                )
                val list = mutableListOf<File>()
                folders.forEach { folder ->
                    folder.listFiles()?.forEach { file ->
                        if (file.isFile && file.extension.lowercase(Locale.ROOT) in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp")) {
                            list.add(file)
                        }
                    }
                }
                localVideos = list
                isLoadingVideos = false
            }
        }
    }

    if (activeVideoUri.isNullOrBlank()) {
        // Video List Selector Screen
        NovaBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    NovaTopBar(
                        title = "Video Library",
                        onBack = onBack,
                        neonAccent = NeonMagenta
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isLoadingVideos) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonMagenta)
                        }
                    } else if (localVideos.isEmpty()) {
                        NovaEmptyState(
                            icon = Icons.Rounded.OndemandVideo,
                            title = "No local videos found",
                            subtitle = "Place videos in your Movies or Downloads folder.",
                            neonColor = NeonMagenta
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(localVideos) { index, file ->
                                NovaAnimatedItem(index = index) {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        neonAccent = NeonMagenta.copy(alpha = 0.2f),
                                        onClick = { activeVideoUri = file.absolutePath }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NeonMagenta.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Rounded.PlayCircleOutline, null, tint = NeonMagenta)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = file.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val sizeMb = file.length() / (1024f * 1024f)
                                                Text(
                                                    text = String.format(Locale.getDefault(), "%.1f MB", sizeMb),
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Icon(Icons.Rounded.ChevronRight, null, tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Active ExoPlayer Render Layout
        VideoPlayerLayout(
            videoUri = activeVideoUri!!,
            onBack = {
                if (videoUri != null) {
                    onBack()
                } else {
                    activeVideoUri = null
                }
            }
        )
    }
}

@Composable
fun VideoPlayerLayout(
    videoUri: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    val scope = rememberCoroutineScope()

    // ExoPlayer State
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // UI overlays / controls
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    
    // Gestures HUD
    var brightnessOverlayVal by remember { mutableStateOf<Float?>(null) }
    var volumeOverlayVal by remember { mutableStateOf<Int?>(null) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) } // "left" or "right"

    // System Services
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    // Disable Screen Timeout & Lock Orientation if needed
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-fade controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Initialize ExoPlayer
    DisposableEffect(videoUri) {
        val player = ExoPlayer.Builder(context).build().apply {
            val uri = if (videoUri.startsWith("content://") || videoUri.startsWith("file://")) {
                Uri.parse(videoUri)
            } else {
                Uri.fromFile(File(videoUri))
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlay: Boolean) {
                    isPlaying = isPlay
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        duration = this@apply.duration
                    }
                }
            })
        }
        exoPlayer = player

        // Position Tracker Loop
        val tracker = scope.launch {
            while (true) {
                currentPosition = player.currentPosition
                delay(500)
            }
        }

        onDispose {
            tracker.cancel()
            player.release()
            exoPlayer = null
        }
    }

    BackHandler {
        if (isLocked) {
            Toast.makeText(context, "Unlock controls first!", Toast.LENGTH_SHORT).show()
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val halfWidth = size.width / 2
                            if (offset.x > halfWidth) {
                                // Double tap right -> seek +10s
                                exoPlayer?.let {
                                    val newPos = (it.currentPosition + 10000).coerceAtMost(duration)
                                    it.seekTo(newPos)
                                    scope.launch {
                                        doubleTapFeedback = "right"
                                        delay(600)
                                        doubleTapFeedback = null
                                    }
                                }
                            } else {
                                // Double tap left -> seek -10s
                                exoPlayer?.let {
                                    val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                                    it.seekTo(newPos)
                                    scope.launch {
                                        doubleTapFeedback = "left"
                                        delay(600)
                                        doubleTapFeedback = null
                                    }
                                }
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
            // Swipe Gestures for Brightness (Left Side) & Volume (Right Side)
            .pointerInput(isLocked) {
                if (!isLocked) {
                    var isDragLeft = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragLeft = offset.x < size.width / 2
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 300f // Higher means slower change
                            if (isDragLeft) {
                                // Adjust Brightness
                                val attrs = activity?.window?.attributes
                                val currentBrightness = if (attrs?.screenBrightness ?: -1f < 0f) 0.5f else attrs!!.screenBrightness
                                val delta = -dragAmount.y / sensitivity
                                val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
                                attrs?.screenBrightness = newBrightness
                                activity?.window?.attributes = attrs
                                brightnessOverlayVal = newBrightness
                            } else {
                                // Adjust Volume
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = -dragAmount.y / (sensitivity / maxVolume)
                                val newVol = (currentVol + delta.roundToInt()).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                volumeOverlayVal = newVol
                            }
                        },
                        onDragEnd = {
                            brightnessOverlayVal = null
                            volumeOverlayVal = null
                        }
                    )
                }
            }
    ) {
        // Android native PlayerView wrapper
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Double Tap Ripple HUD
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(NovaMidnightBlue.copy(alpha = 0.8f))
                    .border(BorderStroke(1.5.dp, NeonMagenta), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (doubleTapFeedback == "right") Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        contentDescription = null,
                        tint = NeonMagenta,
                        modifier = Modifier.size(36.dp)
                    )
                    Text("10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Brightness/Volume HUD Overlays
        brightnessOverlayVal?.let { valPct ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NovaMidnightBlue.copy(alpha = 0.8f))
                    .border(BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.BrightnessMedium, null, tint = NeonMagenta, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(String.format(Locale.getDefault(), "%.0f%%", valPct * 100), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        volumeOverlayVal?.let { vol ->
            val volPct = (vol.toFloat() / maxVolume * 100).roundToInt()
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NovaMidnightBlue.copy(alpha = 0.8f))
                    .border(BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (vol == 0) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp, null, tint = NeonMagenta, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("$volPct%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Controls UI Layer
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Top controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NovaMidnightBlue.copy(alpha = 0.7f))
                        .border(BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!isLocked) {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NeonMagenta.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                    }
                    
                    val filename = remember(videoUri) {
                        try {
                            File(videoUri).name
                        } catch (e: Exception) {
                            "Offline Video"
                        }
                    }
                    Text(
                        text = filename,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )

                    Row {
                        IconButton(
                            onClick = {
                                if (!isLocked) {
                                    playbackSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.5f
                                        else -> 1.0f
                                    }
                                    exoPlayer?.setPlaybackParameters(PlaybackParameters(playbackSpeed))
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonMagenta.copy(alpha = 0.2f))
                                .size(40.dp)
                        ) {
                            Text("${playbackSpeed}x", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (!isLocked) {
                                    resizeMode = when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(NeonMagenta.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Rounded.AspectRatio, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (!isLocked) {
                                    val currentOrient = activity?.requestedOrientation
                                    if (currentOrient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(NeonMagenta.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Rounded.ScreenRotation, null, tint = Color.White)
                        }
                    }
                }

                // Center Lock Button pulsing ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(64.dp)
                ) {
                    if (isLocked) {
                        NovaPulseRing(
                            neonColor = Color.Red,
                            isActive = true,
                            baseRadius = 24f,
                            pulseAmplitude = 6f
                        )
                    }
                    IconButton(
                        onClick = { isLocked = !isLocked },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isLocked) Color.Red.copy(alpha = 0.8f) else NeonMagenta.copy(alpha = 0.8f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Lock",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isLocked) {
                    // Bottom Controls Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(NovaMidnightBlue.copy(alpha = 0.7f))
                            .border(BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDuration(currentPosition),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            NovaSlider(
                                value = currentPosition.toFloat(),
                                onValueChange = {
                                    currentPosition = it.toLong()
                                    exoPlayer?.seekTo(currentPosition)
                                },
                                valueRange = 0f..maxOf(1f, duration.toFloat()),
                                neonColor = NeonMagenta,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = formatDuration(duration),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { exoPlayer?.seekTo(maxOf(0, currentPosition - 10000)) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Replay10, null, tint = NeonMagenta, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(24.dp))
                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer?.pause() else exoPlayer?.play()
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(NeonMagenta, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = PureWhite,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(Modifier.width(24.dp))
                            IconButton(
                                onClick = { exoPlayer?.seekTo(minOf(duration, currentPosition + 10000)) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Forward10, null, tint = NeonMagenta, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Convert Milliseconds into HH:MM:SS format
private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private val softLightBg = Color(0xFFF8FAFC)
