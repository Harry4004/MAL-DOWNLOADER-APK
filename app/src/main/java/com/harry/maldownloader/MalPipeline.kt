package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.text.Normalizer
import java.util.concurrent.TimeUnit

class MalPipeline(private val context: Context, private val onLog: (String) -> Unit) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun processMalFile(uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = parseXml(uri)
        val root = ensureOutputRoot()
        var success = 0
        var failed = 0
        for ((index, e) in entries.withIndex()) {
            val ok = downloadAllForEntry(root, e)
            if (ok) success++ else failed++
            onLog("[${index + 1}/${entries.size}] ${e.title}: ${if (ok) "OK" else "FAILED"}")
            // Gentle pacing to avoid server throttling
            delay(200L)
        }
        onLog("Download summary: success=$success, failed=$failed, outDir=${root.absolutePath}")
        entries
    }

    private fun ensureOutputRoot(): File {
        // Pictures/MAL_Export inside app-specific external files dir
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val out = File(base, "MAL_Export")
        out.mkdirs()
        return out
    }

    private suspend fun downloadAllForEntry(root: File, e: AnimeEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val folder = File(root, sanitize(e.title)).apply { mkdirs() }
            var any = false

            // 1) Try to find an image URL inside comments/tags if present (very heuristic)
            val possibleUrls = mutableListOf<String>()
            e.comments?.let { possibleUrls += extractUrls(it) }
            e.tags?.let { possibleUrls += extractUrls(it) }
            // 2) Also consider conventional MAL fields if available in exported XML (some dumps include image url fields)
            // NOTE: Your current AnimeEntry does not carry an explicit imageUrl. If you later add it, prioritize that.

            val poster = File(folder, "poster.jpg")
            val ok = tryAllUrls(possibleUrls.distinct(), poster)
            if (ok) any = true else onLog("No direct image URL for '${e.title}', created folder only")

            // 3) Always write a minimal metadata text for the entry
            val meta = File(folder, "meta.txt")
            meta.writeText(buildString {
                appendLine("title=${e.title}")
                appendLine("status=${e.status ?: ""}")
                appendLine("episodes=${e.episodesWatched ?: 0}/${e.episodesTotal ?: 0}")
                appendLine("score=${e.userScore ?: ""}")
                appendLine("tags=${e.tags ?: ""}")
                appendLine("comments=${e.comments ?: ""}")
            })
            true
        } catch (ex: Exception) {
            onLog("Error saving '${e.title}': ${ex.message}")
            false
        }
    }

    private fun extractUrls(text: String): List<String> {
        val rx = "(https?://[\\w./%#?=&+-]+)".toRegex()
        return rx.findAll(text).map { it.groupValues[1] }.toList()
    }

    private suspend fun tryAllUrls(urls: List<String>, out: File): Boolean {
        if (urls.isEmpty()) return false
        for (u in urls) {
            if (downloadWithRetries(u, out)) return true
        }
        return false
    }

    private suspend fun downloadWithRetries(url: String, out: File, retries: Int = 4): Boolean = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt < retries) {
            try {
                val req = Request.Builder().url(url)
                    .header("User-Agent", "MAL-Downloader/1.0 (Android)")
                    .build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                    val body = resp.body ?: throw IllegalStateException("Empty body")
                    out.outputStream().use { os -> body.byteStream().copyTo(os) }
                    return@withContext true
                }
            } catch (e: Exception) {
                attempt++
                val backoff = 500L * attempt
                onLog("Download error (${attempt}/$retries) $url: ${e.message}. Retrying in ${backoff}ms")
                delay(backoff)
            }
        }
        return@withContext false
    }

    private fun sanitize(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFKC)
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .ifEmpty { "untitled" }

    private fun parseXml(uri: Uri): List<AnimeEntry> {
        val entries = mutableListOf<AnimeEntry>()
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
        }
        onLog("Parsed ${entries.size} anime entries")
        return entries
    }
}
