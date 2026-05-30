/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 *
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.designsystem.theme

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════
// NOVA COMPONENT LIBRARY — V3.2
// Futuristic UI primitives: Glass cards, Neon buttons,
// Widget launchers, Inputs, and Feedback components
// ═══════════════════════════════════════════════════════

// ─────────────────────────────────────────────────
// GLASS CARD — Frosted glassmorphism surface
// ─────────────────────────────────────────────────

/**
 * GlassCard — A translucent card with frosted glass aesthetics.
 *
 * Features:
 * - Translucent fill (adapts to light/dark)
 * - Optional neon-accented border that glows
 * - Press animation with scale bounce
 * - Neon glow halo behind card (optional)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    neonAccent: Color = Color.Transparent,
    enableGlow: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = NovaTokens.Radius.xl,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovaTokens.Motion.pressScaleCard else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "glass_card_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) NovaTokens.Glass.glowAlphaActive else NovaTokens.Glass.glowAlphaIdle,
        animationSpec = tween(NovaTokens.Motion.fast),
        label = "glass_glow_alpha"
    )

    val glassFill = if (isDark)
        Color.White.copy(alpha = NovaTokens.Glass.fillAlphaDark)
    else
        Color.Black.copy(alpha = NovaTokens.Glass.fillAlphaLight)

    val borderColor = if (neonAccent != Color.Transparent) {
        neonAccent.copy(alpha = if (isPressed) 0.6f else NovaTokens.Glass.neonBorderAlpha)
    } else {
        if (isDark) NovaBorderDark.copy(alpha = 0.3f)
        else NovaBorderLight.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (enableGlow && neonAccent != Color.Transparent) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonAccent.copy(alpha = glowAlpha * 0.3f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.maxDimension * NovaTokens.Glass.glowRadius
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx()),
                            size = Size(
                                size.width + 16.dp.toPx(),
                                size.height + 16.dp.toPx()
                            ),
                            topLeft = Offset(-8.dp.toPx(), -8.dp.toPx())
                        )
                    }
                } else Modifier
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    } else Modifier
                ),
            shape = RoundedCornerShape(cornerRadius),
            border = BorderStroke(
                width = NovaTokens.Glass.borderWidth,
                color = borderColor
            ),
            colors = CardDefaults.cardColors(
                containerColor = glassFill
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = NovaTokens.Elevation.low
            )
        ) {
            Column {
                content()
            }
        }
    }
}

// ─────────────────────────────────────────────────
// NEON HERO CARD — Full-width gradient card
// ─────────────────────────────────────────────────

/**
 * NeonHeroCard — A premium full-width card with animated gradient background.
 *
 * Used for hero banners, storage cards, and continue reading widgets.
 */
@Composable
fun NeonHeroCard(
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovaTokens.Motion.pressScaleCard else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "hero_card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                } else Modifier
            ),
        shape = RoundedCornerShape(NovaTokens.Radius.xxl),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = NovaTokens.Elevation.medium)
    ) {
        Column(
            modifier = Modifier.background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
        ) {
            content()
        }
    }
}

// ─────────────────────────────────────────────────
// NOVA WIDGET ITEM — Dashboard tool launcher
// ─────────────────────────────────────────────────

/**
 * NovaWidgetItem — The futuristic dashboard tool launcher.
 *
 * Features:
 * - Glass circle icon container with neon ring border
 * - Neon-colored icon (not white)
 * - Press animation: deep bounce (0.88x) + icon tilt + glow burst
 * - 2-line labels with no truncation for common names
 * - Category-coded neon accent
 */
@Composable
fun NovaWidgetItem(
    icon: ImageVector,
    label: String,
    neonColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press animation: deep bounce
    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovaTokens.Motion.pressScaleWidget else 1f,
        animationSpec = NovaTokens.Motion.springBouncy,
        label = "widget_scale"
    )

    // Neon ring alpha: pulse when idle, bright when pressed
    val neonRingAlpha by animateFloatAsState(
        targetValue = when {
            isPressed -> NovaTokens.Neon.pressedAlpha
            isActive -> NovaTokens.Neon.activeAlpha
            else -> NovaTokens.Neon.pulseMinAlpha
        },
        animationSpec = tween(NovaTokens.Motion.fast),
        label = "widget_ring_alpha"
    )

    // Icon tilt on press
    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) 8f else 0f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "widget_icon_tilt"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                NovaHaptics.click(view)
                onClick()
            }
            .padding(
                vertical = NovaTokens.Spacing.xs,
                horizontal = NovaTokens.Spacing.xxs
            )
    ) {
        // Icon container with neon ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(NovaTokens.Widget.iconContainerSize)
                .drawBehind {
                    // Outer neon glow halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = neonRingAlpha * 0.4f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2 + NovaTokens.Neon.ringPadding.toPx()
                    )

                    // Neon border ring
                    drawCircle(
                        color = neonColor.copy(alpha = neonRingAlpha),
                        radius = size.minDimension / 2,
                        style = Stroke(width = NovaTokens.Neon.ringStrokeWidth.toPx())
                    )
                }
        ) {
            // Glass circle background
            Box(
                modifier = Modifier
                    .size(NovaTokens.Widget.iconContainerSize - 4.dp)
                    .clip(CircleShape)
                    .background(
                        neonColor.copy(
                            alpha = if (isDark) NovaTokens.Glass.fillAlphaDark
                            else NovaTokens.Glass.fillAlphaLight
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = neonColor,
                    modifier = Modifier
                        .size(NovaTokens.Widget.iconSize)
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }
        }

        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xxs))

        // Label
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.widthIn(max = NovaTokens.Widget.labelMaxWidth)
        )
    }
}

// ─────────────────────────────────────────────────
// NOVA TOP BAR — Glass toolbar with neon accent
// ─────────────────────────────────────────────────

/**
 * NovaTopBar — A glassmorphism top app bar.
 *
 * Features:
 * - Transparent by default, glass-tinted when scrolled
 * - Premium back button with glass background
 * - Optional neon accent color for category coding
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    neonAccent: Color = Color.Transparent,
    showHowItWorks: Boolean = false,
    onHowItWorks: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            NovaBackButton(onClick = onBack)
        },
        actions = {
            actions()
            if (showHowItWorks && onHowItWorks != null) {
                IconButton(onClick = {
                    NovaHaptics.click(view)
                    onHowItWorks()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "How It Works",
                        tint = if (neonAccent != Color.Transparent) neonAccent
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = if (isDark)
                NovaDeepSpace.copy(alpha = 0.85f)
            else
                NovaPureWhite.copy(alpha = 0.85f)
        ),
        modifier = modifier
    )
}

/**
 * NovaBackButton — Premium glass back button with haptic feedback.
 */
@Composable
fun NovaBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    IconButton(
        onClick = {
            NovaHaptics.click(view)
            onClick()
        },
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(NovaTokens.Radius.md))
            .background(
                if (isDark) NovaDarkSlate.copy(alpha = 0.6f)
                else NovaCoolGray100.copy(alpha = 0.6f)
            )
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = "Back",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────
// NOVA BUTTONS — Neon-glow interactive buttons
// ─────────────────────────────────────────────────

/**
 * NovaPrimaryButton — A neon-filled button with glow halo.
 */
@Composable
fun NovaPrimaryButton(
    text: String,
    neonColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovaTokens.Motion.pressScaleButton else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "btn_scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 0.2f,
        animationSpec = tween(NovaTokens.Motion.fast),
        label = "btn_glow"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension * 0.7f
                        ),
                        cornerRadius = CornerRadius(NovaTokens.Radius.sm.toPx()),
                        size = Size(
                            size.width + 12.dp.toPx(),
                            size.height + 12.dp.toPx()
                        ),
                        topLeft = Offset(-6.dp.toPx(), -6.dp.toPx())
                    )
                }
            }
    ) {
        Button(
            onClick = {
                if (!isLoading) {
                    NovaHaptics.click(view)
                    onClick()
                }
            },
            enabled = enabled && !isLoading,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(NovaTokens.Radius.sm),
            colors = ButtonDefaults.buttonColors(
                containerColor = neonColor,
                contentColor = NovaDeepInk,
                disabledContainerColor = neonColor.copy(alpha = 0.3f),
                disabledContentColor = NovaDeepInk.copy(alpha = 0.5f)
            ),
            modifier = Modifier.height(NovaTokens.Touch.comfortable)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NovaDeepInk,
                    strokeWidth = 2.dp
                )
            } else {
                if (icon != null) {
                    Icon(
                        icon, null,
                        Modifier.size(NovaTokens.IconSize.sm)
                    )
                    Spacer(Modifier.width(NovaTokens.Spacing.xs))
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * NovaSecondaryButton — Glass-filled button with neon border.
 */
@Composable
fun NovaSecondaryButton(
    text: String,
    neonColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) NovaTokens.Motion.pressScaleCard else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "sec_btn_scale"
    )

    OutlinedButton(
        onClick = {
            NovaHaptics.click(view)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(NovaTokens.Radius.sm),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isPressed) neonColor else neonColor.copy(alpha = 0.5f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDark)
                neonColor.copy(alpha = 0.08f)
            else
                neonColor.copy(alpha = 0.05f),
            contentColor = neonColor
        ),
        modifier = modifier
            .scale(scale)
            .height(NovaTokens.Touch.minimum)
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(NovaTokens.IconSize.sm))
            Spacer(Modifier.width(NovaTokens.Spacing.xs))
        }
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────
// NOVA SECTION HEADER — Category header with neon dot
// ─────────────────────────────────────────────────

/**
 * NovaSectionHeader — A section header with neon accent dot.
 */
@Composable
fun NovaSectionHeader(
    title: String,
    neonColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: Int? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = NovaTokens.Spacing.xl,
                vertical = NovaTokens.Spacing.sm
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Neon accent dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(neonColor)
            )
            Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = neonColor,
                    modifier = Modifier.size(NovaTokens.IconSize.sm)
                )
                Spacer(modifier = Modifier.width(NovaTokens.Spacing.xxs))
            }

            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (count != null) {
                Spacer(modifier = Modifier.width(NovaTokens.Spacing.xs))
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = neonColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(NovaTokens.Radius.xs))
                        .background(neonColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = neonColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────
// NOVA EMPTY STATE — Animated empty state
// ─────────────────────────────────────────────────

/**
 * NovaEmptyState — A premium empty state with animated icon and CTA.
 */
@Composable
fun NovaEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    neonColor: Color = NeonBlue,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_icon_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(NovaTokens.Spacing.xxxl)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = iconAlpha * 0.2f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.8f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = neonColor.copy(alpha = iconAlpha),
                modifier = Modifier.size(NovaTokens.IconSize.xxl)
            )
        }

        Spacer(modifier = Modifier.height(NovaTokens.Spacing.lg))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(NovaTokens.Spacing.xs))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(NovaTokens.Spacing.xl))
            NovaPrimaryButton(
                text = actionLabel,
                neonColor = neonColor,
                onClick = onAction
            )
        }
    }
}

// ─────────────────────────────────────────────────
// NOVA SNACKBAR — Glass snackbar with neon accent
// ─────────────────────────────────────────────────

/**
 * NovaSnackbarHost — Custom snackbar with glassmorphism styling.
 */
@Composable
fun NovaSnackbar(
    message: String,
    neonColor: Color = NovaInfo,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = NovaTokens.Spacing.md,
                vertical = NovaTokens.Spacing.xs
            ),
        shape = RoundedCornerShape(NovaTokens.Radius.md),
        border = BorderStroke(
            1.dp,
            neonColor.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark)
                NovaMidnightBlue.copy(alpha = 0.95f)
            else
                NovaPureWhite.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(NovaTokens.Elevation.high)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NovaTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(neonColor)
                )
                Spacer(modifier = Modifier.width(NovaTokens.Spacing.sm))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = neonColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────
// NOVA TEXT FIELD — Glass-bordered input with focus glow
// ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    neonColor: Color = NeonBlue,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val isDark = isSystemInDarkTheme()
    var isFocused by remember { mutableStateOf(false) }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) NovaTokens.Glass.glowAlphaActive else 0f,
        animationSpec = tween(NovaTokens.Motion.fast),
        label = "tf_glow_alpha"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.8f else 0.2f,
        animationSpec = tween(NovaTokens.Motion.fast),
        label = "tf_border_alpha"
    )

    Box(
        modifier = modifier
            .drawBehind {
                if (isFocused) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = glowAlpha * 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension * 0.6f
                        ),
                        cornerRadius = CornerRadius(NovaTokens.Radius.md.toPx()),
                        size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()),
                        topLeft = Offset(-6.dp.toPx(), -6.dp.toPx())
                    )
                }
            }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isFocused) neonColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(NovaTokens.IconSize.md)
                    )
                }
            },
            trailingIcon = trailingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isFocused) neonColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(NovaTokens.IconSize.md)
                            .then(
                                if (onTrailingClick != null) Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTrailingClick() } else Modifier
                            )
                    )
                }
            },
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.6f) else NovaPureWhite.copy(alpha = 0.6f),
                unfocusedContainerColor = if (isDark) NovaMidnightBlue.copy(alpha = 0.3f) else NovaPureWhite.copy(alpha = 0.3f),
                focusedBorderColor = neonColor.copy(alpha = borderAlpha),
                unfocusedBorderColor = if (isDark) NovaBorderDark.copy(alpha = 0.2f) else NovaBorderLight.copy(alpha = 0.2f),
                cursorColor = neonColor,
                selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
                    handleColor = neonColor,
                    backgroundColor = neonColor.copy(alpha = 0.2f)
                )
            ),
            shape = RoundedCornerShape(NovaTokens.Radius.md),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}

// ─────────────────────────────────────────────────
// NOVA SWITCH — Pill switch with neon glow track
// ─────────────────────────────────────────────────

@Composable
fun NovaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    neonColor: Color = NeonLime
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Thumb position offset
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 500f
        ),
        label = "switch_thumb_offset"
    )

    // Thumb scale change on press
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "switch_thumb_scale"
    )

    // Track color interpolation
    val trackBgColor = when {
        checked -> neonColor.copy(alpha = 0.15f)
        isDark -> NovaMidnightBlue.copy(alpha = 0.5f)
        else -> NovaCoolGray100.copy(alpha = 0.5f)
    }

    val trackBorderColor = when {
        checked -> neonColor.copy(alpha = 0.6f)
        isDark -> NovaBorderDark.copy(alpha = 0.4f)
        else -> NovaBorderLight.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .size(44.dp, 24.dp)
            .clip(RoundedCornerShape(NovaTokens.Radius.pill))
            .background(trackBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                NovaHaptics.toggle(view)
                onCheckedChange(!checked)
            }
            .drawBehind {
                // Neon glow behind switch if checked
                if (checked) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension * 0.7f
                        ),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        size = size
                    )
                }
                
                // Track border
                drawRoundRect(
                    color = trackBorderColor,
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(20.dp)
                .scale(thumbScale)
                .clip(CircleShape)
                .background(if (checked) neonColor else if (isDark) NovaFrostWhite else NovaDeepInk)
                .then(
                    if (checked) {
                        Modifier.drawBehind {
                            drawCircle(
                                color = neonColor.copy(alpha = 0.4f),
                                radius = size.minDimension * 0.7f,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    } else Modifier
                )
        )
    }
}

// ─────────────────────────────────────────────────
// NOVA SLIDER — Neon-tracked slider with tooltip
// ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    neonColor: Color = NeonMagenta,
    steps: Int = 0
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 1.25f else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "slider_thumb_scale"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            activeTrackColor = neonColor,
            inactiveTrackColor = if (isDark) NovaBorderDark.copy(alpha = 0.3f) else NovaBorderLight.copy(alpha = 0.3f),
            thumbColor = neonColor,
            activeTickColor = neonColor.copy(alpha = 0.7f),
            inactiveTickColor = if (isDark) NovaBorderDark.copy(alpha = 0.5f) else NovaBorderLight.copy(alpha = 0.5f)
        ),
        thumb = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .scale(thumbScale)
                    .drawBehind {
                        // Outer glowing aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    neonColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            radius = 16.dp.toPx()
                        )
                        
                        // Neon border ring
                        drawCircle(
                            color = neonColor,
                            radius = 9.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
            ) {
                // Solid center dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isDark) NovaPureWhite else NovaDeepInk)
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

// ─────────────────────────────────────────────────
// NOVA CHIP — Glass chip with neon border
// ─────────────────────────────────────────────────

@Composable
fun NovaChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    neonColor: Color = NeonBlue,
    icon: ImageVector? = null
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = NovaTokens.Motion.springSnappy,
        label = "chip_scale"
    )

    val chipBgColor = when {
        selected -> neonColor.copy(alpha = 0.12f)
        isDark -> NovaMidnightBlue.copy(alpha = 0.4f)
        else -> NovaCoolGray50.copy(alpha = 0.4f)
    }

    val chipBorderColor = when {
        selected -> neonColor.copy(alpha = 0.7f)
        isDark -> NovaBorderDark.copy(alpha = 0.25f)
        else -> NovaBorderLight.copy(alpha = 0.25f)
    }

    val chipTextColor = when {
        selected -> neonColor
        isDark -> NovaFrostWhite.copy(alpha = 0.8f)
        else -> NovaDeepInk.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(NovaTokens.Radius.pill))
            .background(chipBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                NovaHaptics.click(view)
                onClick()
            }
            .drawBehind {
                drawRoundRect(
                    color = chipBorderColor,
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                if (selected) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension * 0.6f
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        size = size
                    )
                }
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = chipTextColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = chipTextColor
            )
        }
    }
}

// ─────────────────────────────────────────────────
// NOVA SEGMENTED CONTROL — Animated segment selector
// ─────────────────────────────────────────────────

@Composable
fun NovaSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    neonColor: Color = NeonBlue
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    // Smooth transition for selector offset
    val transition = updateTransition(targetState = selectedIndex, label = "segmented_control")
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(NovaTokens.Radius.md))
            .background(
                if (isDark) NovaMidnightBlue.copy(alpha = 0.4f)
                else NovaCoolGray100.copy(alpha = 0.4f)
            )
            .drawBehind {
                drawRoundRect(
                    color = if (isDark) NovaBorderDark.copy(alpha = 0.2f)
                    else NovaBorderLight.copy(alpha = 0.2f),
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        val segmentCount = items.size
        val segmentWidth = maxWidth / segmentCount
        
        // Sliding indicator with neon glow
        val slideOffset by transition.animateDp(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 400f
                )
            },
            label = "indicator_slide"
        ) { state ->
            segmentWidth * state
        }

        // Indicator box
        Box(
            modifier = Modifier
                .padding(start = slideOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isDark) neonColor.copy(alpha = 0.15f)
                    else neonColor.copy(alpha = 0.1f)
                )
                .drawBehind {
                    // Outer neon border
                    drawRoundRect(
                        color = neonColor.copy(alpha = 0.5f),
                        size = size,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // Inner soft neon glow
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                neonColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension * 0.5f
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        size = size
                    )
                }
        )

        // Overlay the interactive segment labels
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, text ->
                val isSelected = index == selectedIndex
                val textColor = when {
                    isSelected -> neonColor
                    isDark -> NovaFrostWhite.copy(alpha = 0.6f)
                    else -> NovaDeepInk.copy(alpha = 0.6f)
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (index != selectedIndex) {
                                NovaHaptics.click(view)
                                onIndexSelected(index)
                            }
                        }
                ) {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

