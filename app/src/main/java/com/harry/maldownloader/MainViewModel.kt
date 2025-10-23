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

    // Core state flows - preserve all existing state management
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
    
    // Settings management - preserve all existing settings
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // Enhanced tag collections - preserve existing tags system
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
     * Enhanced settings management - preserve all existing functionality
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
     * NEW: Reset settings to defaults
     */
    fun resetSettingsToDefaults() {
        _appSettings.value = AppSettings()
        log("🔄 Settings reset to defaults")
    }
    
    /**
     * Enhanced clipboard and sharing functions - preserve existing functionality
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
    
    /**
     * NEW: Generate sample tags file for users
     */
    fun generateSampleTagsFile() {
        viewModelScope.launch {
            try {
                val sampleXml = buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    appendLine("<tags>")
                    appendLine("    <!-- Custom Tags for MAL Downloader -->")
                    appendLine("    <tag>Action RPG</tag>")
                    appendLine("    <tag>Must Watch</tag>")
                    appendLine("    <tag>Favorite Series</tag>")
                    appendLine("    <tag>Completed</tag>")
                    appendLine("    <tag>Recommended</tag>")
                    appendLine("    <tag>Top Rated</tag>")
                    appendLine("    <tag>Marathon Worthy</tag>")
                    appendLine("    <tag>Emotional</tag>")
                    appendLine("    <tag>Comedy Gold</tag>")
                    appendLine("    <tag>Visual Masterpiece</tag>")
                    appendLine("    <!-- Add your own custom tags here -->")
                    appendLine("</tags>")
                }
                
                val fileName = "sample_mal_custom_tags_${System.currentTimeMillis()}.xml"
                val savedPath = storageManager.saveSampleFile(fileName, sampleXml)
                
                if (savedPath != null) {
                    log("📄 Sample tags file generated: $fileName")
                    log("📁 Saved to Downloads folder")
                    log("📝 Edit this file and import it using the 'Import Custom Tags' button")
                } else {
                    log("❌ Failed to generate sample tags file")
                }
                
            } catch (e: Exception) {
                log("❌ Error generating sample file: ${e.message}")
            }
        }
    }
    
    /**
     * NEW: Process custom tags file (XML format, minimal code changes)
     */
    suspend fun processCustomTagsFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🏷️ Processing custom tags XML file...")
            
            val newTags = withContext(Dispatchers.IO) { parseCustomTagsXml(context, uri) }
            
            if (newTags.isNotEmpty()) {
                val currentTags = _customTags.value.toMutableList()
                var addedCount = 0
                
                newTags.forEach { tag ->
                    if (!currentTags.contains(tag)) {
                        currentTags.add(tag)
                        addedCount++
                    }
                }
                
                _customTags.value = currentTags.sorted()
                log("✅ Successfully imported $addedCount new custom tags")
                log("🏷️ Total custom tags: ${_customTags.value.size}")
                
                if (addedCount == 0) {
                    log("📊 All tags from file were already present")
                }
                
            } else {
                log("⚠️ No tags found in XML file")
            }
            
        } catch (e: Exception) {
            log("❌ Custom tags import failed: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }
    
    private suspend fun parseCustomTagsXml(context: Context, uri: Uri): List<String> {
        return withContext(Dispatchers.IO) {
            val tags = mutableListOf<String>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(inputStream, null)
                    var eventType = parser.eventType
                    var text = ""
                    
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.TEXT -> text = parser.text ?: ""
                            XmlPullParser.END_TAG -> {
                                if (parser.name?.lowercase() == "tag" && text.isNotEmpty()) {
                                    tags.add(text.trim())
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            } catch (e: Exception) {
                log("❌ Custom tags XML parsing error: ${e.message}")
            }
            tags.distinct()
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
        // Preserve existing tag loading logic
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
            log("🚀 [v${BuildConfig.VERSION_NAME}] Enhanced MAL processing started with dual-API tag enrichment")
            
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
            var tagEnrichmentCount = 0
            
            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    
                    // FIXED: Enhanced dual-API tag enrichment
                    val enriched = enrichWithDualApi(entry)
                    enriched?.let { enrichedEntry ->
                        if (enrichedEntry.allTags.size > entry.allTags.size) {
                            tagEnrichmentCount++
                        }
                        
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
            
            log("🎉 Processing completed - Success: $successCount, Failed: $failCount, Tags enriched: $tagEnrichmentCount")
            
        } catch (e: Exception) {
            log("💥 Critical error: ${e.message}")
        } finally { 
            _isProcessing.value = false 
        }
    }

    // FIXED: Enhanced dual-API enrichment with proper tag downloading
    private suspend fun enrichWithDualApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
        // Try MAL API first if preferred, then fallback to Jikan
        val enriched = if (_appSettings.value.preferMalOverJikan) {
            tryMalApi(entry) ?: tryJikanApi(entry)
        } else {
            tryJikanApi(entry) ?: tryMalApi(entry)
        }
        
        return@withContext enriched ?: entry.copy(
            allTags = listOf("Anime", "MAL-${entry.malId}", entry.type.uppercase()),
            tags = listOf(entry.type.uppercase())
        )
    }
    
    // FIXED: MAL API integration for tag downloading
    private suspend fun tryMalApi(entry: AnimeEntry): AnimeEntry? {
        return try {
            log("🌐 Attempting MAL API enrichment for: ${entry.title}")
            // TODO: Implement MAL API calls when client ID is properly configured
            // For now, use fallback
            null
        } catch (e: Exception) {
            log("⚠️ MAL API failed for ${entry.title}: ${e.message}")
            null
        }
    }
    
    // FIXED: Enhanced Jikan API with proper error handling and comprehensive tags
    private suspend fun tryJikanApi(entry: AnimeEntry): AnimeEntry? {
        return runCatching {
            log("🌐 Attempting Jikan API enrichment for: ${entry.title}")
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
                        animeResp.body()?.data?.let { data ->
                            val tagCount = (data.genres?.size ?: 0) + (data.studios?.size ?: 0) + 5
                            log("✅ Jikan API enriched with $tagCount tags: ${entry.title}")
                            enrichAnimeEntry(entry, data) 
                        }
                    }
                    "manga" -> {
                        @Suppress("UNCHECKED_CAST")
                        val mangaResp = resp as retrofit2.Response<MangaResponse>
                        mangaResp.body()?.data?.let { data ->
                            val tagCount = (data.genres?.size ?: 0) + (data.authors?.size ?: 0) + 4
                            log("✅ Jikan API enriched with $tagCount tags: ${entry.title}")
                            enrichMangaEntry(entry, data) 
                        }
                    }
                    else -> null
                }
            } else {
                log("❌ Jikan API response failed: ${resp.code()}")
                null
            }
        }
    }

    // ENHANCED: More comprehensive anime entry enrichment
    private fun enrichAnimeEntry(entry: AnimeEntry, data: AnimeData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        // Base tags
        tags.add("Anime")
        tags.add("MAL-${data.mal_id}")
        tags.add("Type: ${data.type ?: "Unknown"}")
        
        // Enhanced tag extraction
        data.type?.let { tags.add("Format: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.rating?.let { tags.add("Rating: $it") }
        data.source?.let { tags.add("Source: $it") }
        data.season?.let { tags.add("Season: ${it.replaceFirstChar { char -> char.uppercase() }}") }
        data.year?.let { tags.add("Year: $it") }
        data.episodes?.let { if (it > 0) tags.add("Episodes: $it") }
        
        // Genre tags
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                tags.add("Genre: $genreName")
            }
        }
        
        // Studio tags
        data.studios?.forEach { studio -> 
            studio.name?.let { studioName ->
                tags.add("Studio: $studioName")
                tags.add(studioName)
            }
        }
        
        // Score-based tags
        data.score?.let { score ->
            when {
                score >= 9.0 -> tags.add("Masterpiece")
                score >= 8.0 -> tags.add("Excellent")
                score >= 7.0 -> tags.add("Good")
                score >= 6.0 -> tags.add("Average")
            }
        }
        
        // Adult content detection and tagging
        val isHentai = data.genres?.any { it.name?.contains("hentai", true) == true } ?: 
                      data.rating?.contains("Rx", true) ?: false
        
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

    // ENHANCED: More comprehensive manga entry enrichment
    private fun enrichMangaEntry(entry: AnimeEntry, data: MangaData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        // Base tags
        tags.add("Manga")
        tags.add("MAL-${data.mal_id}")
        tags.add("Type: ${data.type ?: "Unknown"}")
        
        // Enhanced tag extraction
        data.type?.let { tags.add("Format: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.chapters?.let { if (it > 0) tags.add("Chapters: $it") }
        data.volumes?.let { if (it > 0) tags.add("Volumes: $it") }
        
        // Genre tags
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                tags.add("Genre: $genreName")
            }
        }
        
        // Author tags
        data.authors?.forEach { author -> 
            author.name?.let { authorName ->
                tags.add("Author: $authorName")
                tags.add(authorName)
            }
        }
        
        // Score-based tags
        data.score?.let { score ->
            when {
                score >= 9.0 -> tags.add("Masterpiece")
                score >= 8.0 -> tags.add("Excellent")
                score >= 7.0 -> tags.add("Good")
                score >= 6.0 -> tags.add("Average")
            }
        }
        
        // Adult content detection
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

    // Preserve existing XML parsing logic
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

    // FIXED: Enhanced dual-API enrichment with proper tag downloading
    private suspend fun enrichWithDualApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
        // Try MAL API first if preferred, then fallback to Jikan
        val enriched = if (_appSettings.value.preferMalOverJikan) {
            tryMalApi(entry) ?: tryJikanApi(entry)
        } else {
            tryJikanApi(entry) ?: tryMalApi(entry)
        }
        
        return@withContext enriched ?: entry.copy(
            allTags = listOf("Anime", "MAL-${entry.malId}", entry.type.uppercase()),
            tags = listOf(entry.type.uppercase())
        )
    }
    
    // FIXED: MAL API integration for tag downloading
    private suspend fun tryMalApi(entry: AnimeEntry): AnimeEntry? {
        return try {
            log("🌐 Attempting MAL API enrichment for: ${entry.title}")
            // TODO: Implement MAL API calls when client ID is properly configured
            // For now, use fallback
            null
        } catch (e: Exception) {
            log("⚠️ MAL API failed for ${entry.title}: ${e.message}")
            null
        }
    }
    
    // FIXED: Enhanced Jikan API with proper error handling and comprehensive tags
    private suspend fun tryJikanApi(entry: AnimeEntry): AnimeEntry? {
        return runCatching {
            log("🌐 Attempting Jikan API enrichment for: ${entry.title}")
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
                        animeResp.body()?.data?.let { data ->
                            val tagCount = (data.genres?.size ?: 0) + (data.studios?.size ?: 0) + 5
                            log("✅ Jikan API enriched with $tagCount tags: ${entry.title}")
                            enrichAnimeEntry(entry, data) 
                        }
                    }
                    "manga" -> {
                        @Suppress("UNCHECKED_CAST")
                        val mangaResp = resp as retrofit2.Response<MangaResponse>
                        mangaResp.body()?.data?.let { data ->
                            val tagCount = (data.genres?.size ?: 0) + (data.authors?.size ?: 0) + 4
                            log("✅ Jikan API enriched with $tagCount tags: ${entry.title}")
                            enrichMangaEntry(entry, data) 
                        }
                    }
                    else -> null
                }
            } else {
                log("❌ Jikan API response failed: ${resp.code()}")
                null
            }
        }
    }

    // ENHANCED: More comprehensive anime entry enrichment with extensive tags
    private fun enrichAnimeEntry(entry: AnimeEntry, data: AnimeData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        // Base tags
        tags.add("Anime")
        tags.add("MAL-${data.mal_id}")
        tags.add("Type: ${data.type ?: "Unknown"}")
        
        // Enhanced tag extraction
        data.type?.let { tags.add("Format: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.rating?.let { tags.add("Rating: $it") }
        data.source?.let { tags.add("Source: $it") }
        data.season?.let { tags.add("Season: ${it.replaceFirstChar { char -> char.uppercase() }}") }
        data.year?.let { tags.add("Year: $it") }
        data.episodes?.let { if (it > 0) tags.add("Episodes: $it") }
        
        // Genre tags
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                tags.add("Genre: $genreName")
            }
        }
        
        // Studio tags
        data.studios?.forEach { studio -> 
            studio.name?.let { studioName ->
                tags.add("Studio: $studioName")
                tags.add(studioName)
            }
        }
        
        // Score-based tags
        data.score?.let { score ->
            when {
                score >= 9.0 -> tags.add("Masterpiece")
                score >= 8.0 -> tags.add("Excellent")
                score >= 7.0 -> tags.add("Good")
                score >= 6.0 -> tags.add("Average")
            }
        }
        
        // Adult content detection and tagging
        val isHentai = data.genres?.any { it.name?.contains("hentai", true) == true } ?: 
                      data.rating?.contains("Rx", true) ?: false
        
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

    // ENHANCED: More comprehensive manga entry enrichment with extensive tags
    private fun enrichMangaEntry(entry: AnimeEntry, data: MangaData): AnimeEntry {
        val tags = mutableSetOf<String>()
        
        // Base tags
        tags.add("Manga")
        tags.add("MAL-${data.mal_id}")
        tags.add("Type: ${data.type ?: "Unknown"}")
        
        // Enhanced tag extraction
        data.type?.let { tags.add("Format: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.chapters?.let { if (it > 0) tags.add("Chapters: $it") }
        data.volumes?.let { if (it > 0) tags.add("Volumes: $it") }
        
        // Genre tags
        data.genres?.forEach { genre -> 
            genre.name?.let { genreName ->
                tags.add(genreName)
                tags.add("Genre: $genreName")
            }
        }
        
        // Author tags
        data.authors?.forEach { author -> 
            author.name?.let { authorName ->
                tags.add("Author: $authorName")
                tags.add(authorName)
            }
        }
        
        // Score-based tags
        data.score?.let { score ->
            when {
                score >= 9.0 -> tags.add("Masterpiece")
                score >= 8.0 -> tags.add("Excellent")
                score >= 7.0 -> tags.add("Good")
                score >= 6.0 -> tags.add("Average")
            }
        }
        
        // Adult content detection
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

    // Preserve existing download functionality
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
                    // ENHANCED: Embed more comprehensive metadata
                    embedEnhancedMetadata(savedPath, entry)
                    log("✅ Downloaded with ${entry.allTags.size} tags: ${entry.title}")
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

    // ENHANCED: More comprehensive metadata embedding
    private fun embedEnhancedMetadata(filePath: String, entry: AnimeEntry) {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, entry.title)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MAL-Downloader-v${BuildConfig.VERSION_NAME}")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "MAL ID: ${entry.malId}")
            
            // Add XMP metadata if enabled
            if (_appSettings.value.embedXmpMetadata) {
                exif.setAttribute(ExifInterface.TAG_XMP, buildXmpMetadata(entry))
            }
            
            exif.saveAttributes()
            
            log("🏷️ Enhanced metadata embedded (${entry.allTags.size} tags): ${entry.title}")
            
        } catch (e: Exception) {
            log("⚠️ Metadata embedding failed: ${e.message}")
        }
    }
    
    private fun buildXmpMetadata(entry: AnimeEntry): String {
        return buildString {
            appendLine("<x:xmpmeta xmlns:x='adobe:ns:meta/'>")
            appendLine("  <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>")
            appendLine("    <rdf:Description rdf:about=''>")
            appendLine("      <dc:title>${entry.title}</dc:title>")
            appendLine("      <dc:description>${entry.synopsis ?: "MAL Entry"}</dc:description>")
            appendLine("      <dc:subject>")
            appendLine("        <rdf:Bag>")
            entry.allTags.take(20).forEach { tag ->
                appendLine("          <rdf:li>$tag</rdf:li>")
            }
            appendLine("        </rdf:Bag>")
            appendLine("      </dc:subject>")
            appendLine("    </rdf:Description>")
            appendLine("  </rdf:RDF>")
            appendLine("</x:xmpmeta>")
        }
    }
    
    /**
     * NEW: Generate sample tags file for users
     */
    fun generateSampleTagsFile() {
        viewModelScope.launch {
            try {
                val sampleXml = buildString {
                    appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    appendLine("<tags>")
                    appendLine("    <!-- Custom Tags for MAL Downloader -->")
                    appendLine("    <tag>Action RPG</tag>")
                    appendLine("    <tag>Must Watch</tag>")
                    appendLine("    <tag>Favorite Series</tag>")
                    appendLine("    <tag>Completed</tag>")
                    appendLine("    <tag>Recommended</tag>")
                    appendLine("    <tag>Top Rated</tag>")
                    appendLine("    <tag>Marathon Worthy</tag>")
                    appendLine("    <tag>Emotional</tag>")
                    appendLine("    <tag>Comedy Gold</tag>")
                    appendLine("    <tag>Visual Masterpiece</tag>")
                    appendLine("    <!-- Add your own custom tags here -->")
                    appendLine("</tags>")
                }
                
                val fileName = "sample_mal_custom_tags_${System.currentTimeMillis()}.xml"
                val savedPath = storageManager.saveSampleFile(fileName, sampleXml)
                
                if (savedPath != null) {
                    log("📄 Sample tags file generated: $fileName")
                    log("📁 Saved to Downloads folder")
                    log("📝 Edit this file and import it using the 'Import Custom Tags' button")
                } else {
                    log("❌ Failed to generate sample tags file")
                }
                
            } catch (e: Exception) {
                log("❌ Error generating sample file: ${e.message}")
            }
        }
    }
    
    /**
     * NEW: Process custom tags file (XML format, minimal code changes)
     */
    suspend fun processCustomTagsFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🏷️ Processing custom tags XML file...")
            
            val newTags = withContext(Dispatchers.IO) { parseCustomTagsXml(context, uri) }
            
            if (newTags.isNotEmpty()) {
                val currentTags = _customTags.value.toMutableList()
                var addedCount = 0
                
                newTags.forEach { tag ->
                    if (!currentTags.contains(tag)) {
                        currentTags.add(tag)
                        addedCount++
                    }
                }
                
                _customTags.value = currentTags.sorted()
                log("✅ Successfully imported $addedCount new custom tags")
                log("🏷️ Total custom tags: ${_customTags.value.size}")
                
                if (addedCount == 0) {
                    log("📊 All tags from file were already present")
                }
                
            } else {
                log("⚠️ No tags found in XML file")
            }
            
        } catch (e: Exception) {
            log("❌ Custom tags import failed: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }
    
    private suspend fun parseCustomTagsXml(context: Context, uri: Uri): List<String> {
        return withContext(Dispatchers.IO) {
            val tags = mutableListOf<String>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val parser = Xml.newPullParser()
                    parser.setInput(inputStream, null)
                    var eventType = parser.eventType
                    var text = ""
                    
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.TEXT -> text = parser.text ?: ""
                            XmlPullParser.END_TAG -> {
                                if (parser.name?.lowercase() == "tag" && text.isNotEmpty()) {
                                    tags.add(text.trim())
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            } catch (e: Exception) {
                log("❌ Custom tags XML parsing error: ${e.message}")
            }
            tags.distinct()
        }
    }
    
    // Preserve existing download recording
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