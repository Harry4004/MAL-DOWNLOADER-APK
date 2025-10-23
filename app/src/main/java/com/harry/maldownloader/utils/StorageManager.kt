package com.harry.maldownloader.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Comprehensive storage manager for MAL Downloader v3.1
 * Handles both legacy external storage and modern scoped storage APIs
 * Ensures images are saved to visible Pictures directory across all Android versions
 */
class StorageManager(private val context: Context) {

    companion object {
        private const val MAL_FOLDER_NAME = "MAL_Images"
        private const val ANIME_FOLDER = "ANIME"
        private const val MANGA_FOLDER = "MANGA"
        private const val ADULT_FOLDER = "Adult"
        private const val GENERAL_FOLDER = "General"
    }

    /**
     * Get the optimal storage directory based on Android version
     * Android 10+: Uses MediaStore API for public Pictures
     * Android 9-: Uses legacy external storage
     */
    fun getStorageDirectory(contentType: String, isAdult: Boolean): File {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use public Pictures directory
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        } else {
            // Legacy Android - Use external storage root
            Environment.getExternalStorageDirectory()
        }

        val malDir = File(baseDir, MAL_FOLDER_NAME)
        val typeDir = File(malDir, contentType.uppercase())
        val categoryDir = File(typeDir, if (isAdult) ADULT_FOLDER else GENERAL_FOLDER)

        // Ensure directory structure exists
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }

        return categoryDir
    }

    /**
     * Save image to public Pictures directory with proper scoped storage handling
     * Returns the saved file path or null if failed
     */
    suspend fun saveImageToPublicDirectory(
        inputStream: InputStream,
        filename: String,
        contentType: String,
        isAdult: Boolean,
        mimeType: String = "image/jpeg"
    ): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern approach using MediaStore
                saveUsingMediaStore(inputStream, filename, contentType, isAdult, mimeType)
            } else {
                // Legacy approach using direct file writing
                saveLegacyMethod(inputStream, filename, contentType, isAdult)
            }
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveUsingMediaStore(
        inputStream: InputStream,
        filename: String,
        contentType: String,
        isAdult: Boolean,
        mimeType: String
    ): String? {
        val resolver = context.contentResolver
        
        // Create content values with metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            
            // Create relative path in Pictures/MAL_Images/TYPE/CATEGORY/
            val relativePath = "$MAL_FOLDER_NAME/${contentType.uppercase()}/${if (isAdult) ADULT_FOLDER else GENERAL_FOLDER}"
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$relativePath")
            
            // Add metadata
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        }

        // Insert into MediaStore
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null

        // Write file data
        resolver.openOutputStream(uri)?.use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        // Get the actual file path for return
        return getFilePathFromUri(resolver, uri)
    }

    private fun saveLegacyMethod(
        inputStream: InputStream,
        filename: String,
        contentType: String,
        isAdult: Boolean
    ): String? {
        val directory = getStorageDirectory(contentType, isAdult)
        val file = File(directory, filename)
        
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        
        // Notify media scanner for visibility
        notifyMediaScanner(file)
        
        return file.absolutePath
    }

    /**
     * Get file path from MediaStore URI (Android 10+)
     */
    private fun getFilePathFromUri(resolver: ContentResolver, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    /**
     * Notify media scanner for file visibility in gallery apps
     */
    private fun notifyMediaScanner(file: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
        }
    }

    /**
     * Check if external storage is available and writable
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Get human-readable storage path for display
     */
    fun getDisplayPath(contentType: String, isAdult: Boolean): String {
        val category = if (isAdult) ADULT_FOLDER else GENERAL_FOLDER
        return "Pictures/$MAL_FOLDER_NAME/${contentType.uppercase()}/$category/"
    }

    /**
     * Check if file already exists to prevent duplicates
     */
    fun fileExists(filename: String, contentType: String, isAdult: Boolean): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check via MediaStore query
            checkFileExistsInMediaStore(filename)
        } else {
            // Check direct file system
            val directory = getStorageDirectory(contentType, isAdult)
            File(directory, filename).exists()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkFileExistsInMediaStore(filename: String): Boolean {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(filename)
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    /**
     * Clean up old or failed downloads
     */
    fun cleanupTempFiles() {
        try {
            val cacheDir = context.externalCacheDir
            cacheDir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("mal_temp_") && 
                    System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}