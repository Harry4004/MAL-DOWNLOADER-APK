package com.harry.maldownloader.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing app settings with business logic
 * Handles validation and defaults for UI scaling and other settings
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    
    // Settings flow with defaults
    val settingsFlow: Flow<AppSettings> = settingsDao.getSettingsFlow()
        .map { it ?: AppSettings() }
    
    // Individual scaling flows with validation
    val iconScaleFlow: Flow<Float> = settingsFlow
        .map { it.iconScale.coerceIn(0.85f, 1.30f) }
        
    val fontScaleFlow: Flow<Float> = settingsFlow
        .map { it.fontScale.coerceIn(0.90f, 1.30f) }
    
    /**
     * Initialize settings if they don't exist
     */
    suspend fun initializeSettings() {
        if (!settingsDao.hasSettings()) {
            settingsDao.saveSettings(AppSettings())
        }
    }
    
    /**
     * Update icon scale with validation
     */
    suspend fun updateIconScale(scale: Float) {
        val validScale = scale.coerceIn(0.85f, 1.30f)
        settingsDao.updateIconScale(validScale)
    }
    
    /**
     * Update font scale with validation
     */
    suspend fun updateFontScale(scale: Float) {
        val validScale = scale.coerceIn(0.90f, 1.30f)
        settingsDao.updateFontScale(validScale)
    }
    
    /**
     * Reset scaling settings to defaults
     */
    suspend fun resetScaling() {
        settingsDao.updateIconScale(1.0f)
        settingsDao.updateFontScale(1.0f)
    }
    
    /**
     * Get current settings (blocking)
     */
    suspend fun getCurrentSettings(): AppSettings {
        return settingsDao.getSettings() ?: AppSettings()
    }
    
    /**
     * Get current scaling settings only
     */
    suspend fun getCurrentScalingSettings(): ScalingSettings {
        return settingsDao.getScalingSettings() ?: ScalingSettings(1.0f, 1.0f)
    }
    
    /**
     * Save complete settings object
     */
    suspend fun saveSettings(settings: AppSettings) {
        // Apply validation before saving
        val validatedSettings = settings.copy(
            iconScale = settings.iconScale.coerceIn(0.85f, 1.30f),
            fontScale = settings.fontScale.coerceIn(0.90f, 1.30f),
            maxConcurrentDownloads = settings.maxConcurrentDownloads.coerceIn(1, 5),
            maxTagsPerImage = settings.maxTagsPerImage.coerceIn(10, 100),
            apiDelayMs = settings.apiDelayMs.coerceIn(500L, 5000L)
        )
        settingsDao.saveSettings(validatedSettings)
    }
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetAllSettings() {
        settingsDao.resetToDefaults()
        settingsDao.saveSettings(AppSettings())
    }
}