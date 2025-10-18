package com.harry.maldownloader

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.data.DownloadItem
import com.harry.maldownloader.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: DownloadRepository) : AndroidViewModel(repository.context as Application) {

    // Permission state
    private val _notificationPermissionGranted = MutableLiveData(false)
    val notificationPermissionGranted: LiveData<Boolean> = _notificationPermissionGranted

    private val _storagePermissionGranted = MutableLiveData(false)
    val storagePermissionGranted: LiveData<Boolean> = _storagePermissionGranted

    // App state flows
    private val _animeEntries = MutableStateFlow<List<AnimeEntry>>(emptyList())
    val animeEntries: StateFlow<List<AnimeEntry>> = _animeEntries.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        checkInitialPermissions()
        loadEntries()
        loadDownloads()
        loadLogs()
    }

    // Permission functions
    fun setNotificationPermission(granted: Boolean) {
        _notificationPermissionGranted.postValue(granted)
    }

    fun setStoragePermission(granted: Boolean) {
        _storagePermissionGranted.postValue(granted)
    }

    fun checkInitialPermissions() {
        val context = getApplication<Application>()
        val notificationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        _notificationPermissionGranted.postValue(notificationGranted)

        val storageGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        _storagePermissionGranted.postValue(storageGranted)
    }

    // Core app functions
    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("Starting MAL file processing...")
            // TODO: Implement MAL XML parsing
            log("MAL file processing completed")
        } catch (e: Exception) {
            log("Error processing MAL file: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                log("Starting image download for ${entry.title}")
                // TODO: Implement image downloading
                log("Image download completed for ${entry.title}")
            } catch (e: Exception) {
                log("Error downloading images: ${e.message}")
            }
        }
    }

    suspend fun updateEntryTags(entry: AnimeEntry, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                log("Updating tags for ${entry.title}")
                // TODO: Implement tag updating
                log("Tags updated for ${entry.title}")
            } catch (e: Exception) {
                log("Error updating tags: ${e.message}")
            }
        }
    }

    // Download management
    suspend fun pauseDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.pauseDownload(id)
            loadDownloads()
        }
    }

    suspend fun resumeDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resumeDownload(id)
            loadDownloads()
        }
    }

    suspend fun cancelDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cancelDownload(id)
            loadDownloads()
        }
    }

    // NEWLY ADDED retryDownload method
    suspend fun retryDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.retryDownload(id)
            loadDownloads()
        }
    }

    // Data loading
    private fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: Load from database
                val entries = emptyList<AnimeEntry>()
                _animeEntries.value = entries
            } catch (e: Exception) {
                log("Error loading entries: ${e.message}")
            }
        }
    }

    private fun loadDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloads = repository.getAllDownloads()
                _downloads.value = downloads
            } catch (e: Exception) {
                log("Error loading downloads: ${e.message}")
            }
        }
    }

    private fun loadLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logs = repository.getRecentLogs()
                _logs.value = logs.map { "${it.timestamp}: [${it.level}] ${it.message}" }
            } catch (e: Exception) {
                log("Error loading logs: ${e.message}")
            }
        }
    }

    // Logging
    fun log(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentLogs = _logs.value.toMutableList()
            currentLogs.add(0, "${System.currentTimeMillis()}: $message")
            if (currentLogs.size > 1000) {
                currentLogs.removeAt(currentLogs.size - 1)
            }
            _logs.value = currentLogs
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
