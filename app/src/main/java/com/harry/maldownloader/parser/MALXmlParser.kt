import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream

class MALXmlParser {
    
    suspend fun parseAnimeEntries(xmlInput: InputStream): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AnimeEntry>()
        val parser = Xml.newPullParser()
        try {
            parser.setInput(xmlInput, null)
            var eventType = parser.eventType
            var currentEntry: AnimeEntry? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "anime" -> {
                                currentEntry = AnimeEntry(
                                    seriesId = 0,  // Will be updated
                                    title = "",
                                    type = null,
                                    episodesTotal = null,
                                    userId = null,
                                    episodesWatched = null,
                                    startDate = null,
                                    finishDate = null,
                                    userScore = null,
                                    dvd = null,
                                    storage = null,
                                    status = null,
                                    comments = null,
                                    timesWatched = null,
                                    rewatchValue = null,
                                    tags = null,
                                    rewatching = null,
                                    rewatchingEp = null
                                )
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        currentEntry?.let { entry ->
                            val text = parser.text?.trim() ?: ""
                            when (parser.name) {
                                "series_animedb_id" -> entry.copy(seriesId = text.toIntOrNull() ?: 0)
                                "series_title" -> entry.copy(title = text)
                                "series_type" -> entry.copy(type = text.ifEmpty { null })
                                "series_episodes" -> entry.copy(episodesTotal = text.toIntOrNull())
                                "my_id" -> entry.copy(userId = text.toIntOrNull())
                                "my_watched_episodes" -> entry.copy(episodesWatched = text.toIntOrNull())
                                "my_start_date" -> entry.copy(startDate = text.ifEmpty { null })
                                "my_finish_date" -> entry.copy(finishDate = text.ifEmpty { null })
                                "my_score" -> entry.copy(userScore = text.toIntOrNull())
                                "my_dvd" -> entry.copy(dvd = text.ifEmpty { null })
                                "my_storage" -> entry.copy(storage = text.ifEmpty { null })
                                "my_status" -> entry.copy(status = text.ifEmpty { null })
                                "my_comments" -> entry.copy(comments = text.ifEmpty { null })
                                "my_times_watched" -> entry.copy(timesWatched = text.toIntOrNull())
                                "my_rewatch_value" -> entry.copy(rewatchValue = text.ifEmpty { null })
                                "my_tags" -> entry.copy(tags = text.ifEmpty { null })
                                "my_rewatching" -> entry.copy(rewatching = text.toIntOrNull()?.let { it == 1 })
                                "my_rewatching_ep" -> entry.copy(rewatchingEp = text.toIntOrNull())
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "anime" && currentEntry != null) {
                            if (currentEntry.seriesId > 0) {  // Filter invalid entries
                                entries.add(currentEntry)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            // Handle parsing errors silently, return empty list
        } catch (e: Exception) {
            // Handle IO or other errors
        }
        entries
    }
}
