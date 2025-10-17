package com.harry.maldownloader.download

import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import androidx.work.*
import com.harry.maldownloader.data.DownloadItem
import com.harry.maldownloader.data.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val _downloadQueue = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadQueue: StateFlow<List<DownloadItem>> = _downloadQueue.asStateFlow()
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    
    fun queueDownload(title: String, imageUrl: String): String {
        val downloadId = UUID.randomUUID().toString()
        val downloadItem = DownloadItem(
            id = downloadId,
            title = title,
            imageUrl = imageUrl,
            status = DownloadStatus.QUEUED
        )
        
        updateQueue { it + downloadItem }
        addLog("Queued download: $title")
        
        // Start WorkManager task
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    "download_id" to downloadId,
                    "title" to title,
                    "image_url" to imageUrl
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
        workManager.enqueue(workRequest)
        return downloadId
    }
    
    fun cancelDownload(downloadId: String) {
        workManager.cancelAllWorkByTag(downloadId)
        updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
        addLog("Cancelled download: $downloadId")
    }
    
    fun retryDownload(downloadId: String) {
        val item = _downloadQueue.value.find { it.id == downloadId }
        if (item != null) {
            updateDownloadStatus(downloadId, DownloadStatus.QUEUED)
            queueDownload(item.title, item.imageUrl)
            addLog("Retrying download: ${item.title}")
        }
    }
    
    fun updateDownloadProgress(downloadId: String, progress: Float) {
        updateQueue { queue ->
            queue.map { item ->
                if (item.id == downloadId) {
                    item.copy(
                        progress = progress,
                        status = DownloadStatus.DOWNLOADING,
                        updatedAt = System.currentTimeMillis()
                    )
                } else item
            }
        }
    }
    
    fun updateDownloadStatus(downloadId: String, status: DownloadStatus, errorMessage: String? = null, localPath: String? = null) {
        updateQueue { queue ->
            queue.map { item ->
                if (item.id == downloadId) {
                    item.copy(
                        status = status,
                        errorMessage = errorMessage,
                        localPath = localPath,
                        updatedAt = System.currentTimeMillis()
                    )
                } else item
            }
        }
    }
    
    private fun updateQueue(update: (List<DownloadItem>) -> List<DownloadItem>) {
        _downloadQueue.value = update(_downloadQueue.value)
    }
    
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        _logs.value = _logs.value + "[$timestamp] $message"
    }
    
    suspend fun downloadImageToGallery(imageUrl: String, title: String): String? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(imageUrl)
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to download: ${response.code}")
            }
            
            val inputStream = response.body?.byteStream()
                ?: throw Exception("Empty response body")
            
            // Save to MediaStore (Pictures/MAL_Export)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "${sanitizeFileName(title)}.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MAL_Export")
            }
            
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")
            
            resolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw Exception("Failed to open output stream")
            
            addLog("Saved to gallery: $title")
            uri.toString()
            
        } catch (e: Exception) {
            addLog("Download failed: ${e.message}")
            throw e
        }
    }
    
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\s-_]"), "")
            .trim()
            .take(50)
    }
}