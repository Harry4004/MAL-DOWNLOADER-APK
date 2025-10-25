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
import androidx.compose.ui.graphics.Brush
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
        when (searchMode) {
            "Score" -> filtered.sortedByDescending { it.score ?: 0f }
            "Tags" -> filtered.sortedByDescending { it.allTags.size }
            else -> filtered.sortedBy { it.title }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Glass search header
        ModernGlassCard(cornerRadius = 16.dp) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("🔍 Search by $searchMode...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    Row {
                        // Mode selector
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { showSearchModeMenu = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(searchMode)
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                            DropdownMenu(expanded = showSearchModeMenu, onDismissRequest = { showSearchModeMenu = false }) {
                                listOf("Title", "Score", "Tags").forEach { mode ->
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
                                                Spacer(Modifier.width(8.dp))
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
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "Clear search") }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "ANIME", "MANGA").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type; viewModel.log("📊 Filtered by: $type") },
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

            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Showing ${filteredEntries.size} of ${entries.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Mode: $searchMode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filteredEntries.isEmpty()) {
            ModernGlassCard(cornerRadius = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (entries.isEmpty()) "📂" else "🔍", style = MaterialTheme.typography.headlineLarge)
                    Text(text = if (entries.isEmpty()) "No entries loaded" else "No matching entries", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (entries.isEmpty()) "Import a MAL XML file to see your collection" else "Try adjusting your search or filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredEntries) { entry ->
                    EnhancedEntryCard(
                        entry = entry,
                        viewModel = viewModel,
                        onDownloadClick = { onDownloadClick(entry) },
                        onViewDetailsClick = {
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

    // Glass card with type tint
    ModernGlassCard(cornerRadius = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!entry.englishTitle.isNullOrEmpty() && entry.englishTitle != entry.title) {
                        Text(entry.englishTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.MoreVert, contentDescription = "Actions", tint = if (isDownloaded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (hasImageUrl && !isDownloaded) {
                            DropdownMenuItem(text = { Text("📥 Download") }, onClick = { onDownloadClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Download, null) })
                        }
                        DropdownMenuItem(text = { Text("📊 Details") }, onClick = { onViewDetailsClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Info, null) })
                        DropdownMenuItem(text = { Text("🌐 Open MAL") }, onClick = { onOpenMalClick(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Launch, null) })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(entry.type.uppercase(), style = MaterialTheme.typography.labelSmall) })
                if (entry.isHentai) { AssistChip(onClick = { }, label = { Text("18+", style = MaterialTheme.typography.labelSmall) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFE91E63).copy(alpha = 0.2f))) }
                Text("MAL-${entry.malId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.score != null && entry.score > 0) { Text("⭐ ${String.format("%.1f", entry.score)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC107)) }
            }

            if (!entry.synopsis.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(entry.synopsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            if (entry.allTags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("🏷️ ${entry.allTags.size} tags: ${entry.allTags.take(5).joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernButton(onClick = onViewDetailsClick, text = "Details", icon = Icons.Default.Info, modifier = Modifier.weight(1f), variant = ModernButtonVariant.Tertiary)
                if (hasImageUrl) {
                    ModernButton(onClick = onDownloadClick, text = if (isDownloaded) "Downloaded" else "Download", icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download, modifier = Modifier.weight(1f), variant = if (isDownloaded) ModernButtonVariant.Secondary else ModernButtonVariant.Primary)
                } else {
                    ModernButton(onClick = { viewModel.log("⚠️ No image URL available for ${entry.title}") }, text = "No Image", icon = Icons.Default.Warning, modifier = Modifier.weight(1f), enabled = false, variant = ModernButtonVariant.Tertiary)
                }
            }
        }
    }
}
