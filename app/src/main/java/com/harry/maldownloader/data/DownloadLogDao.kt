package com.harry.maldownloader.data

import androidx.room.*

@Dao
interface DownloadLogDao {
    @Query("SELECT * FROM download_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<DownloadLog>

    @Query("SELECT * FROM download_logs WHERE downloadId = :downloadId ORDER BY timestamp ASC")
    suspend fun getLogsForDownload(downloadId: String): List<DownloadLog>

    @Insert
    suspend fun insertLog(log: DownloadLog)

    @Query("DELETE FROM download_logs WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldLogs(cutoffTime: Long)

    @Query("DELETE FROM download_logs")
    suspend fun clearAllLogs()
}