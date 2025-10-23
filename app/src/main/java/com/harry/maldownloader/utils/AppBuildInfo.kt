package com.harry.maldownloader.utils

/**
 * Custom build information constants for MAL Downloader
 * Separate from Android's generated BuildConfig to avoid conflicts
 */
object AppBuildInfo {
    // Features
    const val ENABLE_LOGGING = true
    
    // API Configuration
    const val MAL_CLIENT_ID = "your_mal_client_id_here"
    
    // Build information
    val BUILD_TIME = System.currentTimeMillis()
    const val BUILD_FLAVOR = "enhanced"
    
    // App version info (use Android's BuildConfig for official version)
    const val APP_VERSION = "3.1"
}