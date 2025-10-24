package com.harry.maldownloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isDismissing) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Dialog scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = tween(200),
        label = "Dialog alpha"
    )
    
    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            kotlinx.coroutines.delay(150)
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { isDismissing = true },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0) dragOffset += dragAmount
                        },
                        onDragEnd = {
                            if (dragOffset > 100f) {
                                isDismissing = true
                            } else {
                                dragOffset = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .scale(scale)
                    .alpha(alpha)
                    .wrapContentHeight(),
                cornerRadius = 28.dp,
                surfaceAlpha = 0.25f,
                borderAlpha = 0.4f,
                highlightStrength = 0.2f,
                tintColor = Color.Black,
                backdropEnabled = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Drag indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    content()
                    
                    // Button row
                    if (confirmButton != null || dismissButton != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            dismissButton?.let {
                                Box(modifier = Modifier.weight(1f)) {
                                    it()
                                }
                            }
                            confirmButton?.let {
                                Box(modifier = Modifier.weight(1f)) {
                                    it()
                                }
                            }
                        }
                    }
                    
                    Text(
                        "Swipe down to close",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}