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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    private val permissionRequestInProgress = mutableStateOf(false)

    // Enhanced permission handler with proper storage permission handling
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        try {
            permissionRequestInProgress.value = false
            if (::viewModel.isInitialized) {
                var storageGranted = false
                var notificationGranted = false
                
                permissions.forEach { (permission, granted) ->
                    Log.d("MainActivity", "Permission $permission: $granted")
                    when (permission) {
                        Manifest.permission.POST_NOTIFICATIONS -> {
                            notificationGranted = granted
                        }
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_MEDIA_IMAGES -> {
                            if (granted) storageGranted = true
                        }
                    }
                }
                
                viewModel.setNotificationPermission(notificationGranted)
                viewModel.setStoragePermission(storageGranted)
                
                if (storageGranted) {
                    viewModel.log("✅ Storage permissions granted - Ready for downloads")
                } else {
                    viewModel.log("⚠️ Storage permissions denied - Please grant storage access in Settings")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling permission result", e)
            permissionRequestInProgress.value = false
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

            checkAndRequestPermissions()
            isInitialized.value = true
            Log.d("MainActivity", "Initialization completed successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error during initialization", e)
            criticalError.value = e
        }
    }

    private fun checkAndRequestPermissions() {
        try {
            if (permissionRequestInProgress.value) {
                Log.d("MainActivity", "Permission request already in progress")
                return
            }
            
            val permissions = mutableListOf<String>()
            var hasAllStoragePermissions = false

            // Check storage permissions based on Android version
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13+ - Use READ_MEDIA_IMAGES
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    } else {
                        hasAllStoragePermissions = true
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10-12 - Use READ_EXTERNAL_STORAGE
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        hasAllStoragePermissions = true
                    }
                }
                else -> {
                    // Android 9 and below - Use both READ and WRITE
                    val readGranted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    val writeGranted = ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (!readGranted) {
                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    if (!writeGranted) {
                        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    
                    hasAllStoragePermissions = readGranted && writeGranted
                }
            }

            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Update ViewModel with current permission status
            if (::viewModel.isInitialized) {
                viewModel.setStoragePermission(hasAllStoragePermissions)
                
                val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    
                viewModel.setNotificationPermission(hasNotificationPermission)
            }

            // Request missing permissions
            if (permissions.isNotEmpty()) {
                Log.d("MainActivity", "Requesting permissions: ${permissions.joinToString(", ")}")
                permissionRequestInProgress.value = true
                permissionLauncher.launch(permissions.toTypedArray())
            } else {
                Log.d("MainActivity", "All permissions already granted")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions", e)
            permissionRequestInProgress.value = false
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🚀 Initializing MAL Downloader...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enhanced Edition v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorScreen(error: Throwable) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "❌ Critical Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.message ?: "Unknown error occurred",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please restart the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// AVES-Style Modern Drawer Component
@Composable
fun ModernDrawer(
    onClose: () -> Unit,
    onCustomTagsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    storagePermissionGranted: Boolean,
    notificationPermissionGranted: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Box {
            // Glass blur effect background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📱 MAL Downloader Menu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { 
                                contentDescription = "Close menu"
                                role = Role.Button
                            }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Permission Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Permission Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        PermissionStatusRow(
                            "Storage", 
                            storagePermissionGranted
                        )
                        PermissionStatusRow(
                            "Notifications", 
                            notificationPermissionGranted
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Menu items
                MenuItemWithIcon(
                    icon = Icons.Default.Tag,
                    title = "Custom Tags Manager",
                    subtitle = "Organize your collection",
                    onClick = {
                        onClose()
                        onCustomTagsClick()
                    }
                )
                
                MenuItemWithIcon(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "App configuration",
                    onClick = {
                        onClose()
                        onSettingsClick()
                    }
                )
                
                MenuItemWithIcon(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "App information",
                    onClick = {
                        onClose()
                        onAboutClick()
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionStatusRow(name: String, granted: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (granted) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$name: ${if (granted) "Granted" else "Denied"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun MenuItemWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
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
    val storagePermissionGranted by viewModel.storagePermissionGranted.collectAsState()
    val notificationPermissionGranted by viewModel.notificationPermissionGranted.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showTagManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    
    val tabs = listOf(
        "🍥 Import",
        "📂 Entries (${animeEntries.size})",
        "⬇️ Downloads (${downloads.size})",
        "📋 Logs (${logs.size})"
    )

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModernDrawer(
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onCustomTagsClick = {
                    viewModel.log("🏷️ Opening Custom Tags Manager")
                    showTagManager = true
                },
                onSettingsClick = {
                    viewModel.log("🔧 Opening Settings")
                    showSettings = true
                },
                onAboutClick = {
                    viewModel.log("ℹ️ Opening About")
                    showAbout = true
                },
                storagePermissionGranted = storagePermissionGranted,
                notificationPermissionGranted = notificationPermissionGranted
            )
        },
        gesturesEnabled = true,
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    // Detect swipe from right edge (90% of screen width)
                    if (offset.x > size.width * 0.9f) {
                        scope.launch { 
                            drawerState.open() 
                        }
                    }
                }
            ) { _, _ -> }
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
                                text = "Enhanced Edition - Rich API Tags",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = "Open navigation menu"
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        // Only show clear logs button in Logs tab
                        if (selectedTab == 3 && logs.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearLogs() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics {
                                        contentDescription = "Clear all logs"
                                        role = Role.Button
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
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
                            customTagsCount = customTags.size,
                            storagePermissionGranted = storagePermissionGranted
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

    if (showTagManager) {
        TagManagerDialog(viewModel = viewModel, onDismiss = { showTagManager = false })
    }
    if (showSettings) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showSettings = false })
    }
    if (showAbout) {
        AboutDialog(viewModel = viewModel, onDismiss = { showAbout = false })
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