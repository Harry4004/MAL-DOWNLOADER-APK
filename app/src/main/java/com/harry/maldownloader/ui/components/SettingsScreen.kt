@file:OptIn(ExperimentalMaterial3Api::class)

package com.harry.maldownloader.ui.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Settings states
    var offlineModeEnabled by remember { mutableStateOf(false) }
    var scopedStorageOnly by remember { mutableStateOf(true) }
    var downloadQuality by remember { mutableStateOf("High") }
    var maxConcurrentDownloads by remember { mutableStateOf(3) }
    var wifiOnlyDownloads by remember { mutableStateOf(false) }
    var enableNotifications by remember { mutableStateOf(true) }
    var autoRetryFailedDownloads by remember { mutableStateOf(true) }
    var maxRetryAttempts by remember { mutableStateOf(3) }
    var clearCacheOnExit by remember { mutableStateOf(false) }
    var enableAnalytics by remember { mutableStateOf(false) }
    
    // Network state
    val isNetworkAvailable = remember { isNetworkAvailable(context) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Network & Downloads Section
            item {
                SettingsSection("Network & Downloads") {
                    // Network status
                    SettingsInfoCard(
                        title = "Network Status",
                        description = if (isNetworkAvailable) "Connected" else "Offline",
                        icon = if (isNetworkAvailable) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        iconTint = if (isNetworkAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    
                    // Offline mode toggle
                    SettingsSwitchCard(
                        title = "Offline Mode",
                        description = "Pause all downloads and queue operations",
                        icon = Icons.Filled.CloudOff,
                        checked = offlineModeEnabled,
                        onCheckedChange = { offlineModeEnabled = it }
                    )
                    
                    // WiFi only downloads
                    SettingsSwitchCard(
                        title = "WiFi Only Downloads",
                        description = "Only download when connected to WiFi",
                        icon = Icons.Filled.Wifi,
                        checked = wifiOnlyDownloads,
                        onCheckedChange = { wifiOnlyDownloads = it }
                    )
                    
                    // Download quality
                    SettingsDropdownCard(
                        title = "Download Quality",
                        description = "Image quality preference",
                        icon = Icons.Filled.HighQuality,
                        selectedValue = downloadQuality,
                        options = listOf("Low", "Medium", "High", "Original"),
                        onSelectionChange = { downloadQuality = it }
                    )
                    
                    // Max concurrent downloads
                    SettingsSliderCard(
                        title = "Max Concurrent Downloads",
                        description = "$maxConcurrentDownloads active downloads",
                        icon = Icons.Filled.Download,
                        value = maxConcurrentDownloads,
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { maxConcurrentDownloads = it.toInt() }
                    )
                }
            }
            
            // Storage & Privacy Section
            item {
                SettingsSection("Storage & Privacy") {
                    // Scoped storage mode
                    SettingsSwitchCard(
                        title = "Scoped Storage Only",
                        description = "Use only app-specific storage (recommended for Android 13+)",
                        icon = Icons.Filled.Folder,
                        checked = scopedStorageOnly,
                        onCheckedChange = { scopedStorageOnly = it }
                    )
                    
                    // Clear cache on exit
                    SettingsSwitchCard(
                        title = "Clear Cache on Exit",
                        description = "Automatically clean temporary files when app closes",
                        icon = Icons.Filled.CleaningServices,
                        checked = clearCacheOnExit,
                        onCheckedChange = { clearCacheOnExit = it }
                    )
                    
                    // Analytics toggle
                    SettingsSwitchCard(
                        title = "Enable Analytics",
                        description = "Help improve the app by sending anonymous usage data",
                        icon = Icons.Filled.Analytics,
                        checked = enableAnalytics,
                        onCheckedChange = { enableAnalytics = it }
                    )
                }
            }
            
            // Notifications & Alerts Section
            item {
                SettingsSection("Notifications & Alerts") {
                    // Enable notifications
                    SettingsSwitchCard(
                        title = "Enable Notifications",
                        description = "Show download progress and completion notifications",
                        icon = Icons.Filled.Notifications,
                        checked = enableNotifications,
                        onCheckedChange = { enableNotifications = it }
                    )
                    
                    // Auto-retry failed downloads
                    SettingsSwitchCard(
                        title = "Auto-retry Failed Downloads",
                        description = "Automatically retry downloads that fail",
                        icon = Icons.Filled.Refresh,
                        checked = autoRetryFailedDownloads,
                        onCheckedChange = { autoRetryFailedDownloads = it }
                    )
                    
                    // Max retry attempts
                    SettingsSliderCard(
                        title = "Max Retry Attempts",
                        description = "$maxRetryAttempts retry attempts before giving up",
                        icon = Icons.Filled.RepeatOne,
                        value = maxRetryAttempts,
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { maxRetryAttempts = it.toInt() }
                    )
                }
            }
            
            // Safety & Permissions Section
            item {
                SettingsSection("Safety & Permissions") {
                    // App permissions
                    SettingsActionCard(
                        title = "App Permissions",
                        description = "Manage app permissions in system settings",
                        icon = Icons.Filled.Security,
                        onClick = { openAppPermissionsSettings(context) }
                    )
                    
                    // Notification settings
                    SettingsActionCard(
                        title = "Notification Settings",
                        description = "Configure notification preferences",
                        icon = Icons.Filled.NotificationsActive,
                        onClick = { openNotificationSettings(context) }
                    )
                    
                    // Storage settings
                    SettingsActionCard(
                        title = "Storage Settings",
                        description = "Manage app storage and clear data",
                        icon = Icons.Filled.Storage,
                        onClick = { openAppSettings(context) }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection("About") {
                    SettingsInfoCard(
                        title = "MAL Downloader",
                        description = "Version 1.0.0\nBuilt for Android with Material 3",
                        icon = Icons.Filled.Info
                    )
                    
                    SettingsActionCard(
                        title = "Open Source Licenses",
                        description = "View third-party licenses and attributions",
                        icon = Icons.Filled.Code,
                        onClick = { /* TODO: Open licenses screen */ }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsSwitchCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
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

@Composable
fun SettingsDropdownCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedValue,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSelectionChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSliderCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsInfoCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Utility functions
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

fun openAppPermissionsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = android.net.Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}

fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent)
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = android.net.Uri.fromParts("package", context.packageName, null)
    context.startActivity(intent)
}