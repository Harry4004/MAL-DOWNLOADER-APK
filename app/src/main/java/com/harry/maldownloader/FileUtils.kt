package com.harry.maldownloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object FileUtils {

    fun saveFileToDownloadDir(
        context: Context,
        sourceFileUri: Uri,
        userFolderName: String,
        fileName: String
    ): Boolean {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/MAL_IMAGES/$userFolderName")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val fileUri = resolver.insert(collection, contentValues) ?: return false

        resolver.openInputStream(sourceFileUri).use { inputStream ->
            resolver.openOutputStream(fileUri).use { outputStream ->
                if (inputStream != null && outputStream != null) {
                    inputStream.copyTo(outputStream)
                    
                    // Close streams and mark as not pending
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(fileUri, contentValues, null, null)
                    return true
                }
            }
        }

        return false
    }

    fun saveImageBytesToDownloadDir(
        context: Context,
        imageBytes: ByteArray,
        userFolderName: String,
        fileName: String
    ): Boolean {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/MAL_IMAGES/$userFolderName")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val fileUri = resolver.insert(collection, contentValues) ?: return false

        resolver.openOutputStream(fileUri).use { outputStream ->
            if (outputStream != null) {
                outputStream.write(imageBytes)
                outputStream.flush()
                
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(fileUri, contentValues, null, null)
                return true
            }
        }

        return false
    }

    fun getUserFolderName(): String {
        // Logic to get username or device identifier for folder name
        // For simplicity returning a placeholder here
        return "default_user"
    }
}
