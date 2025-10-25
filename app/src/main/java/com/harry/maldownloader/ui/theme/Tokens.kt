package com.harry.maldownloader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Step 10: Unified spacing & typography tokens

data class Spacing(
    val xs: Int = 4,
    val sm: Int = 8,
    val md: Int = 12,
    val lg: Int = 16,
    val xl: Int = 24
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

val AppTypography = Typography(
    bodySmall = androidx.compose.material3.Typography().bodySmall.copy(fontSize = 12.sp),
    bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontSize = 14.sp),
    titleSmall = androidx.compose.material3.Typography().titleSmall.copy(fontSize = 14.sp),
    titleMedium = androidx.compose.material3.Typography().titleMedium.copy(fontSize = 16.sp),
    headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(fontSize = 20.sp)
)

val LightColors = lightColorScheme()
val DarkColors = darkColorScheme()
