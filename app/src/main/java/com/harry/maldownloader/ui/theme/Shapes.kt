package com.harry.maldownloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Apple Liquid Glass inspired shapes with increased roundness
val LiquidGlassShapes = Shapes(
    // Small components (chips, toggles, small buttons)
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    
    // Medium components (cards, larger buttons)
    medium = RoundedCornerShape(20.dp),
    
    // Large components (sheets, dialogs, containers)
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)