package com.harry.maldownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Comprehensive settings for MAL Downloader v3.1
 * Stores user preferences for downloads, metadata, UI, and API behavior
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1, // Single settings row
    
    // Download Configuration
    val maxConcurrentDownloads: Int = 2,
    val downloadOnlyOnWifi: Boolean = false,
    val pauseOnLowBattery: Boolean = true,
    val retryAttempts: Int = 3,
    val downloadTimeoutSeconds: Int = 60,
    val enableBackgroundDownloads: Boolean = false,
    
    // Storage Configuration
    val folderTemplate: String = "{type}/{category}", // {type}/{category}/{malId}_{title}
    val filenameFormat: String = "{title}_{id}.{ext}", // title_id.jpg format
    val organizeFoldersByGenre: Boolean = false,
    val separateAdultContent: Boolean = true,
    val usePublicPicturesDirectory: Boolean = true,
    
    // Metadata Configuration
    val embedXmpMetadata: Boolean = true,
    val embedIptcKeywords: Boolean = false,
    val includeSynopsis: Boolean = true,
    val maxSynopsisLength: Int = 500,
    val includeUserTags: Boolean = true,
    val prioritizeUserTags: Boolean = true,
    val maxTagsPerImage: Int = 50,
    
    // API Configuration
    val preferMalOverJikan: Boolean = true,
    val apiDelayMs: Long = 1200,
    val enableMalApi: Boolean = true,
    val enableJikanApi: Boolean = true,
    val skipEnrichmentOnError: Boolean = false,
    
    // UI Configuration
    val enableDetailedLogs: Boolean = true,
    val logRetentionCount: Int = 500,
    val showProgressNotifications: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val autoScrollLogs: Boolean = true,
    
    // Advanced Features
    val enableDuplicateDetection: Boolean = true,
    val checkContentHash: Boolean = false,
    val enableImageValidation: Boolean = true,
    val createBackupOnImport: Boolean = false,
    val exportLogsOnError: Boolean = true
)

/**
 * Settings categories for organized UI display
 */
enum class SettingsCategory {
    DOWNLOADS,
    STORAGE,
    METADATA,
    API,
    ADVANCED,
    ABOUT
}

/**
 * Individual setting item for UI display
 */
data class SettingItem(
    val key: String,
    val title: String,
    val description: String,
    val category: SettingsCategory,
    val type: SettingType,
    val defaultValue: Any,
    val options: List<String>? = null, // For dropdown/selection
    val range: Pair<Int, Int>? = null // For sliders (min, max)
)

enum class SettingType {
    BOOLEAN,      // Toggle switch
    INTEGER,      // Number input/slider
    STRING,       // Text input
    SELECTION     // Dropdown/radio buttons
}

/**
 * Predefined setting configurations for the UI
 */
object SettingsConfig {
    val allSettings = listOf(
        // Downloads
        SettingItem(
            "maxConcurrentDownloads",
            "Concurrent Downloads",
            "Number of simultaneous downloads (1-5)",
            SettingsCategory.DOWNLOADS,
            SettingType.INTEGER,
            2,
            range = 1 to 5
        ),
        SettingItem(
            "downloadOnlyOnWifi",
            "Wi-Fi Only",
            "Download images only when connected to Wi-Fi",
            SettingsCategory.DOWNLOADS,
            SettingType.BOOLEAN,
            false
        ),
        SettingItem(
            "pauseOnLowBattery",
            "Pause on Low Battery",
            "Automatically pause downloads when battery is below 20%",
            SettingsCategory.DOWNLOADS,
            SettingType.BOOLEAN,
            true
        ),
        SettingItem(
            "enableBackgroundDownloads",
            "Background Downloads",
            "Continue downloads when app is in background",
            SettingsCategory.DOWNLOADS,
            SettingType.BOOLEAN,
            false
        ),
        
        // Storage
        SettingItem(
            "filenameFormat",
            "Filename Format",
            "How downloaded files are named",
            SettingsCategory.STORAGE,
            SettingType.SELECTION,
            "{title}_{id}.{ext}",
            listOf("{title}_{id}.{ext}", "{id}_{title}.{ext}", "{title}.{ext}")
        ),
        SettingItem(
            "separateAdultContent",
            "Separate Adult Content",
            "Organize 18+ content in separate Adult folders",
            SettingsCategory.STORAGE,
            SettingType.BOOLEAN,
            true
        ),
        SettingItem(
            "usePublicPicturesDirectory",
            "Save to Pictures Folder",
            "Save images to public Pictures directory (visible in gallery)",
            SettingsCategory.STORAGE,
            SettingType.BOOLEAN,
            true
        ),
        
        // Metadata
        SettingItem(
            "embedXmpMetadata",
            "XMP Metadata",
            "Embed comprehensive metadata for AVES Gallery compatibility",
            SettingsCategory.METADATA,
            SettingType.BOOLEAN,
            true
        ),
        SettingItem(
            "includeSynopsis",
            "Include Synopsis",
            "Add anime/manga synopsis to image metadata",
            SettingsCategory.METADATA,
            SettingType.BOOLEAN,
            true
        ),
        SettingItem(
            "maxTagsPerImage",
            "Max Tags per Image",
            "Maximum number of tags to embed (10-100)",
            SettingsCategory.METADATA,
            SettingType.INTEGER,
            50,
            range = 10 to 100
        ),
        
        // API
        SettingItem(
            "preferMalOverJikan",
            "Prefer Official MAL API",
            "Use MAL official API first, fallback to Jikan",
            SettingsCategory.API,
            SettingType.BOOLEAN,
            true
        ),
        SettingItem(
            "apiDelayMs",
            "API Request Delay (ms)",
            "Delay between API requests to respect rate limits",
            SettingsCategory.API,
            SettingType.INTEGER,
            1200,
            range = 500 to 5000
        )
    )
    
    fun getByCategory(category: SettingsCategory) = allSettings.filter { it.category == category }
}