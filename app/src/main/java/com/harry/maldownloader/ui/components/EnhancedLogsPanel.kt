package com.harry.maldownloader.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLogsPanel(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()
    
    var logFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }
    
    // Auto-scroll to top when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && listState.firstVisibleItemIndex < 5) {
            listState.animateScrollToItem(0)
        }
    }
    
    Column(modifier = modifier) {
        // Enhanced toolbar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 Logs (${logs.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MAL Logs", logs.joinToString("\n"))
                                clipboard.setPrimaryClip(clip)
                                viewModel.log("📋 Logs copied to clipboard (${logs.size} entries)")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy All")
                        }
                        
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, logs.joinToString("\n"))
                                    putExtra(Intent.EXTRA_SUBJECT, "MAL Downloader Logs")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Logs"))
                            }
                        ) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, "Clear")
                        }
                    }
                }
                
                // Filter chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("ALL", "ERROR", "WARN", "INFO")
                    filters.forEach { filter ->
                        FilterChip(
                            selected = logFilter == filter,
                            onClick = { logFilter = filter },
                            label = {
                                val count = when (filter) {
                                    "ERROR" -> logs.count { it.contains("❌") || it.contains("💥") }
                                    "WARN" -> logs.count { it.contains("⚠️") || it.contains("🔶") }
                                    "INFO" -> logs.count { it.contains("✅") || it.contains("📊") }
                                    else -> logs.size
                                }
                                Text("$filter ($count)")
                            }
                        )
                    }
                }
            }
        }
        
        // Enhanced log display
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📝",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "No logs yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Import a MAL XML file to see processing logs",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filteredLogs = when (logFilter) {
                        "ERROR" -> logs.filter { it.contains("❌") || it.contains("💥") }
                        "WARN" -> logs.filter { it.contains("⚠️") || it.contains("🔶") }
                        "INFO" -> logs.filter { it.contains("✅") || it.contains("📊") }
                        else -> logs
                    }
                    
                    items(filteredLogs) { logEntry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    logEntry.contains("❌") || logEntry.contains("💥") -> 
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    logEntry.contains("⚠️") -> 
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                    logEntry.contains("✅") -> 
                                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Text(
                                text = logEntry,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = when {
                                    logEntry.contains("❌") || logEntry.contains("💥") -> MaterialTheme.colorScheme.error
                                    logEntry.contains("⚠️") -> MaterialTheme.colorScheme.tertiary
                                    logEntry.contains("✅") -> Color(0xFF4CAF50)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Logs?") },
            text = { Text("This will remove all ${logs.size} log entries. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
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