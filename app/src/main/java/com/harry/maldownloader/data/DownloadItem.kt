package com.harry.maldownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_items")
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val malId: String? = null,
    val title: String? = null,
    val imageType: String? = null,
    val status: String = "pending",
    val progress: Int = 0,
    val etag: String? = null,
    val lastModified: String? = null,
    val partialPath: String? = null,
    val priority: Int = 0,
    val networkType: String? = null,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)