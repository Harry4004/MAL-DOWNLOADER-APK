@file:OptIn(ExperimentalMaterial3Api::class)

package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.harry.maldownloader.data.AnimeEntry

@Composable
fun LibraryItem(entry: AnimeEntry, onDownloadImage: (AnimeEntry) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.imagePath ?: entry.imageUrl,
                contentDescription = entry.title,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleLarge)
                Text(entry.status ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { onDownloadImage(entry) }) {
                Icon(Icons.Filled.Download, contentDescription = "Download image")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    entries: List<AnimeEntry>,
    onDownloadImages: (AnimeEntry) -> Unit,
    onUpdateTags: (AnimeEntry, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Title") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }

    val filteredAndSortedEntries = remember(entries, searchQuery, selectedType, selectedStatus, sortBy) {
        var filtered = entries
        
        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                (it.tags?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        
        // Apply type filter
        if (selectedType != "All") {
            filtered = filtered.filter { it.type == selectedType }
        }
        
        // Apply status filter
        if (selectedStatus != "All") {
            filtered = filtered.filter { it.status == selectedStatus }
        }
        
        // Apply sorting
        when (sortBy) {
            "Title" -> filtered.sortedBy { it.title }
            "Score" -> filtered.sortedByDescending { it.score }
            "Episodes" -> filtered.sortedByDescending { it.episodesWatched }
            "Date Added" -> filtered.sortedByDescending { it.id }
            else -> filtered
        }
    }

    Column(modifier = modifier) {
        // Search and filter bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search library...") },
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
                
                // Filter toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filters & Sort",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Filters"
                        )
                    }
                }
                
                // Expandable filters
                if (showFilters) {
                    Column {
                        // Type filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Anime", "Manga", "Novel").forEach { type ->
                                FilterChip(
                                    onClick = { selectedType = type },
                                    label = { Text(type) },
                                    selected = selectedType == type
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Status filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Completed", "Watching", "Plan to Watch", "Dropped").forEach { status ->
                                FilterChip(
                                    onClick = { selectedStatus = status },
                                    label = { Text(status) },
                                    selected = selectedStatus == status
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Sort dropdown
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = "Sort by: $sortBy",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("Title", "Score", "Episodes", "Date Added").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            sortBy = option
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Results count
        Text(
            "${filteredAndSortedEntries.size} entries",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Multi-select toolbar
        if (selectedItems.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedItems.size} selected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        IconButton(
                            onClick = {
                                // Batch download
                                selectedItems.forEach { id ->
                                    filteredAndSortedEntries.find { it.id == id }?.let { entry ->
                                        onDownloadImages(entry)
                                    }
                                }
                                selectedItems = emptySet()
                            }
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Batch Download")
                        }
                        IconButton(
                            onClick = {
                                // Batch tag edit - TODO: Show tag edit dialog
                                selectedItems = emptySet()
                            }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Tags")
                        }
                        IconButton(
                            onClick = { selectedItems = emptySet() }
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Selection")
                        }
                    }
                }
            }
        }
        
        // Library list
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAndSortedEntries) { entry ->
                LibraryItemCard(
                    entry = entry,
                    isSelected = selectedItems.contains(entry.id),
                    onSelectionChanged = { selected ->
                        selectedItems = if (selected) {
                            selectedItems + entry.id
                        } else {
                            selectedItems - entry.id
                        }
                    },
                    onDownloadImages = { onDownloadImages(entry) },
                    onUpdateTags = { tags -> onUpdateTags(entry, tags) }
                )
            }
        }
    }
}

@Composable
fun LibraryItemCard(
    entry: AnimeEntry,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onDownloadImages: () -> Unit,
    onUpdateTags: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelectionChanged(!isSelected) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.imagePath ?: entry.imageUrl,
                contentDescription = entry.title,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
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
                        label = { Text("★ ${entry.score}") }
                    )
                }
                
                entry.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                    Text(
                        text = "Tags: $tags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column {
                IconButton(onClick = onDownloadImages) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Download Images"
                    )
                }
                
                IconButton(
                    onClick = {
                        // TODO: Show tag edit dialog
                    }
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Tags"
                    )
                }
            }
        }
    }
}