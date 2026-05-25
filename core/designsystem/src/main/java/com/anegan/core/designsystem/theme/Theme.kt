package com.anegan.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Pure white base, Midnight Indigo primary, Platinum secondary
private val AneganColorScheme = lightColorScheme(
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

@Composable
fun AneganTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce the premium light minimal look as requested: "Default UI background: pure white"
    val colorScheme = AneganColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AneganTypography,
        content = content
    )
}
