package com.harry.maldownloader.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.*

/**
 * Enhanced StorageManager with custom tags file support
 */
class StorageManager(private val context: Context) {
    
    private val malImagesDir = "MAL_Images"
    
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    fun fileExists(filename: String, contentType: String, isAdult: Boolean): Boolean {
        return try {
            val folder = getContentFolder(contentType, isAdult)
            val file = File(folder, filename)
            file.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getContentFolder(type: String, isAdult: Boolean): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val malDir = File(picturesDir, malImagesDir)
        
        val typeFolder = when {
            isAdult -> File(malDir, "Adult")
            type == "anime" -> File(malDir, "Anime")
            type == "manga" -> File(malDir, "Manga")
            else -> malDir
        }
        
        if (!typeFolder.exists()) {
            typeFolder.mkdirs()
        }
        
        return typeFolder
    }
    
    fun saveImageToPublicDirectory(
        inputStream: InputStream,
        filename: String,
        contentType: String,
        isAdult: Boolean,
        mimeType: String
    ): String? {
        return try {
            val folder = getContentFolder(contentType, isAdult)
            val file = File(folder, filename)
            
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            
            if (file.exists()) {
                Log.d("StorageManager", "Saved to: ${file.absolutePath}")
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Save failed: ${e.message}")
            null
        }
    }
    
    /**
     * NEW: Save sample tags file to Downloads directory
     */
    fun saveSampleFile(filename: String, content: String): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, filename)
            file.writeText(content, Charsets.UTF_8)
            
            if (file.exists()) {
                Log.d("StorageManager", "Sample file saved to: ${file.absolutePath}")
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Sample file save failed: ${e.message}")
            null
        }
    }
    
    fun cleanupTempFiles() {
        try {
            val tempDir = File(context.cacheDir, "temp_downloads")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                Log.d("StorageManager", "Temp files cleaned")
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Cleanup failed: ${e.message}")
        }
    }
}