package com.harry.maldownloader.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.data.DownloadItem

@Composable
fun DownloadListItem(
    downloadItem: DownloadItem,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = downloadItem.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = downloadItem.status.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when(downloadItem.status) {
                        "completed" -> MaterialTheme.colorScheme.primary
                        "failed" -> MaterialTheme.colorScheme.error
                        "downloading" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (downloadItem.progress > 0 && downloadItem.status == "downloading") {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = downloadItem.progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                downloadItem.errorMessage?.let { error ->
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            when(downloadItem.status) {
                "downloading" -> {
                    IconButton(onClick = { onPause(downloadItem.id) }) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                    IconButton(onClick = { onCancel(downloadItem.id) }) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                    }
                }
                "paused" -> {
                    IconButton(onClick = { onResume(downloadItem.id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                    }
                    IconButton(onClick = { onCancel(downloadItem.id) }) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                    }
                }
                "failed" -> {
                    IconButton(onClick = { onRetry(downloadItem.id) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    }
                }
                "completed" -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadsContent(
    downloads: List<DownloadItem>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (downloads.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No downloads yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(downloads) { item ->
                DownloadListItem(
                    downloadItem = item,
                    onCancel = onCancelDownload,
                    onRetry = onRetryDownload,
                    onPause = onPauseDownload,
                    onResume = onResumeDownload
                )
            }
        }
    }
}
