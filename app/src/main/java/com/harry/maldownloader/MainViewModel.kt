package com.harry.maldownloader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Xml
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.harry.maldownloader.api.*
import com.harry.maldownloader.data.AnimeEntry
import com.harry.maldownloader.data.AppSettings
import com.harry.maldownloader.data.DownloadItem
import com.harry.maldownloader.data.DownloadRepository
import com.harry.maldownloader.utils.StorageManager
import com.harry.maldownloader.utils.AppBuildInfo
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

    private val storageManager by lazy {
        StorageManager(repository.context)
    }

    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Core state flows
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
    
    // Settings management
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Enhanced tag collections
    private val animeCustomTags = mutableSetOf<String>()
    private val mangaCustomTags = mutableSetOf<String>()
    private val hentaiCustomTags = mutableSetOf<String>()

    init {
        log("🚀 [v${BuildConfig.VERSION_NAME}] MAL Downloader Enhanced - Pictures directory storage enabled")
        loadCustomTags()
        loadSettings()
        storageManager.cleanupTempFiles()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = AppSettings() // Default settings for now
                _appSettings.value = settings
                log("🔧 Settings loaded: ${settings.maxConcurrentDownloads} concurrent downloads")
            } catch (e: Exception) {
                log("⚠️ Could not load settings: ${e.message}")
            }
        }
    }
    
    /**
     * Enhanced settings management
     */
    fun updateSetting(key: String, value: Any) {
        val current = _appSettings.value
        val updated = when (key) {
            "maxConcurrentDownloads" -> current.copy(maxConcurrentDownloads = value as Int)
            "downloadOnlyOnWifi" -> current.copy(downloadOnlyOnWifi = value as Boolean)
            "pauseOnLowBattery" -> current.copy(pauseOnLowBattery = value as Boolean)
            "filenameFormat" -> current.copy(filenameFormat = value as String)
            "separateAdultContent" -> current.copy(separateAdultContent = value as Boolean)
            "embedXmpMetadata" -> current.copy(embedXmpMetadata = value as Boolean)
            "preferMalOverJikan" -> current.copy(preferMalOverJikan = value as Boolean)
            "enableDetailedLogs" -> current.copy(enableDetailedLogs = value as Boolean)
            else -> current
        }
        
        _appSettings.value = updated
        log("⚙️ Setting updated: $key = $value")
    }
    
    /**
     * Enhanced clipboard and sharing functions
     */
    fun copyLogsToClipboard(context: Context) {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val logsText = _logs.value.joinToString("\n")
            val clip = ClipData.newPlainText("MAL Downloader Logs", logsText)
            clipboardManager.setPrimaryClip(clip)
            log("📋 ${_logs.value.size} log entries copied to clipboard")
        } catch (e: Exception) {
            log("❌ Failed to copy logs: ${e.message}")
        }
    }
    
    fun shareLogsAsText(context: Context) {
        try {
            val logsText = _logs.value.joinToString("\n")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, logsText)
                putExtra(Intent.EXTRA_SUBJECT, "MAL Downloader v${BuildConfig.VERSION_NAME} Logs")
            }
            context.startActivity(Intent.createChooser(intent, "Share Logs"))
            log("📤 Sharing ${_logs.value.size} log entries")
        } catch (e: Exception) {
            log("❌ Failed to share logs: ${e.message}")
        }
    }

    fun setNotificationPermission(granted: Boolean) { 
        _notificationPermissionGranted.value = granted
        log(if (granted) "✅ Notification permission granted" else "⚠️ Notification permission denied")
    }
    
    fun setStoragePermission(granted: Boolean) { 
        _storagePermissionGranted.value = granted 
        val storageStatus = if (storageManager.isExternalStorageWritable()) "available" else "unavailable"
        log(if (granted) "✅ Storage permission granted - External storage $storageStatus" else "❌ Storage permission denied - Downloads will fail")
    }

    private fun loadCustomTags() {
        animeCustomTags.addAll(listOf(
            "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mecha", 
            "Music", "Mystery", "Romance", "Sci-Fi", "Sports", "Supernatural", "Thriller"
        ))
        
        mangaCustomTags.addAll(listOf(
            "Shounen", "Shoujo", "Seinen", "Josei", "Yaoi", "Yuri", "Oneshot", 
            "Manhwa", "Manhua", "Webtoon", "4-koma"
        ))
        
        hentaiCustomTags.addAll(listOf(
            "Adult", "NSFW", "18+", "Mature"
        ))
        
        val allTags = (animeCustomTags + mangaCustomTags + hentaiCustomTags).sorted()
        _customTags.value = allTags
        log("🏷️ Loaded ${allTags.size} predefined tags")
    }

    fun addCustomTag(tag: String) {
        val updated = _customTags.value.toMutableList().apply {
            if (!contains(tag)) {
                add(tag)
                sort()
            }
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
        log("🧹 Logs cleared by user")
    }

    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 [v${BuildConfig.VERSION_NAME}] Enhanced MAL processing started")
            
            if (!storageManager.isExternalStorageWritable()) {
                log("❌ External storage not available")
                return
            }
            
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
            log("📝 Successfully parsed ${entries.size} entries from MAL XML")
            
            if (entries.isEmpty()) {
                log("❌ No entries found in XML file")
                return
            }
            
            _animeEntries.value = entries
            
            var successCount = 0
            var failCount = 0
            
            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    
                    val enriched = enrichWithBestAvailableApi(entry)
                    enriched?.let { enrichedEntry ->
                        val list = _animeEntries.value.toMutableList()
                        val idx = list.indexOfFirst { it.malId == entry.malId }
                        if (idx != -1) {
                            list[idx] = enrichedEntry
                        }
                        _animeEntries.value = list
                        
                        val downloadSuccess = downloadToPublicPictures(enrichedEntry)
                        if (downloadSuccess) {
                            successCount++
                        } else {
                            failCount++
                        }
                    }
                    
                    delay(_appSettings.value.apiDelayMs)
                } catch (e: Exception) {
                    log("❌ Processing failed for ${entry.title}: ${e.message}")
                    failCount++
                }
            }
            
            log("🎉 Processing completed - Success: $successCount, Failed: $failCount")
            
        } catch (e: Exception) {
            log("💥 Critical error: ${e.message}")
        } finally { 
            _isProcessing.value = false 
        }
    }

    private suspend fun enrichWithBestAvailableApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
        return@withContext tryJikanApi(entry) ?: entry.copy(
            allTags = listOf("Anime", "MAL-${entry.malId}", entry.type.uppercase()),
            tags = listOf(entry.type.uppercase())
        )
    }
    
    private suspend fun tryJikanApi(entry: AnimeEntry): AnimeEntry? {
        return runCatching {
            when (entry.type) {
                "anime" -> jikanApi.getAnimeFull(entry.malId)
                "manga" -> jikanApi.getMangaFull(entry.malId)
                else -> null
            }
        }.getOrNull()?.let { resp ->
            if (resp.isSuccessful) {
                when (entry.type) {
                    "anime" -> {
                        @Suppress("UNCHECKED_CAST")
                        val animeResp = resp as retrofit2.Response<AnimeResponse>
                        animeResp.body()?.data?.let { 
                            log("✅ Jikan API enriched: ${entry.title}")
                            enrichAnimeEntry(entry, it) 
                        }
                    }
                    "manga" -> {
                        @Suppress("UNCHECKED_CAST")
                        val mangaResp = resp as retrofit2.Response<MangaResponse>
                        mangaResp.body()?.data?.let { 
                            log("✅ Jikan API enriched: ${entry.title}")
                            enrichMangaEntry(entry, it) 
                        }
                    }
                    else -> null
                }
            } else {
                null
            }
        }
    }

    private fun enrichAnimeEntry(entry: AnimeEntry, data: AnimeData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        tags.add("Anime")
        tags.add("MAL-${data.mal_id}")
        
        data.type?.let { tags.add("Type: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.rating?.let { tags.add("Rating: $it") }
        data.source?.let { tags.add("Source: $it") }
        data.season?.let { tags.add("Season: ${it.replaceFirstChar { char -> char.uppercase() }}") }
        data.year?.let { tags.add("Year: $it") }
        data.episodes?.let { if (it > 0) tags.add("Episodes: $it") }
        
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
            }
        }
        
        data.studios?.forEach { studio -> studio.name?.let { tags.add("Studio: $it") } }
        
        val isHentai = data.genres?.any { it.name?.contains("hentai", true) == true } ?: false
        
        if (isHentai) {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
        }
        
        return entry.copy(
            title = data.title ?: entry.title,
            englishTitle = data.title_english,
            synopsis = data.synopsis,
            score = data.score?.toFloat(),
            status = data.status,
            episodes = data.episodes,
            source = data.source,
            imageUrl = data.images?.jpg?.large_image_url ?: data.images?.jpg?.image_url,
            allTags = tags.sorted(),
            genres = data.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.sorted(),
            studio = data.studios?.firstOrNull()?.name,
            isHentai = isHentai
        )
    }

    private fun enrichMangaEntry(entry: AnimeEntry, data: MangaData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        tags.add("Manga")
        tags.add("MAL-${data.mal_id}")
        
        data.type?.let { tags.add("Type: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.chapters?.let { if (it > 0) tags.add("Chapters: $it") }
        data.volumes?.let { if (it > 0) tags.add("Volumes: $it") }
        
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
            }
        }
        
        data.authors?.forEach { author -> author.name?.let { tags.add("Author: $it") } }
        
        val isHentai = data.genres?.any { it.name?.contains("hentai", true) == true } ?: false
        
        if (isHentai) {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
        }
        
        return entry.copy(
            title = data.title ?: entry.title,
            englishTitle = data.title_english,
            synopsis = data.synopsis,
            score = data.score?.toFloat(),
            status = data.status,
            chapters = data.chapters,
            volumes = data.volumes,
            imageUrl = data.images?.jpg?.large_image_url ?: data.images?.jpg?.image_url,
            allTags = tags.sorted(),
            genres = data.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.sorted(),
            isHentai = isHentai
        )
    }

    private suspend fun parseMalXml(context: Context, uri: Uri): List<AnimeEntry> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<AnimeEntry>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    log("📖 Reading MAL XML file...")
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
                                "anime" -> { 
                                    currentType = "anime"
                                    malId = 0
                                    title = ""
                                }
                                "manga" -> { 
                                    currentType = "manga"
                                    malId = 0
                                    title = ""
                                }
                            }
                            XmlPullParser.TEXT -> text = parser.text ?: ""
                            XmlPullParser.END_TAG -> when (parser.name?.lowercase()) {
                                "series_animedb_id", "manga_mangadb_id" -> { 
                                    malId = text.toIntOrNull() ?: 0 
                                }
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
            } catch (e: Exception) {
                log("❌ XML parsing error: ${e.message}")
            }
            entries
        }
    }

    suspend fun downloadToPublicPictures(entry: AnimeEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageUrl = entry.imageUrl
            if (imageUrl.isNullOrEmpty()) {
                log("⚠️ No image URL for ${entry.title}")
                return@withContext false
            }

            log("🌐 Downloading: ${entry.title}")
            
            val sanitizedTitle = entry.title
                .replace(Regex("[^a-zA-Z0-9._\\s-]"), "_")
                .replace(Regex("\\s+"), "_")
                .take(40)
            
            val filename = "${entry.malId}_${sanitizedTitle}.jpg"
            
            if (storageManager.fileExists(filename, entry.type, entry.isHentai)) {
                log("✅ Image already exists: $filename")
                return@withContext true
            }

            val request = Request.Builder()
                .url(imageUrl)
                .addHeader("User-Agent", "MAL-Downloader-v${BuildConfig.VERSION_NAME}")
                .build()
            
            val response = downloadClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            
            response.body?.byteStream()?.use { inputStream ->
                val savedPath = storageManager.saveImageToPublicDirectory(
                    inputStream = inputStream,
                    filename = filename,
                    contentType = entry.type,
                    isAdult = entry.isHentai,
                    mimeType = "image/jpeg"
                )
                
                if (savedPath != null) {
                    embedMetadata(savedPath, entry)
                    log("✅ Downloaded: ${entry.title}")
                    recordDownload(entry, imageUrl, savedPath, "completed")
                    return@withContext true
                } else {
                    throw Exception("Save failed")
                }
            }
            
            false
            
        } catch (e: Exception) {
            log("❌ Download failed for ${entry.title}: ${e.message}")
            recordDownload(entry, entry.imageUrl ?: "", null, "failed", e.message)
            false
        }
    }

    private fun embedMetadata(filePath: String, entry: AnimeEntry) {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, entry.title)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MAL-Downloader-v${BuildConfig.VERSION_NAME}")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "MAL ID: ${entry.malId}")
            exif.saveAttributes()
            
            log("🏷️ Metadata embedded: ${entry.title}")
            
        } catch (e: Exception) {
            log("⚠️ Metadata failed: ${e.message}")
        }
    }
    
    private fun recordDownload(entry: AnimeEntry, url: String, path: String?, status: String, error: String? = null) {
        viewModelScope.launch {
            try {
                val downloadItem = DownloadItem(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    fileName = path?.let { File(it).name } ?: "unknown.jpg",
                    malId = entry.malId.toString(),
                    title = entry.title,
                    imageType = entry.type,
                    status = status,
                    progress = if (status == "completed") 100 else 0,
                    errorMessage = error,
                    createdAt = System.currentTimeMillis(),
                    completedAt = if (status == "completed") System.currentTimeMillis() else null
                )
                
                val currentDownloads = _downloads.value.toMutableList()
                currentDownloads.add(downloadItem)
                _downloads.value = currentDownloads
                
            } catch (e: Exception) {
                log("⚠️ Failed to record download: ${e.message}")
            }
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) {
        downloadToPublicPictures(entry)
    }

    fun log(message: String) {
        if (AppBuildInfo.ENABLE_LOGGING) {
            Log.d("MAL-Enhanced", message)
        }
        
        viewModelScope.launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val logEntry = "[$timestamp] $message"
            val current = _logs.value.toMutableList()
            current.add(0, logEntry)
            _logs.value = current.take(500) // Keep last 500 logs
        }
    }
}