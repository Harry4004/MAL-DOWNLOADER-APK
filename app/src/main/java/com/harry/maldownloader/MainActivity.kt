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
import com.harry.maldownloader.utils.AppBuildInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: DownloadRepository

    private val criticalError = mutableStateOf<Throwable?>(null)
    private val isInitialized = mutableStateOf(false)

    // Modern permission handler using Activity Result API
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        try {
            if (::viewModel.isInitialized) {
                permissions.forEach { (permission, granted) ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "Starting MAL Downloader v${BuildConfig.VERSION_NAME}")

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

        initializeApp()
    }

    private fun initializeApp() {
        try {
            Log.d("MainActivity", "Initializing database and repository")
            
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
                permissionLauncher.launch(permissions.toTypedArray())
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions", e)
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
                    text = "🚀 Initializing MAL Downloader v${BuildConfig.VERSION_NAME}...",
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
                        text = "App Version: v${AppBuildInfo.APP_VERSION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Contact: myaninelistapk@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
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

    // Updated state for 4-tab navigation and drawer
    var selectedTab by remember { mutableStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    
    // Only 4 main tabs now
    val tabs = listOf(
        "🎥 Import",
        "📂 Entries (${animeEntries.size})",
        "⬇️ Downloads (${downloads.size})",
        "📋 Logs (${logs.size})"
    )

    // File pickers for both MAL XML and custom tags XML
    val malFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.processMalFile(activity, it)
            }
        }
    }
    
    val tagsFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                viewModel.processCustomTagsFile(activity, it)
            }
        }
    }

    // Navigation Drawer for Settings/About/etc
    ModalNavigationDrawer(
        drawerState = rememberDrawerState(initialValue = if (showDrawer) DrawerValue.Open else DrawerValue.Closed),
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📱 MAL Downloader Menu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDrawer = false }) {
                            Icon(Icons.Default.Close, "Close Menu")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Menu items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Label, null) },
                        label = { Text("🏷️ Custom Tags Manager") },
                        selected = false,
                        onClick = {
                            viewModel.log("🏷️ Opening Custom Tags Manager")
                            showDrawer = false
                            showTagManager = true
                        }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("🔧 Settings") },
                        selected = false,
                        onClick = {
                            viewModel.log("🔧 Opening Settings")
                            showDrawer = false
                            showSettings = true
                        }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, null) },
                        label = { Text("ℹ️ About") },
                        selected = false,
                        onClick = {
                            viewModel.log("ℹ️ Opening About")
                            showDrawer = false
                            showAbout = true
                        }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Analytics, null) },
                        label = { Text("📊 Statistics") },
                        selected = false,
                        onClick = {
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
                            showDrawer = false
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // App info in drawer
                    Text(
                        text = "MAL Downloader v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Enhanced Edition with 25+ Tags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "MAL Downloader v${BuildConfig.VERSION_NAME}",
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
                        // Hamburger menu button (replaces individual Settings button)
                        IconButton(onClick = { showDrawer = !showDrawer }) {
                            Icon(
                                imageVector = if (showDrawer) Icons.Default.Close else Icons.Default.Menu,
                                contentDescription = if (showDrawer) "Close Menu" else "Open Menu"
                            )
                        }
                        
                        // Keep clear logs button for quick access
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
                // Context-sensitive FAB based on current tab
                when (selectedTab) {
                    0 -> FloatingActionButton(
                        onClick = {
                            malFilePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
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
                // Updated tab row with only 4 tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
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
                        0 -> EnhancedImportTab(
                            viewModel = viewModel,
                            isProcessing = isProcessing,
                            onMalImportClick = {
                                malFilePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
                            },
                            onTagsImportClick = {
                                tagsFilePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
                            },
                            customTagsCount = customTags.size
                        )
                        1 -> EnhancedEntriesList(
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
                    }
                }
            }
        }
    }

    // Handle drawer state changes
    LaunchedEffect(showDrawer) {
        // Additional logic if needed for drawer state
    }
    
    // Preserve all existing dialogs
    if (showTagManager) {
        TagManagerDialog(
            viewModel = viewModel,
            onDismiss = { showTagManager = false }
        )
    }
    
    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }
    
    if (showAbout) {
        AboutDialog(
            viewModel = viewModel,
            onDismiss = { showAbout = false }
        )
    }
}

class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}