package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.concurrent.TimeUnit

// Data classes for API responses
data class AnimeSearchResponse(val data: List<JikanAnime>)
data class JikanAnime(
    val mal_id: Int,
    val title: String?,
    val synopsis: String?,
    val images: Map<String, Map<String, String>>?,
    val genres: List<JikanGenre>?
)
data class JikanGenre(val name: String)

class MalPipeline(private val context: Context, private val onLog: (String) -> Unit) {
    
    interface JikanApi {
        @GET("anime")
        suspend fun searchAnime(@Query("q") q: String, @Query("limit") limit: Int = 3): AnimeSearchResponse
    }

    private val httpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.jikan.moe/v4/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
        
    private val api = retrofit.create(JikanApi::class.java)

    suspend fun processMalFile(uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AnimeEntry>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(stream, null)
                
                var event = parser.eventType
                var insideAnime = false
                
                // Temporary variables for current anime
                var seriesId = 0
                var title = ""
                var type: String? = null
                var episodesTotal: Int? = null
                var userId: Int? = null
                var episodesWatched: Int? = null
                var startDate: String? = null
                var finishDate: String? = null
                var userScore: Int? = null
                var status: String? = null
                var tags: String? = null
                
                onLog("Starting XML parsing...")
                
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name?.lowercase()) {
                                "anime" -> {
                                    insideAnime = true
                                    // Reset variables
                                    seriesId = 0
                                    title = ""
                                    type = null
                                    episodesTotal = null
                                    userId = null
                                    episodesWatched = null
                                    startDate = null
                                    finishDate = null
                                    userScore = null
                                    status = null
                                    tags = null
                                }
                                "series_animedb_id" -> if (insideAnime) {
                                    seriesId = parser.nextText().toIntOrNull() ?: 0
                                }
                                "series_title" -> if (insideAnime) {
                                    title = parser.nextText()
                                }
                                "series_type" -> if (insideAnime) {
                                    type = parser.nextText()
                                }
                                "series_episodes" -> if (insideAnime) {
                                    episodesTotal = parser.nextText().toIntOrNull()
                                }
                                "my_id" -> if (insideAnime) {
                                    userId = parser.nextText().toIntOrNull()
                                }
                                "my_watched_episodes" -> if (insideAnime) {
                                    episodesWatched = parser.nextText().toIntOrNull()
                                }
                                "my_start_date" -> if (insideAnime) {
                                    startDate = parser.nextText()
                                }
                                "my_finish_date" -> if (insideAnime) {
                                    finishDate = parser.nextText()
                                }
                                "my_score" -> if (insideAnime) {
                                    userScore = parser.nextText().toIntOrNull()
                                }
                                "my_status" -> if (insideAnime) {
                                    status = parser.nextText()
                                }
                                "my_tags" -> if (insideAnime) {
                                    tags = parser.nextText()
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (insideAnime && parser.name?.lowercase() == "anime") {
                                if (title.isNotEmpty()) {
                                    val entry = AnimeEntry(
                                        seriesId = seriesId,
                                        title = title,
                                        type = type,
                                        episodesTotal = episodesTotal,
                                        userId = userId,
                                        episodesWatched = episodesWatched,
                                        startDate = startDate,
                                        finishDate = finishDate,
                                        userScore = userScore,
                                        dvd = null,
                                        storage = null,
                                        status = status,
                                        comments = null,
                                        timesWatched = null,
                                        rewatchValue = null,
                                        tags = tags,
                                        rewatching = false,
                                        rewatchingEp = null,
                                        imagePath = null
                                    )
                                    entries.add(entry)
                                    onLog("Parsed: $title")
                                }
                                insideAnime = false
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            onLog("Parsed ${entries.size} anime entries")
        } catch (e: Exception) {
            onLog("Error parsing XML: ${e.message}")
        }
        return@withContext entries
    }
    
    private fun sanitizeFileName(name: String): String {
        val tmp = Normalizer.normalize(name, Normalizer.Form.NFKC)
        return tmp.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "untitled" }
    }
}