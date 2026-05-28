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

// Pure white base, Midnight Indigo primary, Platinum secondary
private val AneganLightColorScheme = lightColorScheme(
    primary = MidnightIndigo,
    secondary = PlatinumSilver,
    tertiary = LuminousGlow,
    background = PureWhite,
    surface = SoftCoolGray,
    onPrimary = PureWhite,
    onSecondary = MidnightIndigo,
    onTertiary = MidnightIndigo,
    onBackground = MidnightIndigo,
    onSurface = MidnightIndigo
)

// Midnight Indigo base background, slate surface, silver-blue accent
private val AneganDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0), // Platinum silver accent
    secondary = LuminousGlow,
    tertiary = MidnightIndigo,
    background = MidnightIndigo, // Deep dark indigo background
    surface = Color(0xFF1E293B), // Sleek lighter slate for surface cards
    onPrimary = MidnightIndigo,
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

// AMOLED Pure Black Color Scheme for battery saving and premium high-contrast looks
private val AneganAmoledColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    secondary = LuminousGlow,
    tertiary = MidnightIndigo,
    background = Color(0xFF000000), // AMOLED Pure Black
    surface = Color(0xFF121212),    // Rich deep dark surface card
    onPrimary = MidnightIndigo,
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFE2E8F0)
)

@Composable
fun AneganTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    fontName: String = "Default",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledDark) {
                base.copy(
                    background = Color(0xFF000000),
                    surface = Color(0xFF121212),
                    surfaceVariant = Color(0xFF1E1E1E),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFE2E8F0)
                )
            } else {
                base
            }
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

