package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()
    
    ModernSwipeDialog(
        onDismiss = onDismiss,
        title = "🔧 Application Settings",
        subtitle = "Configure app behavior and preferences",
        showSwipeIndicator = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Download Settings Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Download Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Max concurrent downloads
                Column {
                    Text(
                        text = "Concurrent Downloads: ${appSettings.maxConcurrentDownloads}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Number of simultaneous downloads (1-5)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = appSettings.maxConcurrentDownloads.toFloat(),
                        onValueChange = { 
                            viewModel.updateSetting("maxConcurrentDownloads", it.toInt())
                        },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Wi-Fi only toggle
                ModernSettingsToggle(
                    title = "Download only on Wi-Fi",
                    subtitle = "Prevent mobile data usage",
                    checked = appSettings.downloadOnlyOnWifi,
                    onCheckedChange = { viewModel.updateSetting("downloadOnlyOnWifi", it) },
                    icon = Icons.Default.Wifi
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Pause on low battery
                ModernSettingsToggle(
                    title = "Pause on low battery",
                    subtitle = "Preserve battery when low",
                    checked = appSettings.pauseOnLowBattery,
                    onCheckedChange = { viewModel.updateSetting("pauseOnLowBattery", it) },
                    icon = Icons.Default.BatteryAlert
                )
            }
            
            // Storage Settings Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Storage Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Separate adult content
                ModernSettingsToggle(
                    title = "Separate adult content",
                    subtitle = "Organize 18+ content in separate folders",
                    checked = appSettings.separateAdultContent,
                    onCheckedChange = { viewModel.updateSetting("separateAdultContent", it) },
                    icon = Icons.Default.FolderSpecial
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Filename format selector
                Column {
                    Text(
                        text = "Filename Format",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Current: ${appSettings.filenameFormat}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val formats = listOf(
                            "{title}_{id}.{ext}" to "Title_ID",
                            "{id}_{title}.{ext}" to "ID_Title",
                            "{title}.{ext}" to "Title Only"
                        )
                        formats.forEach { (format, display) ->
                            FilterChip(
                                selected = appSettings.filenameFormat == format,
                                onClick = { 
                                    viewModel.updateSetting("filenameFormat", format)
                                },
                                label = { 
                                    Text(
                                        display, 
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
            
            // Metadata Settings Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Metadata Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // XMP metadata embedding
                ModernSettingsToggle(
                    title = "Embed XMP metadata",
                    subtitle = "Add tags to images for AVES Gallery",
                    checked = appSettings.embedXmpMetadata,
                    onCheckedChange = { viewModel.updateSetting("embedXmpMetadata", it) },
                    icon = Icons.Default.PhotoLibrary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // API preference
                ModernSettingsToggle(
                    title = "Prefer MAL over Jikan",
                    subtitle = "Use official MAL API first for tags",
                    checked = appSettings.preferMalOverJikan,
                    onCheckedChange = { viewModel.updateSetting("preferMalOverJikan", it) },
                    icon = Icons.Default.Api
                )
            }
            
            // Advanced Settings Section
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Advanced Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Detailed logging
                ModernSettingsToggle(
                    title = "Enable detailed logging",
                    subtitle = "More verbose logs for debugging",
                    checked = appSettings.enableDetailedLogs,
                    onCheckedChange = { viewModel.updateSetting("enableDetailedLogs", it) },
                    icon = Icons.Default.BugReport
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernButton(
                onClick = {
                    viewModel.resetSettingsToDefaults()
                    viewModel.log("🔄 Settings reset to defaults")
                },
                text = "Reset",
                modifier = Modifier.weight(1f),
                variant = ModernButtonVariant.Tertiary,
                icon = Icons.Default.RestartAlt
            )
            
            ModernButton(
                onClick = onDismiss,
                text = "Done",
                modifier = Modifier.weight(1f),
                variant = ModernButtonVariant.Primary,
                icon = Icons.Default.Check
            )
        }
    }
}

@Composable
fun ModernSettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
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
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}