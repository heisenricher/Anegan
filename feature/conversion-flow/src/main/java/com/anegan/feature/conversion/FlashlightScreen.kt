/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var cameraId by remember { mutableStateOf<String?>(null) }
    var hasFlash by remember { mutableStateOf(false) }

    // Init Camera flash availability
    LaunchedEffect(Unit) {
        try {
            val list = cameraManager.cameraIdList
            if (list.isNotEmpty()) {
                cameraId = list[0]
                hasFlash = true
            }
        } catch (e: Exception) {
            hasFlash = false
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var isSosActive by remember { mutableStateOf(false) }
    var strobeFrequencyHz by remember { mutableStateOf(0f) } // 0f = Solid light

    // Handler for strobe and SOS execution
    val handler = remember { Handler(Looper.getMainLooper()) }

    // Helper function to toggle flash safely
    fun setFlashMode(on: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, on)
        } catch (e: Exception) {
            // Ignore camera lockouts
        }
    }

    // Effect to control Solid Flashlight state
    LaunchedEffect(isFlashOn, isSosActive, strobeFrequencyHz) {
        // Clear all previous loops
        handler.removeCallbacksAndMessages(null)

        if (isSosActive) {
            // SOS Morse Code pattern: ... --- ... (3 short, 3 long, 3 short)
            val sosPattern = listOf(
                200, 200, 200, 200, 200, 600, // S (...): 3 short pulses
                600, 200, 600, 200, 600, 600, // O (---): 3 long pulses
                200, 200, 200, 200, 200, 1200 // S (...): 3 short pulses + delay
            )
            var patternIdx = 0

            val sosRunnable = object : Runnable {
                var flashState = false
                override fun run() {
                    if (!isSosActive) {
                        setFlashMode(false)
                        return
                    }
                    flashState = !flashState
                    setFlashMode(flashState)

                    val delay = sosPattern[patternIdx]
                    patternIdx = (patternIdx + 1) % sosPattern.size
                    handler.postDelayed(this, delay.toLong())
                }
            }
            handler.post(sosRunnable)
        } else if (isFlashOn && strobeFrequencyHz > 0f) {
            // Strobe frequency flashing mode
            val intervalMs = (1000f / (strobeFrequencyHz * 2f)).toLong()
            val strobeRunnable = object : Runnable {
                var flashState = false
                override fun run() {
                    if (!isFlashOn || strobeFrequencyHz == 0f) {
                        setFlashMode(isFlashOn)
                        return
                    }
                    flashState = !flashState
                    setFlashMode(flashState)
                    handler.postDelayed(this, intervalMs)
                }
            }
            handler.post(strobeRunnable)
        } else {
            // Standard Solid state
            setFlashMode(isFlashOn)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            handler.removeCallbacksAndMessages(null)
            cameraId?.let {
                try {
                    cameraManager.setTorchMode(it, false)
                } catch (e: Exception) {}
            }
        }
    }

    // Animate glow color and shadow depth
    val glowColor by animateColorAsState(
        targetValue = if (isFlashOn || isSosActive) Color(0xFFFFD54F) else Color(0xFFE2E8F0),
        animationSpec = tween(300),
        label = "bulb_glow_color"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Go back to dashboard" }
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                    color = MidnightIndigo
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Flashlight",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        if (!hasFlash) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Camera Flash Detected",
                            color = MidnightIndigo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your device does not support camera torch operations or the camera is currently in use by another app.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))

            // Main Interactive Bulb Button
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(glowColor)
                    .border(
                        width = 4.dp,
                        color = if (isFlashOn || isSosActive) MidnightIndigo else Color.LightGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable {
                        if (isSosActive) {
                            isSosActive = false
                        }
                        isFlashOn = !isFlashOn
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFlashOn || isSosActive) "💡" else "🔌",
                        fontSize = 54.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isFlashOn || isSosActive) "ACTIVE" else "OFF",
                        color = if (isFlashOn || isSosActive) MidnightIndigo else Color.Gray,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Strobe control slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Strobe Mode (Frequency)",
                        fontWeight = FontWeight.Bold,
                        color = MidnightIndigo,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (strobeFrequencyHz == 0f) "Solid flashlight output" else "${strobeFrequencyHz.toInt()} Hz flashing strobe",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = strobeFrequencyHz,
                        onValueChange = {
                            if (isSosActive) isSosActive = false
                            isFlashOn = true
                            strobeFrequencyHz = it
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = MidnightIndigo,
                            activeTrackColor = MidnightIndigo
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency SOS Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSosActive) Color(0xFFFFECEC) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 0.5.dp,
                    color = if (isSosActive) Color.Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency SOS Flasher",
                            fontWeight = FontWeight.Bold,
                            color = if (isSosActive) Color.Red else MidnightIndigo,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Emits standard Morse Code SOS flashing pattern recursively.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            isFlashOn = false
                            strobeFrequencyHz = 0f
                            isSosActive = !isSosActive
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSosActive) Color.Red else MidnightIndigo,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = if (isSosActive) "Stop SOS" else "Start SOS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
