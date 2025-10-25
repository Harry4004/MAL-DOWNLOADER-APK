package com.harry.maldownloader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app settings
 * Handles persistent storage of user preferences
 */
@Dao
interface SettingsDao {
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
    
    @Update
    suspend fun updateSettings(settings: AppSettings)
    
    @Query("SELECT EXISTS(SELECT 1 FROM app_settings WHERE id = 1)")
    suspend fun hasSettings(): Boolean
    
    // Individual setting updates for performance
    @Query("UPDATE app_settings SET maxConcurrentDownloads = :value WHERE id = 1")
    suspend fun updateMaxConcurrentDownloads(value: Int)
    
    @Query("UPDATE app_settings SET downloadOnlyOnWifi = :value WHERE id = 1")
    suspend fun updateDownloadOnlyOnWifi(value: Boolean)
    
    @Query("UPDATE app_settings SET pauseOnLowBattery = :value WHERE id = 1")
    suspend fun updatePauseOnLowBattery(value: Boolean)
    
    @Query("UPDATE app_settings SET enableBackgroundDownloads = :value WHERE id = 1")
    suspend fun updateEnableBackgroundDownloads(value: Boolean)
    
    @Query("UPDATE app_settings SET filenameFormat = :value WHERE id = 1")
    suspend fun updateFilenameFormat(value: String)
    
    @Query("UPDATE app_settings SET separateAdultContent = :value WHERE id = 1")
    suspend fun updateSeparateAdultContent(value: Boolean)
    
    @Query("UPDATE app_settings SET embedXmpMetadata = :value WHERE id = 1")
    suspend fun updateEmbedXmpMetadata(value: Boolean)
    
    @Query("UPDATE app_settings SET preferMalOverJikan = :value WHERE id = 1")
    suspend fun updatePreferMalOverJikan(value: Boolean)
    
    @Query("UPDATE app_settings SET apiDelayMs = :value WHERE id = 1")
    suspend fun updateApiDelayMs(value: Long)
    
    @Query("UPDATE app_settings SET enableDetailedLogs = :value WHERE id = 1")
    suspend fun updateEnableDetailedLogs(value: Boolean)
    
    // UI Scaling methods
    @Query("UPDATE app_settings SET iconScale = :value WHERE id = 1")
    suspend fun updateIconScale(value: Float)
    
    @Query("UPDATE app_settings SET fontScale = :value WHERE id = 1")
    suspend fun updateFontScale(value: Float)
    
    // Convenience method to get just the scaling values
    @Query("SELECT iconScale, fontScale FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getScalingSettings(): ScalingSettings?
    
    // Reset to defaults
    @Query("DELETE FROM app_settings WHERE id = 1")
    suspend fun resetToDefaults()
}

/**
 * Data class for UI scaling queries
 */
data class ScalingSettings(
    val iconScale: Float,
    val fontScale: Float
)