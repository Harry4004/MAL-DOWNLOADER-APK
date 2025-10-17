package com.harry.maldownloader

import android.app.Application
import androidx.room.Room

class MainApplication : Application() {
    lateinit var database: DownloadDatabase

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            DownloadDatabase::class.java, "download_db"
        ).build()
    }
}
