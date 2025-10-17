package com.harry.maldownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_items")
data class DownloadItem(
    @PrimaryKey val id: String,
    val title: String,
    val imageUrl: String,
    val status: DownloadStatus,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val localPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class AppStats(
    val totalEntries: Int = 0,
    val downloadedImages: Int = 0,
    val failedDownloads: Int = 0,
    val queueSize: Int = 0,
    val completedDownloads: Int = 0,
    val activeDownloads: Int = 0
)

data class UiState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.TITLE,
    val filterStatus: FilterStatus = FilterStatus.ALL
)

enum class SortOrder {
    TITLE,
    SCORE,
    DATE_ADDED,
    EPISODES
}

enum class FilterStatus {
    ALL,
    COMPLETED,
    WATCHING,
    PLAN_TO_WATCH,
    DROPPED,
    ON_HOLD
}