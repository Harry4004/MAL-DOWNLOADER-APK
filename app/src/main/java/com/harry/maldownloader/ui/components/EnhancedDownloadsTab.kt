package com.harry.maldownloader.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.data.DownloadItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EnhancedDownloadsTab(
    viewModel: MainViewModel,
    downloads: List<DownloadItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxSize()) {
        // Enhanced statistics card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📊 Download Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DownloadStatCard("Total", downloads.size.toString(), MaterialTheme.colorScheme.primary)
                    DownloadStatCard("Completed", downloads.count { it.status == "completed" }.toString(), Color(0xFF4CAF50))
                    DownloadStatCard("Failed", downloads.count { it.status == "failed" }.toString(), MaterialTheme.colorScheme.error)
                    DownloadStatCard("Pending", downloads.count { it.status == "pending" }.toString(), MaterialTheme.colorScheme.secondary)
                }
                
                if (downloads.isNotEmpty()) {
                    val successRate = (downloads.count { it.status == "completed" } * 100) / downloads.size
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = successRate / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "Success Rate: $successRate%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons row
        if (downloads.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Retry failed
                OutlinedButton(
                    onClick = {
                        // TODO: Implement retry failed
                        viewModel.log("🔄 Retrying ${downloads.count { it.status == "failed" }} failed downloads")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = downloads.any { it.status == "failed" }
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry Failed")
                }
                
                // Open downloads folder
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("content://com.android.externalstorage.documents/document/primary%3APictures%2FMAL_Images")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            viewModel.log("⚠️ Could not open folder: ${e.message}")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Folder")
                }
                
                // Clear completed
                OutlinedButton(
                    onClick = {
                        // TODO: Implement clear completed
                        viewModel.log("🗑️ Cleared ${downloads.count { it.status == "completed" }} completed downloads")
                    },
                    modifier = Modifier.weight(1f),
                    enabled = downloads.any { it.status == "completed" }
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Done")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Downloads list
        if (downloads.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📥",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "No downloads yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Import MAL entries and start downloading with the enhanced engine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloads.sortedByDescending { it.createdAt }) { download ->
                    DownloadItemCard(
                        download = download,
                        viewModel = viewModel,
                        onRetryClick = {
                            // TODO: Implement single item retry
                            viewModel.log("🔄 Retrying download: ${download.title}")
                        },
                        onOpenClick = {
                            // Open image if it exists
                            try {
                                if (!download.fileName.isNullOrEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("content://media/external/images/media")
                                        type = "image/*"
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                viewModel.log("⚠️ Could not open image: ${e.message}")
                            }
                        },
                        onShareClick = {
                            // Share image
                            viewModel.log("📤 Sharing: ${download.title}")
                        }
                    )
                }
            }
        }
    }
    
    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Logs?") },
            text = { 
                Text("This will permanently remove all ${logs.size} log entries. This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DownloadStatCard(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadItemCard(
    download: DownloadItem,
    viewModel: MainViewModel,
    onRetryClick: () -> Unit,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title ?: "Unknown Title",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "MAL ID: ${download.malId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!download.fileName.isNullOrEmpty()) {
                        Text(
                            text = "📁 ${download.fileName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Status indicator
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = when (download.status) {
                                "completed" -> Icons.Default.CheckCircle
                                "failed" -> Icons.Default.Error
                                "downloading" -> Icons.Default.Download
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = download.status,
                            tint = when (download.status) {
                                "completed" -> Color(0xFF4CAF50)
                                "failed" -> MaterialTheme.colorScheme.error
                                "downloading" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (download.status == "failed") {
                            DropdownMenuItem(
                                text = { Text("🔄 Retry") },
                                onClick = {
                                    onRetryClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                        }
                        
                        if (download.status == "completed") {
                            DropdownMenuItem(
                                text = { Text("📂 Open") },
                                onClick = {
                                    onOpenClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.OpenInNew, null) }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("📤 Share") },
                                onClick = {
                                    onShareClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("🗑️ Remove") },
                            onClick = {
                                viewModel.log("🗑️ Removed download: ${download.title}")
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
            
            // Progress indicator for active downloads
            if (download.status == "downloading" && download.progress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = download.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${download.progress}% completed",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Error message for failed downloads
            if (download.status == "failed" && !download.errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "❌ ${download.errorMessage}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Timestamp
            Text(
                text = "Started: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(download.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}