package com.harry.maldownloader.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.draw.scale
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
fun LiquidGlassImportTab(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    onMalImportClick: () -> Unit,
    onTagsImportClick: () -> Unit,
    customTagsCount: Int,
    storagePermissionGranted: Boolean,
    onMenuSwipe: () -> Unit = {}
) {
    var showAddTagsDialog by remember { mutableStateOf(false) }
    var showConfirmSampleDialog by remember { mutableStateOf(false) }
    var showSampleSavedBanner by remember { mutableStateOf<String?>(null) }
    var bannerOffsetPx by remember { mutableStateOf(0f) }
    val bannerOffsetDp: Dp by animateDpAsState(
        targetValue = bannerOffsetPx.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "Banner offset animation"
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(showSampleSavedBanner) {
        if (showSampleSavedBanner != null) {
            delay(4000)
            if (showSampleSavedBanner != null && bannerOffsetPx == 0f) {
                showSampleSavedBanner = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        
        // Status card with enhanced glass effect
        GlassCard(
            surfaceAlpha = 0.15f,
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎯 MAL Authentication & Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Client ID: ${AppBuildInfo.MAL_CLIENT_ID.take(12)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (storagePermissionGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (storagePermissionGranted) Color(0xFF34C759) else Color(0xFFFF453A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (storagePermissionGranted) "Storage ready for downloads" else "Storage permission required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Enhanced dual-API enrichment & Pictures directory storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Main import button with enhanced glass
        GlassButton(
            onClick = onMalImportClick,
            enabled = !isProcessing && storagePermissionGranted,
            surfaceAlpha = 0.2f,
            tintColor = MaterialTheme.colorScheme.primary
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Processing with dual-API engine...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "📄 Import MAL XML & Download Images",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        
        // Add Custom Tags glass button
        GlassButton(
            onClick = { showAddTagsDialog = true },
            surfaceAlpha = 0.12f,
            tintColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                Icons.Default.Label,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "➕ Add Custom Tags",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Info card with minimalist glass
        GlassCard(
            surfaceAlpha = 0.08f,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "📝 Custom Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "• Write your own tags or import from XML",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• XML Structure: <tags><tag>ActionRPG</tag><tag>Favorite</tag></tags>",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                
                GlassButton(
                    onClick = { showConfirmSampleDialog = true },
                    surfaceAlpha = 0.1f,
                    tintColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Generate Sample Tags File",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        
        // Auto-hide glassy banner with enhanced swipe
        if (showSampleSavedBanner != null) {
            GlassBanner(
                text = showSampleSavedBanner!!,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = bannerOffsetDp)
                    .pointerInput(showSampleSavedBanner) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) bannerOffsetPx += dragAmount
                            },
                            onDragEnd = {
                                if (bannerOffsetPx > 140) {
                                    showSampleSavedBanner = null
                                    scope.launch { onMenuSwipe() }
                                } else {
                                    bannerOffsetPx = 0f
                                }
                            }
                        )
                    },
                onDismiss = { showSampleSavedBanner = null },
                autoHideDelay = 4000L,
                tintColor = MaterialTheme.colorScheme.primary
            )
        }
        
        // Processing indicator with glass
        if (isProcessing) {
            GlassCard(
                surfaceAlpha = 0.12f,
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "🔄 Processing in progress...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enriching entries with dual-API tags & downloading images",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }

    // Enhanced Apple-style liquid glass Add Tags dialog
    if (showAddTagsDialog) {
        LiquidGlassDialog(
            onDismissRequest = { showAddTagsDialog = false },
            title = "Add Custom Tags"
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassButton(
                    onClick = { showAddTagsDialog = false },
                    surfaceAlpha = 0.2f,
                    tintColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Write Custom Tags",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                GlassButton(
                    onClick = {
                        showAddTagsDialog = false
                        onTagsImportClick()
                    },
                    surfaceAlpha = 0.15f,
                    tintColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Import Custom Tags File",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    "Swipe down to close",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }

    // Enhanced confirmation dialog for sample generation
    if (showConfirmSampleDialog) {
        LiquidGlassDialog(
            onDismissRequest = { showConfirmSampleDialog = false },
            title = "Generate Sample Tags File?",
            confirmButton = {
                GlassButton(
                    onClick = {
                        showConfirmSampleDialog = false
                        viewModel.generateSampleTagsFile()
                        showSampleSavedBanner = "File saved in the Download folder"
                    },
                    surfaceAlpha = 0.2f,
                    tintColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "Generate",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                GlassButton(
                    onClick = { showConfirmSampleDialog = false },
                    surfaceAlpha = 0.1f,
                    tintColor = Color.Gray
                ) {
                    Text(
                        "Cancel",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        ) {
            Text(
                text = "A sample XML will be written to your Downloads folder with example tags.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}