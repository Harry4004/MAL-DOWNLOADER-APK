package com.harry.maldownloader.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.harry.maldownloader.BuildConfig
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.data.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced download queue manager for MAL Downloader v3.1
 */
class DownloadQueueManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadQueue = ArrayDeque<AnimeEntry>()
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private var semaphore = Semaphore(2)
    private var settings = AppSettings()
    private var onDownloadComplete: (suspend (AnimeEntry, String?, String?) -> Unit)? = null
    private var onLogMessage: ((String) -> Unit)? = null
    
    private val _queueState = MutableStateFlow(QueueState.IDLE)
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()
    
    private val _activeDownloads = MutableStateFlow<Map<Int, DownloadStatus>>(emptyMap())
    val activeDownloads: StateFlow<Map<Int, DownloadStatus>> = _activeDownloads.asStateFlow()
    
    data class DownloadStatus(
        val entry: AnimeEntry,
        val progress: Float = 0f,
        val status: String = "queued",
        val error: String? = null
    )
    
    enum class QueueState { IDLE, RUNNING, PAUSED, COMPLETED }
    
    fun configure(
        settings: AppSettings,
        onComplete: suspend (AnimeEntry, String?, String?) -> Unit,
        onLog: (String) -> Unit
    ) {
        this.settings = settings
        this.onDownloadComplete = onComplete
        this.onLogMessage = onLog
        semaphore = Semaphore(settings.maxConcurrentDownloads)
    }
    
    fun addToQueue(entries: List<AnimeEntry>) {
        entries.forEach { downloadQueue.offer(it) }
        log("Added ${entries.size} items to queue")
    }
    
    fun startQueue() {
        if (_queueState.value == QueueState.RUNNING) return
        _queueState.value = QueueState.RUNNING
        scope.launch { processQueue() }
    }
    
    fun pauseQueue() {
        _queueState.value = QueueState.PAUSED
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }
    
    private suspend fun processQueue() {
        while (_queueState.value == QueueState.RUNNING && downloadQueue.isNotEmpty()) {
            val entry = downloadQueue.poll() ?: break
            
            val job = scope.launch {
                try {
                    semaphore.acquire()
                    downloadEntry(entry)
                } finally {
                    semaphore.release()
                    activeJobs.remove(entry.malId)
                }
            }
            
            activeJobs[entry.malId] = job
            delay(settings.apiDelayMs)
        }
        
        if (downloadQueue.isEmpty()) {
            _queueState.value = QueueState.COMPLETED
        }
    }
    
    private suspend fun downloadEntry(entry: AnimeEntry) {
        try {
            updateStatus(entry.malId, DownloadStatus(entry, status = "downloading"))
            delay(2000) // Simulate download
            updateStatus(entry.malId, DownloadStatus(entry, progress = 1f, status = "completed"))
            onDownloadComplete?.invoke(entry, entry.imagePath, null)
        } catch (e: Exception) {
            updateStatus(entry.malId, DownloadStatus(entry, status = "failed", error = e.message))
        }
    }
    
    private fun updateStatus(malId: Int, status: DownloadStatus?) {
        val current = _activeDownloads.value.toMutableMap()
        if (status != null) current[malId] = status else current.remove(malId)
        _activeDownloads.value = current
    }
    
    private fun log(message: String) {
        if (BuildConfig.ENABLE_LOGGING) Log.d("DownloadQueue", message)
        onLogMessage?.invoke(message)
    }
    
    fun cleanup() {
        scope.cancel()
        activeJobs.clear()
        downloadQueue.clear()
    }
}