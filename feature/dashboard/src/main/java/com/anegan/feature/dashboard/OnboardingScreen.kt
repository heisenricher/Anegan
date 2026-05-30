/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*

data class OnboardingSlide(
    val title: String,
    val description: String,
    val visualType: Int // 0: Welcome, 1: Privacy, 2: Utilities
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlideIndex by remember { mutableStateOf(0) }
    val view = LocalView.current
    
    val slides = listOf(
        OnboardingSlide(
            title = "Welcome to Anegan V3.2",
            description = "The ultimate, all-in-one local file processing suite. Fast, elegant, and exceptionally powerful.",
            visualType = 0
        ),
        OnboardingSlide(
            title = "100% Local & Private 🔒",
            description = "Your files never leave your device. All operations, conversions, and extractions run strictly offline with zero cloud latency.",
            visualType = 1
        ),
        OnboardingSlide(
            title = "Unified Power Utilities 🛠️",
            description = "Batch compress images, play dynamic media files, extract text with OCR, strip EXIF metadata, and share files locally over SMB.",
            visualType = 2
        )
    )

    val currentSlide = slides[currentSlideIndex]
    val isDark = isSystemInDarkTheme()

    NovaBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row (Skip button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentSlideIndex < slides.size - 1) {
                    Text(
                        text = "Skip",
                        color = if (isDark) NovaFrostWhite.copy(alpha = 0.6f) else NovaDeepInk.copy(alpha = 0.6f),
                        fontFamily = SpaceGrotesk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                NovaHaptics.click(view)
                                onFinished()
                            }
                            .padding(8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // Mid section (Visual illustration with premium transitions)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentSlide.visualType,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(450)) togetherWith fadeOut(animationSpec = tween(450))
                    },
                    label = "slide_visual_transition"
                ) { targetVisual ->
                    when (targetVisual) {
                        0 -> WelcomeVisual(isDark)
                        1 -> PrivacyVisual(isDark)
                        2 -> UtilitiesVisual(isDark)
                    }
                }
            }

            // Bottom text and indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Title & Subtitle with Fade-in slide animations
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                    },
                    label = "slide_text_transition"
                ) { slide ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = slide.title,
                            fontFamily = SpaceGrotesk,
                            color = if (isDark) NovaFrostWhite else NovaDeepInk,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = slide.description,
                            fontFamily = FontFamily.SansSerif,
                            color = if (isDark) NovaFrostWhite.copy(alpha = 0.7f) else NovaDeepInk.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Slide Indicators (Active capsule indicator stretches)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    slides.forEachIndexed { index, _ ->
                        val isSelected = index == currentSlideIndex
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "indicator_width"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.3f,
                            label = "indicator_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(NeonBlue.copy(alpha = alpha))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons using NovaPrimaryButton
                NovaPrimaryButton(
                    text = if (currentSlideIndex == slides.size - 1) "Get Started" else "Next Slide",
                    neonColor = NeonBlue,
                    onClick = {
                        NovaHaptics.click(view)
                        if (currentSlideIndex < slides.size - 1) {
                            currentSlideIndex++
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NovaTokens.Touch.minimum)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun WelcomeVisual(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_rot")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = size.minDimension / 2
        
        // Background glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonBlue.copy(alpha = 0.2f), Color.Transparent),
                radius = center * 1.5f
            ),
            radius = center * 1.2f * scale
        )

        // Outer rotating ring
        rotate(rotation) {
            drawCircle(
                color = NeonBlue,
                radius = center * 0.8f,
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 40f), 0f)
                )
            )
        }

        // Inner glowing core
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(NeonBlue, if (isDark) NovaMidnightBlue else NovaGhostWhite)
            ),
            radius = center * 0.5f * scale
        )
    }
}

@Composable
fun PrivacyVisual(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "privacy_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = size.minDimension / 2
        val strokeWidth = 3.dp.toPx()

        // Radial privacy wave
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent),
                radius = center * 1.3f
            ),
            radius = (center * 0.8f) + pulse
        )

        // Draw Shield
        val path = Path().apply {
            moveTo(center, center - 60f)
            lineTo(center + 50f, center - 40f)
            lineTo(center + 50f, center + 10f)
            quadraticBezierTo(center + 50f, center + 60f, center, center + 80f)
            quadraticBezierTo(center - 50f, center + 60f, center - 50f, center + 10f)
            lineTo(center - 50f, center - 40f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(NeonCyan, if (isDark) NovaMidnightBlue else NovaGhostWhite)
            )
        )

        // Draw lock icon outline inside the shield
        drawCircle(
            color = Color.White,
            radius = 16f,
            center = Offset(center, center + 10f),
            style = Stroke(width = strokeWidth)
        )
        
        drawRect(
            color = Color.White,
            topLeft = Offset(center - 20f, center + 10f),
            size = androidx.compose.ui.geometry.Size(40f, 30f)
        )
    }
}

@Composable
fun UtilitiesVisual(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "utils_anim")
    val offset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = size.minDimension / 2
        
        // Media/Creative (Electric Magenta)
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(NeonMagenta, if (isDark) NovaMidnightBlue else NovaGhostWhite)),
            topLeft = Offset(center - 70f, center - 70f + offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Transfer/Connection (Holo Blue)
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(NeonBlue, if (isDark) NovaMidnightBlue else NovaGhostWhite)),
            topLeft = Offset(center + 10f, center - 50f - offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Utility/Tools (Electric Lime)
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(NeonLime, if (isDark) NovaMidnightBlue else NovaGhostWhite)),
            topLeft = Offset(center - 50f, center + 10f - offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Documents/Reading (Cyber Cyan)
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(NeonCyan, if (isDark) NovaMidnightBlue else NovaGhostWhite)),
            topLeft = Offset(center + 20f, center + 20f + offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )
    }
}
