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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
            MaldownloaderTheme {
                when {
                    criticalError.value != null -> ErrorScreen(error = criticalError.value!!)
                    !isInitialized.value -> LoadingScreen()
                    else -> SafeMainScreen(viewModel = viewModel)
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
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
            Spacer(Modifier.height(16.dp))
            Text("🚀 Initializing MAL Downloader...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text("Enhanced Edition v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorScreen(error: Throwable) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Error, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("❌ Critical Error", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(8.dp))
                Text(error.message ?: "Unknown error occurred", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(16.dp))
                Text("Please restart the app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

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
        modifier = Modifier.fillMaxHeight().width(320.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Box {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f)))))
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📱 Menu", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp).semantics { contentDescription = "Close menu"; role = Role.Button }) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Permission Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        PermissionStatusRow("Storage", storagePermissionGranted)
                        PermissionStatusRow("Notifications", notificationPermissionGranted)
                    }
                }
                Spacer(Modifier.height(24.dp))
                MenuItemWithIcon(Icons.Default.Tag, "Custom Tags Manager", "Organize your collection") {
                    onClose()
                    onCustomTagsClick()
                }
                MenuItemWithIcon(Icons.Default.Settings, "Settings", "App configuration") {
                    onClose()
                    onSettingsClick()
                }
                MenuItemWithIcon(Icons.Default.Info, "About", "App information") {
                    onClose()
                    onAboutClick()
                }
            }
        }
    }
}

@Composable
fun PermissionStatusRow(name: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            if (granted) Icons.Default.CheckCircle else Icons.Default.Error,
            null,
            tint = if (granted) Color.Green else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$name: ${if (granted) "Granted" else "Denied"}",
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
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

    LaunchedEffect(pagerState.currentPage) {
        // Track page changes
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModernDrawer(
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
        Scaffold(
            topBar = {
                // Glassmorphism top bar with conditional hamburger visibility
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.12f))))) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hide hamburger when drawer is open
                            val hamburgerAlpha: Float by animateFloatAsState(
                                targetValue = if (drawerState.isOpen) 0f else 1f,
                                animationSpec = tween(300),
                                label = "Hamburger visibility"
                            )
                            
                            IconButton(
                                onClick = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } },
                                modifier = Modifier.size(48.dp).alpha(hamburgerAlpha).semantics {
                                    contentDescription = "Open navigation menu"
                                    role = Role.Button
                                }
                            ) {
                                Icon(Icons.Default.Menu, null, modifier = Modifier.size(24.dp))
                            }
                            
                            Text("MAL Downloader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            
                            if (pagerState.currentPage == 3 && logs.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(24.dp))
                                }
                            } else {
                                Spacer(Modifier.size(48.dp))
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues)) {
                // Glassmorphism tab indicators with swipe support
                Surface(
                    modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            scope.launch {
                                val newPage = when {
                                    dragAmount < 0 && pagerState.currentPage < 3 -> pagerState.currentPage + 1
                                    dragAmount > 0 && pagerState.currentPage > 0 -> pagerState.currentPage - 1
                                    else -> pagerState.currentPage
                                }
                                pagerState.animateScrollToPage(newPage)
                            }
                        }
                    },
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Box(Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)))) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            tabs.forEachIndexed { index, title ->
                                GlassyTabIndicator(
                                    title = title,
                                    selected = pagerState.currentPage == index,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                                )
                            }
                        }
                    }
                }
                
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(12.dp)) { page ->
                    when (page) {
                        0 -> EnhancedImportTab(
                            viewModel = viewModel,
                            isProcessing = isProcessing,
                            onMalImportClick = { malFilePickerLauncher.launch(arrayOf("text/xml", "application/xml")) },
                            onTagsImportClick = { tagsFilePickerLauncher.launch(arrayOf("text/xml", "application/xml")) },
                            customTagsCount = customTags.size,
                            storagePermissionGranted = storagePermissionGranted,
                            onMenuSwipe = { scope.launch { drawerState.open() } }
                        )
                        1 -> EnhancedEntriesList(viewModel = viewModel, entries = animeEntries) { entry ->
                            scope.launch { viewModel.downloadImages(entry) }
                        }
                        2 -> EnhancedDownloadsTab(viewModel = viewModel, downloads = downloads)
                        3 -> EnhancedLogsPanel(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    if (showTagManager) {
        TagManagerDialog(viewModel = viewModel) { showTagManager = false }
    }
    if (showSettings) {
        SettingsDialog(viewModel = viewModel) { showSettings = false }
    }
    if (showAbout) {
        AboutDialog(viewModel = viewModel) { showAbout = false }
    }
}

@Composable
fun GlassyTabIndicator(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(4.dp),
        color = if (selected) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (selected) 
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f)))
                    else 
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent))
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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