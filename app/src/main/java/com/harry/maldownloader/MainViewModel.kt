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
    
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    // NEW: Icon and Font Scale StateFlows
    private val _iconScale = MutableStateFlow(1.0f)
    val iconScale: StateFlow<Float> = _iconScale.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val animeCustomTags = mutableSetOf<String>()
    private val mangaCustomTags = mutableSetOf<String>()
    private val hentaiCustomTags = mutableSetOf<String>()

    init {
        log("🚀 [v${BuildConfig.VERSION_NAME}] MAL Downloader Enhanced - Pictures directory storage enabled")
        loadCustomTags()
        loadSettings()
        loadScaleSettings()
        storageManager.cleanupTempFiles()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = AppSettings()
                _appSettings.value = settings
                log("🔧 Settings loaded: ${settings.maxConcurrentDownloads} concurrent downloads")
            } catch (e: Exception) {
                log("⚠️ Could not load settings: ${e.message}")
            }
        }
    }

    // NEW: Load scale settings from persistent storage
    private fun loadScaleSettings() {
        viewModelScope.launch {
            try {
                // TODO: Load from DataStore when persistence is implemented
                _iconScale.value = 1.0f // Default value
                _fontScale.value = 1.0f // Default value
                log("🎨 UI Scale loaded - Icon: ${_iconScale.value}, Font: ${_fontScale.value}")
            } catch (e: Exception) {
                log("⚠️ Could not load scale settings: ${e.message}")
            }
        }
    }

    // NEW: Set icon scale
    fun setIconScale(scale: Float) {
        val clampedScale = scale.coerceIn(0.85f, 1.30f)
        _iconScale.value = clampedScale
        log("🎨 Icon scale set to: $clampedScale")
        // TODO: Persist to DataStore
    }

    // NEW: Set font scale
    fun setFontScale(scale: Float) {
        val clampedScale = scale.coerceIn(0.90f, 1.30f)
        _fontScale.value = clampedScale
        log("🎨 Font scale set to: $clampedScale")
        // TODO: Persist to DataStore
    }
    
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
    
    fun resetSettingsToDefaults() { 
        _appSettings.value = AppSettings()
        _iconScale.value = 1.0f
        _fontScale.value = 1.0f
        log("🔄 Settings reset to defaults") 
    }
    
    fun copyLogsToClipboard(context: Context) {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val logsText = _logs.value.joinToString("\n")
            val clip = ClipData.newPlainText("MAL Downloader Logs", logsText)
            clipboardManager.setPrimaryClip(clip)
            log("📋 ${_logs.value.size} log entries copied to clipboard")
        } catch (e: Exception) { log("❌ Failed to copy logs: ${e.message}") }
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
        } catch (e: Exception) { log("❌ Failed to share logs: ${e.message}") }
    }
    
    fun generateSampleTagsFile() {
        viewModelScope.launch {
            try {
                val sampleXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <tags>
                        <!-- Custom Tags for MAL Downloader -->
                        <tag>Action RPG</tag>
                        <tag>Must Watch</tag>
                        <tag>Favorite Series</tag>
                        <tag>Completed</tag>
                        <tag>Recommended</tag>
                        <tag>Top Rated</tag>
                        <tag>Marathon Worthy</tag>
                        <tag>Emotional</tag>
                        <tag>Comedy Gold</tag>
                        <tag>Visual Masterpiece</tag>
                    </tags>
                """.trimIndent()
                val fileName = "sample_mal_custom_tags_${System.currentTimeMillis()}.xml"
                val savedPath = storageManager.saveSampleFile(fileName, sampleXml)
                if (savedPath != null) { log("📄 Sample tags file generated: $fileName"); log("📁 Saved to Downloads folder") } else { log("❌ Failed to generate sample tags file") }
            } catch (e: Exception) { log("❌ Error generating sample file: ${e.message}") }
        }
    }
    
    suspend fun processCustomTagsFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🏷️ Processing custom tags XML file...")
            val newTags = withContext(Dispatchers.IO) { parseCustomTagsXml(context, uri) }
            if (newTags.isNotEmpty()) {
                val current = _customTags.value.toMutableList()
                var added = 0
                newTags.forEach { tag -> if (!current.contains(tag)) { current.add(tag); added++ } }
                _customTags.value = current.sorted()
                log("✅ Successfully imported $added new custom tags")
                if (added == 0) log("📊 All tags from file were already present")
            } else { log("⚠️ No tags found in XML file") }
        } catch (e: Exception) { log("❌ Custom tags import failed: ${e.message}") } finally { _isProcessing.value = false }
    }
    
    private suspend fun parseCustomTagsXml(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val tags = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)
                var eventType = parser.eventType
                var text: String = ""
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.TEXT -> text = parser.text ?: ""
                        XmlPullParser.END_TAG -> if (parser.name?.lowercase() == "tag" && text.isNotBlank()) tags.add(text.trim())
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) { log("❌ Custom tags XML parsing error: ${e.message}") }
        tags.distinct()
    }

    fun setNotificationPermission(granted: Boolean) { _notificationPermissionGranted.value = granted; log(if (granted) "✅ Notification permission granted" else "⚠️ Notification permission denied") }
    fun setStoragePermission(granted: Boolean) { _storagePermissionGranted.value = granted; val storageStatus = if (storageManager.isExternalStorageWritable()) "available" else "unavailable"; log(if (granted) "✅ Storage permission granted - External storage $storageStatus" else "❌ Storage permission denied - Downloads will fail") }

    private fun loadCustomTags() {
        animeCustomTags.addAll(listOf("Action","Adventure","Comedy","Drama","Fantasy","Horror","Mecha","Music","Mystery","Romance","Sci-Fi","Sports","Supernatural","Thriller"))
        mangaCustomTags.addAll(listOf("Shounen","Shoujo","Seinen","Josei","Yaoi","Yuri","Oneshot","Manhwa","Manhua","Webtoon","4-koma"))
        hentaiCustomTags.addAll(listOf("Adult","NSFW","18+","Mature"))
        _customTags.value = (animeCustomTags + mangaCustomTags + hentaiCustomTags).sorted(); log("🏷️ Loaded ${_customTags.value.size} predefined tags")
    }

    fun addCustomTag(tag: String) { val u = _customTags.value.toMutableList(); if (!u.contains(tag)) { u.add(tag); u.sort() }; _customTags.value = u; log("✅ Added custom tag: $tag") }
    fun removeCustomTag(tag: String) { val u = _customTags.value.toMutableList(); u.remove(tag); _customTags.value = u; log("🗑️ Removed custom tag: $tag") }
    fun clearLogs() { _logs.value = emptyList(); log("🧹 Logs cleared by user") }

    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 [v${BuildConfig.VERSION_NAME}] Enhanced MAL processing started with dual-API tag enrichment")
            if (!storageManager.isExternalStorageWritable()) { log("❌ External storage not available"); return }
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
            log("📝 Successfully parsed ${entries.size} entries from MAL XML")
            if (entries.isEmpty()) { log("❌ No entries found in XML file"); return }
            _animeEntries.value = entries
            var success = 0; var fail = 0; var enrichedCount = 0
            entries.forEachIndexed { index, entry ->
                try {
                    log("🔍 Processing ${index + 1}/${entries.size}: ${entry.title}")
                    val enriched = enrichWithDualApi(entry)
                    enriched?.let { e ->
                        if (e.allTags.size > entry.allTags.size) enrichedCount++
                        val list = _animeEntries.value.toMutableList(); val idx = list.indexOfFirst { it.malId == entry.malId }; if (idx != -1) list[idx] = e; _animeEntries.value = list
                        if (downloadToPublicPictures(e)) success++ else fail++
                    }
                    delay(_appSettings.value.apiDelayMs)
                } catch (ex: Exception) { log("❌ Processing failed for ${entry.title}: ${ex.message}"); fail++ }
            }
            log("🎉 Processing completed - Success: $success, Failed: $fail, Tags enriched: $enrichedCount")
        } catch (e: Exception) { log("💥 Critical error: ${e.message}") } finally { _isProcessing.value = false }
    }

    private suspend fun enrichWithDualApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
        val preferMal = _appSettings.value.preferMalOverJikan
        val enriched = if (preferMal) { tryMalApi(entry) ?: tryJikanApi(entry) } else { tryJikanApi(entry) ?: tryMalApi(entry) }
        enriched ?: entry.copy(allTags = listOf("Anime", "MAL-${entry.malId}", entry.type.uppercase()), tags = listOf(entry.type.uppercase()))
    }

    private suspend fun tryMalApi(entry: AnimeEntry): AnimeEntry? = try {
        log("🌐 Attempting MAL API enrichment for: ${entry.title}")
        null // Placeholder until MAL client usage is configured
    } catch (e: Exception) { log("⚠️ MAL API failed for ${entry.title}: ${e.message}"); null }

    private suspend fun tryJikanApi(entry: AnimeEntry): AnimeEntry? = runCatching {
        log("🌐 Attempting Jikan API enrichment for: ${entry.title}")
        when (entry.type) { "anime" -> jikanApi.getAnimeFull(entry.malId); "manga" -> jikanApi.getMangaFull(entry.malId); else -> null }
    }.getOrNull()?.let { resp ->
        if (resp.isSuccessful) when (entry.type) {
            "anime" -> { @Suppress("UNCHECKED_CAST") val r = resp as retrofit2.Response<AnimeResponse>; r.body()?.data?.let { enrichAnimeEntry(entry, it) } }
            "manga" -> { @Suppress("UNCHECKED_CAST") val r = resp as retrofit2.Response<MangaResponse>; r.body()?.data?.let { enrichMangaEntry(entry, it) } }
            else -> null
        } else { log("❌ Jikan API response failed: ${resp.code()}"); null }
    }

    private fun enrichAnimeEntry(entry: AnimeEntry, data: AnimeData): AnimeEntry {
        val tags = mutableSetOf<String>()
        tags.add("Anime"); tags.add("MAL-${data.mal_id}"); tags.add("Type: ${data.type ?: "Unknown"}")
        data.type?.let { tags.add("Format: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.rating?.let { tags.add("Rating: $it") }
        data.source?.let { tags.add("Source: $it") }
        data.season?.let { tags.add("Season: ${it.replaceFirstChar { c -> c.uppercase() }}") }
        data.year?.let { tags.add("Year: $it") }
        data.episodes?.let { if (it > 0) tags.add("Episodes: $it") }
        data.genres?.forEach { g -> g.name?.let { n -> tags.add(n); tags.add("Genre: $n") } }
        data.studios?.forEach { s -> s.name?.let { n -> tags.add("Studio: $n"); tags.add(n) } }
        val isHentai = data.genres?.any { it.name?.contains("hentai", true) == true } ?: data.rating?.contains("Rx", true) ?: false
        if (isHentai) { tags.add("Adult Content"); tags.add("NSFW"); tags.add("18+") }
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
        tags.add("Manga"); tags.add("MAL-${data.mal_id}"); tags.add("Type: ${data.type ?: "Unknown"}")
        data.type?.let { tags.add("Format: $it") }
        data.status?.let { tags.add("Status: $it") }
        data.chapters?.let { if (it > 0) tags.add("Chapters: $it") }
        data.volumes?.let { if (it > 0) tags.add("Volumes: $it") }
        data.genres?.forEach { g -> g.name?.let { n -> tags.add(n); tags.add("Genre: $n") } }
        data.authors?.forEach { a -> a.name?.let { n -> tags.add("Author: $n"); tags.add(n) } }
        val isHentai = data.genres?.any { it.name?.contains("hentai", true) == true } ?: false
        if (isHentai) { tags.add("Adult Content"); tags.add("NSFW"); tags.add("18+") }
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

    private suspend fun parseMalXml(context: Context, uri: Uri): List<AnimeEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AnimeEntry>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val parser = Xml.newPullParser(); parser.setInput(inputStream, null)
                var eventType = parser.eventType; var currentType = ""; var malId = 0; var title = ""; var userTagsList = emptyList<String>(); var text = ""
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> when (parser.name?.lowercase()) { "anime" -> { currentType = "anime"; malId = 0; title = "" }; "manga" -> { currentType = "manga"; malId = 0; title = "" } }
                        XmlPullParser.TEXT -> text = parser.text ?: ""
                        XmlPullParser.END_TAG -> when (parser.name?.lowercase()) {
                            "series_animedb_id", "manga_mangadb_id" -> malId = text.toIntOrNull() ?: 0
                            "series_title", "manga_title" -> title = text
                            "my_tags" -> if (text.isNotEmpty()) userTagsList = text.split(",").map { it.trim() }
                            "anime", "manga" -> if (malId > 0 && title.isNotEmpty()) entries.add(AnimeEntry(malId = malId, title = title, type = currentType, userTags = userTagsList))
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) { log("❌ XML parsing error: ${e.message}") }
        entries
    }

    suspend fun downloadToPublicPictures(entry: AnimeEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val imageUrl = entry.imageUrl ?: return@withContext false.also { log("⚠️ No image URL for ${entry.title}") }
            log("🌐 Downloading: ${entry.title}")
            val sanitizedTitle = entry.title.replace(Regex("[^a-zA-Z0-9._\\s-]"), "_").replace(Regex("\\s+"), "_").take(40)
            val filename = "${entry.malId}_${sanitizedTitle}.jpg"
            if (storageManager.fileExists(filename, entry.type, entry.isHentai)) return@withContext true.also { log("✅ Image already exists: $filename") }
            val request = Request.Builder().url(imageUrl).addHeader("User-Agent", "MAL-Downloader-v${BuildConfig.VERSION_NAME}").build()
            val response = downloadClient.newCall(request).execute(); if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.byteStream()?.use { input ->
                val savedPath = storageManager.saveImageToPublicDirectory(input, filename, entry.type, entry.isHentai, "image/jpeg")
                if (savedPath != null) { embedEnhancedMetadata(savedPath, entry); log("✅ Downloaded with ${entry.allTags.size} tags: ${entry.title}"); recordDownload(entry, imageUrl, savedPath, "completed"); true } else { throw Exception("Save failed") }
            } ?: false
        } catch (e: Exception) { log("❌ Download failed for ${entry.title}: ${e.message}"); recordDownload(entry, entry.imageUrl ?: "", null, "failed", e.message); false }
    }

    private fun embedEnhancedMetadata(filePath: String, entry: AnimeEntry) {
        try {
            val file = File(filePath); if (!file.exists()) return
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, entry.title)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MAL-Downloader-v${BuildConfig.VERSION_NAME}")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "MAL ID: ${entry.malId}")
            if (_appSettings.value.embedXmpMetadata) exif.setAttribute(ExifInterface.TAG_XMP, buildXmpMetadata(entry))
            exif.saveAttributes(); log("🏷️ Enhanced metadata embedded (${entry.allTags.size} tags): ${entry.title}")
        } catch (e: Exception) { log("⚠️ Metadata embedding failed: ${e.message}") }
    }

    private fun buildXmpMetadata(entry: AnimeEntry): String = buildString {
        appendLine("<x:xmpmeta xmlns:x='adobe:ns:meta/'>"); appendLine("  <rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"); appendLine("    <rdf:Description rdf:about=''>"); appendLine("      <dc:title>${entry.title}</dc:title>"); appendLine("      <dc:description>${entry.synopsis ?: "MAL Entry"}</dc:description>"); appendLine("      <dc:subject>"); appendLine("        <rdf:Bag>"); entry.allTags.take(20).forEach { tag -> appendLine("          <rdf:li>$tag</rdf:li>") }; appendLine("        </rdf:Bag>"); appendLine("      </dc:subject>"); appendLine("    </rdf:Description>"); appendLine("  </rdf:RDF>"); appendLine("</x:xmpmeta>")
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
                val current = _downloads.value.toMutableList()
                current.add(downloadItem)
                _downloads.value = current
            } catch (e: Exception) {
                log("⚠️ Failed to record download: ${e.message}")
            }
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) { downloadToPublicPictures(entry) }

    fun log(message: String) {
        if (AppBuildInfo.ENABLE_LOGGING) Log.d("MAL-Enhanced", message)
        viewModelScope.launch { val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()); val logEntry = "[$timestamp] $message"; val current = _logs.value.toMutableList(); current.add(0, logEntry); _logs.value = current.take(500) }
    }
}