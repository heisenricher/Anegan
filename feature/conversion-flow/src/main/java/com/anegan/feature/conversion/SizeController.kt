/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*

enum class CompressionMode {
    QUALITY_RESOLUTION,
    TARGET_SIZE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSizeResolutionController(
    modifier: Modifier = Modifier,
    initialMode: CompressionMode = CompressionMode.QUALITY_RESOLUTION,
    initialQuality: Int = 80,
    initialScale: Float = 0.8f,
    initialWidth: String = "",
    initialHeight: String = "",
    initialTargetSizeMb: String = "",
    showResolutionFields: Boolean = false,
    onModeChanged: (CompressionMode) -> Unit,
    onQualityChanged: (Int) -> Unit,
    onScaleChanged: (Float) -> Unit,
    onResolutionChanged: (String, String) -> Unit,
    onTargetSizeChanged: (String) -> Unit,
    neonAccent: Color = NeonMagenta
) {
    var mode by remember { mutableStateOf(initialMode) }
    var quality by remember { mutableStateOf(initialQuality) }
    var scale by remember { mutableStateOf(initialScale) }
    var width by remember { mutableStateOf(initialWidth) }
    var height by remember { mutableStateOf(initialHeight) }
    var targetSizeMb by remember { mutableStateOf(initialTargetSizeMb) }

    val isDark = isSystemInDarkTheme()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        neonAccent = neonAccent,
        enableGlow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Mode Segmented Control
            val modeItems = listOf("Quality & Resolution", "Target Size")
            val selectedIndex = if (mode == CompressionMode.QUALITY_RESOLUTION) 0 else 1
            
            NovaSegmentedControl(
                items = modeItems,
                selectedIndex = selectedIndex,
                onIndexSelected = { index ->
                    val newMode = if (index == 0) CompressionMode.QUALITY_RESOLUTION else CompressionMode.TARGET_SIZE
                    mode = newMode
                    onModeChanged(newMode)
                },
                neonColor = neonAccent
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (mode == CompressionMode.QUALITY_RESOLUTION) {
                Column {
                    // Quality Slider Label & Value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Compression Quality",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                            Text(
                                text = "Adjust lossy quality level",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "$quality%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMono,
                            color = neonAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    NovaSlider(
                        value = quality.toFloat(),
                        onValueChange = {
                            quality = it.toInt()
                            onQualityChanged(quality)
                        },
                        valueRange = 1f..100f,
                        neonColor = neonAccent
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resolution Scale Label & Value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Resolution Scale",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) NovaFrostWhite else NovaDeepInk
                            )
                            Text(
                                text = "Scale dimensions proportionally",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMono,
                            color = neonAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    NovaSlider(
                        value = scale,
                        onValueChange = {
                            scale = it
                            onScaleChanged(scale)
                        },
                        valueRange = 0.1f..1.0f,
                        neonColor = neonAccent
                    )

                    if (showResolutionFields) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Or Set Exact Resolution (px)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) NovaFrostWhite else NovaDeepInk
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Width (px)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                NovaTextField(
                                    value = width,
                                    onValueChange = {
                                        width = it
                                        onResolutionChanged(width, height)
                                    },
                                    placeholder = "e.g. 1920",
                                    neonColor = neonAccent,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Height (px)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                NovaTextField(
                                    value = height,
                                    onValueChange = {
                                        height = it
                                        onResolutionChanged(width, height)
                                    },
                                    placeholder = "e.g. 1080",
                                    neonColor = neonAccent,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }
            } else {
                Column {
                    Text(
                        text = "Target Output Size",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) NovaFrostWhite else NovaDeepInk
                    )
                    Text(
                        text = "Anegan will automatically compute the scale & quality to fit this size.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = targetSizeMb,
                        onValueChange = {
                            targetSizeMb = it
                            onTargetSizeChanged(targetSizeMb)
                        },
                        placeholder = "Target Size (MB), e.g. 1.50",
                        neonColor = neonAccent,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}
