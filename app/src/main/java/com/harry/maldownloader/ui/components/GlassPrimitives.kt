package com.harry.maldownloader.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Core glass surface composable with Apple Liquid Glass effects
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    surfaceAlpha: Float = 0.12f,
    borderAlpha: Float = 0.25f,
    highlightStrength: Float = 0.15f,
    blurRadius: Dp = 20.dp,
    tintColor: Color = Color.White,
    backdropEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tintColor.copy(alpha = surfaceAlpha + highlightStrength),
                        tintColor.copy(alpha = surfaceAlpha),
                        tintColor.copy(alpha = surfaceAlpha * 0.7f)
                    ),
                    radius = 800f
                )
            )
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backdropEnabled) {
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(
                                blurRadius.toPx(), 
                                blurRadius.toPx(), 
                                Shader.TileMode.CLAMP
                            )
                    }
                } else Modifier
            )
    ) {
        // Glass highlight effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = highlightStrength),
                            Color.Transparent,
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )
        
        // Border glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(cornerRadius - 1.dp))
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color.Transparent,
                            Color.White.copy(alpha = borderAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        
        content()
    }
}

// Glass button with spring animation
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    surfaceAlpha: Float = 0.15f,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Glass button scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = tween(150),
        label = "Glass button alpha"
    )

    GlassSurface(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    },
                    onTap = { if (enabled) onClick() }
                )
            },
        surfaceAlpha = surfaceAlpha,
        tintColor = tintColor,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// Glass card with enhanced depth
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    surfaceAlpha: Float = 0.08f,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Glass card scale"
    )

    GlassSurface(
        modifier = modifier
            .scale(scale)
            .let { mod ->
                if (onClick != null) {
                    mod.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                } else mod
            },
        cornerRadius = cornerRadius,
        surfaceAlpha = surfaceAlpha
    ) {
        content()
    }
}

// Glass chip with selection state
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (selected) 0.25f else 0.08f,
        animationSpec = tween(200),
        label = "Chip alpha"
    )
    
    val tintColor = if (selected) MaterialTheme.colorScheme.primary else Color.White

    GlassSurface(
        modifier = modifier.clickable { onClick() },
        cornerRadius = 20.dp,
        surfaceAlpha = surfaceAlpha,
        tintColor = tintColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Glass banner with auto-hide and gesture support
@Composable
fun GlassBanner(
    text: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    autoHideDelay: Long = 3500L,
    tintColor: Color = MaterialTheme.colorScheme.primary
) {
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(autoHideDelay)
        onDismiss()
    }

    GlassSurface(
        modifier = modifier,
        cornerRadius = 12.dp,
        surfaceAlpha = 0.15f,
        tintColor = tintColor,
        highlightStrength = 0.1f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB388FF).copy(alpha = 0.22f),
                            Color(0xFF7C4DFF).copy(alpha = 0.22f)
                        )
                    )
                )
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}