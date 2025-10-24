// GlassComponents.kt — supplementary UI utilities for LiquidGlass update
// Contains reusable GlassCard, GlassButton, and placeholder dialogs to fix unresolved references and build issues.
package com.harry.maldownloader.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(modifier = modifier, cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .then(
                    if (onClick != null) Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { onClick() })
                    } else Modifier
                ),
            content = content
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    GlassSurface(
        modifier = modifier,
        tintColor = color.copy(alpha = 0.3f),
        cornerRadius = 12.dp
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = text, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LiquidGlassTagManagerDialog(viewModel: MainViewModel, onClose: () -> Unit) {
    LiquidGlassDialog(onDismissRequest = onClose, title = "Tags Manager") {
        Text("Tag management UI coming soon…", color = Color.White)
    }
}

@Composable
fun LiquidGlassSettingsDialog(viewModel: MainViewModel, onClose: () -> Unit) {
    LiquidGlassDialog(onDismissRequest = onClose, title = "Settings") {
        Text("Settings UI coming soon…", color = Color.White)
    }
}

@Composable
fun LiquidGlassAboutDialog(viewModel: MainViewModel, onClose: () -> Unit) {
    LiquidGlassDialog(onDismissRequest = onClose, title = "About") {
        Text("MAL Downloader — Liquid Glass Edition", color = Color.White)
    }
}