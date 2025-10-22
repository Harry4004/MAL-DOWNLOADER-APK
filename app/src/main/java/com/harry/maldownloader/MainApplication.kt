package com.harry.maldownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.harry.maldownloader.data.DownloadDatabase
import com.harry.maldownloader.data.DownloadLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application() {
    val database by lazy {
        try { DownloadDatabase.getDatabase(this) } catch (e: Exception) {
            Log.e("MainApplication", "Database initialization failed", e)
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MainApplication", "Starting application initialization")
        try {
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                Log.e("AppCrash", "Fatal crash in thread ${t.name}", e)
                database?.let { db ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            db.logDao().insertLog(
                                DownloadLog(
                                    downloadId = "app",
                                    level = "FATAL",
                                    message = "Uncaught crash in ${t.name}: ${e.javaClass.simpleName} - ${e.message}",
                                    exception = e.stackTraceToString()
                                )
                            )
                        } catch (dbError: Exception) {
                            Log.e("AppCrash", "Failed to log crash to database", dbError)
                        }
                    }
                }
                Thread.sleep(300)
            }
            createNotificationChannels()
        } catch (e: Exception) {
            Log.e("MainApplication", "Critical error during application initialization", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel("download_channel","Download Progress", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel("download_complete","Download Complete", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("download_error","Download Errors", NotificationManager.IMPORTANCE_HIGH)
            )
            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    companion object {
        val MAL_CLIENT_ID: String get() = BuildConfig.MAL_CLIENT_ID
    }
}
