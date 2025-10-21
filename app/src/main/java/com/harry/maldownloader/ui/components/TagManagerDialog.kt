package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.harry.maldownloader.MainViewModel

@Composable
fun TagManagerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏷️ Tag Manager",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add Tag Button
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ Add Custom Tag")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // View Tags Button
                OutlinedButton(
                    onClick = { showViewDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("👁️ View/Remove Tags")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
    
    // Add Tag Dialog
    if (showAddDialog) {
        AddTagDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }
    
    // View Tags Dialog
    if (showViewDialog) {
        ViewTagsDialog(
            viewModel = viewModel,
            onDismiss = { showViewDialog = false }
        )
    }
}

@Composable
fun AddTagDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf("Anime") }
    var tagText by remember { mutableStateOf("") }
    val types = listOf("Anime", "Manga", "Hentai")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Add Custom Tag",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Type Selection
                Text("Select Category:")
                types.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedType == type),
                                onClick = { selectedType = type }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedType == type),
                            onClick = { selectedType = type }
                        )
                        Text(
                            text = type,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tag Input
                OutlinedTextField(
                    value = tagText,
                    onValueChange = { tagText = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g., Action, Colored, NTR") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (tagText.isNotEmpty()) {
                                viewModel.addCustomTag(selectedType.lowercase(), tagText)
                                viewModel.log("✅ Added $selectedType tag: $tagText")
                                onDismiss()
                            }
                        },
                        enabled = tagText.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun ViewTagsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val customTags by viewModel.customTags.collectAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Custom Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (customTags.isEmpty()) {
                    Text(
                        text = "No custom tags yet.\nAdd some tags to organize your collection!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = "Long press to remove:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(customTags) { tag ->
                            TagItem(
                                tag = tag,
                                onRemove = {
                                    viewModel.removeCustomTag(tag)
                                    viewModel.log("🗑️ Removed tag: $tag")
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun TagItem(
    tag: String,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                tag.startsWith("Anime:") -> Color(0xFF3F51B5).copy(alpha = 0.1f)
                tag.startsWith("Manga:") -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                tag.startsWith("Hentai:") -> Color(0xFFE91E63).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            TextButton(
                onClick = { showRemoveDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        }
    }
    
    // Remove confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Tag?") },
            text = { Text("Are you sure you want to remove\\n$tag?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}