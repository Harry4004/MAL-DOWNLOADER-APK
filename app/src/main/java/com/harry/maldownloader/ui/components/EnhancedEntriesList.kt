package com.harry.maldownloader.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.data.AnimeEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedEntriesList(
    viewModel: MainViewModel,
    entries: List<AnimeEntry>,
    onDownloadClick: (AnimeEntry) -> Unit
) {
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }
    var searchMode by remember { mutableStateOf("Title") }
    var showSearchModeMenu by remember { mutableStateOf(false) }
    
    // Filter entries based on search and type - preserve all existing filtering logic
    val filteredEntries = remember(entries, searchQuery, selectedType, searchMode) {
        val filtered = entries.filter { entry ->
            val typeMatch = when (selectedType) {
                "ANIME" -> entry.type == "anime"
                "MANGA" -> entry.type == "manga"
                else -> true
            }
            
            val searchMatch = if (searchQuery.isBlank()) true else {
                when (searchMode) {
                    "Title" -> entry.title.contains(searchQuery, ignoreCase = true) ||
                              entry.englishTitle?.contains(searchQuery, ignoreCase = true) == true
                    "Score" -> entry.score?.toString()?.contains(searchQuery) == true
                    "Tags" -> entry.allTags.any { it.contains(searchQuery, ignoreCase = true) } ||
                              entry.genres.any { it.contains(searchQuery, ignoreCase = true) }
                    else -> entry.title.contains(searchQuery, ignoreCase = true)
                }
            }
            
            typeMatch && searchMatch
        }
        
        // Sort filtered results - preserve existing sorting logic
        when (searchMode) {
            "Score" -> filtered.sortedByDescending { it.score ?: 0f }
            "Tags" -> filtered.sortedByDescending { it.allTags.size }
            else -> filtered.sortedBy { it.title }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Enhanced search bar with always-visible mode selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Main search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("🔍 Search by $searchMode...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        Row {
                            // Search mode selector - always visible like your screenshot
                            Box {
                                FilterChip(
                                    selected = true,
                                    onClick = { showSearchModeMenu = true },
                                    label = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(searchMode)
                                            Icon(
                                                Icons.Default.ArrowDropDown, 
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                
                                DropdownMenu(
                                    expanded = showSearchModeMenu,
                                    onDismissRequest = { showSearchModeMenu = false }
                                ) {
                                    val searchModes = listOf("Title", "Score", "Tags")
                                    searchModes.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { 
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = when (mode) {
                                                            "Title" -> Icons.Default.Title
                                                            "Score" -> Icons.Default.Star
                                                            "Tags" -> Icons.Default.Label
                                                            else -> Icons.Default.Search
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("🔍 Search by $mode")
                                                }
                                            },
                                            onClick = {
                                                searchMode = mode
                                                showSearchModeMenu = false
                                                viewModel.log("🔍 Search mode changed to: $mode")
                                            }
                                        )
                                    }
                                }
                            }
                            
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear search")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Type filter chips - preserve existing filtering
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("ALL", "ANIME", "MANGA")
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { 
                                selectedType = type
                                viewModel.log("📊 Filtered by: $type")
                            },
                            label = { 
                                val count = when (type) {
                                    "ANIME" -> entries.count { it.type == "anime" }
                                    "MANGA" -> entries.count { it.type == "manga" }
                                    else -> entries.size
                                }
                                Text("$type ($count)") 
                            }
                        )
                    }
                }
                
                // Quick stats
                if (entries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Showing ${filteredEntries.size} of ${entries.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Mode: $searchMode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Entries list - preserve all existing functionality
        if (filteredEntries.isEmpty()) {
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
                            text = if (entries.isEmpty()) "📂" else "🔍",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = if (entries.isEmpty()) "No entries loaded" else "No matching entries",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (entries.isEmpty()) 
                                "Import a MAL XML file to see your collection" else
                                "Try adjusting your search or filters",
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
                items(filteredEntries) { entry ->
                    EnhancedEntryCard(
                        entry = entry,
                        viewModel = viewModel,
                        onDownloadClick = { onDownloadClick(entry) },
                        onViewDetailsClick = {
                            // Preserve existing details functionality
                            val details = buildString {
                                appendLine("📊 ${entry.title}")
                                appendLine("🆔 Type: ${entry.type.uppercase()}")
                                appendLine("🔢 MAL ID: ${entry.malId}")
                                entry.score?.let { appendLine("⭐ Score: $it") }
                                entry.episodes?.let { if (it > 0) appendLine("🎥 Episodes: $it") }
                                entry.chapters?.let { if (it > 0) appendLine("📚 Chapters: $it") }
                                if (entry.allTags.isNotEmpty()) {
                                    appendLine("🏷️ Tags (${entry.allTags.size}): ${entry.allTags.take(10).joinToString(", ")}")
                                    if (entry.allTags.size > 10) appendLine("...and ${entry.allTags.size - 10} more")
                                }
                                entry.synopsis?.let { appendLine("📝 Synopsis: ${it.take(200)}...") }
                            }
                            viewModel.log(details)
                        },
                        onOpenMalClick = {
                            // Preserve existing MAL page functionality
                            try {
                                val malUrl = "https://myanimelist.net/${entry.type}/${entry.malId}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(malUrl))
                                context.startActivity(intent)
                                viewModel.log("🌐 Opening MAL page: ${entry.title}")
                            } catch (e: Exception) {
                                viewModel.log("❌ Could not open MAL page: ${e.message}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedEntryCard(
    entry: AnimeEntry,
    viewModel: MainViewModel,
    onDownloadClick: () -> Unit,
    onViewDetailsClick: () -> Unit,
    onOpenMalClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDownloaded = !entry.imagePath.isNullOrEmpty()
    val hasImageUrl = !entry.imageUrl.isNullOrEmpty()
    
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
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with title and menu - preserve all existing functionality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!entry.englishTitle.isNullOrEmpty() && entry.englishTitle != entry.title) {
                        Text(
                            text = entry.englishTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = if (isDownloaded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (hasImageUrl && !isDownloaded) {
                            DropdownMenuItem(
                                text = { Text("📥 Download") },
                                onClick = {
                                    onDownloadClick()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Download, null) }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("📊 Details") },
                            onClick = {
                                onViewDetailsClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Info, null) }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("🌐 Open MAL") },
                            onClick = {
                                onOpenMalClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Launch, null) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Type and metadata - preserve all existing display logic
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                
                Text(
                    text = "MAL-${entry.malId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (entry.score != null && entry.score > 0) {
                    Text(
                        text = "⭐ ${String.format("%.1f", entry.score)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFC107)
                    )
                }
            }
            
            // Synopsis - preserve existing display
            if (!entry.synopsis.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Tags preview - preserve existing tag display logic
            if (entry.allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🏷️ ${entry.allTags.size} tags: ${entry.allTags.take(5).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Action buttons - preserve all existing functionality
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetailsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
                
                if (hasImageUrl) {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.weight(1f),
                        colors = if (isDownloaded) {
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isDownloaded) "Downloaded" else "Download")
                    }
                } else {
                    OutlinedButton(
                        onClick = { 
                            viewModel.log("⚠️ No image URL available for ${entry.title}")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    ) {
                        Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No Image")
                    }
                }
            }
        }
    }
}