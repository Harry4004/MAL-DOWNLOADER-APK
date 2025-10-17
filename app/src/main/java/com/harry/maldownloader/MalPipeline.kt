package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

class MalPipeline(private val context: Context, private val onLog: (String) -> Unit) {

    companion object {
        suspend fun parseMalDataFile(context: Context, path: String, onLog: ((String) -> Unit)? = null): List<AnimeEntry> {
            val pipeline = MalPipeline(context) { msg -> onLog?.invoke(msg) }
            return pipeline.processLocalMalFile(path) { msg -> onLog?.invoke(msg) }
        }
    }

    suspend fun processLocalMalFile(path: String, onLog: (String) -> Unit): List<AnimeEntry> = withContext(Dispatchers.IO) {
        onLog("Opening file '$path'")
        val f = File(path)
        if (!f.exists() || f.length() == 0L) {
            onLog("Error: File missing or empty: $path")
            return@withContext emptyList()
        }
        val uri = Uri.fromFile(f)
        processMalFile(uri)
    }

    suspend fun processMalFile(uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AnimeEntry>()
        var startTagSamplesLogged = 0
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                onLog("Error: Unable to open stream for URI: $uri")
                return@withContext emptyList()
            }
            stream.use { input ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(input, null)

                var event = parser.eventType
                var insideEntry = false
                var entryTagName = "anime" // also allow "manga"

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
                        XmlPullParser.START_TAG -> {
                            val n = parser.name?.lowercase() ?: ""
                            if (startTagSamplesLogged < 30) {
                                onLog("TAG<start>: $n")
                                startTagSamplesLogged++
                            }
                            when (n) {
                                "anime", "manga" -> {
                                    insideEntry = true
                                    entryTagName = n
                                    seriesId = 0; title = ""; type = null; episodesTotal = null; userId = null
                                    episodesWatched = null; startDate = null; finishDate = null; userScore = null
                                    status = null; tags = null; comments = null
                                }
                                // ID variants
                                "series_animedb_id", "series_animedbid" -> if (insideEntry) seriesId = parser.nextText().toIntOrNull() ?: 0
                                // Title variants
                                "series_title", "seriestitle" -> if (insideEntry) title = parser.nextText().trim()
                                // Other standard fields
                                "series_type" -> if (insideEntry) type = parser.nextText()
                                "series_episodes" -> if (insideEntry) episodesTotal = parser.nextText().toIntOrNull()
                                "my_id" -> if (insideEntry) userId = parser.nextText().toIntOrNull()
                                "my_watched_episodes" -> if (insideEntry) episodesWatched = parser.nextText().toIntOrNull()
                                "my_start_date" -> if (insideEntry) startDate = parser.nextText()
                                "my_finish_date" -> if (insideEntry) finishDate = parser.nextText()
                                "my_score" -> if (insideEntry) userScore = parser.nextText().toIntOrNull()
                                "my_status" -> if (insideEntry) status = parser.nextText()
                                // Tags/comments variants
                                "my_tags", "mytags" -> if (insideEntry) tags = parser.nextText()
                                "my_comments", "comments" -> if (insideEntry) comments = parser.nextText()
                                else -> { /* ignore other tags */ }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val n = parser.name?.lowercase() ?: ""
                            if (insideEntry && (n == entryTagName)) {
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
                                } else {
                                    onLog("Skip entry with empty title (id=$seriesId)")
                                }
                                insideEntry = false
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        } catch (e: Exception) {
            onLog("Error parsing XML: ${e.message}")
        }
        onLog("Parsed ${entries.size} anime entries")
        return@withContext entries
    }
}
