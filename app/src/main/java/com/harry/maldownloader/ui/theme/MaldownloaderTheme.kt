package com.harry.maldownloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun MaldownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
            secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
            tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260)
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
            secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
            tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}