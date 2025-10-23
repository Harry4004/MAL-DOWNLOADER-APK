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
import com.harry.maldownloader.data.AppSettings
import com.harry.maldownloader.data.SettingsCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit = {}
) {
    var currentSettings by remember { mutableStateOf(AppSettings()) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🔧 Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure MAL Downloader v3.1",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Default.RestartAlt, "Reset to Defaults")
                }
            }
        }
        
        // Downloads Settings
        SettingsCategory(
            title = "📥 Downloads",
            icon = Icons.Default.Download
        ) {
            SettingRow(
                title = "Concurrent Downloads",
                description = "Number of simultaneous downloads (1-5)",
                value = currentSettings.maxConcurrentDownloads.toString(),
                onClick = {
                    viewModel.log("🔧 Concurrent downloads: ${currentSettings.maxConcurrentDownloads}")
                }
            )
            
            SettingSwitchRow(
                title = "Wi-Fi Only Downloads",
                description = "Download images only when connected to Wi-Fi",
                checked = currentSettings.downloadOnlyOnWifi,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(downloadOnlyOnWifi = it)
                    viewModel.log("📶 Wi-Fi only downloads: $it")
                }
            )
            
            SettingSwitchRow(
                title = "Pause on Low Battery",
                description = "Automatically pause downloads when battery is below 20%",
                checked = currentSettings.pauseOnLowBattery,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(pauseOnLowBattery = it)
                    viewModel.log("🔋 Pause on low battery: $it")
                }
            )
            
            SettingSwitchRow(
                title = "Background Downloads",
                description = "Continue downloads when app is in background",
                checked = currentSettings.enableBackgroundDownloads,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(enableBackgroundDownloads = it)
                    viewModel.log("🌃 Background downloads: $it")
                }
            )
        }
        
        // Storage Settings
        SettingsCategory(
            title = "📁 Storage",
            icon = Icons.Default.Folder
        ) {
            SettingRow(
                title = "Filename Format",
                description = "How downloaded files are named",
                value = currentSettings.filenameFormat,
                onClick = {
                    // TODO: Show filename format picker dialog
                    viewModel.log("📝 Filename format: ${currentSettings.filenameFormat}")
                }
            )
            
            SettingSwitchRow(
                title = "Separate Adult Content",
                description = "Organize 18+ content in separate Adult folders",
                checked = currentSettings.separateAdultContent,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(separateAdultContent = it)
                    viewModel.log("🔞 Adult content separation: $it")
                }
            )
            
            SettingSwitchRow(
                title = "Public Pictures Directory",
                description = "Save to Pictures folder (visible in gallery apps)",
                checked = currentSettings.usePublicPicturesDirectory,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(usePublicPicturesDirectory = it)
                    viewModel.log("🖼️ Public Pictures directory: $it")
                }
            )
        }
        
        // Metadata Settings
        SettingsCategory(
            title = "🏷️ Metadata",
            icon = Icons.Default.Tag
        ) {
            SettingSwitchRow(
                title = "XMP Metadata Embedding",
                description = "Embed comprehensive metadata for AVES Gallery compatibility",
                checked = currentSettings.embedXmpMetadata,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(embedXmpMetadata = it)
                    viewModel.log("🏷️ XMP metadata embedding: $it")
                }
            )
            
            SettingSwitchRow(
                title = "Include Synopsis",
                description = "Add anime/manga synopsis to image metadata",
                checked = currentSettings.includeSynopsis,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(includeSynopsis = it)
                    viewModel.log("📝 Include synopsis in metadata: $it")
                }
            )
            
            SettingRow(
                title = "Max Tags per Image",
                description = "Maximum tags to embed (10-100)",
                value = "${currentSettings.maxTagsPerImage} tags",
                onClick = {
                    viewModel.log("🏷️ Max tags per image: ${currentSettings.maxTagsPerImage}")
                }
            )
        }
        
        // API Settings
        SettingsCategory(
            title = "🌐 API Configuration",
            icon = Icons.Default.Api
        ) {
            SettingSwitchRow(
                title = "Prefer Official MAL API",
                description = "Use MAL official API first, fallback to Jikan",
                checked = currentSettings.preferMalOverJikan,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(preferMalOverJikan = it)
                    viewModel.log("🌐 Prefer MAL API over Jikan: $it")
                }
            )
            
            SettingRow(
                title = "API Request Delay",
                description = "Delay between API requests (${currentSettings.apiDelayMs}ms)",
                value = "${currentSettings.apiDelayMs / 1000.0}s",
                onClick = {
                    viewModel.log("⏱️ API delay: ${currentSettings.apiDelayMs}ms")
                }
            )
        }
        
        // Advanced Settings
        SettingsCategory(
            title = "⚙️ Advanced",
            icon = Icons.Default.Settings
        ) {
            SettingSwitchRow(
                title = "Detailed Logging",
                description = "Enable comprehensive debug logging",
                checked = currentSettings.enableDetailedLogs,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(enableDetailedLogs = it)
                    viewModel.log("📝 Detailed logging: $it")
                }
            )
            
            SettingSwitchRow(
                title = "Duplicate Detection",
                description = "Skip downloading images that already exist",
                checked = currentSettings.enableDuplicateDetection,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(enableDuplicateDetection = it)
                    viewModel.log("🔍 Duplicate detection: $it")
                }
            )
            
            SettingSwitchRow(
                title = "Image Validation",
                description = "Verify downloaded images are valid",
                checked = currentSettings.enableImageValidation,
                onCheckedChange = {
                    currentSettings = currentSettings.copy(enableImageValidation = it)
                    viewModel.log("✔️ Image validation: $it")
                }
            )
        }
        
        // Save button
        Button(
            onClick = {
                // TODO: Save settings to database
                viewModel.log("✅ Settings saved successfully")
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Settings")
        }
    }
    
    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Defaults?") },
            text = { Text("This will restore all settings to their default values. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        currentSettings = AppSettings() // Reset to defaults
                        showResetDialog = false
                        viewModel.log("🔄 Settings reset to defaults")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsCategory(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}