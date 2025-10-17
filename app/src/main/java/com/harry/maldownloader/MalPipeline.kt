package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.text.Normalizer

class MalPipeline(private val context: Context, private val onLog: (String) -> Unit) {

    suspend fun processMalFile(uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AnimeEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(stream, null)

                var event = parser.eventType
                var insideAnime = false

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
                        XmlPullParser.START_TAG -> when (parser.name?.lowercase()) {
                            "anime" -> {
                                insideAnime = true
                                seriesId = 0; title = ""; type = null; episodesTotal = null; userId = null
                                episodesWatched = null; startDate = null; finishDate = null; userScore = null
                                status = null; tags = null
                            }
                            // Support both snake_case and camel/legacy variants
                            "series_animedb_id", "series_animedbid" -> if (insideAnime) seriesId = parser.nextText().toIntOrNull() ?: 0
                            "series_title", "seriestitle" -> if (insideAnime) title = parser.nextText().trim()
                            "series_type" -> if (insideAnime) type = parser.nextText()
                            "series_episodes" -> if (insideAnime) episodesTotal = parser.nextText().toIntOrNull()
                            "my_id" -> if (insideAnime) userId = parser.nextText().toIntOrNull()
                            "my_watched_episodes" -> if (insideAnime) episodesWatched = parser.nextText().toIntOrNull()
                            "my_start_date" -> if (insideAnime) startDate = parser.nextText()
                            "my_finish_date" -> if (insideAnime) finishDate = parser.nextText()
                            "my_score" -> if (insideAnime) userScore = parser.nextText().toIntOrNull()
                            "my_status" -> if (insideAnime) status = parser.nextText()
                            "my_tags", "mytags" -> if (insideAnime) tags = parser.nextText()
                        }
                        XmlPullParser.END_TAG -> if (insideAnime && parser.name?.lowercase() == "anime") {
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
                    event = parser.next()
                }
            }
            onLog("Parsed ${entries.size} anime entries")
        } catch (e: Exception) {
            onLog("Error parsing XML: ${e.message}")
        }
        entries
    }
}
