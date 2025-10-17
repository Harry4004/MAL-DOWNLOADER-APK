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
import java.text.Normalizer
import java.util.concurrent.TimeUnit

// Data classes defining MAL entries and embedded metadata of images
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
    interface JikanApi {
        @GET("anime")
        suspend fun searchAnime(@Query("q") q: String, @Query("limit") limit: Int = 3): AnimeSearchResponse
    }

    data class AnimeSearchResponse(val data: List<JikanAnime>)
    data class JikanAnime(
        val mal_id: Int,
        val title: String?,
        val synopsis: String?,
        val images: Map<String, Map<String, String>>?,
        val genres: List<JikanGenre>?
    )
    data class JikanGenre(val name: String)

    private val httpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.jikan.moe/v4/")
        .addConverterFactory(MoshiConverterFactory.create())
        .client(httpClient)
        .build()

    fun parseMalDataFile(context: Context, filePath: String, onLog: ((String) -> Unit)? = null): List<MalEntry> {
        val out = mutableListOf<MalEntry>()
        try {
            val fis = java.io.FileInputStream(filePath)
            onLog?.invoke("Opened file '$filePath', size=${fis.available()} bytes")
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

            onLog?.invoke("Starting MAL XML parsing loop...")
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        onLog?.invoke("Start tag: ${parser.name}")
                        when (parser.name.lowercase()) {
                            "anime" -> {
                                insideAnime = true
                                title = null
                                imageUrl = null
                                desc = null
                                tags = mutableListOf()
                                animeId = null
                                score = null
                                onLog?.invoke("New anime record started.")
                            }
                            "seriestitle" -> if (insideAnime) {
                                val text = parser.nextText()
                                onLog?.invoke("Found title: $text")
                                title = text
                            }
                            "seriesimage" -> if (insideAnime) {
                                val text = parser.nextText()
                                onLog?.invoke("Found image url: $text")
                                imageUrl = text
                            }
                            "series_synopsis" -> if (insideAnime) {
                                val text = parser.nextText()
                                onLog?.invoke("Found description.")
                                desc = text
                            }
                            "mytags" -> if (insideAnime) {
                                val tagCsv = parser.nextText()
                                if (tagCsv.isNotBlank()) {
                                    onLog?.invoke("Found custom tags: $tagCsv")
                                    tags.addAll(tagCsv.split(",", ";", " ").map { it.trim() }.filter { it.isNotBlank() })
                                }
                            }
                            "series_animedbid" -> if (insideAnime) {
                                val text = parser.nextText()
                                onLog?.invoke("Found MAL ID: $text")
                                animeId = text
                            }
                            "myscore" -> if (insideAnime) {
                                val text = parser.nextText()
                                onLog?.invoke("Found score: $text")
                                score = text
                            }
                            // Fallbacks:
                            "seriesimageurl", "image", "image_url", "poster", "cover" -> if (insideAnime && imageUrl == null) {
                                val text = parser.nextText()
                                onLog?.invoke("Found fallback image url: $text")
                                imageUrl = text
                            }
                            "synopsis", "description" -> if (insideAnime && desc == null) {
                                val text = parser.nextText()
                                onLog?.invoke("Found fallback description.")
                                desc = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (insideAnime && parser.name.lowercase() == "anime") {
                            if (!title.isNullOrBlank()) {
                                blockCount++
                                onLog?.invoke("Completed anime record #$blockCount: $title")
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
                            } else {
                                onLog?.invoke("Anime record skipped due to empty title.")
                            }
                            insideAnime = false
                        }
                    }
                }
                event = parser.next()
            }
            fis.close()
            onLog?.invoke("Parsing complete. Total entries found: ${out.size}.")
        } catch (e: Exception) {
            onLog?.invoke("[Error parsing XML] ${e.localizedMessage}")
        }
        return out
    }

    suspend fun enrichFromJikanIfMissing(entry: MalEntry, onLog: ((String) -> Unit)? = null): MalEntry {
        onLog?.invoke("Enriching ${entry.title} from Jikan API...")
        val api = retrofit.create(JikanApi::class.java)
        return try {
            val resp = api.searchAnime(entry.title, 3)
            val best = resp.data.firstOrNull()
            if (best != null) {
                val poster = entry.imageUrl ?: (best.images?.get("jpg")?.get("image_url")
                    ?: best.images?.get("webp")?.get("image_url"))
                val desc = if (entry.description.isNullOrBlank()) best.synopsis else entry.description
                val extraTags = best.genres?.mapNotNull { it.name } ?: emptyList()
                onLog?.invoke("Enrichment fetched for ${entry.title}")
                entry.copy(
                    imageUrl = poster,
                    description = desc,
                    tags = (entry.tags + extraTags).distinct()
                )
            } else {
                onLog?.invoke("No enrichment found for ${entry.title}")
                entry
            }
        } catch (ex: Exception) {
            onLog?.invoke("Error enriching ${entry.title}: ${ex.localizedMessage}")
            entry
        }
    }

    suspend fun processEntry(root: File, entry: MalEntry, userCustomTags: List<String>, onLog: ((String) -> Unit)? = null): Boolean =
        withContext(Dispatchers.IO) {
            onLog?.invoke("Processing entry ${entry.title}...")
            val (imageFile, metaFile) = buildOutputPaths(root, entry.title)
            val tags = enrichTags(entry.tags, entry.title, entry.description, userCustomTags)
            val ok = ensureImageWithAllFallbacks(entry, imageFile, onLog)
            embedMetadataIntoImage(imageFile, EmbeddedMeta(entry.title, entry.description, tags, "MAL"))
            writeMetaJson(metaFile, EmbeddedMeta(entry.title, entry.description, tags, "MAL"))
            onLog?.invoke("Entry processed: ${entry.title} -> ${if (ok) "OK" else "Placeholder"}")
            return@withContext ok
        }

    private fun sanitizeFileName(name: String): String {
        val tmp = Normalizer.normalize(name, Normalizer.Form.NFKC)
        return tmp.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "untitled" }
    }

    private fun buildOutputPaths(root: File, seriesTitle: String): Pair<File, File> {
        val folderName = sanitizeFileName(seriesTitle)
        val dir = File(root, folderName).apply { mkdirs() }
        val imageFile = File(dir, "poster.jpg")
        val metaFile = File(dir, "meta.json")
        return imageFile to metaFile
    }

    private fun writeMetaJson(metaFile: File, meta: EmbeddedMeta) {
        val json = """
            {
              "title": ${jsonEscape(meta.title)},
              "description": ${jsonEscape(meta.description ?: "")},
              "tags": ${meta.tags.joinToString(prefix = "[", postfix = "]") { jsonEscape(it) }},
              "source": ${jsonEscape(meta.source ?: "")}
            }
        """.trimIndent()
        metaFile.writeText(json)
    }

    private fun jsonEscape(s: String): String = "\"" + s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r") + "\""

    private suspend fun ensureImageWithAllFallbacks(entry: MalEntry, outFile: File, onLog: ((String) -> Unit)? = null): Boolean {
        onLog?.invoke("Downloading image: ${entry.imageUrl}")
        if (downloadImageWithRetries(entry.imageUrl, outFile, onLog)) return true
        val fb = findImageUrlFallback(entry.title)
        if (downloadImageWithRetries(fb, outFile, onLog)) return true
        onLog?.invoke("Failed to download image, writing placeholder")
        return writeTinyPlaceholderPng(outFile)
    }

    private suspend fun downloadImageWithRetries(
        imageUrl: String?,
        outFile: File,
        maxRetries: Int = 4,
        connectTimeoutMs: Int = 8000,
        readTimeoutMs: Int = 12000,
        onLog: ((String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrBlank()) return@withContext false
        var attempt = 0
        while (attempt < maxRetries) {
            try {
                onLog?.invoke("Attempt ${attempt + 1} to download image")
                val url = URL(imageUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                onLog?.invoke("Successfully downloaded image")
                return@withContext true
            } catch (e: Exception) {
                onLog?.invoke("Error downloading image: ${e.localizedMessage}")
                attempt++
            }
        }
        return@withContext false
    }

    private fun writeTinyPlaceholderPng(outFile: File): Boolean {
        return try {
            val w = 32
            val h = 48
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            canvas.drawColor(android.graphics.Color.BLACK)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.DKGRAY
                textSize = 10f
            }
            canvas.drawText("N/A", 6f, 20f, paint)
            FileOutputStream(outFile).use { fos ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
            }
            bmp.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun findImageUrlFallback(title: String): String? = withContext(Dispatchers.IO) {
        val queries = listOf(
            "$title anime poster",
            "$title key visual",
            "$title cover art",
            "$title official art"
        )
        for (q in queries) {
            val url = searchImageViaDuckDuckGo(q)
            if (url != null) return@withContext url
        }
        return@withContext null
    }

    private suspend fun searchImageViaDuckDuckGo(query: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val searchUrl = "https://duckduckgo.com/i.js?q=${java.net.URLEncoder.encode(query, "UTF-8")}&o=json&p=1&s=0&u=bing&f=1&l=wt-wt"
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Android 10; Mobile; rv:81.0) Gecko/81.0 Firefox/81.0")
                .build()
            val response = client.newCall(request).execute()
            val json = response.body?.string()
            if (json?.contains("\"image\"") == true) {
                val regex = "\"image\":\"([^\"]+)\"".toRegex()
                val match = regex.find(json)
                match?.groupValues?.get(1)?.replace("\\/", "/")
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun enrichTags(originalTags: List<String>, title: String, description: String?, customTags: List<String>): List<String> {
        val combined = mutableListOf<String>()
        combined.addAll(originalTags)
        combined.addAll(customTags)
        val titleLower = title.lowercase()
        val descLower = description?.lowercase() ?: ""
        if (titleLower.contains("harem") || descLower.contains("harem")) {
            combined.add("A-Harem")
        }
        if (titleLower.contains("romance") || descLower.contains("romance")) {
            combined.add("A-Romance")
        }
        if (titleLower.contains("action") || descLower.contains("action")) {
            combined.add("A-Action")
        }
        return combined.distinct().sorted()
    }

    private fun embedMetadataIntoImage(imageFile: File, meta: EmbeddedMeta) {
        try {
            if (!imageFile.exists()) return
            val exif = ExifInterface(imageFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ARTIST, meta.source ?: "MAL")
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, meta.title)
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, meta.tags.joinToString(", "))
            if (!meta.description.isNullOrBlank()) {
                exif.setAttribute(ExifInterface.TAG_COPYRIGHT, meta.description.take(255))
            }
            exif.saveAttributes()
        } catch (_: Exception) {}
    }
}
