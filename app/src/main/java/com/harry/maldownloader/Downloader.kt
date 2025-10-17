package com.harry.maldownloader

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URL

object Downloader {

    private val client = OkHttpClient()

    fun fetchAnimeImage(animeId: String, clientId: String): String? {
        val url = "https://api.myanimelist.net/v2/anime/$animeId?fields=main_picture"
        val request = Request.Builder()
            .url(url)
            .addHeader("X-MAL-CLIENT-ID", clientId)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        return json.optJSONObject("main_picture")?.optString("large")
    }

    fun downloadImage(imageUrl: String, saveFile: File) {
        val stream = URL(imageUrl).openStream()
        saveFile.outputStream().use { output ->
            stream.copyTo(output)
        }
        println("Saved ${saveFile.name}")
    }
}