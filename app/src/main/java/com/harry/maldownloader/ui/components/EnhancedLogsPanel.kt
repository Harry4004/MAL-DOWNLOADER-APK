package com.harry.maldownloader.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
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
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Compact header with better space utilization
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 Logs (${logs.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Compact action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("MAL Logs", logs.joinToString("\n"))
                        clipboard.setPrimaryClip(clip)
                        viewModel.log("📋 Logs copied to clipboard (${logs.size} entries)")
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy, 
                        "Copy All",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, logs.joinToString("\n"))
                            putExtra(Intent.EXTRA_SUBJECT, "MAL Downloader Logs")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Logs"))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Share, 
                        "Share",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        "Clear",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Compact filter chips using LazyRow for better space usage
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf("ALL", "ERROR", "WARN", "INFO")
            items(filters) { filter ->
                val count = when (filter) {
                    "ERROR" -> logs.count { it.contains("❌") || it.contains("💥") }
                    "WARN" -> logs.count { it.contains("⚠️") || it.contains("🔶") }
                    "INFO" -> logs.count { it.contains("✅") || it.contains("📊") }
                    else -> logs.size
                }
                FilterChip(
                    selected = logFilter == filter,
                    onClick = { logFilter = filter },
                    label = {
                        Text(
                            "$filter ($count)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Optimized log display - use more vertical space
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No logs yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import a MAL XML file to see processing logs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val filteredLogs = when (logFilter) {
                    "ERROR" -> logs.filter { it.contains("❌") || it.contains("💥") }
                    "WARN" -> logs.filter { it.contains("⚠️") || it.contains("🔶") }
                    "INFO" -> logs.filter { it.contains("✅") || it.contains("📊") }
                    else -> logs
                }
                
                items(filteredLogs) { logEntry ->
                    CompactLogCard(logEntry = logEntry)
                }
            }
        }
    }
    
    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { 
                Text(
                    "Clear Logs?",
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            text = { 
                Text(
                    "This will remove all ${logs.size} log entries. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
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

@Composable
fun CompactLogCard(logEntry: String) {
    // More compact log card design
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                logEntry.contains("❌") || logEntry.contains("💥") -> 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                logEntry.contains("⚠️") -> 
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                logEntry.contains("✅") -> 
                    Color(0xFF4CAF50).copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = logEntry,
            modifier = Modifier.padding(
                horizontal = 12.dp, 
                vertical = 8.dp
            ),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = when {
                logEntry.contains("❌") || logEntry.contains("💥") -> 
                    MaterialTheme.colorScheme.error
                logEntry.contains("⚠️") -> 
                    MaterialTheme.colorScheme.tertiary
                logEntry.contains("✅") -> 
                    Color(0xFF4CAF50)
                else -> MaterialTheme.colorScheme.onSurface
            },
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
        )
    }
}