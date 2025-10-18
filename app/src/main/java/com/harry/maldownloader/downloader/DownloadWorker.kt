package com.harry.maldownloader.downloader

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.harry.maldownloader.R
import com.harry.maldownloader.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val downloadId: String = inputData.getString("downloadId") ?: ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val repo = DownloadRepository(context, DownloadRepository.getDatabase(context))

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (downloadId.isEmpty()) return@withContext Result.failure()

        try {
            val download = repo.getDownloadById(downloadId) ?: return@withContext Result.failure()

            if (download.status == "paused") {
                updateNotification("Paused", 0)
                return@withContext Result.success()
            }

            val request = Request.Builder()
                .url(download.url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                repo.markAsFailed(downloadId, "HTTP ${response.code}")
                updateNotification("Failed", -1)
                return@withContext Result.failure()
            }

            val totalBytes = response.body?.contentLength() ?: -1L
            val file = File(context.filesDir, download.fileName)

            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else -1

                        updateNotification("Downloading", progress)
                    }
                }
            }

            repo.markAsCompleted(downloadId, file.absolutePath)
            updateNotification("Completed", 100, file)

            Result.success()

        } catch (e: IOException) {
            repo.logError(downloadId, "Network error", e.stackTraceToString())
            updateNotification("Retrying...", -1)
            Result.retry()
        } catch (e: Exception) {
            repo.logError(downloadId, "Unexpected error", e.stackTraceToString())
            updateNotification("Failed", -1)
            Result.failure()
        }
    }

    private fun updateNotification(status: String, progress: Int, file: File? = null) {
        val channelId = context.getString(R.string.notification_channel_id)
        val notificationManager = NotificationManagerCompat.from(context)

        val openIntent = file?.let {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(it), "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
        val pendingOpenIntent = openIntent?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val cancelIntent = Intent(context, DownloadCancelReceiver::class.java).apply {
            putExtra("downloadId", downloadId)
        }
        val pendingCancelIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Download: ${downloadId}")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_download)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "Cancel", pendingCancelIntent)
            .setOngoing(status == "Downloading" || status == "Retrying...")

        if (status == "Downloading" && progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, false)
        }

        pendingOpenIntent?.let {
            builder.addAction(android.R.drawable.ic_menu_view, "Open", it)
        }

        val notification = builder.build()
        notificationManager.notify(downloadId.hashCode(), notification)
    }
}
