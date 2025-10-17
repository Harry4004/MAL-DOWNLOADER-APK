package com.harry.maldownloader.parser

import android.util.Xml
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream

class MALXmlParser {

    suspend fun parseAnimeEntries(xmlInput: InputStream): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AnimeEntry>()
        val parser = Xml.newPullParser()
        var currentSeriesId = 0
        var currentTitle = ""
        var currentType: String? = null
        var currentEpisodesTotal: Int? = null
        var currentUserId: Int? = null
        var currentEpisodesWatched: Int? = null
        var currentStartDate: String? = null
        var currentFinishDate: String? = null
        var currentUserScore: Int? = null
        var currentDvd: String? = null
        var currentStorage: String? = null
        var currentStatus: String? = null
        var currentComments: String? = null
        var currentTimesWatched: Int? = null
        var currentRewatchValue: String? = null
        var currentTags: String? = null
        var currentRewatching: Boolean? = null
        var currentRewatchingEp: Int? = null

        try {
            parser.setInput(xmlInput, null)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "anime") {
                            // Reset for new entry
                            currentSeriesId = 0
                            currentTitle = ""
                            currentType = null
                            currentEpisodesTotal = null
                            currentUserId = null
                            currentEpisodesWatched = null
                            currentStartDate = null
                            currentFinishDate = null
                            currentUserScore = null
                            currentDvd = null
                            currentStorage = null
                            currentStatus = null
                            currentComments = null
                            currentTimesWatched = null
                            currentRewatchValue = null
                            currentTags = null
                            currentRewatching = null
                            currentRewatchingEp = null
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> {  // Handle TEXT and CDATA
                        val text = parser.text?.trim()?.takeIf { it.isNotEmpty() } ?: ""
                        when (parser.name) {
                            "series_animedb_id" -> currentSeriesId = text.toIntOrNull() ?: 0
                            "series_title" -> currentTitle = text
                            "series_type" -> currentType = text.ifEmpty { null }
                            "series_episodes" -> currentEpisodesTotal = text.toIntOrNull()
                            "my_id" -> currentUserId = text.toIntOrNull()
                            "my_watched_episodes" -> currentEpisodesWatched = text.toIntOrNull()
                            "my_start_date" -> currentStartDate = text.ifEmpty { null }
                            "my_finish_date" -> currentFinishDate = text.ifEmpty { null }
                            "my_score" -> currentUserScore = text.toIntOrNull()
                            "my_dvd" -> currentDvd = text.ifEmpty { null }
                            "my_storage" -> currentStorage = text.ifEmpty { null }
                            "my_status" -> currentStatus = text.ifEmpty { null }
                            "my_comments" -> currentComments = text.ifEmpty { null }
                            "my_times_watched" -> currentTimesWatched = text.toIntOrNull()
                            "my_rewatch_value" -> currentRewatchValue = text.ifEmpty { null }
                            "my_tags" -> currentTags = text.ifEmpty { null }
                            "my_rewatching" -> currentRewatching = text.toIntOrNull()?.let { it == 1 }
                            "my_rewatching_ep" -> currentRewatchingEp = text.toIntOrNull()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "anime" && currentSeriesId > 0) {
                            entries.add(
                                AnimeEntry(
                                    seriesId = currentSeriesId,
                                    title = currentTitle,
                                    type = currentType,
                                    episodesTotal = currentEpisodesTotal,
                                    userId = currentUserId,
                                    episodesWatched = currentEpisodesWatched,
                                    startDate = currentStartDate,
                                    finishDate = currentFinishDate,
                                    userScore = currentUserScore,
                                    dvd = currentDvd,
                                    storage = currentStorage,
                                    status = currentStatus,
                                    comments = currentComments,
                                    timesWatched = currentTimesWatched,
                                    rewatchValue = currentRewatchValue,
                                    tags = currentTags,
                                    rewatching = currentRewatching,
                                    rewatchingEp = currentRewatchingEp
                                )
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            // Silent fail for malformed XML
        } catch (e: Exception) {
            // Silent fail for IO issues
        } finally {
            xmlInput.close()
        }
        entries
    }
}
