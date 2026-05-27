/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PureWhite
import com.anegan.core.designsystem.theme.LuminousGlow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("anegan_settings", Context.MODE_PRIVATE) }

    // Persistent State
    var dynamicThemeEnabled by remember {
        mutableStateOf(prefs.getBoolean("pref_dynamic_color", true))
    }
    var themeSelection by remember {
        mutableStateOf(prefs.getString("pref_theme_mode", "System") ?: "System") // "System", "Light", "Dark"
    }
    var defaultImgFormat by remember {
        mutableStateOf(prefs.getString("pref_default_img_format", "JPG") ?: "JPG")
    }
    var defaultQuality by remember {
        mutableStateOf(prefs.getInt("pref_default_quality", 80))
    }

    // Cache State
    var cacheSizeMb by remember { mutableStateOf(0.0) }

    fun updateCacheSize() {
        val sizeBytes = getCacheSize(context)
        cacheSizeMb = sizeBytes / (1024.0 * 1024.0)
    }

    LaunchedEffect(Unit) {
        updateCacheSize()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← ",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                color = MidnightIndigo
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: Appearance
        Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Theme selection (System / Light / Dark)
                Text("Theme Mode", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = listOf("System", "Light", "Dark")
                    themes.forEach { mode ->
                        val isSelected = themeSelection == mode
                        Button(
                            onClick = {
                                themeSelection = mode
                                prefs.edit().putString("pref_theme_mode", mode).apply()
                                Toast.makeText(context, "Theme will apply on next restart", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.background,
                                contentColor = if (isSelected) PureWhite else MidnightIndigo
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(mode, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Colors Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Material You Dynamic Color", fontSize = 14.sp, color = MidnightIndigo)
                        Text("Adapt colors to system wallpaper (Android 12+)", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = dynamicThemeEnabled,
                        onCheckedChange = { checked ->
                            dynamicThemeEnabled = checked
                            prefs.edit().putBoolean("pref_dynamic_color", checked).apply()
                            Toast.makeText(context, "Restart app to apply dynamics", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MidnightIndigo, checkedTrackColor = LuminousGlow)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Default Conversion
        Text("Default Conversions", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Default image format
                Text("Default Image Target Format", fontSize = 14.sp, color = MidnightIndigo)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val formats = listOf("JPG", "PNG", "WEBP")
                    formats.forEach { fmt ->
                        val isSelected = defaultImgFormat == fmt
                        Button(
                            onClick = {
                                defaultImgFormat = fmt
                                prefs.edit().putString("pref_default_img_format", fmt).apply()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MidnightIndigo else MaterialTheme.colorScheme.background,
                                contentColor = if (isSelected) PureWhite else MidnightIndigo
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fmt)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Quality
                Text("Default Conversion Quality: $defaultQuality%", fontSize = 14.sp, color = MidnightIndigo)
                Slider(
                    value = defaultQuality.toFloat(),
                    onValueChange = { q ->
                        defaultQuality = q.toInt()
                        prefs.edit().putInt("pref_default_quality", q.toInt()).apply()
                    },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(thumbColor = MidnightIndigo, activeTrackColor = MidnightIndigo)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Storage & Maintenance
        Text("Storage & Maintenance", style = MaterialTheme.typography.titleMedium, color = MidnightIndigo)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Output Directory", fontSize = 14.sp, color = MidnightIndigo)
                Text("Documents/Anegan/", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("App Temp Cache Size", fontSize = 14.sp, color = MidnightIndigo)
                        Text(String.format("%.2f MB cached", cacheSizeMb), fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            clearCache(context)
                            updateCacheSize()
                            Toast.makeText(context, "App cache cleared successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
                    ) {
                        Text("Clear Cache")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Onboarding Tutorial", fontSize = 14.sp, color = MidnightIndigo)
                        Text("Replay the welcome tour", fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            prefs.edit().putBoolean("pref_show_onboarding", true).apply()
                            Toast.makeText(context, "Tutorial reset! Reopen the app or go back to replay.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
                    ) {
                        Text("Replay Tour")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // On-Device offline Privacy Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MidnightIndigo)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔒 100% Offline & Private", color = PureWhite, style = MaterialTheme.typography.titleMedium)
                Text("Your documents and media files are processed locally. No uploads.", color = Color.LightGray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Version
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.13"
                } catch (e: Exception) {
                    "1.0.13"
                }
                Text("Anegan v$currentVersion", color = Color.Gray, fontSize = 12.sp)
                Text("Crafted by Mahilan (heisenricher)", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

private fun getCacheSize(context: Context): Long {
    var size = 0L
    context.cacheDir?.let { size += getDirSize(it) }
    context.externalCacheDir?.let { size += getDirSize(it) }
    return size
}

private fun getDirSize(dir: File): Long {
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) getDirSize(file) else file.length()
    }
    return size
}

private fun clearCache(context: Context) {
    context.cacheDir?.deleteRecursively()
    context.externalCacheDir?.deleteRecursively()
}
