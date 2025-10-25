package com.harry.maldownloader.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
                        renderEffect = RenderEffect.createBlurEffect(
                            blurRadius.toPx(),
                            blurRadius.toPx(),
                            Shader.TileMode.CLAMP
                        )
                    }
                } else Modifier
            )
    ) {
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
                        end = Offset(1000f, 1000f)
                    )
                )
        )

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
                    )
                )
        )

        content()
    }
}

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
        animationSpec = spring(),
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
            .then(
                Modifier
                    .pointerInput(enabled) {
                        detectTapGestures(
                            onPress = {
                                if (enabled) {
                                    isPressed = true
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        isPressed = false
                                    }
                                }
                            },
                            onTap = { if (enabled) onClick() }
                        )
                    }
            ),
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
        animationSpec = spring(),
        label = "Glass card scale"
    )

    GlassSurface(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    isPressed = false
                                }
                            },
                            onTap = { onClick() }
                        )
                    }
                } else Modifier
            ),
        cornerRadius = cornerRadius,
        surfaceAlpha = surfaceAlpha
    ) {
        content()
    }
}