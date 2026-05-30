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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    var cameraId by remember { mutableStateOf<String?>(null) }
    var hasFlash by remember { mutableStateOf(false) }

    var isFlashOn by remember { mutableStateOf(false) }
    var isSosActive by remember { mutableStateOf(false) }
    var strobeFrequencyHz by remember { mutableStateOf(0f) }
    var intensityLevel by remember { mutableStateOf(1f) }
    var maxIntensity by remember { mutableStateOf(1) }

    val primaryAccent = NeonLime // Electric Lime for Utility tools

    // Init Camera flash availability & max intensity level
    LaunchedEffect(Unit) {
        try {
            val list = cameraManager.cameraIdList
            if (list.isNotEmpty()) {
                val id = list[0]
                cameraId = id
                hasFlash = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    maxIntensity = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    intensityLevel = maxIntensity.toFloat()
                }
            }
        } catch (e: Exception) {
            hasFlash = false
        }
    }

    val handler = remember { Handler(Looper.getMainLooper()) }

    fun setFlashMode(on: Boolean, level: Int) {
        val id = cameraId ?: return
        try {
            if (on) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxIntensity > 1) {
                    cameraManager.turnOnTorchWithStrengthLevel(id, level.coerceIn(1, maxIntensity))
                } else {
                    cameraManager.setTorchMode(id, true)
                }
            } else {
                cameraManager.setTorchMode(id, false)
            }
        } catch (e: Exception) {
            // Ignore camera lockouts
        }
    }

    // Effect to control torch state
    LaunchedEffect(isFlashOn, isSosActive, strobeFrequencyHz, intensityLevel, maxIntensity) {
        handler.removeCallbacksAndMessages(null)

        if (isSosActive) {
            val sosPattern = listOf(
                200, 200, 200, 200, 200, 600, // S
                600, 200, 600, 200, 600, 600, // O
                200, 200, 200, 200, 200, 1200 // S
            )
            var patternIdx = 0

            val sosRunnable = object : Runnable {
                var flashState = false
                override fun run() {
                    if (!isSosActive) {
                        setFlashMode(false, 1)
                        return
                    }
                    flashState = !flashState
                    setFlashMode(flashState, intensityLevel.toInt())

                    val delay = sosPattern[patternIdx]
                    patternIdx = (patternIdx + 1) % sosPattern.size
                    handler.postDelayed(this, delay.toLong())
                }
            }
            handler.post(sosRunnable)
        } else if (isFlashOn && strobeFrequencyHz > 0f) {
            val intervalMs = (1000f / (strobeFrequencyHz * 2f)).toLong()
            val strobeRunnable = object : Runnable {
                var flashState = false
                override fun run() {
                    if (!isFlashOn || strobeFrequencyHz == 0f) {
                        setFlashMode(isFlashOn, intensityLevel.toInt())
                        return
                    }
                    flashState = !flashState
                    setFlashMode(flashState, intensityLevel.toInt())
                    handler.postDelayed(this, intervalMs)
                }
            }
            handler.post(strobeRunnable)
        } else {
            setFlashMode(isFlashOn, intensityLevel.toInt())
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

    val isActive = isFlashOn || isSosActive
    val activeColor = if (isSosActive) NovaError else primaryAccent

    val glowColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(NovaTokens.Motion.normal),
        label = "bulb_glow_color"
    )

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            NovaTopBar(
                title = "Flashlight",
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
                    .padding(NovaTokens.Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (!hasFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            neonAccent = NovaError,
                            enableGlow = true
                        ) {
                            Column(
                                modifier = Modifier.padding(NovaTokens.Spacing.xl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🔦", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Camera Flash Detected",
                                    style = NovaTypography.headlineLarge.copy(
                                        color = NovaError,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your device does not support camera torch operations or the camera is currently in use.",
                                    style = NovaTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Holographic Bulb & Pulse Rings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Expanding active pulse rings
                        if (isActive) {
                            NovaPulseRing(
                                neonColor = activeColor,
                                baseRadius = 80f,
                                pulseAmplitude = 18f,
                                pulseDurationMs = if (isSosActive) 1000 else 2000
                            )
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.9f else 1f,
                            animationSpec = NovaTokens.Motion.springBouncy,
                            label = "power_btn_scale"
                        )

                        // Interactive central power button
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(glowColor)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    NovaHaptics.toggle(view)
                                    if (isSosActive) isSosActive = false
                                    isFlashOn = !isFlashOn
                                }
                                .border(
                                    width = 2.dp,
                                    color = if (isActive) activeColor else if (isSystemInDarkTheme()) NovaBorderDark else NovaBorderLight,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))
                                Text(
                                    text = if (isActive) "ACTIVE" else "OFF",
                                    style = NovaTypography.tagMono.copy(
                                        color = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Black
                                    )
                                )
                            }
                        }
                    }

                    // Brightness and Strobe Controls
                    Column(
                        verticalArrangement = Arrangement.spacedBy(NovaTokens.Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Brightness Slider Card
                        GlassCard(
                            neonAccent = primaryAccent,
                            enableGlow = false
                        ) {
                            Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                                Text(
                                    text = "Brightness Intensity",
                                    style = NovaTypography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (maxIntensity > 1) "Level ${intensityLevel.toInt()} of $maxIntensity" else "Hardware intensity control not supported on this device.",
                                    style = NovaTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                NovaSlider(
                                    value = intensityLevel,
                                    onValueChange = {
                                        intensityLevel = it
                                        if (isFlashOn) {
                                            setFlashMode(true, intensityLevel.toInt())
                                        }
                                    },
                                    valueRange = 1f..maxIntensity.toFloat().coerceAtLeast(1f),
                                    steps = if (maxIntensity > 1) maxIntensity - 2 else 0,
                                    neonColor = primaryAccent
                                )
                            }
                        }

                        // Strobe Frequency Slider Card
                        GlassCard(
                            neonAccent = primaryAccent,
                            enableGlow = false
                        ) {
                            Column(modifier = Modifier.padding(NovaTokens.Spacing.md)) {
                                Text(
                                    text = "Strobe Mode (Frequency)",
                                    style = NovaTypography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (strobeFrequencyHz == 0f) "Solid flashlight output" else "${strobeFrequencyHz.toInt()} Hz flashing strobe",
                                    style = NovaTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))
                                NovaSlider(
                                    value = strobeFrequencyHz,
                                    onValueChange = {
                                        if (isSosActive) isSosActive = false
                                        isFlashOn = true
                                        strobeFrequencyHz = it
                                    },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    neonColor = primaryAccent
                                )
                            }
                        }

                        // Emergency SOS Flasher Card (Glass Red Alert Theme)
                        GlassCard(
                            neonAccent = NovaError,
                            enableGlow = isSosActive
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(NovaTokens.Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Emergency SOS Flasher",
                                        style = NovaTypography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSosActive) NovaError else MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "Emits standard Morse Code SOS flashing pattern recursively.",
                                        style = NovaTypography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))
                                Button(
                                    onClick = {
                                        NovaHaptics.warning(view)
                                        isFlashOn = false
                                        strobeFrequencyHz = 0f
                                        isSosActive = !isSosActive
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSosActive) NovaError else NovaError.copy(alpha = 0.15f),
                                        contentColor = if (isSosActive) NovaDeepInk else NovaError
                                    ),
                                    shape = RoundedCornerShape(NovaTokens.Radius.sm),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = if (isSosActive) "Stop" else "Start",
                                        style = NovaTypography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
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
