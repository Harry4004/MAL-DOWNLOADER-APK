package com.harry.maldownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_logs")
data class DownloadLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadId: String,
    val level: String,
    val message: String,
    val exception: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)