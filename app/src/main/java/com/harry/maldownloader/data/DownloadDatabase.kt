package com.harry.maldownloader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "anime_entries")
data class AnimeEntry(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val score: Int = 0,
    val status: String? = null,
    val episodesWatched: Int = 0,
    val totalEpisodes: Int? = null,
    val imageUrl: String? = null,
    val imagePath: String? = null,
    val malUrl: String? = null,
    val synopsis: String? = null,
    val genres: String? = null,
    val studio: String? = null,
    val source: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val tags: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

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
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "download_logs")
data class DownloadLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadId: String,
    val level: String,
    val message: String,
    val exception: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "duplicate_hashes")
data class DuplicateHash(
    @PrimaryKey val hash: String,
    val filePath: String,
    val downloadId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AnimeEntryDao {
    @Query("SELECT * FROM anime_entries ORDER BY title ASC")
    fun getAllEntries(): Flow<List<AnimeEntry>>

    @Query("SELECT * FROM anime_entries WHERE id = :id")
    suspend fun getEntryById(id: String): AnimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: AnimeEntry)

    @Update
    suspend fun updateEntry(entry: AnimeEntry)

    @Delete
    suspend fun deleteEntry(entry: AnimeEntry)
}

@Dao
interface DownloadItemDao {
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

    // Commented out due to build error (missing completedAt column):
    // @Query("DELETE FROM download_items WHERE status = 'completed' AND completedAt < :cutoffTime")
    // suspend fun cleanupOldCompleted(cutoffTime: Long)
}

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

@Dao
interface DuplicateHashDao {
    @Query("SELECT * FROM duplicate_hashes WHERE hash = :hash")
    suspend fun findDuplicate(hash: String): DuplicateHash?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHash(hash: DuplicateHash)

    @Delete
    suspend fun deleteHash(hash: DuplicateHash)
}

@Database(
    entities = [AnimeEntry::class, DownloadItem::class, DownloadLog::class, DuplicateHash::class],
    version = 2,
    exportSchema = false
)
@TypeConverters
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun animeEntryDao(): AnimeEntryDao
    abstract fun downloadDao(): DownloadItemDao
    abstract fun logDao(): DownloadLogDao
    abstract fun duplicateDao(): DuplicateHashDao
}
