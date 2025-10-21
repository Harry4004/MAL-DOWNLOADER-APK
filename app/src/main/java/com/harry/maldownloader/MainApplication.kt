package com.harry.maldownloader

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room

class MainApplication : Application() {
    lateinit var database: DownloadDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            DownloadDatabase::class.java, "download_db"
        ).build()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress notifications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
