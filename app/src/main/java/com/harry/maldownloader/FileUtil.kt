package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileUtil {
    suspend fun copyUriToTempFile(context: Context, uri: Uri, name: String): File? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val dir = File(context.cacheDir, "imports").apply { mkdirs() }
                val out = File(dir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                out
            } catch (_: Exception) { null }
        }
}