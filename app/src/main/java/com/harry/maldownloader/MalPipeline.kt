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

                // Variables matching the NEW AnimeEntry data class
                var seriesId = ""
                var title = ""
                var type = ""
                var episodesWatched = 0
                var totalEpisodes: Int? = null
                var userScore = 0
                var status: String? = null
                var startDate: String? = null
                var endDate: String? = null
                var tags: String? = null
                var imageUrl: String? = null
                var malUrl: String? = null
                var synopsis: String? = null
                var genres: String? = null
                var studio: String? = null
                var source: String? = null

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
                                    // Reset all variables
                                    seriesId = ""; title = ""; type = ""; episodesWatched = 0; totalEpisodes = null
                                    userScore = 0; status = null; startDate = null; endDate = null; tags = null
                                    imageUrl = null; malUrl = null; synopsis = null; genres = null; studio = null; source = null
                                }
                                // ID variants
                                "series_animedb_id", "series_animedbid" -> if (insideEntry) seriesId = parser.nextText().trim()
                                // Title variants
                                "series_title", "seriestitle" -> if (insideEntry) title = parser.nextText().trim()
                                // Type
                                "series_type" -> if (insideEntry) type = parser.nextText().trim()
                                // Episodes
                                "series_episodes" -> if (insideEntry) totalEpisodes = parser.nextText().toIntOrNull()
                                "my_watched_episodes" -> if (insideEntry) episodesWatched = parser.nextText().toIntOrNull() ?: 0
                                // Dates
                                "my_start_date" -> if (insideEntry) startDate = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                "my_finish_date" -> if (insideEntry) endDate = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                // Score
                                "my_score" -> if (insideEntry) userScore = parser.nextText().toIntOrNull() ?: 0
                                // Status
                                "my_status" -> if (insideEntry) status = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                // Tags
                                "my_tags", "mytags" -> if (insideEntry) tags = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                // Additional fields that might be in XML
                                "series_image" -> if (insideEntry) imageUrl = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                "series_synopsis" -> if (insideEntry) synopsis = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                "series_genre" -> if (insideEntry) genres = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                "series_studio" -> if (insideEntry) studio = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                "series_source" -> if (insideEntry) source = parser.nextText().trim().takeIf { it.isNotEmpty() }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val n = parser.name?.lowercase() ?: ""
                            if (insideEntry && (n == entryTagName)) {
                                if (title.isNotEmpty() && seriesId.isNotEmpty()) {
                                    entries.add(
                                        AnimeEntry(
                                            id = seriesId,
                                            title = title,
                                            type = type,
                                            score = userScore,
                                            status = status,
                                            episodesWatched = episodesWatched,
                                            totalEpisodes = totalEpisodes,
                                            imageUrl = imageUrl,
                                            imagePath = null,
                                            malUrl = malUrl,
                                            synopsis = synopsis,
                                            genres = genres,
                                            studio = studio,
                                            source = source,
                                            startDate = startDate,
                                            endDate = endDate,
                                            tags = tags
                                        )
                                    )
                                    onLog("Added entry: $title (ID: $seriesId)")
                                } else {
                                    onLog("Skip entry with empty title or ID (title='$title', id='$seriesId')")
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
        onLog("Parsed ${entries.size} entries")
        return@withContext entries
    }
}