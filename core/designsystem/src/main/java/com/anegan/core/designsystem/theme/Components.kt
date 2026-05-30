package com.anegan.core.designsystem.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium back button with iOS-style chevron on a rounded container.
 * Easy to find and premium looking.
 */
@Composable
fun AneganBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AneganCardSurface)
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowBackIosNew,
            contentDescription = "Back",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Premium top app bar with back button and optional info action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AneganTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showHowItWorks: Boolean = false,
    onHowItWorks: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            AneganBackButton(onClick = onBack)
        },
        actions = {
            actions()
            if (showHowItWorks && onHowItWorks != null) {
                IconButton(onClick = onHowItWorks) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "How It Works",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

/**
 * Widget item for the home screen with gradient icon and file badge.
 * 3 items per row layout.
 */
@Composable
fun WidgetItem(
    icon: ImageVector,
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFileBadge: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "widgetScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Gradient circle background
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // File/link badge at bottom-right
            if (showFileBadge) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(1.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 72.dp)
        )
    }
}

/**
 * Premium gradient card with press animation.
 */
@Composable
fun AneganPremiumCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "cardScale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gradientColors != null) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = if (gradientColors != null) {
                Modifier.background(Brush.linearGradient(gradientColors))
            } else Modifier
        ) {
            content()
        }
    }
}

/**
 * File type badge icon showing the file extension type.
 */
@Composable
fun FileTypeBadge(
    extension: String,
    modifier: Modifier = Modifier
) {
    val (icon, color) = remember(extension) {
        when (extension.lowercase()) {
            "pdf" -> Icons.Rounded.ArrowBackIosNew to Color(0xFFE53935) // placeholder, will use PictureAsPdf
            "doc", "docx" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF1565C0)
            "xls", "xlsx" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF2E7D32)
            "jpg", "jpeg", "png", "gif", "webp" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF7B1FA2)
            "mp4", "mkv", "avi", "mov" -> Icons.Rounded.ArrowBackIosNew to Color(0xFFC62828)
            "mp3", "wav", "flac", "aac" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF6A1B9A)
            "zip", "rar", "7z" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF4E342E)
            "txt", "md" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF546E7A)
            "apk" -> Icons.Rounded.ArrowBackIosNew to Color(0xFF388E3C)
            else -> Icons.Rounded.ArrowBackIosNew to Color(0xFF78909C)
        }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = extension,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Section header with optional action button.
 */
@Composable
fun AneganSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun HowItWorksDialog(
    title: String,
    description: String,
    steps: List<String>,
    tips: List<String> = emptyList(),
    faq: List<Pair<String, String>> = emptyList(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📖 ", fontSize = 24.sp)
                Text(
                    text = "How It Works: $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Steps
                if (steps.isNotEmpty()) {
                    Text(
                        text = "Steps to use:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tips
                if (tips.isNotEmpty()) {
                    Text(
                        text = "Pro Tips:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    tips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("💡", fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // FAQ
                if (faq.isNotEmpty()) {
                    Text(
                        text = "Frequently Asked Questions:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    faq.forEach { (question, answer) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AneganCardSurface)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Q: $question",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A: $answer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

