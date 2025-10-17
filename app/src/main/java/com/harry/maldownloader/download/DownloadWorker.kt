package com.harry.maldownloader.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.harry.maldownloader.R
import com.harry.maldownloader.data.DownloadStatus

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "mal_downloads"
        private const val NOTIFICATION_ID = 1001
    }
    
    private val downloadManager = DownloadManager(applicationContext)
    private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: return Result.failure()
        val imageUrl = inputData.getString("image_url") ?: return Result.failure()
        
        createNotificationChannel()
        
        return try {
            setForeground(createForegroundInfo(title))
            
            // Update status to downloading
            downloadManager.updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING)
            
            // Perform the download
            val localPath = downloadManager.downloadImageToGallery(imageUrl, title)
            
            // Update status to completed
            downloadManager.updateDownloadStatus(downloadId, DownloadStatus.COMPLETED, localPath = localPath)
            
            Result.success(workDataOf("local_path" to localPath))
            
        } catch (e: Exception) {
            downloadManager.updateDownloadStatus(downloadId, DownloadStatus.FAILED, errorMessage = e.message)
            Result.failure(workDataOf("error" to e.message))
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MAL Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Downloads from MyAnimeList"
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createForegroundInfo(title: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading MAL Image")
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
            
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}