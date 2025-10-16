package com.harry.maldownloader

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.Normalizer
import java.util.concurrent.TimeUnit

// Enhanced MAL entry fields for real export
data class MalEntry(
    val title: String,
    val imageUrl: String?,
    val description: String?,
    val tags: List<String>,
    val animeId: String? = null,
    val score: String? = null
)

data class EmbeddedMeta(
    val title: String,
    val description: String?,
    val tags: List<String>,
    val source: String? = "MAL"
)

object MalPipeline {
    fun parseMalDataFile(context: Context, filePath: String, onLog: ((String)->Unit)? = null): List<MalEntry> {
        val out = mutableListOf<MalEntry>()
        try {
            val fis = java.io.FileInputStream(filePath)
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(fis, null)
            var event = parser.eventType
            var insideAnime = false
            var title: String? = null
            var imageUrl: String? = null
            var desc: String? = null
            var tags = mutableListOf<String>()
            var animeId: String? = null
            var score: String? = null
            var blockCount = 0
            onLog?.invoke("Parsing MAL XML...")
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                        "anime" -> {
                            // Start new anime record
                            insideAnime = true
                            title = null
                            imageUrl = null
                            desc = null
                            tags = mutableListOf()
                            animeId = null
                            score = null
                        }
                        "seriestitle" -> if (insideAnime) title = parser.nextText()
                        "seriesimage" -> if (insideAnime) imageUrl = parser.nextText()
                        "series_synopsis" -> if (insideAnime) desc = parser.nextText()
                        "mytags" -> if (insideAnime) {
                            val tagCsv = parser.nextText()
                            if (tagCsv.isNotBlank()) tags.addAll(tagCsv.split(",",";"," "))
                        }
                        "series_animedbid" -> if (insideAnime) animeId = parser.nextText()
                        "myscore" -> if (insideAnime) score = parser.nextText()
                        // Fallbacks:
                        "seriesimageurl", "image", "image_url", "poster", "cover" -> if (insideAnime && imageUrl==null) imageUrl = parser.nextText()
                        "synopsis", "description" -> if (insideAnime && desc==null) desc = parser.nextText()
                    }
                    XmlPullParser.END_TAG -> if (insideAnime && parser.name.lowercase() == "anime") {
                        if (!title.isNullOrBlank()) {
                            blockCount++
                            onLog?.invoke("Parsed: #$blockCount $title")
                            out.add(
                                MalEntry(
                                    title = title!!.trim(),
                                    imageUrl = imageUrl,
                                    description = desc,
                                    tags = tags.filter { it.isNotBlank() },
                                    animeId = animeId,
                                    score = score
                                )
                            )
                        }
                        insideAnime = false
                    }
                }
                event = parser.next()
            }
            fis.close()
            onLog?.invoke("Found ${out.size} anime entries (MAL export)")
        } catch (e: Exception) {
            onLog?.invoke("[Error parsing XML] ${e.localizedMessage}")
        }
        return out
    }
    // ... [The rest remains unchanged, but add onLog callbacks to show step logs for all image/tag/network/output events] ...
}
suspend fun enrichFromJikanIfMissing(entry: MalEntry): MalEntry {
    val api = retrofit.create(JikanApi::class.java)
    return try {
        val resp = api.searchAnime(entry.title, 3)
        val best = resp.data.firstOrNull()
        if (best != null) {
            val poster = entry.imageUrl ?: (best.images?.get("jpg")?.get("image_url")
                ?: best.images?.get("webp")?.get("image_url"))
            val desc = if (entry.description.isNullOrBlank()) best.synopsis else entry.description
            val extraTags = best.genres?.mapNotNull { it.name } ?: emptyList()
            entry.copy(
                imageUrl = poster,
                description = desc,
                tags = (entry.tags + extraTags).distinct()
            )
        } else entry
    } catch (_: Exception) {
        entry
    }
}

suspend fun processEntry(root: File, entry: MalEntry, userCustomTags: List<String>): Boolean {
    val (imageFile, metaFile) = buildOutputPaths(root, entry.title)
    val tags = enrichTags(entry.tags, entry.title, entry.description, userCustomTags)
    val ok = ensureImageWithAllFallbacks(entry, imageFile)
    embedMetadataIntoImage(imageFile, EmbeddedMeta(entry.title, entry.description, tags, "MAL"))
    writeMetaJson(metaFile, EmbeddedMeta(entry.title, entry.description, tags, "MAL"))
    return ok
}suspend fun enrichFromJikanIfMissing(entry: MalEntry): MalEntry {
    val api = retrofit.create(JikanApi::class.java)
    return try {
        val resp = api.searchAnime(entry.title, 3)
        val best = resp.data.firstOrNull()
        if (best != null) {
            val poster = entry.imageUrl ?: (best.images?.get("jpg")?.get("image_url")
                ?: best.images?.get("webp")?.get("image_url"))
            val desc = if (entry.description.isNullOrBlank()) best.synopsis else entry.description
            val extraTags = best.genres?.mapNotNull { it.name } ?: emptyList()
            entry.copy(
                imageUrl = poster,
                description = desc,
                tags = (entry.tags + extraTags).distinct()
            )
        } else entry
    } catch (_: Exception) {
        entry
    }
}

suspend fun processEntry(root: File, entry: MalEntry, userCustomTags: List<String>): Boolean {
    val (imageFile, metaFile) = buildOutputPaths(root, entry.title)
    val tags = enrichTags(entry.tags, entry.title, entry.description, userCustomTags)
    val ok = ensureImageWithAllFallbacks(entry, imageFile)
    embedMetadataIntoImage(imageFile, EmbeddedMeta(entry.title, entry.description, tags, "MAL"))
    writeMetaJson(metaFile, EmbeddedMeta(entry.title, entry.description, tags, "MAL"))
    return ok
}

