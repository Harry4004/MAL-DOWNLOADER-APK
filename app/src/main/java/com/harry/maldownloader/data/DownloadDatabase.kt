package com.harry.maldownloader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.util.Log

@Database(
    entities = [AnimeEntry::class, DownloadItem::class, DownloadLog::class, DuplicateHash::class],
    version = 3, // Increment version to handle conflicts
    exportSchema = false
)
@TypeConverters(DownloadConverters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun animeEntryDao(): AnimeEntryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun logDao(): DownloadLogDao
    abstract fun duplicateDao(): DuplicateDao

    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getDatabase(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        DownloadDatabase::class.java,
                        "download_database"
                    )
                        .fallbackToDestructiveMigration() // Handle version conflicts gracefully
                        .allowMainThreadQueries() // Temporary fix for debugging
                        .build()
                    
                    Log.d("DownloadDatabase", "Database initialized successfully")
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("DownloadDatabase", "Failed to initialize database", e)
                    throw e // Re-throw to be caught by MainApplication
                }
            }
        }
        
        // Helper method to safely clear instance if needed
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}