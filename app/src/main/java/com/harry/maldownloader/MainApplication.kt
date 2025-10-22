package com.harry.maldownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.harry.maldownloader.data.DownloadDatabase
import com.harry.maldownloader.data.DownloadLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application() {
    val database by lazy {
        DownloadDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Global crash logger to capture uncaught exceptions into Logs tab
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    database.logDao().insertLog(
                        DownloadLog(
                            downloadId = "app",
                            level = "FATAL",
                            message = "Uncaught crash in ${t.name}: ${e.javaClass.simpleName} - ${e.message}",
                            exception = e.stackTraceToString()
                        )
                    )
                } catch (_: Exception) { }
            }
            android.util.Log.e("AppCrash", "Fatal crash", e)
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channels = listOf(
                    NotificationChannel(
                        "download_channel",
                        "Download Progress",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Shows download progress notifications"
                        setShowBadge(false)
                    },
                    NotificationChannel(
                        "download_complete",
                        "Download Complete",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifies when downloads are completed"
                    },
                    NotificationChannel(
                        "download_error",
                        "Download Errors",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notifies about download failures"
                    }
                )
                val manager = getSystemService(NotificationManager::class.java)
                channels.forEach { channel ->
                    manager.createNotificationChannel(channel)
                }
            } catch (_: Exception) { }
        }
    }
}
