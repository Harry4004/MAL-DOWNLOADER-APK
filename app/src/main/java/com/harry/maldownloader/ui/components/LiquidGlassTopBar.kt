package com.harry.maldownloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassTopBar(
    drawerState: DrawerState,
    onMenuClick: () -> Unit,
    onClearLogsClick: () -> Unit,
    showClearButton: Boolean = false
) {
    // Animate hamburger visibility when drawer is open
    val hamburgerAlpha: Float by animateFloatAsState(
        targetValue = if (drawerState.isOpen) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Hamburger visibility"
    )
    
    // Animate clear button
    val clearButtonAlpha: Float by animateFloatAsState(
        targetValue = if (showClearButton) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Clear button visibility"
    )

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        cornerRadius = 0.dp,
        surfaceAlpha = 0.15f,
        borderAlpha = 0f,
        highlightStrength = 0.08f,
        backdropEnabled = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated hamburger menu
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .alpha(hamburgerAlpha)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .semantics {
                        contentDescription = "Open navigation menu"
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // App title with glass effect
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MAL Downloader",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Clear logs button (conditionally visible)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .alpha(clearButtonAlpha)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showClearButton) {
                    IconButton(
                        onClick = onClearLogsClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear logs",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }
        }
    }
}