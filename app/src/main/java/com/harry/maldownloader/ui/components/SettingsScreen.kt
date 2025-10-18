package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Download Settings") {
                SettingItem(
                    title = "Max Concurrent Downloads",
                    subtitle = "Number of simultaneous downloads",
                    icon = Icons.Default.Settings,
                    trailing = {
                        Text(
                            text = "3",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
                
                SettingItem(
                    title = "Network Type",
                    subtitle = "Preferred network for downloads",
                    icon = Icons.Default.Settings,
                    trailing = {
                        Text(
                            text = "Any",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                )
                
                SettingItem(
                    title = "Auto-retry Failed Downloads",
                    subtitle = "Retry downloads that fail",
                    icon = Icons.Default.Refresh,
                    trailing = {
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO */ }
                        )
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Storage") {
                SettingItem(
                    title = "Download Directory",
                    subtitle = "Where images are saved",
                    icon = Icons.Default.Folder,
                    trailing = {
                        IconButton(onClick = { /* TODO: Open folder picker */ }) {
                            Icon(Icons.Default.Edit, contentDescription = "Change")
                        }
                    }
                )
                
                SettingItem(
                    title = "Cleanup Old Downloads",
                    subtitle = "Remove old completed downloads",
                    icon = Icons.Default.Delete,
                    trailing = {
                        IconButton(onClick = { /* TODO: Cleanup */ }) {
                            Icon(Icons.Default.Delete, contentDescription = "Cleanup")
                        }
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Notifications") {
                SettingItem(
                    title = "Download Progress",
                    subtitle = "Show download progress notifications",
                    icon = Icons.Default.Notifications,
                    trailing = {
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO */ }
                        )
                    }
                )
                
                SettingItem(
                    title = "Completion Notifications",
                    subtitle = "Notify when downloads complete",
                    icon = Icons.Default.Notifications,
                    trailing = {
                        Switch(
                            checked = true,
                            onCheckedChange = { /* TODO */ }
                        )
                    }
                )
            }
        }
        
        item {
            SettingsSection(title = "Advanced") {
                SettingItem(
                    title = "Debug Logging",
                    subtitle = "Enable detailed logging",
                    icon = Icons.Default.Info,
                    trailing = {
                        Switch(
                            checked = false,
                            onCheckedChange = { /* TODO */ }
                        )
                    }
                )
                
                SettingItem(
                    title = "Export Logs",
                    subtitle = "Save logs to file",
                    icon = Icons.Default.Info,
                    trailing = {
                        IconButton(onClick = { /* TODO: Export logs */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        trailing()
    }
}