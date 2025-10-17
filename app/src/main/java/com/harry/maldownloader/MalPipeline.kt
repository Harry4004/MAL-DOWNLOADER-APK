package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.text.Normalizer

class MalPipeline(private val context: Context, private val onLog: (String) -> Unit) {

    companion object {
        // Backward-compatible shim for older call sites
        suspend fun parseMalDataFile(context: Context, path: String, onLog: ((String) -> Unit)? = null): List<AnimeEntry> {
            val pipeline = MalPipeline(context) { msg -> onLog?.invoke(msg) }
            return pipeline.processLocalMalFile(path) { msg -> onLog?.invoke(msg) }
        }
    }

    suspend fun processLocalMalFile(path: String, onLog: (String) -> Unit): List<AnimeEntry> = withContext(Dispatchers.IO) {
        onLog("Opening file '$path'")
        val uri = Uri.fromFile(File(path))
        processMalFile(uri)
    }

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
                var comments: String? = null

                onLog("Starting XML parsing...")
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> when (parser.name?.lowercase()) {
                            "anime" -> {
                                insideAnime = true
                                seriesId = 0; title = ""; type = null; episodesTotal = null; userId = null
                                episodesWatched = null; startDate = null; finishDate = null; userScore = null
                                status = null; tags = null; comments = null
                            }
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
                            "my_comments", "comments" -> if (insideAnime) comments = parser.nextText()
                    }
                    XmlPullParser.END_TAG -> if (insideAnime && parser.name?.lowercase() == "anime") {
                        if (title.isNotEmpty()) {
                            entries.add(
                                com.harry.maldownloader.data.AnimeEntry(
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
                                    comments = comments,
                                    timesWatched = null,
                                    rewatchValue = null,
                                    tags = tags,
                                    rewatching = false,
                                    rewatchingEp = null,
                                    imagePath = null
                                )
                            )
                        }
                        insideAnime = false
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            onLog("Error parsing XML: ${e.message}")
        }
        onLog("Parsed ${entries.size} anime entries")
        return@withContext entries
    }
}
