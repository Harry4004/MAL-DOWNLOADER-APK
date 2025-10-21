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
                var entryTagName = "anime"

                // Variables matching the NEW AnimeEntry data class
                var malId = 0
                var title = ""
                var type = ""
                var episodesWatched: Int? = null
                var totalEpisodes: Int? = null
                var userScore: Float? = null
                var status: String? = null
                var startDate: String? = null
                var endDate: String? = null
                var tagsString: String? = null
                var imageUrl: String? = null
                var malUrl: String? = null
                var synopsis: String? = null
                var genresString: String? = null
                var studio: String? = null
                var source: String? = null
                var chapters: Int? = null
                var volumes: Int? = null

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
                                    malId = 0; title = ""; type = ""; episodesWatched = null; totalEpisodes = null
                                    userScore = null; status = null; startDate = null; endDate = null; tagsString = null
                                    imageUrl = null; malUrl = null; synopsis = null; genresString = null; studio = null; source = null
                                    chapters = null; volumes = null
                                }
                                "series_animedb_id", "series_animedbid", "manga_mangadb_id" -> if (insideEntry) {
                                    malId = parser.nextText().trim().toIntOrNull() ?: 0
                                }
                                "series_title", "seriestitle", "manga_title" -> if (insideEntry) {
                                    title = parser.nextText().trim()
                                }
                                "series_type" -> if (insideEntry) {
                                    type = parser.nextText().trim()
                                }
                                "series_episodes" -> if (insideEntry) {
                                    totalEpisodes = parser.nextText().toIntOrNull()
                                }
                                "my_watched_episodes" -> if (insideEntry) {
                                    episodesWatched = parser.nextText().toIntOrNull()
                                }
                                "manga_chapters" -> if (insideEntry) {
                                    chapters = parser.nextText().toIntOrNull()
                                }
                                "manga_volumes" -> if (insideEntry) {
                                    volumes = parser.nextText().toIntOrNull()
                                }
                                "my_start_date" -> if (insideEntry) {
                                    startDate = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "my_finish_date" -> if (insideEntry) {
                                    endDate = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "my_score" -> if (insideEntry) {
                                    userScore = parser.nextText().toIntOrNull()?.toFloat()
                                }
                                "my_status" -> if (insideEntry) {
                                    status = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "my_tags", "mytags" -> if (insideEntry) {
                                    tagsString = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "series_image" -> if (insideEntry) {
                                    imageUrl = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "series_synopsis" -> if (insideEntry) {
                                    synopsis = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "series_genre" -> if (insideEntry) {
                                    genresString = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "series_studio" -> if (insideEntry) {
                                    studio = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                                "series_source" -> if (insideEntry) {
                                    source = parser.nextText().trim().takeIf { it.isNotEmpty() }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val n = parser.name?.lowercase() ?: ""
                            if (insideEntry && (n == entryTagName)) {
                                if (title.isNotEmpty() && malId > 0) {
                                    // Convert comma-separated strings to lists
                                    val tagsList = tagsString?.split(",")?.map { it.trim() } ?: emptyList()
                                    val genresList = genresString?.split(",")?.map { it.trim() } ?: emptyList()
                                    
                                    entries.add(
                                        AnimeEntry(
                                            malId = malId,
                                            title = title,
                                            type = entryTagName,
                                            userTags = tagsList,
                                            score = userScore,
                                            status = status,
                                            episodes = totalEpisodes,
                                            episodesWatched = episodesWatched,
                                            totalEpisodes = totalEpisodes,
                                            chapters = chapters,
                                            volumes = volumes,
                                            imageUrl = imageUrl,
                                            malUrl = malUrl,
                                            synopsis = synopsis,
                                            genres = genresList,
                                            studio = studio,
                                            source = source,
                                            startDate = startDate,
                                            endDate = endDate,
                                            tags = tagsList,
                                            allTags = tagsList + genresList
                                        )
                                    )
                                    onLog("Added entry: $title (ID: $malId)")
                                } else {
                                    onLog("Skip entry with empty title or ID (title='$title', id='$malId')")
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