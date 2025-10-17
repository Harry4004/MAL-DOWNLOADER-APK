package com.harry.maldownloader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.data.TagConfiguration
import com.harry.maldownloader.data.TagGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val tagConfig by viewModel.tagConfiguration.collectAsStateWithLifecycle()
    var showTagEditor by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<TagGroup?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportTags(it) }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importTags(it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Download Settings") {
                    SettingItem(
                        title = "Max Concurrent Downloads",
                        subtitle = "Number of simultaneous downloads",
                        trailing = {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = "3",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf(1, 2, 3, 5).forEach { count ->
                                        DropdownMenuItem(
                                            text = { Text(count.toString()) },
                                            onClick = {
                                                expanded = false
                                                // TODO: Update setting
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    
                    SettingItem(
                        title = "Auto-retry Failed Downloads",
                        subtitle = "Retry downloads that fail due to network issues",
                        trailing = {
                            Switch(
                                checked = true,
                                onCheckedChange = { /* TODO */ }
                            )
                        }
                    )
                }
            }
            
            item {
                SettingsSection(title = "Tag Configuration") {
                    SettingItem(
                        title = "Anime Tags",
                        subtitle = "${tagConfig.anime.tags.size} tags configured",
                        trailing = {
                            IconButton(
                                onClick = {
                                    editingGroup = tagConfig.anime
                                    showTagEditor = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    )
                    
                    SettingItem(
                        title = "Manga Tags", 
                        subtitle = "${tagConfig.manga.tags.size} tags configured",
                        trailing = {
                            IconButton(
                                onClick = {
                                    editingGroup = tagConfig.manga
                                    showTagEditor = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    )
                    
                    SettingItem(
                        title = "Hentai Tags",
                        subtitle = "${tagConfig.hentai.tags.size} tags configured",
                        trailing = {
                            IconButton(
                                onClick = {
                                    editingGroup = tagConfig.hentai
                                    showTagEditor = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    )
                }
            }
            
            item {
                SettingsSection(title = "Import/Export") {
                    SettingItem(
                        title = "Export Tag Configuration",
                        subtitle = "Save tags to JSON file",
                        trailing = {
                            IconButton(
                                onClick = {
                                    exportLauncher.launch("mal_tags_${System.currentTimeMillis()}.json")
                                }
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = "Export")
                            }
                        }
                    )
                    
                    SettingItem(
                        title = "Import Tag Configuration",
                        subtitle = "Load tags from JSON file",
                        trailing = {
                            IconButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Import")
                            }
                        }
                    )
                    
                    SettingItem(
                        title = "Export Application Logs",
                        subtitle = "Save debug logs to file",
                        trailing = {
                            IconButton(
                                onClick = { viewModel.exportLogs() }
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = "Export Logs")
                            }
                        }
                    )
                }
            }
        }
    }
    
    if (showTagEditor && editingGroup != null) {
        TagEditorDialog(
            tagGroup = editingGroup!!,
            onDismiss = {
                showTagEditor = false
                editingGroup = null
            },
            onSave = { updatedGroup ->
                // TODO: Update tag configuration
                showTagEditor = false
                editingGroup = null
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        trailing?.invoke()
    }
}

@Composable
fun TagEditorDialog(
    tagGroup: TagGroup,
    onDismiss: () -> Unit,
    onSave: (TagGroup) -> Unit
) {
    var tags by remember { mutableStateOf(tagGroup.tags.toMutableList()) }
    var newTagText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${tagGroup.name} Tags") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(tags) { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { tags.remove(tag) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text("New tag") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newTagText.isNotBlank() && !tags.contains(newTagText)) {
                                tags.add(newTagText)
                                newTagText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(tagGroup.copy(tags = tags))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}