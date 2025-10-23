package com.harry.maldownloader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.BuildConfig
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.utils.AppBuildInfo

@Composable
fun EnhancedImportTab(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    onMalImportClick: () -> Unit,
    onTagsImportClick: () -> Unit,
    customTagsCount: Int,
    storagePermissionGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status card - preserve existing functionality
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

        // Main MAL XML import button - preserve existing functionality
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

        // NEW: Custom Tags Import Button
        OutlinedButton(
            onClick = onTagsImportClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isProcessing
        ) {
            Icon(Icons.Default.Label, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "🏷️ Import Custom Tags File",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // NEW: Information card about custom tags file format
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📝 Custom Tags File Format",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Format: XML file (.xml)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Structure: <tags><tag>ActionRPG</tag><tag>Favorite</tag></tags>",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Use: Organize your collection with personal tags",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Compatible: Works with existing XML parser (minimal code changes)",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        viewModel.generateSampleTagsFile()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Sample Tags File")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preserve existing features card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🚀 Enhanced Features v${AppBuildInfo.APP_VERSION}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val features = listOf(
                    "✅ Public Pictures directory storage (gallery visible)",
                    "✅ 25+ Dynamic tags from dual MAL+Jikan API integration",
                    "✅ XMP metadata embedding (AVES Gallery compatible)",
                    "✅ Enhanced search, filter & sort functionality",
                    "✅ Working action buttons with context menus",
                    "✅ Adult content auto-detection & separation",
                    "✅ Real-time progress with comprehensive logging",
                    "✅ Professional error handling & recovery",
                    "✅ Custom tag management ($customTagsCount tags loaded)",
                    "✅ NEW: Custom tags file import support (XML format)"
                )

                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Processing status indicator
        if (isProcessing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔄 Processing in progress...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Enriching entries with dual-API tags & downloading images",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}