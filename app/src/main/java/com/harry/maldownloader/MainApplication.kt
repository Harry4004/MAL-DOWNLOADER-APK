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
    
    // Use singleton pattern to prevent multiple database instances
    val database by lazy {
        try {
            DownloadDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e("MainApplication", "Database initialization failed", e)
            // Return null to handle gracefully in MainActivity
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        Log.d("MainApplication", "Starting application initialization")

        try {
            // Set up global crash handler with null safety
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                Log.e("AppCrash", "Fatal crash in thread ${t.name}", e)
                
                // Only attempt database logging if database is available
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
                            Log.d("AppCrash", "Crash logged to database successfully")
                        } catch (dbError: Exception) {
                            Log.e("AppCrash", "Failed to log crash to database", dbError)
                        }
                    }
                }
                
                // Give the coroutine a moment to complete
                Thread.sleep(500)
            }

            Log.d("MainApplication", "Setting up notification channels")
            createNotificationChannels()
            
            Log.d("MainApplication", "Application initialization completed successfully")
        } catch (e: Exception) {
            Log.e("MainApplication", "Critical error during application initialization", e)
            // Don't crash the app, let MainActivity handle graceful degradation
        }
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
                
                Log.d("MainApplication", "Notification channels created successfully")
            } catch (e: Exception) {
                Log.e("MainApplication", "Error creating notification channels", e)
            }
        }
    }
}