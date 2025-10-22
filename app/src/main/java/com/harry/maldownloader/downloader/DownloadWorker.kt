package com.harry.maldownloader.downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.harry.maldownloader.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val downloadId: String = inputData.getString("downloadId") ?: ""
    
    // Get repository instance
    private val repo: DownloadRepository by lazy {
        val database = DownloadRepository.getDatabase(context)
        DownloadRepository(context, database)
    }

    // Create a persistent OkHttp client
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (downloadId.isEmpty()) {
            return@withContext Result.failure()
        }

        val download = repo.getDownloadById(downloadId)
            ?: return@withContext Result.failure()

        // Check if download is already completed or cancelled
        if (download.status == "completed" || download.status == "cancelled") {
            return@withContext Result.success()
        }

        try {
            repo.logInfo(downloadId, "⬇️ Download worker started for ${download.fileName}")

            // Create the network request
            val request = Request.Builder()
                .url(download.url)
                .build()

            // Execute the request
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val error = "Network error: ${response.code} - ${response.message}"
                repo.markAsFailed(downloadId, error)
                return@withContext Result.retry()
            }

            val body = response.body
            if (body == null) {
                val error = "Empty response body"
                repo.markAsFailed(downloadId, error)
                return@withContext Result.failure()
            }

            // Create a temporary file to save the image
            val tempFile = File(context.cacheDir, download.fileName)
            
            // Save the image data to the file
            body.byteStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            val savedPath = tempFile.absolutePath
            repo.logInfo(downloadId, "✅ Image saved to temporary path: $savedPath")

            // Mark as completed with the *actual* file path
            repo.markAsCompleted(downloadId, savedPath)
            
            Result.success()

        } catch (e: Exception) {
            repo.logError(downloadId, "Download error: ${e.message}", e.stackTraceToString())
            // Mark as failed so it can be retried
            repo.markAsFailed(downloadId, e.message ?: "Unknown error")
            Result.retry()
        }
    }
}
