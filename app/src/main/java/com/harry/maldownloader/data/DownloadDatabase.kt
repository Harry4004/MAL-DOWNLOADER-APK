package com.harry.maldownloader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.util.Log

@Database(
    entities = [AnimeEntry::class, DownloadItem::class, DownloadLog::class, DuplicateHash::class, AppSettings::class],
    version = 4, // Increment for AppSettings table
    exportSchema = false
)
@TypeConverters(DownloadConverters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun animeEntryDao(): AnimeEntryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun logDao(): DownloadLogDao
    abstract fun duplicateDao(): DuplicateDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getDatabase(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        DownloadDatabase::class.java,
                        "download_database_v4_enhanced"
                    )
                        .fallbackToDestructiveMigration() // Handle version conflicts gracefully
                        .allowMainThreadQueries() // Temporary for settings access
                        .build()
                    
                    Log.d("DownloadDatabase", "Enhanced database v4 initialized successfully with settings support")
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e("DownloadDatabase", "Failed to initialize enhanced database", e)
                    throw e
                }
            }
        }
        
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                Log.d("DownloadDatabase", "Database instance cleared")
            }
        }
    }
}