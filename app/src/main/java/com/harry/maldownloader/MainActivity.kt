package com.harry.maldownloader

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.harry.maldownloader.data.DownloadRepository
import com.harry.maldownloader.ui.theme.MaldownloaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    private lateinit var repository: DownloadRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repository and ViewModel
        repository = DownloadRepository(
            context = this,
            database = DownloadRepository.getDatabase(this)
        )
        
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(repository)
        )[MainViewModel::class.java]
        
        setContent {
            MaldownloaderTheme {
                MainScreen(viewModel = viewModel)
            }
        }
        
        // Check and request permissions
        checkPermissions()
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // Storage permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        // Media permissions (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            
            // Notification permission
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
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1001) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                when (permission) {
                    Manifest.permission.POST_NOTIFICATIONS -> 
                        viewModel.setNotificationPermission(granted)
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES -> 
                        viewModel.setStoragePermission(granted)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val animeEntries by viewModel.animeEntries.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.viewModelScope.launch {
                viewModel.processMalFile(context, it)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "MAL Downloader v3.0 - 25+ Tags Edition",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Import XML Button
        Button(
            onClick = {
                filePickerLauncher.launch(arrayOf("text/xml", "application/xml"))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing with API enrichment...")
            } else {
                Text("Import MAL XML File & Extract 25+ Tags")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("🎯 MAL Client ID: aaf018d4c098158...")
                Text("📄 Entries: ${animeEntries.size}")
                Text("⬇️ Downloads: ${downloads.size}")
                Text("🔄 Active: ${downloads.count { it.status == "downloading" }}")
                Text("✅ Completed: ${downloads.count { it.status == "completed" }}")
                Text("❌ Failed: ${downloads.count { it.status == "failed" }}")
                
                // Tag extraction stats
                val totalTags = animeEntries.sumOf { it.allTags.size }
                if (animeEntries.isNotEmpty()) {
                    Text("🏷️ Total Tags Extracted: $totalTags")
                    Text("📊 Average Tags per Entry: ${totalTags / animeEntries.size}")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logs Section
        Text(
            text = "📝 Processing Logs",
            style = MaterialTheme.typography.titleMedium
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs.take(100)) { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                )
            }
            
            if (logs.isEmpty()) {
                item {
                    Text(
                        text = "Ready to process MAL XML file with comprehensive tag extraction!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// ViewModel Factory
class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}