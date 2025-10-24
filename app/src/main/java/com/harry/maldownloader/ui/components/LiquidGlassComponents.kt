package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.data.DownloadItem

@Composable
fun LiquidGlassEntriesList(
    viewModel: MainViewModel,
    entries: List<AnimeEntry>,
    onDownloadClick: (AnimeEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (entries.isEmpty()) {
            item {
                GlassCard(surfaceAlpha = 0.1f, cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(16.dp))
                        Text("No entries yet", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Import MAL XML to see your anime entries", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(entries) { entry ->
                GlassCard(onClick = { onDownloadClick(entry) }, surfaceAlpha = 0.12f, cornerRadius = 20.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap to download image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassDownloadsTab(viewModel: MainViewModel, downloads: List<DownloadItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (downloads.isEmpty()) {
            item {
                GlassCard(surfaceAlpha = 0.1f, cornerRadius = 24.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(16.dp))
                        Text("No downloads yet", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Downloaded images will appear here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(downloads) { download ->
                GlassCard(surfaceAlpha = 0.12f, cornerRadius = 20.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(download.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(download.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val icon = when (download.status.lowercase()) {
                            "completed" -> Icons.Default.CheckCircle
                            "failed" -> Icons.Default.Error
                            else -> Icons.Default.Downloading
                        }
                        val tint = when (download.status.lowercase()) {
                            "completed" -> Color(0xFF34C759)
                            "failed" -> Color(0xFFFF453A)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassLogsPanel(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val logs by viewModel.logs.collectAsState()
    Column(modifier = modifier) {
        if (logs.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), surfaceAlpha = 0.1f, cornerRadius = 24.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(16.dp))
                    Text("No logs yet", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("App activity logs will appear here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxSize(), surfaceAlpha = 0.08f, cornerRadius = 20.dp) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(logs.reversed()) { log ->
                        Text(log, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}
