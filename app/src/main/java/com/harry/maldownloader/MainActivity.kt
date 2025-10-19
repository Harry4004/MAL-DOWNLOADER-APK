package com.harry.maldownloader

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.harry.maldownloader.data.DownloadDatabase
import com.harry.maldownloader.data.DownloadRepository
import com.harry.maldownloader.ui.components.*
import com.harry.maldownloader.ui.theme.MALDownloaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationPermission(granted)
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setStoragePermission(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = Room.databaseBuilder(
            applicationContext,
            DownloadDatabase::class.java,
            "mal_downloader_db"
        ).build()

        val repository = DownloadRepository(applicationContext, database)
        viewModel = MainViewModel(repository)

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        viewModel.setNotificationPermission(notificationGranted)

        val storageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        viewModel.setStoragePermission(storageGranted)

        setContent {
            MALDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHandler(viewModel)
                    MainScreen(repository = repository, viewModel = viewModel)
                }
            }
        }
    }

    @Composable
    fun PermissionHandler(viewModel: MainViewModel) {
        // Use observeAsState for LiveData properties
        val notificationGranted by viewModel.notificationPermissionGranted.observeAsState(false)
        val storageGranted by viewModel.storagePermissionGranted.observeAsState(false)

        PermissionRequester(
            notificationPermissionGranted = notificationGranted,
            storagePermissionGranted = storageGranted,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onRequestStoragePermission = {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: DownloadRepository,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showLogs by remember { mutableStateOf(false) }

    // Use collectAsState for StateFlow properties
    val entries by viewModel.animeEntries.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        uri?.let {
            val fileName = getFileNameFromUri(context, it)
            viewModel.log("Selected file: $fileName")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MAL Downloader", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showLogs = !showLogs }) {
                        Icon(
                            imageVector = if (showLogs) Icons.Filled.Close else Icons.Filled.Info,
                            contentDescription = "Toggle Logs"
                        )
                    }
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedFileUri?.let { uri ->
                        scope.launch { viewModel.processMalFile(context, uri) }
                    } ?: run { filePicker.launch("application/xml") }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (selectedFileUri != null) "Process File" else "Select File"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Library (${entries.size})") },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Downloads (${downloads.size})") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Statistics") },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> LibraryContent(
                    entries = entries,
                    onDownloadImages = { entry -> scope.launch { viewModel.downloadImages(entry) } },
                    onUpdateTags = { entry, tags -> scope.launch { viewModel.updateEntryTags(entry, tags) } },
                    modifier = Modifier.weight(1f)
                )
                1 -> DownloadsContent(
                    downloads = downloads,
                    onPauseDownload = { id -> scope.launch { viewModel.pauseDownload(id) } },
                    onResumeDownload = { id -> scope.launch { viewModel.resumeDownload(id) } },
                    onCancelDownload = { id -> scope.launch { viewModel.cancelDownload(id) } },
                    onRetryDownload = { id -> scope.launch { viewModel.retryDownload(id) } },
                    modifier = Modifier.weight(1f)
                )
                2 -> StatisticsContent(
                    entries = entries,
                    downloads = downloads,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = showLogs,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LogsPanel(
                    logs = logs,
                    onClearLogs = { viewModel.clearLogs() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "unknown.xml"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex != -1) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}