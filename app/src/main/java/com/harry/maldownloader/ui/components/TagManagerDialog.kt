package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val customTags by viewModel.customTags.collectAsState()
    
    ModernSwipeDialog(
        onDismiss = onDismiss,
        title = "🏷️ Custom Tags Manager",
        subtitle = "Organize your anime collection with ${customTags.size} custom tags",
        showSwipeIndicator = true
    ) {
        if (customTags.isEmpty()) {
            // Enhanced empty state
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Custom Tags Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add custom tags to better organize your anime collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Tags list with modern cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customTags) { tag ->
                    ModernGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 12.dp,
                        elevation = 6.dp,
                        glowIntensity = 0.08f
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            ModernIconButton(
                                onClick = { viewModel.removeCustomTag(tag) },
                                icon = Icons.Default.Delete,
                                contentDescription = "Remove tag: $tag",
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Statistics card
            ModernGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                glowIntensity = 0.1f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = customTags.size.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernButton(
                onClick = onDismiss,
                text = "Close",
                modifier = Modifier.weight(1f),
                variant = ModernButtonVariant.Tertiary,
                icon = Icons.Default.Close
            )
            
            if (customTags.isNotEmpty()) {
                ModernButton(
                    onClick = {
                        // TODO: Add export functionality
                        // viewModel.exportCustomTags()
                    },
                    text = "Export",
                    modifier = Modifier.weight(1f),
                    variant = ModernButtonVariant.Secondary,
                    icon = Icons.Default.FileDownload
                )
            }
        }
    }
}
