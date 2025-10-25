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
    onTagsImportClick: () -> Unit,
    customTagsCount: Int,
    storagePermissionGranted: Boolean,
    onMenuSwipe: () -> Unit
) {
    var showAddTagsDialog by remember { mutableStateOf(false) }
    var showConfirmSampleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glass header with status
        ModernGlassCard(cornerRadius = 20.dp) {
            Text(
                "🎯 MAL Authentication & Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Client ID: ${AppBuildInfo.MAL_CLIENT_ID.take(12)}...",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (storagePermissionGranted) "✅ Storage permission granted - Ready for downloads" else "❌ Storage permission denied - Downloads will fail",
                style = MaterialTheme.typography.bodySmall,
                color = if (storagePermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                "Ready for enhanced dual-API enrichment & Pictures directory storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))

        // Primary action (ModernButton)
        ModernButton(
            onClick = onMalImportClick,
            text = if (isProcessing) "Processing..." else "📄 Import MAL XML & Download Images with Metadata",
            modifier = Modifier.fillMaxWidth(),
            icon = if (isProcessing) Icons.Default.Sync else Icons.Default.Upload,
            variant = ModernButtonVariant.Primary,
            enabled = !isProcessing && storagePermissionGranted
        )

        Spacer(Modifier.height(12.dp))

        // Add Custom Tags action
        ModernButton(
            onClick = { showAddTagsDialog = true },
            text = "➕ Add Custom Tags ($customTagsCount)",
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Label,
            variant = ModernButtonVariant.Secondary
        )

        Spacer(Modifier.height(16.dp))

        // Info card with sample generator
        ModernGlassCard(cornerRadius = 16.dp) {
            Text("📝 Custom Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("• Write your own tags or import from XML", style = MaterialTheme.typography.bodySmall)
            Text("• XML Structure: <tags><tag>ActionRPG</tag><tag>Favorite</tag></tags>", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showConfirmSampleDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Generate Sample Tags File")
            }
        }
    }

    if (showAddTagsDialog) {
        ModernSwipeDialog(
            onDismiss = { showAddTagsDialog = false },
            title = "Add Custom Tags",
            subtitle = "Write or import your tags"
        ) {
            ModernButton(
                onClick = { showAddTagsDialog = false /* TODO open editor */ },
                text = "Write Custom Tags",
                icon = Icons.Default.Edit,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            ModernButton(
                onClick = { showAddTagsDialog = false; onTagsImportClick() },
                text = "Import Custom Tags File",
                icon = Icons.Default.FileOpen,
                modifier = Modifier.fillMaxWidth(),
                variant = ModernButtonVariant.Tertiary
            )
        }
    }

    if (showConfirmSampleDialog) {
        ModernSwipeDialog(
            onDismiss = { showConfirmSampleDialog = false },
            title = "Generate Sample Tags File?",
            subtitle = "A sample XML will be written to your Downloads folder."
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernButton(
                    onClick = { showConfirmSampleDialog = false },
                    text = "Cancel",
                    variant = ModernButtonVariant.Tertiary,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Close
                )
                ModernButton(
                    onClick = {
                        showConfirmSampleDialog = false
                        viewModel.generateSampleTagsFile()
                    },
                    text = "Generate",
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
