package com.harry.maldownloader.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.data.DownloadItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EnhancedDownloadsTab(
    viewModel: MainViewModel,
    downloads: List<DownloadItem>,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Glass statistics header
        ModernGlassCard(cornerRadius = 16.dp) {
            Text("📊 Download Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DownloadStatPill("Total", downloads.size.toString(), MaterialTheme.colorScheme.primary)
                DownloadStatPill("Completed", downloads.count { it.status == "completed" }.toString(), Color(0xFF34C759))
                DownloadStatPill("Failed", downloads.count { it.status == "failed" }.toString(), MaterialTheme.colorScheme.error)
                DownloadStatPill("Pending", downloads.count { it.status == "pending" }.toString(), MaterialTheme.colorScheme.secondary)
            }
            if (downloads.isNotEmpty()) {
                val successRate = (downloads.count { it.status == "completed" } * 100) / downloads.size
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { successRate / 100f }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF34C759))
                Text("Success Rate: $successRate%", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (downloads.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernButton(onClick = { /* TODO: integrate retry */ }, text = "Retry Failed", icon = Icons.Default.Refresh, modifier = Modifier.weight(1f), variant = ModernButtonVariant.Tertiary, enabled = downloads.any { it.status == "failed" })
                ModernButton(onClick = { /* TODO: open folder */ }, text = "Open Folder", icon = Icons.Default.Folder, modifier = Modifier.weight(1f), variant = ModernButtonVariant.Tertiary)
                ModernButton(onClick = { showClearDialog = true }, text = "Clear Done", icon = Icons.Default.DeleteSweep, modifier = Modifier.weight(1f), variant = ModernButtonVariant.Tertiary, enabled = downloads.any { it.status == "completed" })
            }
            Spacer(Modifier.height(12.dp))
        }

        if (downloads.isEmpty()) {
            ModernGlassCard(cornerRadius = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("📥", style = MaterialTheme.typography.headlineLarge)
                    Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                    Text("Import MAL entries and start downloading with the enhanced engine", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads.sortedByDescending { it.createdAt }) { download ->
                    DownloadItemCardModern(download = download, viewModel = viewModel)
                }
            }
        }
    }

    if (showClearDialog) {
        ModernSwipeDialog(onDismiss = { showClearDialog = false }, title = "Clear Completed Downloads?", subtitle = "This will remove completed download records. This action cannot be undone.") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernButton(onClick = { showClearDialog = false }, text = "Cancel", icon = Icons.Default.Close, modifier = Modifier.weight(1f), variant = ModernButtonVariant.Tertiary)
                ModernButton(onClick = { showClearDialog = false /* TODO: clear records */ }, text = "Clear", icon = Icons.Default.Delete, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DownloadStatPill(label: String, value: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadItemCardModern(download: DownloadItem, viewModel: MainViewModel) {
    var showMenu by remember { mutableStateOf(false) }

    ModernGlassCard(cornerRadius = 14.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = download.title ?: "Unknown Title", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(text = "MAL ID: ${download.malId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!download.fileName.isNullOrEmpty()) {
                        Text(text = "📁 ${download.fileName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box {
                    val statusIcon = when (download.status) {
                        "completed" -> Icons.Default.CheckCircle
                        "failed" -> Icons.Default.Error
                        "downloading" -> Icons.Default.Download
                        else -> Icons.Default.Schedule
                    }
                    val statusColor = when (download.status) {
                        "completed" -> Color(0xFF34C759)
                        "failed" -> MaterialTheme.colorScheme.error
                        "downloading" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    IconButton(onClick = { showMenu = true }) { Icon(statusIcon, contentDescription = download.status, tint = statusColor) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (download.status == "failed") {
                            DropdownMenuItem(text = { Text("🔄 Retry") }, onClick = { /* TODO */ showMenu = false }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                        }
                        if (download.status == "completed") {
                            DropdownMenuItem(text = { Text("📂 Open") }, onClick = { /* TODO */ showMenu = false }, leadingIcon = { Icon(Icons.Default.Launch, null) })
                            DropdownMenuItem(text = { Text("📤 Share") }, onClick = { /* TODO */ showMenu = false }, leadingIcon = { Icon(Icons.Default.Share, null) })
                        }
                        DropdownMenuItem(text = { Text("🗑️ Remove") }, onClick = { viewModel.log("🗑️ Removed download: ${download.title}"); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }

            if (download.status == "downloading" && download.progress > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { download.progress / 100f }, modifier = Modifier.fillMaxWidth())
                Text("${download.progress}% completed", style = MaterialTheme.typography.bodySmall)
            }

            if (download.status == "failed" && !download.errorMessage.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp)) {
                    Text("❌ ${download.errorMessage}", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Text("Started: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(download.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
        }
    }
}
