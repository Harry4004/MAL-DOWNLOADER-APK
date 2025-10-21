package com.harry.maldownloader.data

import androidx.room.*

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_items ORDER BY createdAt DESC")
    suspend fun getAllDownloads(): List<DownloadItem>

    @Query("SELECT * FROM download_items WHERE status = :status")
    suspend fun getDownloadsByStatus(status: String): List<DownloadItem>

    @Query("SELECT * FROM download_items WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)

    @Query("DELETE FROM download_items WHERE status = 'completed' AND completedAt < :cutoffTime")
    suspend fun cleanupOldCompleted(cutoffTime: Long)
    
    @Query("DELETE FROM download_items")
    suspend fun clearAllDownloads()
}