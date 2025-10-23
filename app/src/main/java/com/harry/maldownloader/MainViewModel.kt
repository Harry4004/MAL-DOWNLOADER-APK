package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.harry.maldownloader.api.*
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.data.DownloadItem
import com.harry.maldownloader.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import retrofit2.create
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(private val repository: DownloadRepository) : ViewModel() {

    private val malApi by lazy {
        ApiClients.malRetrofit { MainApplication.MAL_CLIENT_ID }.create<MalApiService>()
    }
    private val jikanApi by lazy {
        ApiClients.jikanRetrofit().create<JikanApiService>()
    }

    // Enhanced HTTP client for robust image downloading
    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    private val _storagePermissionGranted = MutableStateFlow(false)
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted.asStateFlow()

    private val _animeEntries = MutableStateFlow<List<AnimeEntry>>(emptyList())
    val animeEntries: StateFlow<List<AnimeEntry>> = _animeEntries.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _customTags = MutableStateFlow<List<String>>(emptyList())
    val customTags: StateFlow<List<String>> = _customTags.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress.asStateFlow()

    // Enhanced tag collections
    private val animeCustomTags = mutableSetOf<String>()
    private val mangaCustomTags = mutableSetOf<String>()
    private val hentaiCustomTags = mutableSetOf<String>()

    init {
        log("🚀 [v${BuildConfig.APP_VERSION}] MAL Downloader Enhanced initialized")
        loadCustomTags()
    }

    fun setNotificationPermission(granted: Boolean) { 
        _notificationPermissionGranted.value = granted
        log(if (granted) "✅ Notification permission granted" else "⚠️ Notification permission denied")
    }
    
    fun setStoragePermission(granted: Boolean) { 
        _storagePermissionGranted.value = granted 
        log(if (granted) "✅ Storage permission granted" else "❌ Storage permission denied")
    }

    private fun loadCustomTags() {
        animeCustomTags.addAll(listOf(
            "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy",
            "Horror", "Mecha", "Music", "Mystery", "Romance", "Sci-Fi"
        ))
        
        mangaCustomTags.addAll(listOf(
            "Shounen", "Shoujo", "Seinen", "Josei", "Oneshot", "Manhwa"
        ))
        
        hentaiCustomTags.addAll(listOf(
            "Vanilla", "NTR", "Ahegao", "Milf", "Tentacles", "Bondage"
        ))
        
        val allTags = (animeCustomTags + mangaCustomTags + hentaiCustomTags).toList()
        _customTags.value = allTags
    }

    fun addCustomTag(tag: String) {
        val updated = _customTags.value.toMutableList().apply {
            if (!contains(tag)) add(tag)
        }
        _customTags.value = updated
        log("✅ Added custom tag: $tag")
    }

    fun removeCustomTag(tag: String) {
        val updated = _customTags.value.toMutableList().apply {
            remove(tag)
        }
        _customTags.value = updated
        log("🗑️ Removed custom tag: $tag")
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log("🧹 Logs cleared")
    }

    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 [v${BuildConfig.APP_VERSION}] Enhanced MAL processing started")
            
            // Try persistent URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                log("✅ URI permission acquired")
            } catch (e: Exception) {
                log("⚠️ URI permission failed: ${e.message}")
            }
            
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
            log("📝 Parsed ${entries.size} entries from XML")
            
            if (entries.isEmpty()) {
                log("❌ No entries found - check file format and permissions")
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val size = stream.available()
                        log("📏 File accessible, size: $size bytes")
                    }
                } catch (e: Exception) {
                    log("❌ File access error: ${e.message}")
                }
                return
            }
            
            _animeEntries.value = entries

            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    
                    val enriched = enrichWithBestAvailableApi(entry)
                    enriched?.let {
                        val list = _animeEntries.value.toMutableList()
                        val idx = list.indexOfFirst { it.malId == entry.malId }
                        if (idx != -1) list[idx] = it else list.add(it)
                        _animeEntries.value = list
                        
                        // Enhanced download with metadata
                        downloadImagesWithMetadata(it)
                    }
                    delay(1200)
                } catch (e: Exception) {
                    log("❌ Failed ${entry.title}: ${e.message}")
                }
            }
            log("🎉 Processing completed successfully")
        } catch (e: Exception) {
            log("💥 Critical error: ${e.message}")
        } finally { 
            _isProcessing.value = false 
        }
    }

    private suspend fun enrichWithBestAvailableApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
        // Try MAL API first
        runCatching {
            when (entry.type) {
                "anime" -> malApi.getAnime(entry.malId)
                "manga" -> malApi.getManga(entry.malId)
                else -> null
            }
        }.onSuccess { resp ->
            if (resp != null && resp.isSuccessful) {
                return@withContext when (entry.type) {
                    "anime" -> {
                        val animeResp = resp as retrofit2.Response<MalAnimeResponse>
                        animeResp.body()?.let { 
                            log("✅ MAL API: ${entry.title}")
                            mapFromMalAnime(entry, it) 
                        }
                    }
                    "manga" -> {
                        val mangaResp = resp as retrofit2.Response<MalMangaResponse>
                        mangaResp.body()?.let { 
                            log("✅ MAL API: ${entry.title}")
                            mapFromMalManga(entry, it) 
                        }
                    }
                    else -> null
                }
            }
        }
        
        // Fallback to Jikan
        runCatching {
            when (entry.type) {
                "anime" -> jikanApi.getAnimeFull(entry.malId)
                "manga" -> jikanApi.getMangaFull(entry.malId)
                else -> null
            }
        }.onSuccess { resp ->
            if (resp != null && resp.isSuccessful) {
                return@withContext when (entry.type) {
                    "anime" -> {
                        val animeResp = resp as retrofit2.Response<AnimeResponse>
                        animeResp.body()?.data?.let { 
                            log("✅ Jikan API: ${entry.title}")
                            enrichAnimeEntry(entry, it) 
                        }
                    }
                    "manga" -> {
                        val mangaResp = resp as retrofit2.Response<MangaResponse>
                        mangaResp.body()?.data?.let { 
                            log("✅ Jikan API: ${entry.title}")
                            enrichMangaEntry(entry, it) 
                        }
                    }
                    else -> null
                }
            }
        }
        null
    }

    private fun mapFromMalAnime(entry: AnimeEntry, mal: MalAnimeResponse): AnimeEntry {
        val tags = mutableSetOf<String>()
        tags.add("Anime"); tags.add("MAL-${mal.id}")
        mal.media_type?.let { tags.add(it) }
        mal.status?.let { tags.add(it) }
        mal.genres?.mapNotNull { it.name }?.forEach { tags.add(it) }
        val isHentai = (mal.nsfw ?: "").contains("hentai", true)
        return entry.copy(
            title = mal.title ?: entry.title,
            synopsis = mal.synopsis,
            score = mal.mean?.toFloat(),
            status = mal.status,
            episodes = mal.num_episodes,
            imageUrl = mal.main_picture?.large ?: mal.main_picture?.medium,
            allTags = tags.toList(),
            genres = mal.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.toList(),
            isHentai = isHentai
        )
    }

    private fun mapFromMalManga(entry: AnimeEntry, mal: MalMangaResponse): AnimeEntry {
        val tags = mutableSetOf<String>()
        tags.add("Manga"); tags.add("MAL-${mal.id}")
        mal.media_type?.let { tags.add(it) }
        mal.status?.let { tags.add(it) }
        mal.genres?.mapNotNull { it.name }?.forEach { tags.add(it) }
        val isHentai = (mal.nsfw ?: "").contains("hentai", true)
        return entry.copy(
            title = mal.title ?: entry.title,
            synopsis = mal.synopsis,
            score = mal.mean?.toFloat(),
            status = mal.status,
            chapters = mal.chapters,
            volumes = mal.volumes,
            imageUrl = mal.main_picture?.large ?: mal.main_picture?.medium,
            allTags = tags.toList(),
            genres = mal.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.toList(),
            isHentai = isHentai
        )
    }

    private fun enrichAnimeEntry(entry: AnimeEntry, data: AnimeData): AnimeEntry {
        val tags = mutableSetOf<String>()
        tags.add("Anime")
        tags.add("MAL-${data.mal_id}")
        data.type?.let { tags.add(it) }
        data.status?.let { tags.add(it) }
        data.rating?.let { tags.add(it) }
        data.source?.let { tags.add(it) }
        data.season?.let { tags.add(it) }
        data.year?.let { tags.add(it.toString()) }
        
        data.genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.explicit_genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.themes?.forEach { theme -> theme.name?.let { tags.add(it) } }
        data.demographics?.forEach { demo -> demo.name?.let { tags.add(it) } }
        data.studios?.forEach { studio -> studio.name?.let { tags.add("Studio: $it") } }
        data.producers?.forEach { producer -> producer.name?.let { tags.add("Producer: $it") } }
        
        val isHentai = data.explicit_genres?.any { it.name?.contains("hentai", true) == true } ?: false ||
                      data.rating?.contains("hentai", true) ?: false
        
        return entry.copy(
            title = data.title ?: entry.title,
            englishTitle = data.title_english,
            japaneseTitle = data.title_japanese,
            synopsis = data.synopsis,
            score = data.score?.toFloat(),
            status = data.status,
            episodes = data.episodes,
            source = data.source,
            imageUrl = data.images?.jpg?.large_image_url ?: data.images?.jpg?.image_url,
            allTags = tags.toList(),
            genres = data.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.toList(),
            studio = data.studios?.firstOrNull()?.name,
            isHentai = isHentai
        )
    }

    private fun enrichMangaEntry(entry: AnimeEntry, data: MangaData): AnimeEntry {
        val tags = mutableSetOf<String>()
        tags.add("Manga")
        tags.add("MAL-${data.mal_id}")
        data.type?.let { tags.add(it) }
        data.status?.let { tags.add(it) }
        data.chapters?.let { tags.add("Chapters: $it") }
        data.volumes?.let { tags.add("Volumes: $it") }
        
        data.genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.explicit_genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.themes?.forEach { theme -> theme.name?.let { tags.add(it) } }
        data.demographics?.forEach { demo -> demo.name?.let { tags.add(it) } }
        data.authors?.forEach { author -> author.name?.let { tags.add("Author: $it") } }
        
        val isHentai = data.explicit_genres?.any { it.name?.contains("hentai", true) == true } ?: false
        
        return entry.copy(
            title = data.title ?: entry.title,
            englishTitle = data.title_english,
            japaneseTitle = data.title_japanese,
            synopsis = data.synopsis,
            score = data.score?.toFloat(),
            status = data.status,
            chapters = data.chapters,
            volumes = data.volumes,
            imageUrl = data.images?.jpg?.large_image_url ?: data.images?.jpg?.image_url,
            allTags = tags.toList(),
            genres = data.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.toList(),
            isHentai = isHentai
        )
    }

    private suspend fun parseMalXml(context: Context, uri: Uri): List<AnimeEntry> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<AnimeEntry>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    log("📖 Reading XML file...")
                    val parser = Xml.newPullParser()
                    parser.setInput(inputStream, null)
                    var eventType = parser.eventType
                    var currentType = ""
                    var malId = 0
                    var title = ""
                    var userTagsList = emptyList<String>()
                    var text = ""
                    
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> when (parser.name?.lowercase()) {
                                "anime" -> { currentType = "anime"; malId = 0; title = ""; userTagsList = emptyList() }
                                "manga" -> { currentType = "manga"; malId = 0; title = ""; userTagsList = emptyList() }
                                "myanimelist" -> log("📄 MAL XML format detected")
                            }
                            XmlPullParser.TEXT -> text = parser.text ?: ""
                            XmlPullParser.END_TAG -> when (parser.name?.lowercase()) {
                                "series_animedb_id", "manga_mangadb_id" -> { malId = text.toIntOrNull() ?: 0 }
                                "series_title", "manga_title" -> { title = text }
                                "my_tags" -> { 
                                    if (text.isNotEmpty()) {
                                        userTagsList = text.split(",").map { it.trim() }
                                    }
                                }
                                "anime", "manga" -> if (malId > 0 && title.isNotEmpty()) {
                                    entries.add(AnimeEntry(
                                        malId = malId,
                                        title = title,
                                        type = currentType,
                                        userTags = userTagsList
                                    ))
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
                log("📈 XML parsing completed")
            } catch (e: Exception) {
                log("❌ XML parsing error: ${e.message}")
            }
            entries
        }
    }

    suspend fun downloadImagesWithMetadata(entry: AnimeEntry) = withContext(Dispatchers.IO) {
        try {
            val imageUrl = entry.imageUrl
            if (imageUrl.isNullOrEmpty()) {
                log("⚠️ No image URL for ${entry.title}")
                return@withContext
            }

            log("🌐 Downloading: ${entry.title}")
            
            // Enhanced directory structure
            val baseDir = repository.context.getExternalFilesDir(null) ?: repository.context.filesDir
            val malDir = File(baseDir, "MAL_Images")
            val typeDir = File(malDir, entry.type.uppercase())
            val categoryDir = if (entry.isHentai) File(typeDir, "Adult") else File(typeDir, "General")
            
            if (!categoryDir.exists()) {
                categoryDir.mkdirs()
                log("📁 Created: ${categoryDir.path}")
            }

            val sanitizedTitle = entry.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(30)
            val extension = when {
                imageUrl.contains(".jpg", true) -> "jpg"
                imageUrl.contains(".png", true) -> "png"
                imageUrl.contains(".webp", true) -> "webp"
                else -> "jpg"
            }
            
            val targetFile = File(categoryDir, "${entry.malId}_${sanitizedTitle}.$extension")
            
            if (targetFile.exists()) {
                log("✅ Already exists: ${targetFile.name}")
                updateEntryWithPath(entry, targetFile.absolutePath)
                return@withContext
            }

            // Robust download with retry
            var attempts = 0
            val maxAttempts = 3
            
            while (attempts < maxAttempts) {
                try {
                    attempts++
                    log("🔄 Attempt $attempts/$maxAttempts")
                    
                    val request = Request.Builder()
                        .url(imageUrl)
                        .addHeader("User-Agent", "MAL-Downloader-v${BuildConfig.APP_VERSION}")
                        .addHeader("Referer", "https://myanimelist.net/")
                        .build()
                    
                    val response = downloadClient.newCall(request).execute()
                    
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}")
                    }
                    
                    val contentLength = response.body?.contentLength() ?: 0
                    log("📦 Downloading ${contentLength / 1024}KB")
                    
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Embed metadata
                    embedXmpMetadata(targetFile, entry)
                    
                    log("✅ Saved: ${targetFile.name} (${targetFile.length() / 1024}KB)")
                    updateEntryWithPath(entry, targetFile.absolutePath)
                    
                    // Record successful download
                    recordDownload(entry, imageUrl, targetFile.absolutePath, "completed")
                    break
                    
                } catch (e: Exception) {
                    log("❌ Attempt $attempts failed: ${e.message}")
                    if (attempts >= maxAttempts) {
                        log("💀 Download failed permanently: ${entry.title}")
                        recordDownload(entry, imageUrl, null, "failed", e.message)
                    } else {
                        delay(2000 * attempts)
                    }
                }
            }
        } catch (e: Exception) {
            log("💥 Critical download error: ${e.message}")
        }
    }

    private fun embedXmpMetadata(file: File, entry: AnimeEntry) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, entry.synopsis ?: entry.title)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MAL-Downloader-v${BuildConfig.APP_VERSION}")
            exif.setAttribute(ExifInterface.TAG_ARTIST, entry.studio ?: "Unknown")
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "MAL ID: ${entry.malId}")
            exif.saveAttributes()
            log("🏷️ Metadata embedded: ${entry.allTags.size} tags")
        } catch (e: Exception) {
            log("⚠️ Metadata failed: ${e.message}")
        }
    }
    
    private fun updateEntryWithPath(entry: AnimeEntry, path: String) {
        val list = _animeEntries.value.toMutableList()
        val idx = list.indexOfFirst { it.malId == entry.malId }
        if (idx != -1) {
            list[idx] = list[idx].copy(imagePath = path)
            _animeEntries.value = list
        }
    }
    
    private fun recordDownload(entry: AnimeEntry, url: String, path: String?, status: String, error: String? = null) {
        viewModelScope.launch {
            val downloadItem = DownloadItem(
                id = 0,
                entryId = entry.id,
                url = url,
                localPath = path,
                filename = path?.let { File(it).name },
                status = status,
                createdAt = System.currentTimeMillis(),
                completedAt = if (status == "completed") System.currentTimeMillis() else null,
                error = error
            )
            
            val currentDownloads = _downloads.value.toMutableList()
            currentDownloads.add(downloadItem)
            _downloads.value = currentDownloads
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) {
        downloadImagesWithMetadata(entry)
    }

    fun log(message: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d("MAL-Enhanced", message)
        }
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val logEntry = "[$timestamp] $message"
            val current = _logs.value.toMutableList()
            current.add(0, logEntry)
            _logs.value = current.take(500)
            
            try {
                repository.logInfo("app", logEntry)
            } catch (e: Exception) {
                // Ignore to prevent crashes
            }
        }
    }
}