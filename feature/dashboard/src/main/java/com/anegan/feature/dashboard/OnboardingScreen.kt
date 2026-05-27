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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite

data class OnboardingSlide(
    val title: String,
    val description: String,
    val visualType: Int // 0: Welcome, 1: Privacy, 2: Utilities, 3: Assistant
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlideIndex by remember { mutableStateOf(0) }
    
    val slides = listOf(
        OnboardingSlide(
            title = "Welcome to Anegan ✨",
            description = "The ultimate, all-in-one local file processing suite. Fast, elegant, and powerful.",
            visualType = 0
        ),
        OnboardingSlide(
            title = "100% Local & Private 🔒",
            description = "Your files never leave your device. All conversions, compressions, and AI models run strictly offline.",
            visualType = 1
        ),
        OnboardingSlide(
            title = "File Power Tools 🛠️",
            description = "Batch compress images, trim videos, split PDFs, extract text with OCR, strip EXIF metadata, and more.",
            visualType = 2
        ),
        OnboardingSlide(
            title = "Ask Anegan AI ⚡",
            description = "Describe what you need in natural language. Our smart offline assistant will find and launch the right tool instantly.",
            visualType = 3
        )
    )

    val currentSlide = slides[currentSlideIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightIndigo)
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
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onFinished() }
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
                    fadeIn(animationSpec = tween(400)) with fadeOut(animationSpec = tween(400))
                },
                label = "slide_visual_transition"
            ) { targetVisual ->
                when (targetVisual) {
                    0 -> WelcomeVisual()
                    1 -> PrivacyVisual()
                    2 -> UtilitiesVisual()
                    3 -> AssistantVisual()
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
                    (slideInHorizontally { width -> width / 3 } + fadeIn()) with
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
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = slide.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
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
                        targetValue = if (isSelected) 1f else 0.4f,
                        label = "indicator_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7C6FFF).copy(alpha = alpha))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (currentSlideIndex < slides.size - 1) {
                        currentSlideIndex++
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C6FFF),
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = if (currentSlideIndex == slides.size - 1) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WelcomeVisual() {
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
                colors = listOf(Color(0xFF7C6FFF).copy(alpha = 0.2f), Color.Transparent),
                radius = center * 1.5f
            ),
            radius = center * 1.2f * scale
        )

        // Outer rotating ring
        rotate(rotation) {
            drawCircle(
                color = Color(0xFF7C6FFF),
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
                colors = listOf(Color(0xFF7C6FFF), Color(0xFF0F2952))
            ),
            radius = center * 0.5f * scale
        )
    }
}

@Composable
fun PrivacyVisual() {
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
                colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.15f), Color.Transparent),
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
                colors = listOf(Color(0xFF06B6D4), Color(0xFF0F2952))
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
fun UtilitiesVisual() {
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
        
        // Drawer layout visual grid represent file types
        // Image preset
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(Color(0xFFEF4444), Color(0xFF8B5CF6))),
            topLeft = Offset(center - 70f, center - 70f + offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Video preset
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(Color(0xFF3B82F6), Color(0xFF00F2FE))),
            topLeft = Offset(center + 10f, center - 50f - offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Audio preset
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(Color(0xFF10B981), Color(0xFF059669))),
            topLeft = Offset(center - 50f, center + 10f - offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // PDF preset
        drawRoundRect(
            brush = Brush.linearGradient(colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            topLeft = Offset(center + 20f, center + 20f + offset),
            size = androidx.compose.ui.geometry.Size(60f, 60f),
            cornerRadius = CornerRadius(16f, 16f)
        )
    }
}

@Composable
fun AssistantVisual() {
    val infiniteTransition = rememberInfiniteTransition(label = "ass_dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val center = size.minDimension / 2

        // Speech bubble path
        val path = Path().apply {
            moveTo(center - 70f, center - 40f)
            lineTo(center + 70f, center - 40f)
            quadraticBezierTo(center + 90f, center - 40f, center + 90f, center - 10f)
            lineTo(center + 90f, center + 30f)
            quadraticBezierTo(center + 90f, center + 50f, center + 70f, center + 50f)
            lineTo(center - 10f, center + 50f)
            lineTo(center - 40f, center + 75f)
            lineTo(center - 45f, center + 50f)
            lineTo(center - 70f, center + 50f)
            quadraticBezierTo(center - 90f, center + 50f, center - 90f, center + 30f)
            lineTo(center - 90f, center - 10f)
            quadraticBezierTo(center - 90f, center - 40f, center - 70f, center - 40f)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF7C6FFF), Color(0xFFC13584))
            )
        )

        // Glowing particles
        drawCircle(color = Color.White, radius = 5f * dot1Scale, center = Offset(center - 40f, center + 5f))
        drawCircle(color = Color.White, radius = 5f * dot2Scale, center = Offset(center, center + 5f))
        drawCircle(color = Color.White, radius = 5f * dot3Scale, center = Offset(center + 40f, center + 5f))
    }
}
