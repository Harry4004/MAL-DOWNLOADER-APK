package com.harry.maldownloader.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.*

/**
 * Enhanced StorageManager with proper permission checks and content folders
 */
class StorageManager(private val context: Context) {
    
    private val malImagesDir = "MAL_Images"
    
    fun hasStoragePermission(): Boolean {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                else ->
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    fun fileExists(filename: String, contentType: String, isAdult: Boolean): Boolean {
        return try {
            val folder = getContentFolder(contentType, isAdult)
            val file = java.io.File(folder, filename)
            file.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getContentFolder(type: String, isAdult: Boolean): java.io.File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val malDir = java.io.File(picturesDir, malImagesDir)
        
        val typeFolder = when {
            isAdult -> java.io.File(malDir, "Adult")
            type == "anime" -> java.io.File(malDir, "Anime")
            type == "manga" -> java.io.File(malDir, "Manga")
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
            val file = java.io.File(folder, filename)
            
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
     * Save sample tags file to Downloads directory
     */
    fun saveSampleFile(filename: String, content: String): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = java.io.File(downloadsDir, filename)
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
            val tempDir = java.io.File(context.cacheDir, "temp_downloads")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
                Log.d("StorageManager", "Temp files cleaned")
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Cleanup failed: ${e.message}")
        }
    }
}