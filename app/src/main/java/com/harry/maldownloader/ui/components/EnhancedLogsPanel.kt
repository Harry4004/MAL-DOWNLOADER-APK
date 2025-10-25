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
    
    Column(modifier = modifier.fillMaxSize()) {
        // Glass header with actions
        ModernGlassCard(cornerRadius = 16.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📋 System Logs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${logs.size} entries recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModernIconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("MAL Logs", logs.joinToString("\n"))
                            clipboard.setPrimaryClip(clip)
                            viewModel.log("📋 Logs copied to clipboard (${logs.size} entries)")
                        },
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Copy All"
                    )
                    
                    ModernIconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, logs.joinToString("\n"))
                                putExtra(Intent.EXTRA_SUBJECT, "MAL Downloader Logs")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Logs"))
                        },
                        icon = Icons.Default.Share,
                        contentDescription = "Share"
                    )
                    
                    ModernIconButton(
                        onClick = { showClearDialog = true },
                        icon = Icons.Default.Delete,
                        contentDescription = "Clear"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp),
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
                    label = { Text("$filter ($count)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Logs display
        if (logs.isEmpty()) {
            ModernGlassCard(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 16.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filteredLogs = when (logFilter) {
                    "ERROR" -> logs.filter { it.contains("❌") || it.contains("💥") }
                    "WARN" -> logs.filter { it.contains("⚠️") || it.contains("🔶") }
                    "INFO" -> logs.filter { it.contains("✅") || it.contains("📊") }
                    else -> logs
                }
                
                items(filteredLogs) { logEntry ->
                    ModernLogCard(logEntry = logEntry)
                }
            }
        }
    }
    
    // Clear confirmation dialog
    if (showClearDialog) {
        ModernSwipeDialog(
            onDismiss = { showClearDialog = false },
            title = "Clear All Logs?",
            subtitle = "This will remove all ${logs.size} log entries. This action cannot be undone."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernButton(
                    onClick = { showClearDialog = false },
                    text = "Cancel",
                    variant = ModernButtonVariant.Tertiary,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Close
                )
                ModernButton(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    },
                    text = "Clear",
                    icon = Icons.Default.Delete,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ModernLogCard(logEntry: String) {
    ModernGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        elevation = 4.dp,
        glowIntensity = 0.05f
    ) {
        Text(
            text = logEntry,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = when {
                logEntry.contains("❌") || logEntry.contains("💥") -> 
                    MaterialTheme.colorScheme.error
                logEntry.contains("⚠️") || logEntry.contains("🔶") -> 
                    MaterialTheme.colorScheme.tertiary
                logEntry.contains("✅") || logEntry.contains("📊") -> 
                    Color(0xFF34C759)
                else -> MaterialTheme.colorScheme.onSurface
            },
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
        )
    }
}