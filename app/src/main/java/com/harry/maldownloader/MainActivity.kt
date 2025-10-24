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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.graphicsLayer
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
import com.harry.maldownloader.ui.theme.LiquidGlassTheme
import com.harry.maldownloader.utils.AppBuildInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: DownloadRepository
    private val criticalError = mutableStateOf<Throwable?>(null)
    private val isInitialized = mutableStateOf(false)
    private val permissionRequestInProgress = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        try {
            permissionRequestInProgress.value = false
            if (::viewModel.isInitialized) {
                var storageGranted = false
                var notificationGranted = false
                permissions.forEach { (permission, granted) ->
                    when (permission) {
                        Manifest.permission.POST_NOTIFICATIONS -> notificationGranted = granted
                        Manifest.permission.READ_EXTERNAL_STORAGE, 
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                        Manifest.permission.READ_MEDIA_IMAGES -> if (granted) storageGranted = true
                    }
                }
                viewModel.setNotificationPermission(notificationGranted)
                viewModel.setStoragePermission(storageGranted)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling permission result", e)
            permissionRequestInProgress.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiquidGlassTheme {
                when {
                    criticalError.value != null -> ErrorScreen(error = criticalError.value!!)
                    !isInitialized.value -> LoadingScreen()
                    else -> LiquidGlassMainScreen(viewModel = viewModel)
                }
            }
        }
        initializeApp()
    }

    private fun initializeApp() {
        try {
            val app = application as MainApplication
            val database = app.database ?: throw Exception("Database initialization failed")
            repository = DownloadRepository(context = this, database = database)
            viewModel = ViewModelProvider(this, MainViewModelFactory(repository))[MainViewModel::class.java]
            checkAndRequestPermissions()
            isInitialized.value = true
        } catch (e: Exception) {
            criticalError.value = e
        }
    }

    private fun checkAndRequestPermissions() {
        if (permissionRequestInProgress.value) return
        val permissions = mutableListOf<String>()
        var hasAllStoragePermissions = false
        
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    hasAllStoragePermissions = true
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    hasAllStoragePermissions = true
                }
            }
            else -> {
                val readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                val writeGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (!readGranted) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (!writeGranted) permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                hasAllStoragePermissions = readGranted && writeGranted
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (::viewModel.isInitialized) {
            viewModel.setStoragePermission(hasAllStoragePermissions)
            val hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            viewModel.setNotificationPermission(hasNotificationPermission)
        }
        
        if (permissions.isNotEmpty()) {
            permissionRequestInProgress.value = true
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    radius = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            surfaceAlpha = 0.15f,
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "🚀 Initializing MAL Downloader...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Liquid Glass Edition v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(error: Throwable) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    radius = 1000f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            surfaceAlpha = 0.15f,
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "❌ Critical Error",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error.message ?: "Unknown error occurred",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Please restart the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LiquidGlassDrawer(
    onClose: () -> Unit,
    onCustomTagsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    storagePermissionGranted: Boolean,
    notificationPermissionGranted: Boolean
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxHeight()
            .width(340.dp),
        cornerRadius = 28.dp,
        surfaceAlpha = 0.18f,
        borderAlpha = 0.3f,
        highlightStrength = 0.2f,
        tintColor = Color.Black,
        backdropEnabled = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📱 Menu",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close menu",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Permission Status with glass card
            GlassCard(
                surfaceAlpha = 0.12f,
                cornerRadius = 20.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Permission Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    PermissionStatusRow("Storage", storagePermissionGranted)
                    Spacer(Modifier.height(8.dp))
                    PermissionStatusRow("Notifications", notificationPermissionGranted)
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // Menu items with glass effects
            GlassMenuItemWithIcon(Icons.Default.Label, "Custom Tags Manager", "Organize your collection") {
                onClose()
                onCustomTagsClick()
            }
            
            Spacer(Modifier.height(12.dp))
            
            GlassMenuItemWithIcon(Icons.Default.Settings, "Settings", "App configuration") {
                onClose()
                onSettingsClick()
            }
            
            Spacer(Modifier.height(12.dp))
            
            GlassMenuItemWithIcon(Icons.Default.Info, "About", "App information") {
                onClose()
                onAboutClick()
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
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    if (granted) Color(0xFF34C759).copy(alpha = 0.8f) 
                    else Color(0xFFFF453A).copy(alpha = 0.8f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$name: ${if (granted) "Granted" else "Denied"}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.95f)
        )
    }
}

@Composable
fun GlassMenuItemWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(
        onClick = onClick,
        surfaceAlpha = 0.1f,
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassMainScreen(viewModel: MainViewModel) {
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
    
    val pagerState = rememberPagerState(pageCount = { 4 })
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

    val malFilePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch { viewModel.processMalFile(activity, it) } }
    }
    
    val tagsFilePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch { viewModel.processCustomTagsFile(activity, it) } }
    }

    // Background with subtle gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                LiquidGlassDrawer(
                    onClose = { scope.launch { drawerState.close() } },
                    onCustomTagsClick = { showTagManager = true },
                    onSettingsClick = { showSettings = true },
                    onAboutClick = { showAbout = true },
                    storagePermissionGranted = storagePermissionGranted,
                    notificationPermissionGranted = notificationPermissionGranted
                )
            },
            gesturesEnabled = true,
            modifier = Modifier.pointerInput(Unit) {
                detectDragGestures(onDragStart = { offset ->
                    if (offset.x > size.width * 0.9f) {
                        scope.launch { drawerState.open() }
                    }
                }) { _, _ -> }
            }
        ) {
            Column(Modifier.fillMaxSize()) {
                // Liquid Glass Top Bar
                LiquidGlassTopBar(
                    drawerState = drawerState,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onClearLogsClick = { viewModel.clearLogs() },
                    showClearButton = pagerState.currentPage == 3 && logs.isNotEmpty()
                )
                
                // Glass Tab Row with swipe navigation
                LiquidGlassTabRow(
                    tabs = tabs,
                    selectedTab = pagerState.currentPage,
                    onTabSelected = { scope.launch { pagerState.animateScrollToPage(it) } },
                    onSwipeNavigation = { direction ->
                        scope.launch {
                            val newPage = when {
                                direction < 0 && pagerState.currentPage < 3 -> pagerState.currentPage + 1
                                direction > 0 && pagerState.currentPage > 0 -> pagerState.currentPage - 1
                                else -> pagerState.currentPage
                            }
                            pagerState.animateScrollToPage(newPage)
                        }
                    }
                )
                
                // Content with HorizontalPager
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(16.dp)) { page ->
                    when (page) {
                        0 -> LiquidGlassImportTab(
                            viewModel = viewModel,
                            isProcessing = isProcessing,
                            onMalImportClick = { malFilePickerLauncher.launch(arrayOf("text/xml", "application/xml")) },
                            onTagsImportClick = { tagsFilePickerLauncher.launch(arrayOf("text/xml", "application/xml")) },
                            customTagsCount = customTags.size,
                            storagePermissionGranted = storagePermissionGranted,
                            onMenuSwipe = { scope.launch { drawerState.open() } }
                        )
                        1 -> LiquidGlassEntriesList(
                            viewModel = viewModel,
                            entries = animeEntries
                        ) { entry ->
                            scope.launch { viewModel.downloadImages(entry) }
                        }
                        2 -> LiquidGlassDownloadsTab(
                            viewModel = viewModel,
                            downloads = downloads
                        )
                        3 -> LiquidGlassLogsPanel(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Glass dialogs
    if (showTagManager) {
        LiquidGlassTagManagerDialog(viewModel = viewModel) { showTagManager = false }
    }
    if (showSettings) {
        LiquidGlassSettingsDialog(viewModel = viewModel) { showSettings = false }
    }
    if (showAbout) {
        LiquidGlassAboutDialog(viewModel = viewModel) { showAbout = false }
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