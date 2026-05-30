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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("anegan_settings", Context.MODE_PRIVATE) }

    // Persistent State
    var dynamicThemeEnabled by remember {
        mutableStateOf(prefs.getBoolean("pref_dynamic_color", true))
    }
    var themeSelection by remember {
        mutableStateOf(prefs.getString("pref_theme_mode", "System") ?: "System") // "System", "Light", "Dark"
    }
    var amoledThemeEnabled by remember {
        mutableStateOf(prefs.getBoolean("pref_amoled_theme", false))
    }
    var customFontSelection by remember {
        mutableStateOf(prefs.getString("pref_custom_font", "Default") ?: "Default")
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

    NovaBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            NovaTopBar(
                title = "Settings",
                onBack = onBack,
                neonAccent = NeonGold
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Section 1: Appearance
                NovaSectionHeader(
                    title = "Appearance",
                    neonColor = NeonGold,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
                
                GlassCard(
                    neonAccent = NeonGold.copy(alpha = 0.2f),
                    cornerRadius = NovaTokens.Radius.xl,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Theme selection (System / Light / Dark)
                        Text(
                            "🌓 Theme Mode", 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val themes = listOf("System", "Light", "Dark")
                        NovaSegmentedControl(
                            items = themes,
                            selectedIndex = themes.indexOf(themeSelection),
                            onIndexSelected = { idx ->
                                val mode = themes[idx]
                                themeSelection = mode
                                prefs.edit().putString("pref_theme_mode", mode).apply()
                            },
                            neonColor = NeonGold
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Dynamic Colors Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "🌈 Material You Dynamic Color", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Adapt colors to system wallpaper (Android 12+)", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            NovaSwitch(
                                checked = dynamicThemeEnabled,
                                onCheckedChange = { checked ->
                                    dynamicThemeEnabled = checked
                                    prefs.edit().putBoolean("pref_dynamic_color", checked).apply()
                                },
                                neonColor = NeonGold
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // AMOLED Dark Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "🕶️ AMOLED Dark Mode", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Pure black background to save battery", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            NovaSwitch(
                                checked = amoledThemeEnabled,
                                onCheckedChange = { checked ->
                                    amoledThemeEnabled = checked
                                    prefs.edit().putBoolean("pref_amoled_theme", checked).apply()
                                },
                                neonColor = NeonGold
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Custom Fonts Selector
                        Text(
                            "✍️ App Font Style", 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val fontOptions = listOf("Default", "Outfit", "Montserrat", "Playfair Display", "Fira Code")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(fontOptions) { font ->
                                val isSelected = customFontSelection == font
                                NovaChip(
                                    text = font,
                                    selected = isSelected,
                                    onClick = {
                                        customFontSelection = font
                                        prefs.edit().putString("pref_custom_font", font).apply()
                                    },
                                    neonColor = NeonGold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Default Conversion
                NovaSectionHeader(
                    title = "Default Conversions",
                    neonColor = NeonGold,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
                
                GlassCard(
                    neonAccent = NeonGold.copy(alpha = 0.2f),
                    cornerRadius = NovaTokens.Radius.xl,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Default image format
                        Text(
                            "🖼️ Default Image Target Format", 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val formats = listOf("JPG", "PNG", "WEBP")
                        NovaSegmentedControl(
                            items = formats,
                            selectedIndex = formats.indexOf(defaultImgFormat),
                            onIndexSelected = { idx ->
                                val fmt = formats[idx]
                                defaultImgFormat = fmt
                                prefs.edit().putString("pref_default_img_format", fmt).apply()
                            },
                            neonColor = NeonGold
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Default Quality
                        Text(
                            "🎯 Default Conversion Quality: $defaultQuality%", 
                            fontSize = 13.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        NovaSlider(
                            value = defaultQuality.toFloat(),
                            onValueChange = { q ->
                                defaultQuality = q.toInt()
                                prefs.edit().putInt("pref_default_quality", q.toInt()).apply()
                            },
                            valueRange = 10f..100f,
                            neonColor = NeonGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Storage & Maintenance
                NovaSectionHeader(
                    title = "Storage & Maintenance",
                    neonColor = NeonGold,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
                
                GlassCard(
                    neonAccent = NeonGold.copy(alpha = 0.2f),
                    cornerRadius = NovaTokens.Radius.xl,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Directory
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "📂 Output Directory", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Public downloads folder for easy user access", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                "Anegan/", 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold,
                                color = NeonGold,
                                fontFamily = FiraCodeFontFamily
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Cache
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "🗑️ App Temp Cache Size", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    String.format("%.2f MB cached", cacheSizeMb), 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGold,
                                    fontFamily = FiraCodeFontFamily
                                )
                            }
                            
                            NovaSecondaryButton(
                                text = "Clear Cache",
                                neonColor = NeonGold,
                                onClick = {
                                    clearCache(context)
                                    updateCacheSize()
                                    Toast.makeText(context, "App cache cleared successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Tour Replay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "🚀 Onboarding Tutorial", 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Replay the welcome tour guide", 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            
                            NovaSecondaryButton(
                                text = "Replay Tour",
                                neonColor = NeonGold,
                                onClick = {
                                    prefs.edit().putBoolean("pref_show_onboarding", true).apply()
                                    Toast.makeText(context, "Tutorial reset! Reopen the app or go back to replay.", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // On-Device offline Privacy Badge
                GlassCard(
                    neonAccent = NovaSuccess.copy(alpha = 0.4f),
                    cornerRadius = NovaTokens.Radius.xl,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔒", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "100% Offline & Hardware Secure", 
                                color = NovaSuccess, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Your documents and media files are processed locally on device. No analytics, tracking, or network telemetry.", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App Version
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val currentVersion = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "3.2.5"
                        } catch (e: Exception) {
                            "3.2.5"
                        }
                        Text(
                            text = "Anegan Console v$currentVersion", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                            fontSize = 11.sp,
                            fontFamily = FiraCodeFontFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Crafted with focus by Mahilan (heisenricher)", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                            fontSize = 10.sp,
                            fontFamily = FiraCodeFontFamily
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
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
