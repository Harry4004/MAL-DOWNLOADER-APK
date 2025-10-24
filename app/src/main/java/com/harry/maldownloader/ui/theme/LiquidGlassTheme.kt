package com.harry.maldownloader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity

// Apple Liquid Glass inspired color palette
private val LiquidGlassLight = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0x1A007AFF),
    onPrimaryContainer = Color(0xFF003E7F),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0x1A5856D6),
    onSecondaryContainer = Color(0xFF2C2B6B),
    tertiary = Color(0xFFAF52DE),
    onTertiary = Color.White,
    tertiaryContainer = Color(0x1AAF52DE),
    onTertiaryContainer = Color(0xFF57296F),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0x1AFF3B30),
    onErrorContainer = Color(0xFF7F1D18),
    background = Color(0x0D000000),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0x0DFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0x14F2F2F7),
    onSurfaceVariant = Color(0xFF48484A),
    outline = Color(0x33C7C7CC),
    outlineVariant = Color(0x1AC7C7CC),
    scrim = Color(0x66000000),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = Color(0xFF64D2FF)
)

private val LiquidGlassDark = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFF003A6B),
    primaryContainer = Color(0x1A0A84FF),
    onPrimaryContainer = Color(0xFF64D2FF),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color(0xFF2C2B6B),
    secondaryContainer = Color(0x1A5E5CE6),
    onSecondaryContainer = Color(0xFFB3B2F7),
    tertiary = Color(0xFFBF5AF2),
    onTertiary = Color(0xFF57296F),
    tertiaryContainer = Color(0x1ABF5AF2),
    onTertiaryContainer = Color(0xFFE2B9FF),
    error = Color(0xFFFF453A),
    onError = Color(0xFF680003),
    errorContainer = Color(0x1AFF453A),
    onErrorContainer = Color(0xFFFFB3B0),
    background = Color(0x0DFFFFFF),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0x0D000000),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0x141C1C1E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0x333A3A3C),
    outlineVariant = Color(0x1A3A3A3C),
    scrim = Color(0x66000000),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = Color(0xFF007AFF)
)

@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LiquidGlassDark
        else -> LiquidGlassLight
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as ComponentActivity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LiquidGlassTypography,
        shapes = LiquidGlassShapes,
        content = content
    )
}