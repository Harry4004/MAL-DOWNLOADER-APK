package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.data.AnimeEntry

@Composable
fun EntriesList(
    viewModel: MainViewModel,
    entries: List<AnimeEntry>,
    onDownloadClick: (AnimeEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📄",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "No entries yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Import your MAL XML file to see entries here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(entries) { entry ->
                EntryCard(
                    entry = entry,
                    onDownloadClick = { onDownloadClick(entry) },
                    onTagsPreview = { viewModel.log("🏷️ Tags for ${entry.title}: ${entry.allTags.take(10).joinToString(", ")}") }
                )
            }
        }
    }
}

@Composable
fun EntryCard(
    entry: AnimeEntry,
    onDownloadClick: () -> Unit,
    onTagsPreview: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                entry.isHentai -> Color(0xFFE91E63).copy(alpha = 0.1f)
                entry.type == "anime" -> Color(0xFF3F51B5).copy(alpha = 0.1f)
                entry.type == "manga" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type badge
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = entry.type.uppercase(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (entry.type) {
                                    "anime" -> Color(0xFF3F51B5).copy(alpha = 0.2f)
                                    "manga" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                }
                            )
                        )
                        
                        // Hentai badge
                        if (entry.isHentai) {
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "18+",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFE91E63).copy(alpha = 0.2f)
                                )
                            )
                        }
                        
                        // MAL ID
                        Text(
                            text = "MAL: ${entry.malId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Synopsis preview
            if (!entry.synopsis.isNullOrEmpty()) {
                Text(
                    text = entry.synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Score and stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score
                if (entry.score != null && entry.score > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⭐",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${entry.score}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Episodes/Chapters
                when (entry.type) {
                    "anime" -> entry.episodes?.let { eps ->
                        if (eps > 0) {
                            Text(
                                text = "🎥 $eps eps",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    "manga" -> {
                        val info = mutableListOf<String>()
                        entry.chapters?.let { if (it > 0) info.add("$it ch") }
                        entry.volumes?.let { if (it > 0) info.add("$it vol") }
                        if (info.isNotEmpty()) {
                            Text(
                                text = "📚 ${info.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                // Tag count
                Text(
                    text = "🏷️ ${entry.allTags.size} tags",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTagsPreview,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preview Tags")
                }
                
                Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⬇️ Download")
                }
            }
        }
    }
}