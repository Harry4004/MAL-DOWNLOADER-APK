package com.harry.maldownloader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harry.maldownloader.UiState

// Dark theme colors
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkPrimary = Color(0xFF6200EE)
private val DarkOnSurface = Color(0xFFE1E1E1)
private val DarkOutline = Color(0xFF4A4A4A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUI(
    state: UiState,
    onPickMalClick: () -> Unit,
    onCustomTagsChanged: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = DarkSurface,
            primary = DarkPrimary,
            onSurface = DarkOnSurface,
            outline = DarkOutline
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "MAL Downloader",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // File picker button
                Button(
                    onClick = onPickMalClick,
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (state.isProcessing) "Processing..." else "Pick MAL XML File",
                        color = Color.White
                    )
                }

                // Custom tags input
                OutlinedTextField(
                    value = state.customTagsCsv,
                    onValueChange = onCustomTagsChanged,
                    label = { Text("Custom Tags (CSV)") },
                    placeholder = { Text("H-NTR,M-Colored,A-Harem") },
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Processing indicator
                if (state.isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Processing MAL data...",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Logs section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logs (${state.logs.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (state.logs.isNotEmpty()) {
                        TextButton(
                            onClick = onClearLogs,
                            enabled = !state.isProcessing
                        ) {
                            Text(
                                text = "Clear",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Logs display
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (state.logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No logs yet. Pick an MAL XML file to start processing.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()
                        
                        LaunchedEffect(state.logs.size) {
                            if (state.logs.isNotEmpty()) {
                                listState.animateScrollToItem(state.logs.size - 1)
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.logs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    color = when {
                                        log.contains("Error:") -> Color(0xFFFF5252)
                                        log.contains("Done") -> Color(0xFF4CAF50)
                                        log.contains("Enriching") -> Color(0xFF2196F3)
                                        log.contains("Saved:") -> Color(0xFF9C27B0)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // Status footer
                Text(
                    text = "Files saved to: Pictures/MAL_Export/",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}