package com.harry.maldownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.ui.components.LiquidGlassDialog as DialogComponent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(modifier = modifier) {
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
        androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.Text(text = text, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun LiquidGlassTagManagerDialog(viewModel: MainViewModel, onClose: () -> Unit) {
    DialogComponent(onDismissRequest = onClose, title = "Tags Manager") {
        androidx.compose.material3.Text("Tag management UI coming soon…", color = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
fun LiquidGlassSettingsDialog(viewModel: MainViewModel, onClose: () -> Unit) {
    DialogComponent(onDismissRequest = onClose, title = "Settings") {
        androidx.compose.material3.Text("Settings UI coming soon…", color = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
fun LiquidGlassAboutDialog(viewModel: MainViewModel, onClose: () -> Unit) {
    DialogComponent(onDismissRequest = onClose, title = "About") {
        androidx.compose.material3.Text("MAL Downloader — Liquid Glass Edition", color = androidx.compose.ui.graphics.Color.White)
    }
}