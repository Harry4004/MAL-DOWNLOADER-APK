package com.harry.maldownloader

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Xml
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harry.maldownloader.api.JikanApiService
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
import org.xmlpull.v1.XmlPullParser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel(private val repository: DownloadRepository) : AndroidViewModel(repository.context as Application) {

    // MAL Client ID
    companion object {
        private const val MAL_CLIENT_ID = "aaf018d4c098158bd890089f32125add"
        private const val JIKAN_BASE_URL = "https://api.jikan.moe/v4/"
    }

    // API Service
    private val jikanApiService: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(JIKAN_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JikanApiService::class.java)
    }

    // Permission state
    private val _notificationPermissionGranted = MutableLiveData(false)
    val notificationPermissionGranted: LiveData<Boolean> = _notificationPermissionGranted

    private val _storagePermissionGranted = MutableLiveData(false)
    val storagePermissionGranted: LiveData<Boolean> = _storagePermissionGranted

    // App state flows
    private val _animeEntries = MutableStateFlow<List<AnimeEntry>>(emptyList())
    val animeEntries: StateFlow<List<AnimeEntry>> = _animeEntries.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Custom tags management
    private val _customTags = MutableStateFlow<List<String>>(emptyList())
    val customTags: StateFlow<List<String>> = _customTags.asStateFlow()
    
    private val animeCustomTags = mutableSetOf<String>()
    private val mangaCustomTags = mutableSetOf<String>()
    private val hentaiCustomTags = mutableSetOf<String>()
    
    private val sharedPrefs = getApplication<Application>().getSharedPreferences("MALDownloaderPrefs", Context.MODE_PRIVATE)

    init {
        checkInitialPermissions()
        loadCustomTags()
        loadEntries()
        loadDownloads()
        loadLogs()
    }

    // Permission functions
    fun setNotificationPermission(granted: Boolean) {
        _notificationPermissionGranted.postValue(granted)
    }

    fun setStoragePermission(granted: Boolean) {
        _storagePermissionGranted.postValue(granted)
    }

    fun checkInitialPermissions() {
        val context = getApplication<Application>()
        val notificationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        _notificationPermissionGranted.postValue(notificationGranted)

        val storageGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        _storagePermissionGranted.postValue(storageGranted)
    }

    // Custom Tags Management
    private fun loadCustomTags() {
        animeCustomTags.clear()
        animeCustomTags.addAll(sharedPrefs.getStringSet("animeCustomTags", emptySet()) ?: emptySet())
        mangaCustomTags.clear()
        mangaCustomTags.addAll(sharedPrefs.getStringSet("mangaCustomTags", emptySet()) ?: emptySet())
        hentaiCustomTags.clear()
        hentaiCustomTags.addAll(sharedPrefs.getStringSet("hentaiCustomTags", emptySet()) ?: emptySet())
        updateCustomTagsFlow()
    }
    
    private fun saveCustomTags() {
        sharedPrefs.edit()
            .putStringSet("animeCustomTags", animeCustomTags)
            .putStringSet("mangaCustomTags", mangaCustomTags)
            .putStringSet("hentaiCustomTags", hentaiCustomTags)
            .apply()
        updateCustomTagsFlow()
    }
    
    private fun updateCustomTagsFlow() {
        val allTags = mutableListOf<String>()
        allTags.addAll(animeCustomTags.map { "Anime: $it" })
        allTags.addAll(mangaCustomTags.map { "Manga: $it" })
        allTags.addAll(hentaiCustomTags.map { "Hentai: $it" })
        _customTags.value = allTags
    }
    
    fun addCustomTag(type: String, tag: String) {
        when (type.lowercase()) {
            "anime" -> animeCustomTags.add(tag)
            "manga" -> mangaCustomTags.add(tag)
            "hentai" -> hentaiCustomTags.add(tag)
        }
        saveCustomTags()
    }
    
    fun removeCustomTag(fullTag: String) {
        when {
            fullTag.startsWith("Anime: ") -> animeCustomTags.remove(fullTag.removePrefix("Anime: "))
            fullTag.startsWith("Manga: ") -> mangaCustomTags.remove(fullTag.removePrefix("Manga: "))
            fullTag.startsWith("Hentai: ") -> hentaiCustomTags.remove(fullTag.removePrefix("Hentai: "))
        }
        saveCustomTags()
    }

    // Core app functions - IMPLEMENTED MAL XML PROCESSING
    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 Starting MAL file processing with Client ID: ${MAL_CLIENT_ID.take(8)}...")
            
            val entries = withContext(Dispatchers.IO) {
                parseMalXml(context, uri)
            }
            
            log("📄 Parsed ${entries.size} entries from MAL XML")
            _animeEntries.value = entries
            
            // Process each entry with API enrichment
            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    val enrichedEntry = enrichWithApiData(entry)
                    
                    if (enrichedEntry != null) {
                        // Update the entry in the list with enriched data
                        val updatedEntries = _animeEntries.value.toMutableList()
                        val entryIndex = updatedEntries.indexOfFirst { it.malId == entry.malId }
                        if (entryIndex != -1) {
                            updatedEntries[entryIndex] = enrichedEntry
                            _animeEntries.value = updatedEntries
                        }
                        
                        downloadImages(enrichedEntry)
                        delay(1500) // Rate limiting
                    }
                } catch (e: Exception) {
                    log("❌ Failed to process ${entry.title}: ${e.message}")
                }
            }
            
            log("🎉 MAL file processing completed")
        } catch (e: Exception) {
            log("❌ Error processing MAL file: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }

    private suspend fun parseMalXml(context: Context, uri: Uri): List<AnimeEntry> {
        return withContext(Dispatchers.IO) {
            val entries = mutableListOf<AnimeEntry>()
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)
                
                var eventType = parser.eventType
                var currentEntry: AnimeEntry? = null
                var text = ""
                
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name?.lowercase()) {
                                "anime" -> currentEntry = AnimeEntry(type = "anime")
                                "manga" -> currentEntry = AnimeEntry(type = "manga")
                                else -> {}
                            }
                        }
                        XmlPullParser.TEXT -> text = parser.text ?: ""
                        XmlPullParser.END_TAG -> {
                            currentEntry?.let { entry ->
                                when (parser.name?.lowercase()) {
                                    "series_animedb_id", "manga_mangadb_id" -> {
                                        entry.malId = text.toIntOrNull() ?: 0
                                    }
                                    "series_title", "manga_title" -> {
                                        entry.title = text
                                    }
                                    "my_tags" -> {
                                        if (text.isNotEmpty()) {
                                            entry.userTags = text.split(",").map { it.trim() }
                                        }
                                    }
                                    "anime", "manga" -> {
                                        if (entry.malId > 0 && entry.title.isNotEmpty()) {
                                            entries.add(entry)
                                        }
                                        currentEntry = null
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
            
            entries
        }
    }

    private suspend fun enrichWithApiData(entry: AnimeEntry): AnimeEntry? {
        return withContext(Dispatchers.IO) {
            try {
                when (entry.type) {
                    "anime" -> {
                        val response = jikanApiService.getAnimeFull(entry.malId)
                        if (response.isSuccessful) {
                            response.body()?.data?.let { data ->
                                enrichAnimeEntry(entry, data)
                            }
                        } else {
                            log("⚠️ API error for ${entry.title}: ${response.code()}")
                            null
                        }
                    }
                    "manga" -> {
                        val response = jikanApiService.getMangaFull(entry.malId)
                        if (response.isSuccessful) {
                            response.body()?.data?.let { data ->
                                enrichMangaEntry(entry, data)
                            }
                        } else {
                            log("⚠️ API error for ${entry.title}: ${response.code()}")
                            null
                        }
                    }
                    else -> null
                }
            } catch (e: Exception) {
                log("❌ API enrichment failed for ${entry.title}: ${e.message}")
                null
            }
        }
    }

    private fun enrichAnimeEntry(entry: AnimeEntry, data: com.harry.maldownloader.api.AnimeData): AnimeEntry {
        // Extract ALL possible tags (25+ dynamic tags)
        val allTags = mutableSetOf<String>()
        
        // Basic info tags
        allTags.add("Anime")
        allTags.add("MyAnimeList")
        allTags.add("MAL")
        allTags.add("MAL-${data.mal_id}")
        
        // Type and status
        data.type?.let { allTags.add(it) }
        data.status?.let { allTags.add(it.replace("_", " ")) }
        data.rating?.let { allTags.add(it) }
        data.source?.let { allTags.add("Source: $it") }
        
        // Season and year
        data.season?.let { season ->
            allTags.add("$season Season")
            data.year?.let { year -> allTags.add("$season $year") }
        }
        data.year?.let { allTags.add(it.toString()) }
        
        // Score-based quality tags
        data.score?.let { score ->
            when {
                score >= 9.0 -> allTags.add("Masterpiece")
                score >= 8.0 -> allTags.add("Great")
                score >= 7.0 -> allTags.add("Good")
                score >= 6.0 -> allTags.add("Fine")
                else -> allTags.add("Poor")
            }
        }
        
        // Genres (main content tags)
        data.genres?.forEach { genre ->
            genre.name?.let { allTags.add(it) }
        }
        
        // Explicit genres (important for hentai)
        data.explicit_genres?.forEach { genre ->
            genre.name?.let { allTags.add(it) }
        }
        
        // Themes
        data.themes?.forEach { theme ->
            theme.name?.let { allTags.add(it) }
        }
        
        // Demographics
        data.demographics?.forEach { demo ->
            demo.name?.let { allTags.add(it) }
        }
        
        // Studios
        data.studios?.forEach { studio ->
            studio.name?.let { allTags.add("Studio: $it") }
        }
        
        // Producers
        data.producers?.forEach { producer ->
            producer.name?.let { allTags.add("Producer: $it") }
        }
        
        // Detect hentai content
        val isHentai = allTags.any { it.contains("Hentai", true) }
        
        // User tags with prefixes (use loaded custom tags)
        val userCustomTags = when {
            isHentai -> hentaiCustomTags.map { "H-$it" }
            entry.type == "anime" -> animeCustomTags.map { "A-$it" }
            else -> emptyList()
        }
        allTags.addAll(userCustomTags)
        
        // Add user tags from XML
        entry.userTags.forEach { tag ->
            when {
                isHentai -> allTags.add("H-$tag")
                else -> allTags.add("A-$tag")
            }
        }
        
        // Content analysis tags from synopsis
        data.synopsis?.let { synopsis ->
            addContentTags(synopsis, allTags)
        }
        
        log("📊 Extracted ${allTags.size} tags for ${entry.title}")
        
        return entry.copy(
            title = data.title ?: entry.title,
            englishTitle = data.title_english,
            japaneseTitle = data.title_japanese,
            synopsis = data.synopsis,
            score = data.score?.toFloat(),
            status = data.status,
            episodes = data.episodes,
            imageUrl = data.images?.jpg?.large_image_url ?: data.images?.jpg?.image_url,
            allTags = allTags.toList(),
            isHentai = isHentai
        )
    }

    private fun enrichMangaEntry(entry: AnimeEntry, data: com.harry.maldownloader.api.MangaData): AnimeEntry {
        // Extract ALL possible tags (25+ dynamic tags)
        val allTags = mutableSetOf<String>()
        
        // Basic info tags
        allTags.add("Manga")
        allTags.add("MyAnimeList")
        allTags.add("MAL")
        allTags.add("MAL-${data.mal_id}")
        
        // Type and status
        data.type?.let { allTags.add(it) }
        data.status?.let { allTags.add(it.replace("_", " ")) }
        
        // Score-based quality tags
        data.score?.let { score ->
            when {
                score >= 9.0 -> allTags.add("Masterpiece")
                score >= 8.0 -> allTags.add("Great")
                score >= 7.0 -> allTags.add("Good")
                score >= 6.0 -> allTags.add("Fine")
                else -> allTags.add("Poor")
            }
        }
        
        // Genres (main content tags)
        data.genres?.forEach { genre ->
            genre.name?.let { allTags.add(it) }
        }
        
        // Explicit genres (important for hentai)
        data.explicit_genres?.forEach { genre ->
            genre.name?.let { allTags.add(it) }
        }
        
        // Themes
        data.themes?.forEach { theme ->
            theme.name?.let { allTags.add(it) }
        }
        
        // Demographics
        data.demographics?.forEach { demo ->
            demo.name?.let { allTags.add(it) }
        }
        
        // Authors
        data.authors?.forEach { author ->
            author.name?.let { allTags.add("Author: $it") }
        }
        
        // Serializations
        data.serializations?.forEach { serialization ->
            serialization.name?.let { allTags.add("Published: $it") }
        }
        
        // Detect hentai content
        val isHentai = allTags.any { it.contains("Hentai", true) }
        
        // User tags with prefixes (use loaded custom tags)
        val userCustomTags = when {
            isHentai -> hentaiCustomTags.map { "H-$it" }
            entry.type == "manga" -> mangaCustomTags.map { "M-$it" }
            else -> emptyList()
        }
        allTags.addAll(userCustomTags)
        
        // Add user tags from XML
        entry.userTags.forEach { tag ->
            when {
                isHentai -> allTags.add("H-$tag")
                else -> allTags.add("M-$tag")
            }
        }
        
        // Content analysis tags from synopsis
        data.synopsis?.let { synopsis ->
            addContentTags(synopsis, allTags)
        }
        
        log("📊 Extracted ${allTags.size} tags for ${entry.title}")
        
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
            allTags = allTags.toList(),
            isHentai = isHentai
        )
    }

    private fun addContentTags(synopsis: String, tags: MutableSet<String>) {
        // Add content-based tags from synopsis analysis
        val contentKeywords = mapOf(
            "school" to "School Life",
            "romance" to "Romance",
            "comedy" to "Comedy", 
            "drama" to "Drama",
            "action" to "Action",
            "fantasy" to "Fantasy",
            "magic" to "Magic",
            "supernatural" to "Supernatural",
            "mystery" to "Mystery",
            "horror" to "Horror",
            "slice of life" to "Slice of Life",
            "ecchi" to "Ecchi",
            "harem" to "Harem",
            "sports" to "Sports",
            "music" to "Music",
            "mecha" to "Mecha",
            "military" to "Military",
            "psychological" to "Psychological",
            "thriller" to "Thriller",
            "adventure" to "Adventure",
            "ntr" to "NTR",
            "netorare" to "Netorare",
            "yuri" to "Yuri",
            "yaoi" to "Yaoi",
            "shota" to "Shota",
            "loli" to "Loli"
        )
        
        val synopsisLower = synopsis.lowercase()
        contentKeywords.forEach { (keyword, tag) ->
            if (synopsisLower.contains(keyword)) {
                tags.add(tag)
            }
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                log("⬇️ Starting image download for ${entry.title}")
                
                entry.imageUrl?.let { imageUrl ->
                    val fileName = "${sanitizeFileName(entry.title)}_${entry.malId}.jpg"
                    
                    // Queue download with comprehensive metadata
                    repository.queueDownloadWithMetadata(
                        url = imageUrl,
                        fileName = fileName,
                        entry = entry
                    )
                    
                    log("✅ Queued download: $fileName with ${entry.allTags.size} tags")
                } ?: run {
                    log("⚠️ No image URL for ${entry.title}")
                }
            } catch (e: Exception) {
                log("❌ Error downloading images: ${e.message}")
            }
        }
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("[^\\w\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(50)
    }

    suspend fun updateEntryTags(entry: AnimeEntry, tags: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                log("🏷️ Updating tags for ${entry.title}")
                val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val updatedEntry = entry.copy(userTags = tagList)
                
                // Update in list
                val currentEntries = _animeEntries.value.toMutableList()
                val index = currentEntries.indexOfFirst { it.malId == entry.malId }
                if (index != -1) {
                    currentEntries[index] = updatedEntry
                    _animeEntries.value = currentEntries
                }
                
                log("✅ Tags updated for ${entry.title}")
            } catch (e: Exception) {
                log("❌ Error updating tags: ${e.message}")
            }
        }
    }

    // Download management
    suspend fun pauseDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.pauseDownload(id)
            loadDownloads()
        }
    }

    suspend fun resumeDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resumeDownload(id)
            loadDownloads()
        }
    }

    suspend fun cancelDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cancelDownload(id)
            loadDownloads()
        }
    }

    suspend fun retryDownload(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.retryDownload(id)
            loadDownloads()
        }
    }

    // Data loading
    private fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Entries loaded from XML processing
                val entries = emptyList<AnimeEntry>()
                _animeEntries.value = entries
            } catch (e: Exception) {
                log("❌ Error loading entries: ${e.message}")
            }
        }
    }

    private fun loadDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloads = repository.getAllDownloads()
                _downloads.value = downloads
            } catch (e: Exception) {
                log("❌ Error loading downloads: ${e.message}")
            }
        }
    }

    private fun loadLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logs = repository.getRecentLogs()
                _logs.value = logs.map { "${it.timestamp}: [${it.level}] ${it.message}" }
            } catch (e: Exception) {
                log("❌ Error loading logs: ${e.message}")
            }
        }
    }

    // Logging
    fun log(message: String) {
        viewModelScope.launch {
            val currentLogs = _logs.value.toMutableList()
            currentLogs.add(0, "${System.currentTimeMillis() % 100000}: $message")
            if (currentLogs.size > 1000) {
                currentLogs.removeAt(currentLogs.size - 1)
            }
            _logs.value = currentLogs
            
            // Also log to repository
            repository.logInfo("app", message)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}