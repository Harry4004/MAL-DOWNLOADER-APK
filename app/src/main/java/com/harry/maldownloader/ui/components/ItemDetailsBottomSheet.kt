@file:OptIn(ExperimentalMaterial3Api::class)

package com.harry.maldownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.harry.maldownloader.data.AnimeEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsBottomSheet(
    entry: AnimeEntry,
    onDismiss: () -> Unit,
    onDownloadImages: (AnimeEntry) -> Unit,
    onRedownload: (AnimeEntry) -> Unit,
    onUpdateTags: (AnimeEntry, String) -> Unit,
    onOpenMalPage: (String) -> Unit,
    onOpenJikanPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTagEditor by remember { mutableStateOf(false) }
    var editedTags by remember { mutableStateOf(entry.tags ?: "") }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header with image and basic info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = entry.imagePath ?: entry.imageUrl,
                    contentDescription = entry.title,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Type and Status chips
                    Row {
                        AssistChip(
                            onClick = { },
                            label = { Text(entry.type) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when(entry.type) {
                                        "Anime" -> Icons.Filled.PlayArrow
                                        "Manga" -> Icons.Filled.MenuBook
                                        else -> Icons.Filled.Article
                                    },
                                    contentDescription = null
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        AssistChip(
                            onClick = { },
                            label = { Text(entry.status ?: "Unknown") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Score and episodes
                    Row {
                        Card(
                            modifier = Modifier.padding(end = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "★ ${entry.score}/10",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "${entry.episodesWatched}/${entry.totalEpisodes ?: "?"}  eps",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ElevatedButton(
                        onClick = { onDownloadImages(entry) },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download")
                    }
                }
                
                item {
                    OutlinedButton(
                        onClick = { onRedownload(entry) },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-download")
                    }
                }
                
                item {
                    OutlinedButton(
                        onClick = { showTagEditor = !showTagEditor },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Tags")
                    }
                }
                
                item {
                    OutlinedButton(
                        onClick = { onOpenMalPage(entry.malUrl ?: "") },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Filled.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MAL Page")
                    }
                }
                
                item {
                    OutlinedButton(
                        onClick = { onOpenJikanPage(entry.id) },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            Icons.Filled.Api,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jikan API")
                    }
                }
            }
            
            // Tag editor
            if (showTagEditor) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Edit Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = editedTags,
                            onValueChange = { editedTags = it },
                            label = { Text("Tags (comma separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            supportingText = {
                                Text("Example: action, shounen, romance")
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    editedTags = entry.tags ?: ""
                                    showTagEditor = false
                                }
                            ) {
                                Text("Cancel")
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    onUpdateTags(entry, editedTags)
                                    showTagEditor = false
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Synopsis/Description
            entry.synopsis?.let { synopsis ->
                Text(
                    text = "Synopsis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = synopsis,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Genres
            entry.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
                Text(
                    text = "Genres",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres.split(",").map { it.trim() }) { genre ->
                        AssistChip(
                            onClick = { },
                            label = { Text(genre) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Additional info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    InfoRow("MAL ID", entry.id)
                    InfoRow("Start Date", entry.startDate ?: "Unknown")
                    InfoRow("End Date", entry.endDate ?: "Unknown")
                    InfoRow("Studio", entry.studio ?: "Unknown")
                    InfoRow("Source", entry.source ?: "Unknown")
                    
                    entry.tags?.takeIf { it.isNotEmpty() }?.let {
                        InfoRow("Current Tags", it)
                    }
                }
            }
            
            // Bottom padding for better scrolling
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}