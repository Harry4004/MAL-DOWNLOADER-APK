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
import com.harry.maldownloader.utils.DownloadQueueManager
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

    // Enhanced storage manager for public Pictures directory
    private val storageManager by lazy {
        StorageManager(repository.context)
    }
    
    // Advanced download queue manager
    private val queueManager by lazy {
        DownloadQueueManager(repository.context)
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
    
    // Enhanced settings management
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()
    
    // Queue management states
    val queueState = queueManager.queueState
    val activeDownloads = queueManager.activeDownloads

    // Enhanced tag collections
    private val animeCustomTags = mutableSetOf<String>()
    private val mangaCustomTags = mutableSetOf<String>()
    private val hentaiCustomTags = mutableSetOf<String>()

    init {
        log("🚀 [v${BuildConfig.APP_VERSION}] MAL Downloader Enhanced - Pictures directory storage enabled")
        loadCustomTags()
        loadSettings()
        setupQueueManager()
        storageManager.cleanupTempFiles()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // TODO: Load from database
                val settings = AppSettings() // Default settings for now
                _appSettings.value = settings
                log("🔧 Settings loaded: ${settings.maxConcurrentDownloads} concurrent downloads")
            } catch (e: Exception) {
                log("⚠️ Could not load settings: ${e.message}")
            }
        }
    }
    
    private fun setupQueueManager() {
        queueManager.configure(
            settings = _appSettings.value,
            onComplete = { entry, path, error ->
                if (error != null) {
                    log("❌ Download failed: ${entry.title} - $error")
                } else {
                    log("✅ Download completed: ${entry.title}")
                }
                recordDownload(entry, entry.imageUrl ?: "", path, if (error != null) "failed" else "completed", error)
            },
            onLog = { message ->
                log(message)
            }
        )
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
            "enableBackgroundDownloads" -> current.copy(enableBackgroundDownloads = value as Boolean)
            "filenameFormat" -> current.copy(filenameFormat = value as String)
            "separateAdultContent" -> current.copy(separateAdultContent = value as Boolean)
            "embedXmpMetadata" -> current.copy(embedXmpMetadata = value as Boolean)
            "preferMalOverJikan" -> current.copy(preferMalOverJikan = value as Boolean)
            "apiDelayMs" -> current.copy(apiDelayMs = value as Long)
            "enableDetailedLogs" -> current.copy(enableDetailedLogs = value as Boolean)
            else -> current
        }
        
        _appSettings.value = updated
        
        // Reconfigure queue manager with new settings
        if (key in listOf("maxConcurrentDownloads", "downloadOnlyOnWifi", "pauseOnLowBattery")) {
            setupQueueManager()
        }
        
        log("⚙️ Setting updated: $key = $value")
        
        // TODO: Persist to database
        viewModelScope.launch {
            try {
                // repository.saveSettings(updated)
            } catch (e: Exception) {
                log("⚠️ Could not save setting: ${e.message}")
            }
        }
    }
    
    /**
     * Enhanced queue management functions
     */
    fun startDownloadQueue() {
        queueManager.startQueue()
        log("🚀 Download queue started")
    }
    
    fun pauseDownloadQueue() {
        queueManager.pauseQueue()
        log("⏸️ Download queue paused")
    }
    
    fun stopDownloadQueue() {
        queueManager.stopQueue()
        log("🛑 Download queue stopped")
    }
    
    fun retryFailedDownloads() {
        val failedDownloads = _downloads.value.filter { it.status == "failed" }
        val failedEntries = _animeEntries.value.filter { entry ->
            failedDownloads.any { it.malId == entry.malId.toString() }
        }
        
        if (failedEntries.isNotEmpty()) {
            queueManager.addToQueue(failedEntries)
            if (queueState.value == DownloadQueueManager.QueueState.IDLE) {
                startDownloadQueue()
            }
            log("🔄 Added ${failedEntries.size} failed downloads back to queue")
        }
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
                putExtra(Intent.EXTRA_SUBJECT, "MAL Downloader v${BuildConfig.APP_VERSION} Logs")
            }
            context.startActivity(Intent.createChooser(intent, "Share Logs"))
            log("📤 Sharing ${_logs.value.size} log entries")
        } catch (e: Exception) {
            log("❌ Failed to share logs: ${e.message}")
        }
    }
    
    /**
     * Enhanced entry management functions
     */
    fun refreshEntryMetadata(entry: AnimeEntry) {
        viewModelScope.launch {
            try {
                log("🔄 Refreshing metadata for: ${entry.title}")
                val enriched = enrichWithBestAvailableApi(entry)
                enriched?.let { newEntry ->
                    val list = _animeEntries.value.toMutableList()
                    val idx = list.indexOfFirst { it.malId == entry.malId }
                    if (idx != -1) {
                        list[idx] = newEntry
                        _animeEntries.value = list
                        log("✅ Metadata refreshed: ${newEntry.allTags.size} tags loaded")
                    }
                }
            } catch (e: Exception) {
                log("❌ Failed to refresh metadata: ${e.message}")
            }
        }
    }
    
    fun openMalPage(context: Context, entry: AnimeEntry) {
        try {
            val malUrl = "https://myanimelist.net/${entry.type}/${entry.malId}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(malUrl))
            context.startActivity(intent)
            log("🌐 Opening MAL page for: ${entry.title}")
        } catch (e: Exception) {
            log("❌ Could not open MAL page: ${e.message}")
        }
    }
    
    fun openJikanPage(context: Context, entry: AnimeEntry) {
        try {
            val jikanUrl = "https://jikan.moe/${entry.type}/${entry.malId}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(jikanUrl))
            context.startActivity(intent)
            log("🌐 Opening Jikan page for: ${entry.title}")
        } catch (e: Exception) {
            log("❌ Could not open Jikan page: ${e.message}")
        }
    }
    
    /**
     * Enhanced batch operations
     */
    fun downloadAllEntries() {
        val downloadableEntries = _animeEntries.value.filter { 
            !it.imageUrl.isNullOrEmpty() && it.imagePath.isNullOrEmpty()
        }
        
        if (downloadableEntries.isNotEmpty()) {
            queueManager.addToQueue(downloadableEntries)
            startDownloadQueue()
            log("📥 Added ${downloadableEntries.size} entries to download queue")
        } else {
            log("⚠️ No downloadable entries found")
        }
    }
    
    fun exportEntriesAsJson(context: Context) {
        viewModelScope.launch {
            try {
                val entriesJson = _animeEntries.value.take(10).joinToString(",\n") { entry ->
                    """
                    {
                        "malId": ${entry.malId},
                        "title": "${entry.title}",
                        "type": "${entry.type}",
                        "score": ${entry.score ?: "null"},
                        "tags": [${entry.allTags.joinToString(",") { "\"$it\"" }}],
                        "synopsis": "${entry.synopsis?.take(100)?.replace("\"", "\\\"")}..."
                    }
                    """.trimIndent()
                }
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, "[\n$entriesJson\n]")
                    putExtra(Intent.EXTRA_SUBJECT, "MAL Entries Export (First 10)")
                }
                context.startActivity(Intent.createChooser(intent, "Export Entries"))
                log("📤 Exported first 10 entries as JSON")
            } catch (e: Exception) {
                log("❌ Failed to export entries: ${e.message}")
            }
        }
    }
    
    /**
     * Enhanced statistics and diagnostics
     */
    fun generateDiagnosticReport(): String {
        val settings = _appSettings.value
        return buildString {
            appendLine("📊 MAL Downloader v${BuildConfig.APP_VERSION} Diagnostic Report")
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine()
            appendLine("📂 Entries: ${_animeEntries.value.size}")
            appendLine("⬇️ Downloads: ${_downloads.value.size}")
            appendLine("📋 Log Entries: ${_logs.value.size}")
            appendLine("🏷️ Custom Tags: ${_customTags.value.size}")
            appendLine()
            appendLine("🔧 Settings:")
            appendLine("  Concurrent Downloads: ${settings.maxConcurrentDownloads}")
            appendLine("  Wi-Fi Only: ${settings.downloadOnlyOnWifi}")
            appendLine("  Pause on Low Battery: ${settings.pauseOnLowBattery}")
            appendLine("  XMP Metadata: ${settings.embedXmpMetadata}")
            appendLine("  Filename Format: ${settings.filenameFormat}")
            appendLine()
            appendLine("📱 System:")
            appendLine("  Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("  Build Type: ${if (BuildConfig.DEBUG) "Debug" else "Release"}")
            appendLine("  Logging: ${if (BuildConfig.ENABLE_LOGGING) "Enabled" else "Disabled"}")
            appendLine()
            appendLine("📡 Storage:")
            appendLine("  External Storage: ${if (storageManager.isExternalStorageWritable()) "Available" else "Unavailable"}")
            appendLine("  Pictures Directory: ${storageManager.getDisplayPath("anime", false)}")
            
            val recentErrors = _logs.value.filter { it.contains("❌") || it.contains("💥") }.take(3)
            if (recentErrors.isNotEmpty()) {
                appendLine()
                appendLine("❌ Recent Errors:")
                recentErrors.forEach { error ->
                    appendLine("  $error")
                }
            }
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
        // Load comprehensive predefined tag sets for better organization
        animeCustomTags.addAll(listOf(
            "Action", "Adventure", "Comedy", "Drama", "Ecchi", "Fantasy", "Horror", "Mecha", 
            "Music", "Mystery", "Psychological", "Romance", "Sci-Fi", "Slice of Life", "Sports", 
            "Supernatural", "Thriller", "Military", "School", "Historical"
        ))
        
        mangaCustomTags.addAll(listOf(
            "Shounen", "Shoujo", "Seinen", "Josei", "Yaoi", "Yuri", "Oneshot", "Doujinshi", 
            "Manhwa", "Manhua", "Webtoon", "4-koma", "Award Winning", "Full Color"
        ))
        
        hentaiCustomTags.addAll(listOf(
            "Vanilla", "NTR", "Ahegao", "Bondage", "Tentacles", "Futanari", "Milf", 
            "Mind Break", "Netorare", "Yandere", "Tsundere", "Incest"
        ))
        
        val allTags = (animeCustomTags + mangaCustomTags + hentaiCustomTags).sorted()
        _customTags.value = allTags
        log("🏷️ Loaded ${allTags.size} predefined tags for enhanced categorization")
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
    
    /**
     * Enhanced log filtering
     */
    fun getFilteredLogs(filter: String): List<String> {
        return when (filter.uppercase()) {
            "ERROR" -> _logs.value.filter { it.contains("❌") || it.contains("💥") || it.contains("🚨") }
            "WARN" -> _logs.value.filter { it.contains("⚠️") || it.contains("🔶") }
            "INFO" -> _logs.value.filter { it.contains("✅") || it.contains("📊") || it.contains("🔍") || it.contains("🚀") }
            else -> _logs.value
        }
    }

    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 [v${BuildConfig.APP_VERSION}] Enhanced MAL processing started - saving to public Pictures directory")
            log("📱 Storage path: ${storageManager.getDisplayPath("anime", false)}")
            
            // Check storage availability
            if (!storageManager.isExternalStorageWritable()) {
                log("❌ External storage not available - downloads will fail")
                return
            }
            
            // Try persistent URI permission for file access
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                log("✅ Persistent URI permission acquired")
            } catch (e: Exception) {
                log("⚠️ Could not acquire persistent URI permission: ${e.message}")
            }
            
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
            log("📝 Successfully parsed ${entries.size} entries from MAL XML")
            
            if (entries.isEmpty()) {
                log("❌ No entries found in XML file")
                performXmlDiagnostics(context, uri)
                return
            }
            
            _animeEntries.value = entries
            log("📋 Loaded ${entries.size} entries into memory for processing")

            var successCount = 0
            var failCount = 0
            
            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    
                    // Update progress for UI feedback
                    _downloadProgress.value = _downloadProgress.value + (entry.malId to (index.toFloat() / entries.size))
                    
                    val enriched = enrichWithBestAvailableApi(entry)
                    enriched?.let { enrichedEntry ->
                        // Update entries list with enriched data
                        val list = _animeEntries.value.toMutableList()
                        val idx = list.indexOfFirst { it.malId == entry.malId }
                        if (idx != -1) {
                            list[idx] = enrichedEntry
                        } else {
                            list.add(enrichedEntry)
                        }
                        _animeEntries.value = list
                        
                        // Download with enhanced public directory support
                        val downloadSuccess = downloadToPublicPictures(enrichedEntry)
                        if (downloadSuccess) {
                            successCount++
                        } else {
                            failCount++
                        }
                    }
                    
                    delay(_appSettings.value.apiDelayMs) // Configurable API delay
                } catch (e: Exception) {
                    log("❌ Processing failed for ${entry.title}: ${e.message}")
                    failCount++
                }
            }
            
            log("🎉 Processing completed - Success: $successCount, Failed: $failCount")
            log("📏 Images saved to: Pictures/MAL_Images/ (visible in gallery apps)")
            
        } catch (e: Exception) {
            log("💥 Critical error during MAL processing: ${e.message}")
        } finally { 
            _isProcessing.value = false 
            _downloadProgress.value = emptyMap()
        }
    }
    
    private suspend fun performXmlDiagnostics(context: Context, uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val size = stream.available()
                if (size == 0) {
                    log("❌ File is empty (0 bytes)")
                } else {
                    log("📏 File size: ${size / 1024}KB - analyzing format...")
                    
                    val buffer = ByteArray(200)
                    val readBytes = stream.read(buffer)
                    val preview = String(buffer, 0, readBytes)
                    
                    when {
                        !preview.contains("<?xml") -> log("❌ File does not appear to be XML format")
                        !preview.contains("myanimelist", true) -> log("❌ File may not be a MyAnimeList export")
                        !preview.contains("<anime>") && !preview.contains("<manga>") -> 
                            log("❌ File does not contain anime or manga entries")
                        else -> log("✅ XML format appears valid - may be corrupted or incomplete")
                    }
                }
            }
        } catch (e: Exception) {
            log("❌ File diagnostic error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    private suspend fun enrichWithBestAvailableApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
        val settings = _appSettings.value
        
        // Try APIs based on user preference
        if (settings.preferMalOverJikan) {
            // Try MAL first, then Jikan
            tryMalApi(entry) ?: tryJikanApi(entry)
        } else {
            // Try Jikan first, then MAL
            tryJikanApi(entry) ?: tryMalApi(entry)
        }
    }
    
    private suspend fun tryMalApi(entry: AnimeEntry): AnimeEntry? {
        return runCatching {
            when (entry.type) {
                "anime" -> malApi.getAnime(entry.malId)
                "manga" -> malApi.getManga(entry.malId)
                else -> null
            }
        }.getOrNull()?.let { resp ->
            if (resp.isSuccessful) {
                when (entry.type) {
                    "anime" -> {
                        val animeResp = resp as retrofit2.Response<MalAnimeResponse>
                        animeResp.body()?.let { 
                            log("✅ MAL Official API enriched: ${entry.title} (premium data)")
                            mapFromMalAnime(entry, it) 
                        }
                    }
                    "manga" -> {
                        val mangaResp = resp as retrofit2.Response<MalMangaResponse>
                        mangaResp.body()?.let { 
                            log("✅ MAL Official API enriched: ${entry.title} (premium data)")
                            mapFromMalManga(entry, it) 
                        }
                    }
                    else -> null
                }
            } else {
                log("⚠️ MAL API returned ${resp.code()} for ${entry.title}")
                null
            }
        }
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
                        val animeResp = resp as retrofit2.Response<AnimeResponse>
                        animeResp.body()?.data?.let { 
                            log("✅ Jikan API enriched: ${entry.title} (comprehensive data)")
                            enrichAnimeEntry(entry, it) 
                        }
                    }
                    "manga" -> {
                        val mangaResp = resp as retrofit2.Response<MangaResponse>
                        mangaResp.body()?.data?.let { 
                            log("✅ Jikan API enriched: ${entry.title} (comprehensive data)")
                            enrichMangaEntry(entry, it) 
                        }
                    }
                    else -> null
                }
            } else {
                log("⚠️ Jikan API returned ${resp.code()} for ${entry.title}")
                null
            }
        }
    }

    private fun mapFromMalAnime(entry: AnimeEntry, mal: MalAnimeResponse): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        // Core identification tags
        tags.add("Anime")
        tags.add("MAL-${mal.id}")
        
        // Enhanced content metadata using correct field names from MalAnimeResponse
        mal.media_type?.let { tags.add("Type: $it") }
        mal.status?.let { tags.add("Status: $it") }
        mal.num_episodes?.let { if (it > 0) tags.add("Episodes: $it") }
        
        // Season and year information
        mal.start_season?.let { season ->
            season.season?.let { tags.add("Season: ${it.replaceFirstChar { char -> char.uppercase() }}") }
            season.year?.let { tags.add("Year: $it") }
        }
        
        // Genre processing with enhanced categorization
        mal.genres?.forEach { genre ->
            genre.name?.let { genreName ->
                tags.add(genreName)
                animeCustomTags.add(genreName)
            }
        }
        
        // Studio information
        mal.studios?.forEach { studio ->
            studio.name?.let { studioName ->
                tags.add("Studio: $studioName")
            }
        }
        
        // Enhanced NSFW detection
        val isHentai = (mal.nsfw ?: "").contains("hentai", true) || 
                      mal.genres?.any { it.name?.contains("hentai", true) == true } ?: false
                      
        if (isHentai) {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
        }
        
        return entry.copy(
            title = mal.title ?: entry.title,
            synopsis = mal.synopsis,
            score = mal.mean?.toFloat(),
            status = mal.status,
            episodes = mal.num_episodes,
            imageUrl = mal.main_picture?.large ?: mal.main_picture?.medium,
            allTags = tags.sorted(),
            genres = mal.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.sorted(),
            isHentai = isHentai
        )
    }

    private fun mapFromMalManga(entry: AnimeEntry, mal: MalMangaResponse): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        tags.add("Manga")
        tags.add("MAL-${mal.id}")
        
        mal.media_type?.let { tags.add("Type: $it") }
        mal.status?.let { tags.add("Status: $it") }
        mal.chapters?.let { if (it > 0) tags.add("Chapters: $it") }
        mal.volumes?.let { if (it > 0) tags.add("Volumes: $it") }
        
        mal.genres?.forEach { genre ->
            genre.name?.let { genreName ->
                tags.add(genreName)
                mangaCustomTags.add(genreName)
            }
        }
        
        val isHentai = (mal.nsfw ?: "").contains("hentai", true) ||
                      mal.genres?.any { it.name?.contains("hentai", true) == true } ?: false
                      
        if (isHentai) {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
        }
        
        return entry.copy(
            title = mal.title ?: entry.title,
            synopsis = mal.synopsis,
            score = mal.mean?.toFloat(),
            status = mal.status,
            chapters = mal.chapters,
            volumes = mal.volumes,
            imageUrl = mal.main_picture?.large ?: mal.main_picture?.medium,
            allTags = tags.sorted(),
            genres = mal.genres?.mapNotNull { it.name } ?: emptyList(),
            tags = tags.sorted(),
            isHentai = isHentai
        )
    }

    private fun enrichAnimeEntry(entry: AnimeEntry, data: AnimeData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        tags.add("Anime")
        tags.add("MAL-${data.mal_id}")
        
        // Comprehensive Jikan data extraction
        data.type?.let { tags.add("Type: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.rating?.let { tags.add("Rating: $it") }
        data.source?.let { tags.add("Source: $it") }
        data.season?.let { tags.add("Season: ${it.replaceFirstChar { char -> char.uppercase() }}") }
        data.year?.let { tags.add("Year: $it") }
        data.episodes?.let { if (it > 0) tags.add("Episodes: $it") }
        
        // Enhanced genre and content categorization
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                animeCustomTags.add(genreName)
            }
        }
        data.explicit_genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                hentaiCustomTags.add(genreName)
            }
        }
        data.themes?.forEach { theme -> theme.name?.let { tags.add("Theme: $it") } }
        data.demographics?.forEach { demo -> demo.name?.let { tags.add("Demo: $it") } }
        
        // Production information
        data.studios?.forEach { studio -> studio.name?.let { tags.add("Studio: $it") } }
        data.producers?.forEach { producer -> producer.name?.let { tags.add("Producer: $it") } }
        data.licensors?.forEach { licensor -> licensor.name?.let { tags.add("Licensor: $it") } }
        
        // Enhanced NSFW detection
        val isHentai = data.explicit_genres?.any { it.name?.contains("hentai", true) == true } ?: false ||
                      data.rating?.contains("hentai", true) ?: false ||
                      data.genres?.any { it.name?.contains("hentai", true) == true } ?: false ||
                      data.rating?.contains("rx", true) ?: false
        
        if (isHentai) {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
            tags.addAll(hentaiCustomTags.take(3))
        }
        
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
        data.published?.from?.let { tags.add("Published: $it") }
        
        // Enhanced genre processing
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                mangaCustomTags.add(genreName)
            }
        }
        data.explicit_genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                hentaiCustomTags.add(genreName)
            }
        }
        data.themes?.forEach { theme -> theme.name?.let { tags.add("Theme: $it") } }
        data.demographics?.forEach { demo -> demo.name?.let { tags.add("Demo: $it") } }
        
        // Author and publication information
        data.authors?.forEach { author -> author.name?.let { tags.add("Author: $it") } }
        data.serializations?.forEach { serialization -> 
            serialization.name?.let { tags.add("Magazine: $it") }
        }
        
        val isHentai = data.explicit_genres?.any { it.name?.contains("hentai", true) == true } ?: false ||
                      data.genres?.any { it.name?.contains("hentai", true) == true } ?: false
        
        if (isHentai) {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
            tags.addAll(hentaiCustomTags.take(3))
        }
        
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
                    log("📖 Reading and parsing MAL XML file...")
                    val parser = Xml.newPullParser()
                    parser.setInput(inputStream, null)
                    var eventType = parser.eventType
                    var currentType = ""
                    var malId = 0
                    var title = ""
                    var userTagsList = emptyList<String>()
                    var text = ""
                    var entryCount = 0
                    
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> when (parser.name?.lowercase()) {
                                "anime" -> { 
                                    currentType = "anime"
                                    malId = 0
                                    title = ""
                                    userTagsList = emptyList()
                                }
                                "manga" -> { 
                                    currentType = "manga"
                                    malId = 0
                                    title = ""
                                    userTagsList = emptyList()
                                }
                                "myanimelist" -> log("📄 Detected official MyAnimeList XML export format")
                            }
                            XmlPullParser.TEXT -> text = parser.text ?: ""
                            XmlPullParser.END_TAG -> when (parser.name?.lowercase()) {
                                "series_animedb_id", "manga_mangadb_id" -> { 
                                    malId = text.toIntOrNull() ?: 0 
                                }
                                "series_title", "manga_title" -> { title = text }
                                "my_tags" -> { 
                                    if (text.isNotEmpty()) {
                                        userTagsList = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    }
                                }
                                "anime", "manga" -> if (malId > 0 && title.isNotEmpty()) {
                                    entries.add(AnimeEntry(
                                        malId = malId,
                                        title = title,
                                        type = currentType,
                                        userTags = userTagsList
                                    ))
                                    entryCount++
                                    
                                    if (entryCount % 50 == 0) {
                                        log("📊 XML parsing progress: $entryCount entries processed...")
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    log("📈 XML parsing completed successfully: $entryCount total entries")
                }
            } catch (e: Exception) {
                log("❌ XML parsing error: ${e.javaClass.simpleName} - ${e.message}")
            }
            entries
        }
    }

    suspend fun downloadToPublicPictures(entry: AnimeEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageUrl = entry.imageUrl
            if (imageUrl.isNullOrEmpty()) {
                log("⚠️ No image URL available for ${entry.title}")
                return@withContext false
            }

            log("🌐 Downloading to Pictures directory: ${entry.title}")
            
            val settings = _appSettings.value
            val sanitizedTitle = entry.title
                .replace(Regex("[^a-zA-Z0-9._\\s-]"), "_")
                .replace(Regex("\\s+"), "_")
                .take(40)
            
            val extension = when {
                imageUrl.contains(".jpg", true) || imageUrl.contains("jpeg", true) -> "jpg"
                imageUrl.contains(".png", true) -> "png"
                imageUrl.contains(".webp", true) -> "webp"
                else -> "jpg"
            }
            
            // Apply user's filename format preference
            val filename = when (settings.filenameFormat) {
                "{title}_{id}.{ext}" -> "${sanitizedTitle}_${entry.malId}.$extension"
                "{id}_{title}.{ext}" -> "${entry.malId}_${sanitizedTitle}.$extension"
                "{title}.{ext}" -> "${sanitizedTitle}.$extension"
                else -> "${entry.malId}_${sanitizedTitle}.$extension"
            }
            
            // Check for duplicates if enabled
            if (settings.enableDuplicateDetection && 
                storageManager.fileExists(filename, entry.type, entry.isHentai)) {
                log("✅ Image already exists: $filename")
                val displayPath = storageManager.getDisplayPath(entry.type, entry.isHentai) + filename
                updateEntryWithPath(entry, displayPath)
                recordDownload(entry, imageUrl, displayPath, "completed")
                return@withContext true
            }

            // Enhanced download with retry logic
            var attempts = 0
            val maxAttempts = settings.retryAttempts
            
            while (attempts < maxAttempts) {
                try {
                    attempts++
                    log("🔄 Download attempt $attempts/$maxAttempts for ${entry.title}")
                    
                    val request = Request.Builder()
                        .url(imageUrl)
                        .addHeader("User-Agent", "MAL-Downloader-v${BuildConfig.APP_VERSION} (Android)")
                        .addHeader("Referer", "https://myanimelist.net/")
                        .addHeader("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                        .build()
                    
                    val response = downloadClient.newCall(request).execute()
                    
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                    
                    val contentLength = response.body?.contentLength() ?: 0
                    log("📦 Downloading ${entry.title}: ${contentLength / 1024}KB")
                    
                    response.body?.byteStream()?.use { inputStream ->
                        val savedPath = storageManager.saveImageToPublicDirectory(
                            inputStream = inputStream,
                            filename = filename,
                            contentType = entry.type,
                            isAdult = entry.isHentai && settings.separateAdultContent,
                            mimeType = when (extension) {
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                else -> "image/jpeg"
                            }
                        )
                        
                        if (savedPath != null) {
                            // Embed metadata if enabled
                            if (settings.embedXmpMetadata) {
                                embedEnhancedXmpMetadata(savedPath, entry)
                            }
                            
                            val fileSizeKB = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentLength / 1024
                            } else {
                                File(savedPath).length() / 1024
                            }
                            
                            log("✅ Image saved to Pictures: ${File(savedPath).name} (${fileSizeKB}KB)")
                            log("📏 Location: Pictures/MAL_Images/${entry.type.uppercase()}/${if (entry.isHentai && settings.separateAdultContent) "Adult" else "General"}/")
                            
                            updateEntryWithPath(entry, savedPath)
                            recordDownload(entry, imageUrl, savedPath, "completed")
                            return@withContext true
                        } else {
                            throw Exception("Failed to save to Pictures directory")
                        }
                    }
                    
                    break
                    
                } catch (e: Exception) {
                    log("❌ Download attempt $attempts failed for ${entry.title}: ${e.message}")
                    if (attempts >= maxAttempts) {
                        log("💀 All download attempts exhausted for ${entry.title}")
                        recordDownload(entry, imageUrl, null, "failed", e.message)
                        return@withContext false
                    } else {
                        delay(2000L * attempts)
                    }
                }
            }
            
            false
        } catch (e: Exception) {
            log("💥 Critical download error for ${entry.title}: ${e.message}")
            recordDownload(entry, entry.imageUrl ?: "", null, "failed", e.message)
            false
        }
    }

    private fun embedEnhancedXmpMetadata(filePath: String, entry: AnimeEntry) {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            
            val exif = ExifInterface(file.absolutePath)
            val settings = _appSettings.value
            
            // Basic EXIF metadata
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, 
                if (settings.includeSynopsis && !entry.synopsis.isNullOrEmpty()) {
                    entry.synopsis.take(settings.maxSynopsisLength)
                } else {
                    entry.title
                }
            )
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MAL-Downloader-v${BuildConfig.APP_VERSION}")
            exif.setAttribute(ExifInterface.TAG_ARTIST, entry.studio ?: "Unknown Studio")
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "MyAnimeList ID: ${entry.malId}")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Enhanced with ${entry.allTags.size} dynamic tags")
            
            // Enhanced XMP metadata
            val xmpData = buildEnhancedXmpMetadata(entry, settings)
            exif.setAttribute(ExifInterface.TAG_XMP, xmpData)
            
            exif.saveAttributes()
            log("🏷️ Enhanced XMP metadata embedded: ${entry.allTags.take(settings.maxTagsPerImage).size} tags")
            
        } catch (e: Exception) {
            log("⚠️ Metadata embedding failed for ${entry.title}: ${e.message}")
        }
    }
    
    private fun buildEnhancedXmpMetadata(entry: AnimeEntry, settings: AppSettings): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        val tagsToEmbed = if (settings.prioritizeUserTags && entry.userTags.isNotEmpty()) {
            (entry.userTags + entry.allTags).distinct().take(settings.maxTagsPerImage)
        } else {
            entry.allTags.take(settings.maxTagsPerImage)
        }
        
        return """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about="" 
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:xmp="http://ns.adobe.com/xap/1.0/">
                <dc:title>${entry.title}</dc:title>
                <dc:description>${if (settings.includeSynopsis && !entry.synopsis.isNullOrEmpty()) entry.synopsis.take(settings.maxSynopsisLength) else "MAL ID: ${entry.malId}"}</dc:description>
                <dc:subject>
                    <rdf:Bag>
                        ${tagsToEmbed.joinToString("") { "<rdf:li>$it</rdf:li>" }}
                    </rdf:Bag>
                </dc:subject>
                <dc:creator>${entry.studio ?: "MAL-Downloader-Enhanced"}</dc:creator>
                <dc:source>MyAnimeList ID: ${entry.malId}</dc:source>
                <dc:type>${entry.type.uppercase()}</dc:type>
                <dc:format>image/jpeg</dc:format>
                <xmp:Rating>${entry.score?.toInt() ?: 0}</xmp:Rating>
                <xmp:CreateDate>$timestamp</xmp:CreateDate>
                <xmp:ModifyDate>$timestamp</xmp:ModifyDate>
                <xmp:CreatorTool>MAL-Downloader-v${BuildConfig.APP_VERSION}</xmp:CreatorTool>
                <xmp:Label>${if (entry.isHentai) "Adult Content" else "General"}</xmp:Label>
            </rdf:Description>
            </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }

    private fun updateEntryWithPath(entry: AnimeEntry, path: String) {
        viewModelScope.launch {
            val list = _animeEntries.value.toMutableList()
            val idx = list.indexOfFirst { it.malId == entry.malId }
            if (idx != -1) {
                list[idx] = list[idx].copy(imagePath = path)
                _animeEntries.value = list
            }
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
                
                if (BuildConfig.ENABLE_LOGGING) {
                    Log.d("MAL-Enhanced", "Download recorded: ${entry.title} -> $status")
                }
            } catch (e: Exception) {
                log("⚠️ Failed to record download: ${e.message}")
            }
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) {
        downloadToPublicPictures(entry)
    }

    /**
     * Enhanced logging with filtering and management
     */
    fun log(message: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d("MAL-Enhanced", message)
        }
        
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                val logEntry = "[$timestamp] $message"
                val current = _logs.value.toMutableList()
                current.add(0, logEntry)
                val maxLogs = _appSettings.value.logRetentionCount
                _logs.value = current.take(maxLogs)
                
                try {
                    repository.logInfo("app", logEntry)
                } catch (e: Exception) {
                    if (BuildConfig.ENABLE_LOGGING) {
                        Log.w("MAL-Enhanced", "Failed to persist log: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.ENABLE_LOGGING) {
                    Log.e("MAL-Enhanced", "Critical logging error: ${e.message}")
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        queueManager.cleanup()
        log("🧹 MainViewModel cleaned up")
    }
}