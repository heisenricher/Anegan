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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PlatinumSilver

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
    onTargetSizeChanged: (String) -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    var quality by remember { mutableStateOf(initialQuality) }
    var scale by remember { mutableStateOf(initialScale) }
    var width by remember { mutableStateOf(initialWidth) }
    var height by remember { mutableStateOf(initialHeight) }
    var targetSizeMb by remember { mutableStateOf(initialTargetSizeMb) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Apple-style Segmented Control (Pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf(
                CompressionMode.QUALITY_RESOLUTION to "Quality & Resolution",
                CompressionMode.TARGET_SIZE to "Target Size"
            )
            modes.forEach { (cMode, label) ->
                val isSelected = mode == cMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .border(
                            width = if (isSelected) 0.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            mode = cMode
                            onModeChanged(cMode)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MidnightIndigo else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (mode == CompressionMode.QUALITY_RESOLUTION) {
            Column {
                // Quality Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Compression Quality",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MidnightIndigo
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
                        color = MidnightIndigo
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = quality.toFloat(),
                    onValueChange = {
                        quality = it.toInt()
                        onQualityChanged(quality)
                    },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MidnightIndigo,
                        activeTrackColor = MidnightIndigo,
                        inactiveTrackColor = PlatinumSilver
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resolution Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Resolution Scale",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MidnightIndigo
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
                        color = MidnightIndigo
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                        onScaleChanged(scale)
                    },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MidnightIndigo,
                        activeTrackColor = MidnightIndigo,
                        inactiveTrackColor = PlatinumSilver
                    )
                )

                if (showResolutionFields) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Or Set Exact Resolution (px)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MidnightIndigo
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = width,
                            onValueChange = {
                                width = it
                                onResolutionChanged(width, height)
                            },
                            label = { Text("Width (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MidnightIndigo,
                                focusedLabelColor = MidnightIndigo
                            )
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = {
                                height = it
                                onResolutionChanged(width, height)
                            },
                            label = { Text("Height (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MidnightIndigo,
                                focusedLabelColor = MidnightIndigo
                            )
                        )
                    }
                }
            }
        } else {
            Column {
                Text(
                    text = "Target Output Size",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MidnightIndigo
                )
                Text(
                    text = "Anegan will automatically compute the scale & quality to fit this size.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = targetSizeMb,
                    onValueChange = {
                        targetSizeMb = it
                        onTargetSizeChanged(targetSizeMb)
                    },
                    placeholder = { Text("e.g. 1.50 or 2") },
                    label = { Text("Target Size (MB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidnightIndigo,
                        focusedLabelColor = MidnightIndigo
                    )
                )
            }
        }
    }
}
