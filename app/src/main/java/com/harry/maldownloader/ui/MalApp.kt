package com.harry.maldownloader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.harry.maldownloader.MainViewModel
import com.harry.maldownloader.data.AnimeEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MalApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Library", "Queue", "Logs", "Stats")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAL Downloader") },
                actions = {
                    IconButton(onClick = { viewModel.openFileImport() }) {
                        Icon(Icons.Default.Add, contentDescription = "Import XML")
                    }
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Progress indicators
            if (uiState.isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { uiState.progress }
                )
                Text(
                    text = uiState.statusMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> LibraryTab(
                    library = library,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onDownloadImage = viewModel::downloadImage
                )
                1 -> QueueTab(
                    queue = queue,
                    onCancelDownload = viewModel::cancelDownload,
                    onRetryDownload = viewModel::retryDownload
                )
                2 -> LogsTab(logs = logs)
                3 -> StatsTab(stats = stats)
            }
        }
    }
}

@Composable
private fun LibraryTab(
    library: List<AnimeEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDownloadImage: (AnimeEntry) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search anime...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Grid of anime
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(library) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = { onDownloadImage(anime) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimeCard(
    anime: AnimeEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(anime.imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = anime.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = anime.title,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QueueTab(
    queue: List<com.harry.maldownloader.data.DownloadItem>,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(queue) { item ->
            DownloadItemCard(
                item = item,
                onCancel = { onCancelDownload(item.id) },
                onRetry = { onRetryDownload(item.id) }
            )
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: com.harry.maldownloader.data.DownloadItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                when (item.status) {
                    com.harry.maldownloader.data.DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                        }
                    }
                    com.harry.maldownloader.data.DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        }
                    }
                    else -> {}
                }
            }
            
            when (item.status) {
                com.harry.maldownloader.data.DownloadStatus.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                com.harry.maldownloader.data.DownloadStatus.COMPLETED -> {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }
                com.harry.maldownloader.data.DownloadStatus.FAILED -> {
                    Text(
                        text = "Failed: ${item.errorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                com.harry.maldownloader.data.DownloadStatus.QUEUED -> {
                    Text(
                        text = "Queued",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun LogsTab(logs: List<String>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        reverseLayout = true
    ) {
        items(logs) { log ->
            Text(
                text = log,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StatsTab(stats: com.harry.maldownloader.data.AppStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatsCard(
            title = "Library",
            stats = listOf(
                "Total Entries" to stats.totalEntries.toString(),
                "Downloaded Images" to stats.downloadedImages.toString(),
                "Failed Downloads" to stats.failedDownloads.toString()
            )
        )
        
        StatsCard(
            title = "Downloads",
            stats = listOf(
                "Queue Size" to stats.queueSize.toString(),
                "Completed" to stats.completedDownloads.toString(),
                "In Progress" to stats.activeDownloads.toString()
            )
        )
    }
}

@Composable
private fun StatsCard(
    title: String,
    stats: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            stats.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label)
                    Text(value, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}