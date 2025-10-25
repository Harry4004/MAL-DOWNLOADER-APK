package com.harry.maldownloader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset

/**
 * Modern UI Components for MAL Downloader
 * Step 1: Foundation components with glassmorphism design
 */

@Composable
fun ModernGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = 12.dp,
    cornerRadius: Dp = 20.dp,
    glowIntensity: Float = 0.15f,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else modifier

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = glowIntensity),
                            Color.White.copy(alpha = glowIntensity * 0.3f),
                            Color.Transparent
                        ),
                        radius = 400f
                    )
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}

@Composable
fun ModernSwipeDialog(
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    showSwipeIndicator: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Dialog offset animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.5f)
                    ),
                    radius = 1000f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY > 150) {
                            onDismiss()
                        } else {
                            offsetY = 0f
                        }
                    }
                ) { _, dragAmount ->
                    if (dragAmount.y > 0) {
                        offsetY = (offsetY + dragAmount.y).coerceAtLeast(0f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .offset(y = animatedOffset.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Prevent dismiss when clicking on card */ },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Swipe indicator
                    if (showSwipeIndicator) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(5.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(3.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Title section
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    subtitle?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showSwipeIndicator) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Swipe down to close",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun ModernIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Button press animation"
    )

    Surface(
        onClick = {
            isPressed = true
            onClick()
            isPressed = false
        },
        modifier = modifier
            .size(48.dp)
            .scale(animatedScale),
        color = containerColor,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 50f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ModernButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: ModernButtonVariant = ModernButtonVariant.Primary
) {
    val containerColor = when (variant) {
        ModernButtonVariant.Primary -> MaterialTheme.colorScheme.primary
        ModernButtonVariant.Secondary -> MaterialTheme.colorScheme.secondary
        ModernButtonVariant.Tertiary -> Color.Transparent
    }

    val contentColor = when (variant) {
        ModernButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary
        ModernButtonVariant.Secondary -> MaterialTheme.colorScheme.onSecondary
        ModernButtonVariant.Tertiary -> MaterialTheme.colorScheme.primary
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        enabled = enabled && !loading,
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = if (variant == ModernButtonVariant.Tertiary) 0.dp else 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (variant != ModernButtonVariant.Tertiary) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 200f
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    }
                )
                .then(
                    if (variant == ModernButtonVariant.Tertiary) {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }
    }
}

enum class ModernButtonVariant {
    Primary, Secondary, Tertiary
}

@Composable
fun GlassmorphismTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onActionClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernIconButton(
                    onClick = onMenuClick,
                    icon = Icons.Default.Menu,
                    contentDescription = "Open menu"
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (onActionClick != null && actionIcon != null) {
                    ModernIconButton(
                        onClick = onActionClick,
                        icon = actionIcon,
                        contentDescription = null
                    )
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ModernGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 8.dp,
        glowIntensity = 0.1f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                AnimatedTabItem(
                    title = title,
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AnimatedTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200),
        label = "tab-bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tab-fg"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        color = bgColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // App-styled glyph: use string contains instead of char literals
            val glyph = when {
                title.contains("🍥") -> "🍥"
                title.contains("📂") -> "📂"
                title.contains("⬇") -> "⬇️"
                title.contains("📋") -> "📋"
                else -> "•"
            }
            Text(
                text = glyph,
                color = contentColor,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.replace("\n", " "),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ModernGlassmorphismDrawer(
    onClose: () -> Unit,
    onCustomTagsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    storagePermissionGranted: Boolean,
    notificationPermissionGranted: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(340.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.92f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                // Header section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MAL Downloader",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enhanced Edition",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    ModernIconButton(
                        onClick = onClose,
                        icon = Icons.Default.Close,
                        contentDescription = "Close menu",
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Permission status card
                ModernGlassCard(
                    cornerRadius = 16.dp,
                    glowIntensity = 0.2f
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "System Permissions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    ModernPermissionChip("Storage Access", storagePermissionGranted)
                    Spacer(modifier = Modifier.height(12.dp))
                    ModernPermissionChip("Notifications", notificationPermissionGranted)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Menu items
                ModernDrawerMenuItem(
                    icon = Icons.Default.Tag,
                    title = "Custom Tags Manager",
                    subtitle = "Organize your anime collection",
                    onClick = {
                        onClose()
                        onCustomTagsClick()
                    }
                )

                ModernDrawerMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Application Settings",
                    subtitle = "Configure app behavior",
                    onClick = {
                        onClose()
                        onSettingsClick()
                    }
                )

                ModernDrawerMenuItem(
                    icon = Icons.Default.Info,
                    title = "About & Information",
                    subtitle = "App details and credits",
                    onClick = {
                        onClose()
                        onAboutClick()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Enhanced UI • Modern Design",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernPermissionChip(name: String, granted: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (granted) 
            Color.Green.copy(alpha = 0.25f) 
        else 
            Color.Red.copy(alpha = 0.25f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (granted) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$name: ${if (granted) "Granted" else "Required"}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernDrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}