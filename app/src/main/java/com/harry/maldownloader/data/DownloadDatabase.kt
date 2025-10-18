package com.harry.maldownloader.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "download_queue")
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val fileName: String,
    val malId: String?,
    val title: String?,
    val imageType: String?, // anime, manga, character
    val etag: String?,
    val lastModified: String?,
    val partialPath: String?, // path to partial download file
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val status: String = "pending", // pending, downloading, paused, completed, failed
    val retryCount: Int = 0,
    val lastAttempt: Long = 0,
    val errorMessage: String?,
    val priority: Int = 0,
    val networkType: String? = null, // wifi, cellular, any
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "download_logs")
data class DownloadLog(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val downloadId: String,
    val level: String, // INFO, WARN, ERROR
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val exception: String? = null
)

@Entity(tableName = "duplicate_hashes")
data class DuplicateHash(
    @PrimaryKey val hash: String,
    val filePath: String,
    val downloadId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_queue ORDER BY priority DESC, createdAt ASC")
    suspend fun getAllDownloads(): List<DownloadItem>
    
    @Query("SELECT * FROM download_queue WHERE status = :status")
    suspend fun getDownloadsByStatus(status: String): List<DownloadItem>
    
    @Query("SELECT * FROM download_queue WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem)
    
    @Update
    suspend fun updateDownload(item: DownloadItem)
    
    @Delete
    suspend fun deleteDownload(item: DownloadItem)
    
    @Query("DELETE FROM download_queue WHERE status = 'completed' AND completedAt < :olderThan")
    suspend fun cleanupOldCompleted(olderThan: Long)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM download_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 1000): List<DownloadLog>
    
    @Query("SELECT * FROM download_logs WHERE downloadId = :downloadId ORDER BY timestamp DESC")
    suspend fun getLogsForDownload(downloadId: String): List<DownloadLog>
    
    @Insert
    suspend fun insertLog(log: DownloadLog)
    
    @Query("DELETE FROM download_logs WHERE timestamp < :olderThan")
    suspend fun cleanupOldLogs(olderThan: Long)
}

@Dao
interface DuplicateDao {
    @Query("SELECT * FROM duplicate_hashes WHERE hash = :hash")
    suspend fun findDuplicate(hash: String): DuplicateHash?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHash(hash: DuplicateHash)
    
    @Query("DELETE FROM duplicate_hashes WHERE filePath = :filePath")
    suspend fun deleteHashForFile(filePath: String)
}

@Database(
    entities = [DownloadItem::class, DownloadLog::class, DuplicateHash::class],
    version = 1,
    exportSchema = false
)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun logDao(): LogDao
    abstract fun duplicateDao(): DuplicateDao
}