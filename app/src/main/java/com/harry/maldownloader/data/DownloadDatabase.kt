package com.harry.maldownloader.data

import androidx.room.*

@Entity(tableName = "download_queue")
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val etag: String?,
    val progress: Int,
    val status: String
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_queue")
    fun getAll(): List<DownloadItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: DownloadItem)

    @Delete
    fun delete(item: DownloadItem)
}

@Database(entities = [DownloadItem::class], version = 1)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
