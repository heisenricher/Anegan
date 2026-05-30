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
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anegan.core.conversion.StorageManager
import com.anegan.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class RecorderState { IDLE, RECORDING, PAUSED }
enum class RecordQuality { LOW, NORMAL, HIGH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    // Recorder configuration & state
    var recorderState by remember { mutableStateOf(RecorderState.IDLE) }
    var selectedQuality by remember { mutableStateOf(RecordQuality.HIGH) }
    var timerText by remember { mutableStateOf("00:00:00") }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    
    // Waveform state
    val amplitudes = remember { mutableStateListOf<Float>() }
    
    // Media Recorder instances
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var startTime by remember { mutableStateOf(0L) }
    var elapsedPausedTime by remember { mutableStateOf(0L) }

    // Saved recordings list
    val recordings = remember { mutableStateListOf<File>() }
    
    // Audio playback state
    var playingFile by remember { mutableStateOf<File?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }

    val primaryAccent = NeonLime // Electric Lime for Utility tools

    // Helper to refresh recordings list
    fun refreshRecordings() {
        scope.launch(Dispatchers.IO) {
            val dir = StorageManager.getAneganOutputDirectory("Recordings")
            val files = dir.listFiles()?.filter { it.isFile && (it.extension == "m4a" || it.extension == "mp3" || it.extension == "wav") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
            withContext(Dispatchers.Main) {
                recordings.clear()
                recordings.addAll(files)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshRecordings()
    }

    // Timer and amplitude updates
    LaunchedEffect(recorderState) {
        if (recorderState == RecorderState.RECORDING) {
            val baseTime = if (elapsedPausedTime > 0L) {
                SystemClock.elapsedRealtime() - elapsedPausedTime
            } else {
                SystemClock.elapsedRealtime()
            }
            startTime = baseTime

            while (recorderState == RecorderState.RECORDING) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val secs = (elapsed / 1000) % 60
                val mins = (elapsed / 60000) % 60
                val hrs = (elapsed / 3600000)
                timerText = String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
                
                // Read amplitude
                mediaRecorder?.let {
                    val amp = try {
                        it.maxAmplitude.toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                    // Normalize to 0..1 scale (maxAmplitude typically goes up to 32767)
                    val normAmp = (amp / 32767f).coerceIn(0.03f, 1f)
                    amplitudes.add(normAmp)
                    if (amplitudes.size > 50) {
                        amplitudes.removeAt(0)
                    }
                }
                delay(100)
            }
        }
    }

    // Playback progress update loop
    LaunchedEffect(playingFile, isPlaying) {
        if (isPlaying && mediaPlayer != null) {
            while (isPlaying) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    val duration = mp.duration.toFloat()
                    if (duration > 0f) {
                        playbackProgress = mp.currentPosition.toFloat() / duration
                    }
                } else {
                    isPlaying = false
                }
                delay(200)
            }
        }
    }

    // Cleanup players and recorders on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaPlayer?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                // Ignore errors on sudden exit
            }
        }
    }

    // Function to start recording
    fun startRecording() {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            // Stop any active playback
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            playingFile = null
            isPlaying = false

            val outputDir = StorageManager.getAneganOutputDirectory("Recordings")
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "REC_${formatter.format(Date())}.m4a"
            val file = File(outputDir, filename)
            recordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder = recorder

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(file.absolutePath)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            when (selectedQuality) {
                RecordQuality.HIGH -> {
                    recorder.setAudioSamplingRate(44100)
                    recorder.setAudioEncodingBitRate(256000)
                }
                RecordQuality.NORMAL -> {
                    recorder.setAudioSamplingRate(44100)
                    recorder.setAudioEncodingBitRate(128000)
                }
                RecordQuality.LOW -> {
                    recorder.setAudioSamplingRate(22050)
                    recorder.setAudioEncodingBitRate(64000)
                }
            }

            recorder.prepare()
            recorder.start()
            
            amplitudes.clear()
            elapsedPausedTime = 0L
            recorderState = RecorderState.RECORDING
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_LONG).show()
            recorderState = RecorderState.IDLE
        }
    }

    // Function to pause recording
    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recorderState == RecorderState.RECORDING) {
            try {
                mediaRecorder?.pause()
                elapsedPausedTime = SystemClock.elapsedRealtime() - startTime
                recorderState = RecorderState.PAUSED
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Function to resume recording
    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recorderState == RecorderState.PAUSED) {
            try {
                mediaRecorder?.resume()
                recorderState = RecorderState.RECORDING
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Function to stop recording
    fun stopRecording() {
        if (recorderState != RecorderState.IDLE) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaRecorder = null
            recorderState = RecorderState.IDLE
            timerText = "00:00:00"
            elapsedPausedTime = 0L
            amplitudes.clear()
            refreshRecordings()
            Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
        }
    }

    // Playback controls
    fun playRecording(file: File) {
        try {
            if (playingFile == file) {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    mediaPlayer?.start()
                    isPlaying = true
                }
            } else {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPlaying = false
                        playbackProgress = 0f
                    }
                    start()
                }
                playingFile = file
                isPlaying = true
                playbackProgress = 0f
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteRecording(file: File) {
        if (playingFile == file) {
            mediaPlayer?.release()
            mediaPlayer = null
            playingFile = null
            isPlaying = false
        }
        if (file.exists()) {
            file.delete()
        }
        refreshRecordings()
        Toast.makeText(context, "Recording deleted", Toast.LENGTH_SHORT).show()
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Voice Recorder",
                onBack = onBack,
                neonAccent = primaryAccent
            )
        }
    ) { innerPadding ->
        NovaBackground {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Recorder Main Card
                GlassCard(
                    neonAccent = primaryAccent,
                    enableGlow = (recorderState == RecorderState.RECORDING)
                ) {
                    Column(
                        modifier = Modifier.padding(NovaTokens.Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Quality Settings (Only editable when IDLE)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Encoding Quality",
                                style = NovaTypography.tagMono.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                RecordQuality.values().forEach { quality ->
                                    val isSelected = selectedQuality == quality
                                    NovaChip(
                                        text = quality.name,
                                        selected = isSelected,
                                        onClick = {
                                            if (recorderState == RecorderState.IDLE) {
                                                selectedQuality = quality
                                            }
                                        },
                                        neonColor = primaryAccent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                        // Waveform canvas visualizer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(NovaTokens.Radius.md))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (recorderState == RecorderState.RECORDING) primaryAccent.copy(alpha = 0.3f) else (if (isDark) NovaBorderDark.copy(alpha = 0.15f) else NovaBorderLight.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(NovaTokens.Radius.md)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (recorderState == RecorderState.RECORDING && amplitudes.isNotEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = NovaTokens.Spacing.sm, vertical = NovaTokens.Spacing.xs)) {
                                    val width = size.width
                                    val height = size.height
                                    val spacing = 5.dp.toPx()
                                    val barWidth = 3.dp.toPx()
                                    val maxBars = (width / (barWidth + spacing)).toInt()

                                    // Draw cyber background lines
                                    val gridCount = 5
                                    for (i in 0..gridCount) {
                                        val y = (height / gridCount) * i
                                        drawLine(
                                            color = primaryAccent.copy(alpha = 0.05f),
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(width, y),
                                            strokeWidth = 1f
                                        )
                                    }

                                    val startIdx = (amplitudes.size - maxBars).coerceAtLeast(0)
                                    val visibleAmps = amplitudes.subList(startIdx, amplitudes.size)

                                    visibleAmps.forEachIndexed { index, amp ->
                                        val x = index * (barWidth + spacing) + barWidth / 2
                                        val barHeight = amp * height * 0.85f
                                        val yStart = (height - barHeight) / 2
                                        val yEnd = yStart + barHeight
                                        
                                        drawLine(
                                            color = primaryAccent,
                                            start = androidx.compose.ui.geometry.Offset(x, yStart),
                                            end = androidx.compose.ui.geometry.Offset(x, yEnd),
                                            strokeWidth = barWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = null,
                                        tint = primaryAccent.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (recorderState == RecorderState.PAUSED) "Recording Session Paused" else "Quantum Audio Recorder Ready",
                                        style = NovaTypography.tagMono.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                        // Timer
                        Text(
                            text = timerText,
                            style = NovaTypography.displayLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (recorderState == RecorderState.RECORDING) NovaError else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                        // Recorder Action Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause/Resume button
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recorderState != RecorderState.IDLE) {
                                val intSrc = remember { MutableInteractionSource() }
                                val isPausePressed by intSrc.collectIsPressedAsState()
                                val pauseScale by animateFloatAsState(
                                    targetValue = if (isPausePressed) 0.85f else 1f,
                                    animationSpec = NovaTokens.Motion.springSnappy,
                                    label = "pause_btn_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(end = NovaTokens.Spacing.lg)
                                        .size(56.dp)
                                        .scale(pauseScale)
                                        .clip(CircleShape)
                                        .background(if (isDark) NovaDarkSlate.copy(alpha = 0.6f) else NovaCoolGray100.copy(alpha = 0.6f))
                                        .clickable(
                                            interactionSource = intSrc,
                                            indication = null
                                        ) {
                                            NovaHaptics.toggle(view)
                                            if (recorderState == RecorderState.RECORDING) {
                                                pauseRecording()
                                            } else {
                                                resumeRecording()
                                            }
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = primaryAccent.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (recorderState == RecorderState.RECORDING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (recorderState == RecorderState.RECORDING) "Pause Recording" else "Resume Recording",
                                        tint = primaryAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Main Record / Stop button
                            val isRecording = recorderState != RecorderState.IDLE
                            val buttonColor = if (isRecording) NovaError else primaryAccent
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            
                            val recScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.88f else 1f,
                                animationSpec = NovaTokens.Motion.springBouncy,
                                label = "rec_btn_scale"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(88.dp)
                            ) {
                                // Dynamic expanding pulse ring during active recording
                                if (recorderState == RecorderState.RECORDING) {
                                    NovaPulseRing(
                                        neonColor = NovaError,
                                        baseRadius = 40f,
                                        pulseAmplitude = 12f,
                                        pulseDurationMs = 1200
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .scale(recScale)
                                        .clip(CircleShape)
                                        .background(buttonColor)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            NovaHaptics.recording(view)
                                            if (recorderState == RecorderState.IDLE) {
                                                startRecording()
                                            } else {
                                                stopRecording()
                                            }
                                        }
                                        .drawBehind {
                                            drawCircle(
                                                color = buttonColor.copy(alpha = 0.3f),
                                                radius = size.minDimension / 2 + 6.dp.toPx(),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                        tint = if (isRecording) Color.White else NovaDeepInk,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NovaTokens.Spacing.md))

                // Saved Recordings Section Header
                NovaSectionHeader(
                    title = "Saved Recording Files",
                    neonColor = primaryAccent,
                    count = recordings.size
                )

                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

                // Player Overlay for the current playing item
                AnimatedVisibility(
                    visible = playingFile != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    playingFile?.let { file ->
                        GlassCard(
                            neonAccent = primaryAccent,
                            enableGlow = true
                        ) {
                            Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Headphones, contentDescription = null, tint = primaryAccent)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = NovaTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sizeMb = file.length() / (1024f * 1024f)
                                        Text(
                                            text = String.format("%.2f MB", sizeMb),
                                            style = NovaTypography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            NovaHaptics.click(view)
                                            playRecording(file)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = primaryAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                LinearProgressIndicator(
                                    progress = playbackProgress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = primaryAccent,
                                    trackColor = primaryAccent.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }

                // Recordings List
                if (recordings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📭",
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No local recordings archived yet",
                                style = NovaTypography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.xs),
                        contentPadding = PaddingValues(bottom = NovaTokens.Spacing.lg)
                    ) {
                        items(recordings, key = { it.absolutePath }) { file ->
                            val isPlayingThis = playingFile == file
                            
                            GlassCard(
                                neonAccent = if (isPlayingThis) primaryAccent else Color.Transparent,
                                enableGlow = false,
                                onClick = { playRecording(file) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = NovaTokens.Spacing.md, vertical = NovaTokens.Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primaryAccent.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis && isPlaying) Icons.Rounded.VolumeUp else Icons.Rounded.Mic,
                                            contentDescription = null,
                                            tint = primaryAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = NovaTypography.headlineSmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sizeMb = file.length() / (1024f * 1024f)
                                        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                        val dateStr = formatter.format(Date(file.lastModified()))
                                        Text(
                                            text = String.format(Locale.getDefault(), "%s • %.2f MB", dateStr, sizeMb),
                                            style = NovaTypography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            NovaHaptics.warning(view)
                                            deleteRecording(file)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Recording",
                                            tint = NovaError.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
