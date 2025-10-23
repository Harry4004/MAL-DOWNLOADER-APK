package com.harry.maldownloader

/**
 * Build configuration constants for MAL Downloader
 */
object BuildConfig {
    const val DEBUG = true
    const val APPLICATION_ID = "com.harry.maldownloader"
    const val BUILD_TYPE = "debug"
    const val VERSION_CODE = 31
    const val VERSION_NAME = "3.1"
    const val APP_VERSION = "3.1"
    
    // Features
    const val ENABLE_LOGGING = true
    
    // API Configuration
    const val MAL_CLIENT_ID = "your_mal_client_id_here"
    
    // Build information
    val BUILD_TIME = System.currentTimeMillis()
    const val BUILD_FLAVOR = "enhanced"
}
