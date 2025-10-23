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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.harry.maldownloader.data.DownloadRepository
import com.harry.maldownloader.ui.components.EntriesList
import com.harry.maldownloader.ui.components.LogsPanel
import com.harry.maldownloader.ui.components.TagManagerDialog
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
                    text = "Enhanced with robust downloading & XMP metadata",
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
                text = "• Restart the app\n• Clear app data and cache\n• Grant all requested permissions\n• Reinstall if issue persists",
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
                            text = "MAL Downloader v${BuildConfig.APP_VERSION}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enhanced Download Engine & XMP Metadata",
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
                    3 -> {
                        LogsPanel(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
                    text = "Client ID: ${BuildConfig.MAL_CLIENT_ID.take(12)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Ready for enhanced API enrichment & download",
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
                Text("Processing with enhanced API & download engine...")
            } else {
                Text(
                    text = "📄 Import MAL XML & Download Images with Metadata",
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
                    text = "🚀 Enhanced Features v3.1",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val features = listOf(
                    "✅ 25+ Dynamic tags from MAL/Jikan API",
                    "✅ Robust image downloading with retry logic",
                    "✅ XMP metadata embedding for gallery apps",
                    "✅ AVES gallery full compatibility",
                    "✅ Organized folder structure with tags",
                    "✅ Custom tag management ($customTagsCount tags)",
                    "✅ Hentai content detection & filtering",
                    "✅ Rate-limited API calls with fallback",
                    "✅ Duplicate prevention & resume support",
                    "✅ Enhanced error reporting & diagnostics"
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val downloadedCount = entries.count { !it.imagePath.isNullOrEmpty() }
                        Text(
                            text = "$downloadedCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text("Downloaded", style = MaterialTheme.typography.bodySmall)
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
                    text = "Import MAL entries and download them with enhanced engine",
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
                text = "Enhanced download management with XMP metadata embedding and robust retry logic.",
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