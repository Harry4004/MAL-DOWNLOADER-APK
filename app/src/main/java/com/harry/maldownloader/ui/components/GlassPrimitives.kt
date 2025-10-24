// GlassPrimitives.kt - Fully Updated Version
// Includes blur effects, gestures, and new composables used in LiquidGlass components.
package com.harry.maldownloader.ui.components

import android.os.Build
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.tryAwaitRelease
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Shader.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Creates a blurred glass-like surface effect
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White.copy(alpha = 0.1f),
    surfaceAlpha: Float = 0.2f,
    cornerRadius: Dp = 16.dp,
    backdropEnabled: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    Box(
        modifier = modifier
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(60f, 60f, TileMode.CLAMP)
                }
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(tintColor.copy(alpha = surfaceAlpha))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

// Glass banner - auto fades and dismisses optionally
@Composable
fun GlassBanner(
    text: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    autoHideDelay: Long = 3000L,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    surfaceAlpha: Float = 0.15f
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(autoHideDelay) {
        if (autoHideDelay > 0) {
            delay(autoHideDelay)
            isVisible = false
            onDismiss?.invoke()
        }
    }

    if (isVisible) {
        GlassSurface(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            surfaceAlpha = surfaceAlpha,
            tintColor = tintColor,
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (onDismiss != null) {
                    IconButton(onClick = {
                        isVisible = false
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Draggable glass-style dialog with dismiss gesture
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismissRequest) {
        GlassSurface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .offset(y = dragOffset.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        tryAwaitRelease()
                    })
                },
            surfaceAlpha = 0.25f,
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                content()
                if (confirmButton != null || dismissButton != null) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
                    ) {
                        dismissButton?.invoke()
                        confirmButton?.invoke()
                    }
                }
            }
        }
    }
}
