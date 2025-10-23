package com.harry.maldownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.utils.AppBuildInfo

@Composable
fun EnhancedImportTab(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    onMalImportClick: () -> Unit,
    onTagsImportClick: () -> Unit, // kept for file picker route
    customTagsCount: Int,
    storagePermissionGranted: Boolean
) {
    var showAddTagsDialog by remember { mutableStateOf(false) }
    var manualTagsText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎯 MAL Authentication & Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Client ID: ${AppBuildInfo.MAL_CLIENT_ID.take(12)}...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (storagePermissionGranted) "✅ Storage permission granted - Ready for downloads" else "❌ Storage permission denied - Downloads will fail",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Ready for enhanced dual-API enrichment & Pictures directory storage",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main MAL XML import button
        Button(
            onClick = onMalImportClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isProcessing && storagePermissionGranted
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing with enhanced dual-API & download engine...")
            } else {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📄 Import MAL XML & Download Images with Metadata",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // REPLACED: Old orange box -> New glassy Add Custom Tags button
        ElevatedButton(
            onClick = { showAddTagsDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Label, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "➕ Add Custom Tags",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Info card about custom tags
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📝 Custom Tags",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Write your own tags or import from XML", style = MaterialTheme.typography.bodySmall)
                Text("• XML Structure: <tags><tag>ActionRPG</tag><tag>Favorite</tag></tags>", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.generateSampleTagsFile() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Sample Tags File")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Features card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🚀 Enhanced Features v${AppBuildInfo.APP_VERSION}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "✅ Public Pictures directory storage (gallery visible)",
                    "✅ 25+ Dynamic tags from dual MAL+Jikan API integration",
                    "✅ XMP metadata embedding (AVES Gallery compatible)",
                    "✅ Enhanced search, filter & sort functionality",
                    "✅ Working action buttons with context menus",
                    "✅ Adult content auto-detection & separation",
                    "✅ Real-time progress with comprehensive logging",
                    "✅ Professional error handling & recovery",
                    "✅ Custom tag management ($customTagsCount tags loaded)",
                    "✅ NEW: Manual write + XML import for custom tags"
                ).forEach { feature ->
                    Text(feature, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 1.dp))
                }
            }
        }
    }

    // Apple-style transparent dialog for Add Custom Tags
    if (showAddTagsDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagsDialog = false },
            confirmButton = {},
            dismissButton = {},
            title = null,
            text = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Add Custom Tags",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        // Option 1: Write Custom Tags
                        ElevatedButton(
                            onClick = { /* open inline field: toggle a second stage */ showAddTagsDialog = false; viewModel.openManualTagsInput() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Write Custom Tags", color = Color.White)
                        }

                        Spacer(Modifier.height(10.dp))

                        // Option 2: Import Custom Tags File (reuse existing picker)
                        OutlinedButton(
                            onClick = { showAddTagsDialog = false; onTagsImportClick() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Import Custom Tags File", color = Color.White)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Tip: You can paste tags separated by commas or new lines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(Modifier.height(6.dp))

                        TextButton(onClick = { showAddTagsDialog = false }) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        )
    }
}
