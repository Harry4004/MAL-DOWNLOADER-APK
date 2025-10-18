package com.harry.maldownloader.data

import android.content.Context
import androidx.work.*
import com.harry.maldownloader.downloader.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadRepository(val context: Context, private val database: DownloadDatabase) {

    private val downloadDao = database.downloadDao()
    private val logDao = database.logDao()
    private val duplicateDao = database.duplicateDao()
    private val workManager = WorkManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getDatabase(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "download_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun queueDownload(
        url: String,
        fileName: String,
        malId: String? = null,
        title: String? = null,
        imageType: String? = null,
        priority: Int = 0,
        networkType: String = "any"
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val downloadItem = DownloadItem(
            id = downloadId,
            url = url,
            fileName = fileName,
            malId = malId,
            title = title,
            imageType = imageType,
            etag = null,
            lastModified = null,
            partialPath = null,
            priority = priority,
            networkType = networkType,
            errorMessage = null
        )

        downloadDao.insertDownload(downloadItem)
        logInfo(downloadId, "Download queued: $fileName")

        scheduleDownloadWork(downloadId, networkType, priority)

        return downloadId
    }

    private fun scheduleDownloadWork(downloadId: String, networkType: String, priority: Int) {
        val constraints = Constraints.Builder().apply {
            when (networkType) {
                "wifi" -> setRequiredNetworkType(NetworkType.UNMETERED)
                "cellular" -> setRequiredNetworkType(NetworkType.METERED)
                else -> setRequiredNetworkType(NetworkType.CONNECTED)
            }
            setRequiresBatteryNotLow(false)
        }.build()

        val inputData = workDataOf("downloadId" to downloadId)

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("download_$downloadId")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15000, //OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_$downloadId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    suspend fun pauseDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "downloading") {
            downloadDao.updateDownload(download.copy(status = "paused"))
            workManager.cancelUniqueWork("download_$downloadId")
            logInfo(downloadId, "Download paused")
        }
    }

    suspend fun resumeDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "paused") {
            downloadDao.updateDownload(download.copy(status = "pending"))
            scheduleDownloadWork(downloadId, download.networkType ?: "any", download.priority)
            logInfo(downloadId, "Download resumed")
        }
    }

    suspend fun retryDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "failed") {
            downloadDao.updateDownload(
                download.copy(
                    status = "pending",
                    retryCount = download.retryCount + 1,
                    errorMessage = null
                )
            )
            scheduleDownloadWork(downloadId, download.networkType ?: "any", download.priority)
            logInfo(downloadId, "Download retry scheduled (attempt ${download.retryCount + 1})")
        }
    }

    suspend fun cancelDownload(downloadId: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null && download.status == "downloading") {
            downloadDao.updateDownload(download.copy(status = "cancelled"))
            workManager.cancelUniqueWork("download_$downloadId")
            logInfo(downloadId, "Download cancelled")
        }
    }

    suspend fun checkDuplicate(filePath: String): DuplicateHash? {
        val file = File(filePath)
        if (!file.exists()) return null

        val hash = calculateFileHash(file)
        return duplicateDao.findDuplicate(hash)
    }

    suspend fun markAsCompleted(downloadId: String, filePath: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null) {
            val file = File(filePath)
            if (file.exists()) {
                val hash = calculateFileHash(file)
                duplicateDao.insertHash(
                    DuplicateHash(
                        hash = hash,
                        filePath = filePath,
                        downloadId = downloadId
                    )
                )
            }

            downloadDao.updateDownload(
                download.copy(
                    status = "completed",
                    completedAt = System.currentTimeMillis()
                )
            )
            logInfo(downloadId, "Download completed: ${download.fileName}")
        }
    }

    suspend fun markAsFailed(downloadId: String, error: String) {
        val download = downloadDao.getDownloadById(downloadId)
        if (download != null) {
            downloadDao.updateDownload(
                download.copy(
                    status = "failed",
                    errorMessage = error
                )
            )
            logError(downloadId, "Download failed: $error")
        }
    }

    private fun calculateFileHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun getAllDownloads() = downloadDao.getAllDownloads()
    suspend fun getDownloadsByStatus(status: String) = downloadDao.getDownloadsByStatus(status)
    suspend fun getDownloadById(id: String) = downloadDao.getDownloadById(id)

    suspend fun logInfo(downloadId: String, message: String) {
        logDao.insertLog(DownloadLog(downloadId = downloadId, level = "INFO", message = message))
    }

    suspend fun logWarning(downloadId: String, message: String) {
        logDao.insertLog(DownloadLog(downloadId = downloadId, level = "WARN", message = message))
    }

    suspend fun logError(downloadId: String, message: String, exception: String? = null) {
        logDao.insertLog(DownloadLog(downloadId = downloadId, level = "ERROR", message = message, exception = exception))
    }

    suspend fun getRecentLogs(limit: Int = 1000) = logDao.getRecentLogs(limit)

    suspend fun cleanup() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        // Temporarily commented out until we fix the database schema
        // downloadDao.cleanupOldCompleted(oneWeekAgo)
        logDao.cleanupOldLogs(oneWeekAgo)
    }
}
