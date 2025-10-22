package com.harry.maldownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.harry.maldownloader.data.DownloadDatabase
import com.harry.maldownloader.data.DownloadRepository
import com.harry.maldownloader.ui.components.EntriesList
import com.harry.maldownloader.ui.components.TagManagerDialog
import com.harry.maldownloader.ui.theme.MaldownloaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: DownloadRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "Starting onCreate")
        
        try {
            Log.d("MainActivity", "Initializing database")
            val database = DownloadDatabase.getDatabase(this)
            
            Log.d("MainActivity", "Initializing repository")
            repository = DownloadRepository(
                context = this,
                database = database
            )

            Log.d("MainActivity", "Creating ViewModel")
            viewModel = ViewModelProvider(
                this,
                MainViewModelFactory(repository)
            )[MainViewModel::class.java]

            Log.d("MainActivity", "Setting content")
            setContent {
                MaldownloaderTheme {
                    SafeMainScreen(viewModel = viewModel)
                }
            }

            Log.d("MainActivity", "Checking permissions")
            checkPermissions()
            
            Log.d("MainActivity", "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error in onCreate", e)
            finish()
        }
    }

    private fun checkPermissions() {
        try {
            val permissions = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                }

                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (permissions.isNotEmpty()) {
                requestPermissions(permissions.toTypedArray(), 1001)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        try {
            if (requestCode == 1001) {
                permissions.forEachIndexed { index, permission ->
                    val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                    when (permission) {
                        Manifest.permission.POST_NOTIFICATIONS ->
                            viewModel.setNotificationPermission(granted)
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_MEDIA_IMAGES ->
                            viewModel.setStoragePermission(granted)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling permission result", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeMainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()

    val isProcessing by viewModel.isProcessing.collectAsState()
    val animeEntries by viewModel.animeEntries.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val customTags by viewModel.customTags.collectAsState()

    var showTagManager by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🎥 Import", "📁 Entries", "⬇️ Downloads", "📝 Logs")

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch { 
                viewModel.processMalFile(activity, it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MAL Downloader v3.0",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "25+ Dynamic Tags Edition",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTagManager = true }) {
                        Icon(Icons.Default.Settings, "Manage Tags")
                    }
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(Icons.Default.Clear, "Clear Logs")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Import XML")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> SafeImportTab(
                        viewModel = viewModel,
                        isProcessing = isProcessing,
                        onImportClick = {
                            filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
                        },
                        customTagsCount = customTags.size
                    )
                    1 -> SafeEntriesTab(
                        viewModel = viewModel,
                        entries = animeEntries
                    )
                    2 -> SafeDownloadsTab(
                        viewModel = viewModel,
                        downloads = downloads
                    )
                    3 -> SafeLogsTab(
                        viewModel = viewModel,
                        logs = logs
                    )
                }
            }
        }
    }

    if (showTagManager) {
        TagManagerDialog(
            viewModel = viewModel,
            onDismiss = { showTagManager = false }
        )
    }
}

@Composable
fun SafeImportTab(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    onImportClick: () -> Unit,
    customTagsCount: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    text = "🎯 MAL Authentication",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Client ID: aaf018d4c098158...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ready for API enrichment",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onImportClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing with API enrichment...")
            } else {
                Text(
                    text = "📄 Import MAL XML & Extract 25+ Tags",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🚀 Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val features = listOf(
                    "✅ 25+ Dynamic tags from MAL/Jikan API",
                    "✅ Comprehensive XMP metadata embedding",
                    "✅ AVES gallery compatibility",
                    "✅ Organized folder structure",
                    "✅ Custom tag management ($customTagsCount tags)",
                    "✅ Hentai content detection & tagging",
                    "✅ Rate-limited API calls",
                    "✅ Duplicate prevention"
                )

                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SafeEntriesTab(
    viewModel: MainViewModel,
    entries: List<com.harry.maldownloader.data.AnimeEntry>
) {
    val scope = rememberCoroutineScope()
    
    Column {
        if (entries.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${entries.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Entries", style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val totalTags = entries.sumOf { it.allTags.size }
                        Text(
                            text = "$totalTags",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text("Total Tags", style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val avgTags = if (entries.isNotEmpty()) entries.sumOf { it.allTags.size } / entries.size else 0
                        Text(
                            text = "$avgTags",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text("Avg Tags", style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val hentaiCount = entries.count { it.isHentai }
                        Text(
                            text = "$hentaiCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                        Text("Adult", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        EntriesList(
            viewModel = viewModel,
            entries = entries,
            onDownloadClick = { entry ->
                scope.launch { 
                    viewModel.downloadImages(entry)
                }
            }
        )
    }
}

@Composable
fun SafeDownloadsTab(
    viewModel: MainViewModel,
    downloads: List<com.harry.maldownloader.data.DownloadItem>
) {
    if (downloads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⬇️",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "No downloads yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Import MAL entries and download them",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        Column {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DownloadStat("Total", downloads.size.toString(), MaterialTheme.colorScheme.primary)
                    DownloadStat("Active", downloads.count { it.status == "downloading" }.toString(), MaterialTheme.colorScheme.secondary)
                    DownloadStat("Complete", downloads.count { it.status == "completed" }.toString(), Color(0xFF4CAF50))
                    DownloadStat("Failed", downloads.count { it.status == "failed" }.toString(), MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Download management UI will be enhanced in future updates",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun DownloadStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SafeLogsTab(
    viewModel: MainViewModel,
    logs: List<String>
) {
    Column {
        if (logs.isNotEmpty()) {
            Button(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Clear, "Clear")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Logs (${logs.size})")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ready to process MAL XML!\n\nLogs will appear here during processing.\n\nAny crashes will be captured here too.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    logs.forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 1.dp),
                            color = if (log.contains("FATAL")) Color.Red 
                                   else if (log.contains("ERROR")) Color(0xFFFF6B35)
                                   else if (log.contains("WARN")) Color(0xFFFF9800)
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}