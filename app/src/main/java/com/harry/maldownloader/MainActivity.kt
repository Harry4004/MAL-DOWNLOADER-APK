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
import androidx.compose.foundation.shape.RoundedCornerShape
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModernGlassmorphismDrawer(
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
                GlassmorphismTopBar(
                    title = "MAL Downloader",
                    onMenuClick = { scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() } },
                    onActionClick = if (pagerState.currentPage == 3 && logs.isNotEmpty()) { { viewModel.clearLogs() } } else null,
                    actionIcon = if (pagerState.currentPage == 3 && logs.isNotEmpty()) Icons.Default.Clear else null
                )
            }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues)) {
                // Animated tabs
                AnimatedTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    tabs = tabs,
                    onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )

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

class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
