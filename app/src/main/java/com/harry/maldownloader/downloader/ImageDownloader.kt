package com.harry.maldownloader.downloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.harry.maldownloader.api.JikanApiService
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class ImageDownloader(private val context: Context) {
    private val client: OkHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "MAL-Downloader-Android/1.0")
            .build()
        chain.proceed(request)
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.jikan.moe/v4/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    private val apiService = retrofit.create(JikanApiService::class.java)

    suspend fun downloadImageForEntry(entry: AnimeEntry): AnimeEntry = withContext(Dispatchers.IO) {
        var updatedEntry = entry
        val imageFile = File(context.filesDir, "images/${entry.seriesId}.jpg")
        imageFile.parentFile?.mkdirs()

        if (imageFile.exists()) {
            return@withContext updatedEntry.copy(imagePath = imageFile.absolutePath)
        }

        val imageUrl = fetchImageUrl(entry.seriesId)
        if (!imageUrl.isNullOrBlank()) {
            downloadAndSaveImage(imageUrl, imageFile, maxRetries = 3)
            if (imageFile.exists()) {
                return@withContext updatedEntry.copy(imagePath = imageFile.absolutePath)
            }
        }

        // Fallback: Generate placeholder
        val placeholder = createPlaceholderBitmap(entry.title, 300, 400)
        saveBitmapToFile(placeholder, imageFile)
        updatedEntry.copy(imagePath = imageFile.absolutePath)
    }

    private suspend fun fetchImageUrl(seriesId: Int): String? {
        repeat(3) { attempt ->
            try {
                val response = apiService.getAnimeFull(seriesId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val url = body?.data?.images?.jpg?.image_url
                    if (!url.isNullOrBlank()) return url
                }
            } catch (_: Exception) {
                // ignore, back off below
            }
            if (attempt < 2) {
                val delayMs = 1000L shl attempt // 1s, 2s
                delay(delayMs)
            }
        }
        return null
    }

    private suspend fun downloadAndSaveImage(url: String, file: File, maxRetries: Int): Boolean {
        repeat(maxRetries) { attempt ->
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    return true
                }
            } catch (_: Exception) {
                // ignore, back off below
            }
            if (attempt < maxRetries - 1) {
                val delayMs = 1000L * (attempt + 1)
                delay(delayMs)
            }
        }
        return false
    }

    private fun createPlaceholderBitmap(title: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.DKGRAY)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val textBounds = android.graphics.Rect()
        paint.getTextBounds(title, 0, min(title.length, 20), textBounds)
        val x = width / 2f
        val y = (height / 2f) - textBounds.exactCenterY()
        canvas.drawText(title, x, y, paint)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }
}
