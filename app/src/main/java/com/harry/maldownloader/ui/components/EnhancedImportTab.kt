package com.harry.maldownloader.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.utils.AppBuildInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EnhancedImportTab(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    onMalImportClick: () -> Unit,
    onTagsImportClick: () -> Unit,
    customTagsCount: Int,
    storagePermissionGranted: Boolean,
    onMenuSwipe: () -> Unit = {} // call this to open menu
) {
    var showAddTagsDialog by remember { mutableStateOf(false) }
    var showConfirmSampleDialog by remember { mutableStateOf(false) }
    var showSampleSavedBanner by remember { mutableStateOf<String?>(null) }
    var bannerOffsetPx by remember { mutableStateOf(0f) }
    val bannerOffsetDp: Dp by animateDpAsState(targetValue = bannerOffsetPx.dp, animationSpec = tween(250), label = "Banner offset animation")

    val scope = rememberCoroutineScope()

    LaunchedEffect(showSampleSavedBanner) {
        if (showSampleSavedBanner != null) {
            delay(3500)
            if (showSampleSavedBanner != null && bannerOffsetPx == 0f) showSampleSavedBanner = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎯 MAL Authentication & Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Client ID: ${AppBuildInfo.MAL_CLIENT_ID.take(12)}...", style = MaterialTheme.typography.bodyMedium)
                Text(if (storagePermissionGranted) "✅ Storage permission granted - Ready for downloads" else "❌ Storage permission denied - Downloads will fail", style = MaterialTheme.typography.bodySmall)
                Text("Ready for enhanced dual-API enrichment & Pictures directory storage", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onMalImportClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isProcessing && storagePermissionGranted
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Processing with enhanced dual-API & download engine...")
            } else {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("📄 Import MAL XML & Download Images with Metadata", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        ElevatedButton(
            onClick = { showAddTagsDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Label, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("➕ Add Custom Tags", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
            Column(Modifier.padding(12.dp)) {
                Text("📝 Custom Tags", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• Write your own tags or import from XML", style = MaterialTheme.typography.bodySmall)
                Text("• XML Structure: <tags><tag>ActionRPG</tag><tag>Favorite</tag></tags>", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showConfirmSampleDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Generate Sample Tags File")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Glassy sample saved banner with swipe-to-right & auto-hide logic
        if (showSampleSavedBanner != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = bannerOffsetDp)
                    .pointerInput(showSampleSavedBanner) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                // Only allow swipe right gesture
                                if (dragAmount > 0) bannerOffsetPx += dragAmount
                            },
                            onDragEnd = {
                                if (bannerOffsetPx > 85) {
                                    showSampleSavedBanner = null
                                    scope.launch { onMenuSwipe() }
                                } else {
                                    bannerOffsetPx = 0f
                                }
                            }
                        )
                    },
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB388FF).copy(alpha = 0.22f), Color(0xFF7C4DFF).copy(alpha = 0.22f))
                        )
                    ).padding(vertical = 8.dp, horizontal = 16.dp),
                ) {
                    Text(showSampleSavedBanner!!, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (isProcessing) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("🔄 Processing in progress...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Enriching entries with dual-API tags & downloading images", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    // Glassy Add Tags dialog with swipe-to-close
    if (showAddTagsDialog) {
        var dragOffset by remember { mutableStateOf(0f) }
        AlertDialog(
            onDismissRequest = { showAddTagsDialog = false },
            confirmButton = {},
            dismissButton = {},
            title = null,
            text = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .draggable(orientation = Orientation.Vertical, state = rememberDraggableState { delta ->
                            dragOffset += delta
                            if (dragOffset > 100f) showAddTagsDialog = false
                        }),
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.10f))))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Add Custom Tags", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        ElevatedButton(onClick = { showAddTagsDialog = false; viewModel.openManualTagsInput() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("Write Custom Tags", color = Color.White)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { showAddTagsDialog = false; onTagsImportClick() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.FileOpen, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("Import Custom Tags File", color = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Swipe down to close", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        )
    }
    if (showConfirmSampleDialog) {
        var dragOffset2 by remember { mutableStateOf(0f) }
        AlertDialog(
            onDismissRequest = { showConfirmSampleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmSampleDialog = false
                    val path = viewModel.generateSampleTagsFile()
                    showSampleSavedBanner = "File saved in the Download folder"
                }) { Text("Generate") }
            },
            dismissButton = { TextButton(onClick = { showConfirmSampleDialog = false }) { Text("Cancel") } },
            title = null,
            text = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .draggable(orientation = Orientation.Vertical, state = rememberDraggableState { d ->
                            dragOffset2 += d; if (dragOffset2 > 100f) showConfirmSampleDialog = false
                        }),
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(alpha=0.14f), Color.White.copy(alpha=0.10f)))).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Generate Sample Tags File?", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("A sample XML will be written to your Downloads folder.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("Swipe down to close", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        )
    }
}
