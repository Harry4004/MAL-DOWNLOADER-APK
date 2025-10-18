@file:OptIn(ExperimentalMaterial3Api::class)

package com.harry.maldownloader.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val tag: String? = null,
    val exception: String? = null
)

enum class LogLevel(val displayName: String, val color: Color) {
    DEBUG("DEBUG", Color(0xFF9E9E9E)),
    INFO("INFO", Color(0xFF2196F3)),
    WARN("WARN", Color(0xFFFF9800)),
    ERROR("ERROR", Color(0xFFF44336))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsPanel(
    logs: List<String>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Enhanced logs panel with filtering and search
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    // Parse log entries from strings
    val logEntries = remember(logs) {
        logs.mapNotNull { logString ->
            parseLogEntry(logString)
        }
    }
    
    // Apply filters
    val filteredLogs = remember(logEntries, searchQuery, selectedLevel) {
        var filtered = logEntries
        
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.message.contains(searchQuery, ignoreCase = true) ||
                it.tag?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        
        selectedLevel?.let { level ->
            filtered = filtered.filter { it.level == level }
        }
        
        filtered
    }
    
    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logs (${filteredLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    // Auto-scroll toggle
                    IconButton(
                        onClick = { autoScroll = !autoScroll }
                    ) {
                        Icon(
                            imageVector = if (autoScroll) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = "Auto-scroll",
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Filter toggle
                    IconButton(
                        onClick = { showFilters = !showFilters }
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filters",
                            tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Export logs
                    IconButton(
                        onClick = { exportLogs(context, logEntries) }
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Export Logs"
                        )
                    }
                    
                    // Clear logs
                    IconButton(
                        onClick = onClearLogs
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Clear Logs"
                        )
                    }
                }
            }
            
            // Search and filters
            if (showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search logs...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Level filter chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { selectedLevel = null },
                            label = { Text("All") },
                            selected = selectedLevel == null
                        )
                        
                        LogLevel.entries.forEach { level ->
                            FilterChip(
                                onClick = { selectedLevel = if (selectedLevel == level) null else level },
                                label = { Text(level.displayName) },
                                selected = selectedLevel == level,
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                level.color,
                                                RoundedCornerShape(50)
                                            )
                                    )
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Divider()
            
            // Logs list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredLogs) { logEntry ->
                    LogEntryItem(logEntry = logEntry)
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(
    logEntry: LogEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Level indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        logEntry.level.color,
                        RoundedCornerShape(50)
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Timestamp and level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(logEntry.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Text(
                        text = logEntry.level.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = logEntry.level.color,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Tag if present
                logEntry.tag?.let { tag ->
                    Text(
                        text = "[$tag]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Message
                Text(
                    text = logEntry.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Exception if present
                logEntry.exception?.let { exception ->
                    Text(
                        text = exception,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = LogLevel.ERROR.color,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun parseLogEntry(logString: String): LogEntry? {
    return try {
        // Simple parsing - adapt based on your log format
        val parts = logString.split(": ", limit = 2)
        if (parts.size < 2) return null
        
        val timestamp = parts[0].toLongOrNull() ?: System.currentTimeMillis()
        val message = parts[1]
        
        // Determine log level from message content
        val level = when {
            message.contains("ERROR", ignoreCase = true) -> LogLevel.ERROR
            message.contains("WARN", ignoreCase = true) -> LogLevel.WARN
            message.contains("DEBUG", ignoreCase = true) -> LogLevel.DEBUG
            else -> LogLevel.INFO
        }
        
        LogEntry(
            timestamp = timestamp,
            level = level,
            message = message
        )
    } catch (e: Exception) {
        null
    }
}

fun formatTimestamp(timestamp: Long): String {
    return java.text.SimpleDateFormat(
        "HH:mm:ss.SSS", 
        java.util.Locale.getDefault()
    ).format(java.util.Date(timestamp))
}

fun exportLogs(context: Context, logs: List<LogEntry>) {
    val logsText = logs.joinToString("\n") { entry ->
        "${formatTimestamp(entry.timestamp)} [${entry.level.displayName}] ${entry.tag?.let { "[$it] " } ?: ""}${entry.message}${entry.exception?.let { "\n$it" } ?: ""}"
    }
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, logsText)
        putExtra(Intent.EXTRA_SUBJECT, "MAL Downloader Logs")
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
}