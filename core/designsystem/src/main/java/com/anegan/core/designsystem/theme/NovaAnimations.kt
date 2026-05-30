/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.designsystem.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════
// NOVA ANIMATION PRIMITIVES — V3.2
// Reusable animation components for the futuristic UI
// ═══════════════════════════════════════════════════════

// ─────────────────────────────────────────────────
// NOVA BACKGROUND — Animated grid + particles
// ─────────────────────────────────────────────────

private data class NovaParticle(
    val x: Float,
    val baseY: Float,
    val radius: Float,
    val color: Color,
    val speed: Float,
    val amplitude: Float
)

private fun generateNovaParticles(
    width: Float,
    height: Float,
    count: Int = 20,
    isDark: Boolean
): List<NovaParticle> {
    val neonColors = if (isDark) {
        listOf(
            NeonMagenta.copy(alpha = 0.08f),
            NeonCyan.copy(alpha = 0.06f),
            NeonLime.copy(alpha = 0.05f),
            NeonPurple.copy(alpha = 0.07f),
            NeonBlue.copy(alpha = 0.06f),
            NeonGold.copy(alpha = 0.05f)
        )
    } else {
        listOf(
            NeonMagenta.copy(alpha = 0.04f),
            NeonCyan.copy(alpha = 0.03f),
            NeonPurple.copy(alpha = 0.04f),
            NeonBlue.copy(alpha = 0.03f)
        )
    }

    return List(count) {
        NovaParticle(
            x = Random.nextFloat() * width,
            baseY = Random.nextFloat() * height,
            radius = Random.nextFloat() * 3f + 1f,
            color = neonColors.random(),
            speed = Random.nextFloat() * 0.5f + 0.2f,
            amplitude = Random.nextFloat() * 30f + 10f
        )
    }
}

/**
 * NovaBackground — A living, breathing background canvas.
 *
 * Features:
 * - Subtle dot grid pattern (cyberpunk aesthetic)
 * - Floating neon particles that drift gently
 * - CRT scanline overlay in dark mode
 * - Adapts automatically to light/dark theme
 */
@Composable
fun NovaBackground(
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    showParticles: Boolean = true,
    showScanlines: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) NovaDeepSpace else NovaGhostWhite

    val infiniteTransition = rememberInfiniteTransition(label = "nova_bg")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nova_bg_time"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                val gridSpacing = 40.dp.toPx()

                // Draw subtle dot grid
                if (showGrid) {
                    val dotColor = if (isDark)
                        NovaBorderDark.copy(alpha = 0.15f)
                    else
                        NovaBorderLight.copy(alpha = 0.10f)

                    val dotRadius = if (isDark) 1f else 0.8f

                    var x = 0f
                    while (x < size.width) {
                        var y = 0f
                        while (y < size.height) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += gridSpacing
                        }
                        x += gridSpacing
                    }
                }

                // Draw floating particles
                if (showParticles) {
                    val particles = generateNovaParticles(size.width, size.height, 15, isDark)
                    particles.forEach { p ->
                        val animY = p.baseY + sin((time * p.speed).toDouble()).toFloat() * p.amplitude
                        val wrappedY = ((animY % size.height) + size.height) % size.height
                        drawCircle(
                            color = p.color,
                            radius = p.radius * (if (isDark) 2f else 1.5f),
                            center = Offset(p.x, wrappedY)
                        )
                    }
                }

                // CRT scanlines (dark mode only)
                if (showScanlines && isDark) {
                    val scanlineColor = Color.White.copy(alpha = 0.02f)
                    var sy = 0f
                    while (sy < size.height) {
                        drawLine(
                            color = scanlineColor,
                            start = Offset(0f, sy),
                            end = Offset(size.width, sy),
                            strokeWidth = 1f
                        )
                        sy += 4f
                    }
                }
            }
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────
// HOLOGRAPHIC SHIMMER — Premium loading effect
// ─────────────────────────────────────────────────

/**
 * NovaHoloShimmer — A rainbow holographic sweep effect for loading states.
 *
 * Replaces the basic gray shimmer with a futuristic neon rainbow sweep
 * that rotates through all category neon colors.
 */
@Composable
fun NovaHoloShimmer(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(NovaTokens.Radius.md)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "holo_shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "holo_translate"
    )

    val isDark = isSystemInDarkTheme()
    val baseAlpha = if (isDark) 0.12f else 0.08f

    val holoColors = listOf(
        NeonMagenta.copy(alpha = baseAlpha),
        NeonCyan.copy(alpha = baseAlpha * 1.2f),
        NeonLime.copy(alpha = baseAlpha),
        NeonPurple.copy(alpha = baseAlpha * 1.1f),
        NeonBlue.copy(alpha = baseAlpha),
        NeonGold.copy(alpha = baseAlpha * 1.2f),
        NeonMagenta.copy(alpha = baseAlpha)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (isDark) NovaMidnightBlue.copy(alpha = 0.5f)
                else NovaCoolGray100.copy(alpha = 0.5f)
            )
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = holoColors,
                        start = Offset(translateAnim - 500f, 0f),
                        end = Offset(translateAnim + 500f, size.height)
                    )
                )
            }
    )
}

// ─────────────────────────────────────────────────
// PULSE RING — Expanding neon ring for active states
// ─────────────────────────────────────────────────

/**
 * NovaPulseRing — An expanding/contracting neon ring animation.
 *
 * Used for:
 * - Recording states (voice recorder, camera)
 * - Active/live indicators
 * - Attention-grabbing highlights
 */
@Composable
fun NovaPulseRing(
    neonColor: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    baseRadius: Float = 40f,
    pulseAmplitude: Float = 12f,
    pulseDurationMs: Int = 1500
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Canvas(modifier = modifier.size((baseRadius * 2 + pulseAmplitude * 2).dp)) {
        // Outer expanding ring
        drawCircle(
            color = neonColor.copy(alpha = pulseAlpha * 0.3f),
            radius = baseRadius + pulseAmplitude * pulseScale,
            style = Stroke(width = 3f)
        )

        // Inner ring (steady glow)
        drawCircle(
            color = neonColor.copy(alpha = 0.15f),
            radius = baseRadius,
            style = Stroke(width = 2f)
        )

        // Core glow
        drawCircle(
            color = neonColor.copy(alpha = pulseAlpha * 0.1f),
            radius = baseRadius * (1f + pulseScale * 0.3f)
        )
    }
}

// ─────────────────────────────────────────────────
// STAGGERED LIST — Items enter with cascade animation
// ─────────────────────────────────────────────────

/**
 * NovaAnimatedItem — A wrapper that animates a single item's entry.
 *
 * Use inside a LazyColumn/Column to create staggered entry effects.
 * Each item fades in + slides up with a configurable delay.
 *
 * @param index The item's position in the list (used for stagger delay)
 * @param staggerDelay Milliseconds between each item's animation start
 */
@Composable
fun NovaAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    staggerDelay: Int = NovaTokens.Motion.staggerDelay,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index.toLong() * staggerDelay)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = NovaTokens.Motion.normal,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            initialOffsetY = { 24 },
            animationSpec = tween(
                durationMillis = NovaTokens.Motion.normal,
                easing = FastOutSlowInEasing
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────
// NEON GLOW MODIFIER — Adds glow behind any element
// ─────────────────────────────────────────────────

/**
 * Adds a subtle neon glow effect behind the composable.
 * Best used on dark backgrounds where the glow is visible.
 */
fun Modifier.neonGlow(
    color: Color,
    alpha: Float = 0.15f,
    radiusMultiplier: Float = 0.6f
): Modifier = this.drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                Color.Transparent
            )
        ),
        radius = size.maxDimension * radiusMultiplier
    )
}
