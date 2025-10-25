package com.harry.maldownloader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.ui.components.ModernGlassCard
import com.harry.maldownloader.ui.components.ModernIconButton

@Composable
fun PermissionStatusRow(name: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (granted) Color(0xFF34C759) else Color(0xFFFF3B30)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$name: ${if (granted) "Granted" else "Required"}",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun MenuItemWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f))
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            ModernIconButton(onClick = onClick, icon = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun ModernDrawer(
    onClose: () -> Unit,
    onCustomTagsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    storagePermissionGranted: Boolean,
    notificationPermissionGranted: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(320.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0E0E10).copy(alpha = 0.95f),
                            Color(0xFF0E0E10).copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Menu", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("Quick actions", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    ModernIconButton(onClick = onClose, icon = Icons.Default.Close, contentDescription = "Close menu")
                }

                Spacer(Modifier.height(24.dp))
                ModernGlassCard(cornerRadius = 14.dp) {
                    Text("Permission Status", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    PermissionStatusRow("Storage", storagePermissionGranted)
                    Spacer(Modifier.height(8.dp))
                    PermissionStatusRow("Notifications", notificationPermissionGranted)
                }

                Spacer(Modifier.height(24.dp))
                MenuItemWithIcon(Icons.Default.Tag, "Custom Tags Manager", "Organize your collection") {
                    onClose(); onCustomTagsClick()
                }
                Spacer(Modifier.height(12.dp))
                MenuItemWithIcon(Icons.Default.Settings, "Settings", "App configuration") {
                    onClose(); onSettingsClick()
                }
                Spacer(Modifier.height(12.dp))
                MenuItemWithIcon(Icons.Default.Info, "About", "App information") {
                    onClose(); onAboutClick()
                }

                Spacer(Modifier.weight(1f))
                Text("v${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
