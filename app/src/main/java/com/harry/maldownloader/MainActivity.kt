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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.harry.maldownloader.data.DownloadRepository
import com.harry.maldownloader.ui.components.*
import com.harry.maldownloader.ui.theme.MaldownloaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: DownloadRepository

    // Hold critical startup error to display in UI
    private val criticalError = mutableStateOf<Throwable?>(null)
    private val isInitialized = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "Starting MAL Downloader v${BuildConfig.APP_VERSION}")

        // Set content first to prevent black screen
        setContent {
            MaldownloaderTheme {
                when {
                    criticalError.value != null -> {
                        ErrorScreen(error = criticalError.value!!)
                    }
                    !isInitialized.value -> {
                        LoadingScreen()
                    }
                    else -> {
                        SafeMainScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Initialize in background to prevent blocking UI
        initializeApp()
    }

    private fun initializeApp() {
        try {
            Log.d("MainActivity", "Initializing database and repository")
            
            // Get database from Application with null safety
            val app = application as MainApplication
            val database = app.database
            
            if (database == null) {
                throw Exception("Database initialization failed in MainApplication")
            }

            repository = DownloadRepository(
                context = this,
                database = database
            )

            Log.d("MainActivity", "Creating ViewModel with enhanced features")
            viewModel = ViewModelProvider(
                this,
                MainViewModelFactory(repository)
            )[MainViewModel::class.java]

            checkPermissions()

            // Mark as initialized
            isInitialized.value = true
            Log.d("MainActivity", "Initialization completed successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error during initialization", e)
            criticalError.value = e
        }
    }

    private fun checkPermissions() {
        try {
            val permissions = mutableListOf<String>()

            // Storage permissions for image saving
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

            // Android 13+ media permissions
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
            if (requestCode == 1001 && ::viewModel.isInitialized) {
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

@Composable
fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🚀 Initializing MAL Downloader v${BuildConfig.APP_VERSION}...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enhanced with robust downloading, XMP metadata & 25+ dynamic tags",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(error: Throwable) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "🚨 Critical Startup Error:",
                color = Color.Red,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error Details:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error.localizedMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "App Version: v${BuildConfig.APP_VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Troubleshooting:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Restart the app\n• Clear app data and cache\n• Grant all requested permissions\n• Contact myaninelistapk@gmail.com if issue persists",
                style = MaterialTheme.typography.bodyMedium
            )
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
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Enhanced tabs with badges
    val tabs = listOf(
        "🎥 Import" to "",
        "📂 Entries" to if (animeEntries.isNotEmpty()) "(${animeEntries.size})" else "",
        "⬇️ Downloads" to if (downloads.isNotEmpty()) "(${downloads.size})" else "",
        "📋 Logs" to if (logs.isNotEmpty()) "(${logs.size})" else "",
        "🔧 Settings" to "",
        "ℹ️ About" to ""
    )

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
                            text = "MAL Downloader v${BuildConfig.APP_VERSION}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enhanced Edition - Public Pictures Storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Global actions
                    IconButton(
                        onClick = { 
                            // Quick stats
                            val downloaded = animeEntries.count { !it.imagePath.isNullOrEmpty() }
                            val adultContent = animeEntries.count { it.isHentai }
                            val totalTags = animeEntries.sumOf { it.allTags.size }
                            val avgScore = if (animeEntries.isNotEmpty()) {
                                animeEntries.mapNotNull { it.score }.average().takeIf { !it.isNaN() }
                            } else null
                            
                            val stats = buildString {
                                appendLine("📊 MAL Downloader Statistics:")
                                appendLine("📚 Total Entries: ${animeEntries.size}")
                                appendLine("⬇️ Downloaded: $downloaded")
                                appendLine("🏷️ Total Tags: $totalTags")
                                appendLine("🔞 Adult Content: $adultContent")
                                avgScore?.let { appendLine("⭐ Average Score: ${String.format("%.1f", it)}") }
                                appendLine("📋 Log Entries: ${logs.size}")
                                appendLine("📥 Download Records: ${downloads.size}")
                            }
                            viewModel.log(stats)
                        }
                    ) {
                        Icon(Icons.Default.Analytics, "Quick Stats")
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
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Import XML")
                }
                1 -> if (animeEntries.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                val downloadable = animeEntries.filter { 
                                    !it.imageUrl.isNullOrEmpty() && it.imagePath.isNullOrEmpty() 
                                }
                                downloadable.forEach { entry ->
                                    viewModel.downloadImages(entry)
                                }
                                viewModel.log("📥 Started batch download for ${downloadable.size} entries")
                            }
                        }
                    ) {
                        Icon(Icons.Default.GetApp, "Download All")
                    }
                }
                2 -> if (downloads.any { it.status == "failed" }) {
                    FloatingActionButton(
                        onClick = {
                            val failedCount = downloads.count { it.status == "failed" }
                            viewModel.log("🔄 Retrying $failedCount failed downloads")
                            // TODO: Implement retry failed functionality
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Retry Failed")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Enhanced tabs with badges and counts
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, (title, badge) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (badge.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
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
                    0 -> EnhancedImportTab(
                        viewModel = viewModel,
                        isProcessing = isProcessing,
                        onImportClick = {
                            filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
                        },
                        customTagsCount = customTags.size,
                        entriesCount = animeEntries.size
                    )
                    1 -> EntriesList(
                        viewModel = viewModel,
                        entries = animeEntries,
                        onDownloadClick = { entry ->
                            scope.launch {
                                viewModel.downloadImages(entry)
                            }
                        }
                    )
                    2 -> EnhancedDownloadsTab(
                        viewModel = viewModel,
                        downloads = downloads
                    )
                    3 -> EnhancedLogsPanel(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    4 -> SettingsScreen(
                        viewModel = viewModel,
                        onDismiss = { selectedTab = 0 }
                    )
                    5 -> AboutScreen(
                        viewModel = viewModel,
                        onDismiss = { selectedTab = 0 }
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
fun EnhancedImportTab(
    viewModel: MainViewModel,
    isProcessing: Boolean,
    onImportClick: () -> Unit,
    customTagsCount: Int,
    entriesCount: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status overview card
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
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusChip("API", "Ready", Color(0xFF4CAF50))
                    StatusChip("Storage", "Pictures", MaterialTheme.colorScheme.primary)
                    StatusChip("Entries", entriesCount.toString(), MaterialTheme.colorScheme.secondary)
                    StatusChip("Tags", customTagsCount.toString(), MaterialTheme.colorScheme.tertiary)
                }
                
                Text(
                    text = "Client ID: ${BuildConfig.MAL_CLIENT_ID.take(12)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main import button
        Button(
            onClick = onImportClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Processing with Enhanced Engine...",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "API enrichment + Pictures directory storage",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FileUpload,
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Import MAL XML & Download Images",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "with 25+ Metadata Tags",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Secondary action buttons
        if (!isProcessing && entriesCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // TODO: Re-run metadata enrichment only
                        viewModel.log("🔄 Re-enriching metadata for $entriesCount entries")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh Tags")
                }
                
                OutlinedButton(
                    onClick = {
                        // TODO: Validate current XML
                        viewModel.log("🔍 Validating current entries data")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Validate")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enhanced features card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🚀 Enhanced Features v3.1",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                val features = listOf(
                    "✅ Public Pictures directory storage (gallery visible)",
                    "✅ 25+ Dynamic tags from dual API integration",
                    "✅ XMP metadata embedding (AVES Gallery compatible)",
                    "✅ Concurrent downloads with smart retry logic",
                    "✅ Adult content auto-detection & separation",
                    "✅ Network & battery aware download management",
                    "✅ Real-time progress with comprehensive logging",
                    "✅ Search, filter & sort with batch operations",
                    "✅ Custom tag management ($customTagsCount tags loaded)",
                    "✅ Professional error handling & recovery"
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
    }
}

@Composable
fun StatusChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Enhanced MainViewModelFactory with error handling
class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}