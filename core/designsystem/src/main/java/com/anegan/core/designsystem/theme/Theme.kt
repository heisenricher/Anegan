/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 🌌 NOVA LIGHT COLOR SCHEME (60-30-10 Harmony)
private val AneganLightColorScheme = lightColorScheme(
    primary = NovaDeepInk,
    secondary = NovaSlateGray,
    tertiary = NeonBlue,
    background = NovaGhostWhite,
    surface = NovaPureWhite,
    onPrimary = NovaPureWhite,
    onSecondary = NovaDeepInk,
    onTertiary = NovaDeepInk,
    onBackground = NovaDeepInk,
    onSurface = NovaDeepInk,
    surfaceVariant = NovaCoolGray50,
    outline = NovaBorderLight
)

// 🌌 NOVA DARK COLOR SCHEME (Sleek Cyberpunk/Neon Dark)
private val AneganDarkColorScheme = darkColorScheme(
    primary = NovaFrostWhite,
    secondary = NeonBlue,
    tertiary = NovaDeepInk,
    background = NovaDeepSpace,
    surface = NovaMidnightBlue,
    onPrimary = NovaDeepInk,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = NovaFrostWhite,
    onSurface = NovaFrostWhite,
    surfaceVariant = NovaDarkSlate,
    outline = NovaBorderDark
)

// 🌌 NOVA AMOLED PURE BLACK COLOR SCHEME (Battery Saving + High Contrast Cyberpunk)
private val AneganAmoledColorScheme = darkColorScheme(
    primary = NovaFrostWhite,
    secondary = NeonBlue,
    tertiary = NovaDeepInk,
    background = NovaVoidBlack,
    surface = Color(0xFF121212),
    onPrimary = NovaDeepInk,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = NovaFrostWhite,
    surfaceVariant = Color(0xFF1A1A1A),
    outline = NovaBorderDark
)

@Composable
fun AneganTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    fontName: String = "Default",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Dynamic color is supported on Android 12+
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            if (amoledDark) AneganAmoledColorScheme else AneganDarkColorScheme
        }
        else -> AneganLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypographyForFont(fontName),
        content = content
    )
}
