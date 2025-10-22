package com.harry.maldownloader

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import android.util.Xml
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
import org.xmlpull.v1.XmlPullParser
import retrofit2.create

class MainViewModel(private val repository: DownloadRepository) : ViewModel() {

    private val malApi by lazy {
        ApiClients.malRetrofit { MainApplication.MAL_CLIENT_ID }.create<MalApiService>()
    }
    private val jikanApi by lazy {
        ApiClients.jikanRetrofit().create<JikanApiService>()
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

    private val animeCustomTags = mutableSetOf<String>()
    private val mangaCustomTags = mutableSetOf<String>()
    private val hentaiCustomTags = mutableSetOf<String>()

    fun setNotificationPermission(granted: Boolean) { _notificationPermissionGranted.value = granted }
    fun setStoragePermission(granted: Boolean) { _storagePermissionGranted.value = granted }

    fun addCustomTag(tag: String) {
        val updated = _customTags.value.toMutableList().apply {
            if (!contains(tag)) add(tag)
        }
        _customTags.value = updated
    }

    fun removeCustomTag(tag: String) {
        val updated = _customTags.value.toMutableList().apply {
            remove(tag)
        }
        _customTags.value = updated
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 Starting MAL file processing with Client ID: ${MainApplication.MAL_CLIENT_ID.take(8)}...")
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
            log("📝 Parsed entries: ${entries.size}, URI: $uri")
            if (entries.isEmpty()) {
                log("❌ No entries found. The file content may be unreadable, not a valid MAL XML export, or permission was denied.")
                try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    log("❌ Could not open XML file: ${e.javaClass.simpleName} - ${e.localizedMessage}")
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
                        downloadImages(it)
                    }
                    delay(1200)
                } catch (e: Exception) {
                    log("❌ Failed ${entry.title}: ${e.message}")
                }
            }
            log("🎉 Completed")
        } finally { _isProcessing.value = false }
    }

    private suspend fun enrichWithBestAvailableApi(entry: AnimeEntry): AnimeEntry? = withContext(Dispatchers.IO) {
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
                        animeResp.body()?.let { mapFromMalAnime(entry, it) }
                    }
                    "manga" -> {
                        val mangaResp = resp as retrofit2.Response<MalMangaResponse>
                        mangaResp.body()?.let { mapFromMalManga(entry, it) }
                    }
                    else -> null
                }
            }
        }
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
                        animeResp.body()?.data?.let { enrichAnimeEntry(entry, it) }
                    }
                    "manga" -> {
                        val mangaResp = resp as retrofit2.Response<MangaResponse>
                        mangaResp.body()?.data?.let { enrichMangaEntry(entry, it) }
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
        
        // Add genres
        data.genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.explicit_genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.themes?.forEach { theme -> theme.name?.let { tags.add(it) } }
        data.demographics?.forEach { demo -> demo.name?.let { tags.add(it) } }
        
        // Add studios and producers
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
        
        // Add genres
        data.genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.explicit_genres?.forEach { genre -> genre.name?.let { tags.add(it) } }
        data.themes?.forEach { theme -> theme.name?.let { tags.add(it) } }
        data.demographics?.forEach { demo -> demo.name?.let { tags.add(it) } }
        
        // Add authors
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
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val parser = Xml.newPullParser()
                parser.setInput(inputStream, null)
                var eventType = parser.eventType
                var currentType = ""; var malId = 0; var title = ""; var userTagsList = emptyList<String>(); var text = ""
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> when (parser.name?.lowercase()) {
                            "anime" -> { currentType = "anime"; malId = 0; title = ""; userTagsList = emptyList() }
                            "manga" -> { currentType = "manga"; malId = 0; title = ""; userTagsList = emptyList() }
                        }
                        XmlPullParser.TEXT -> text = parser.text ?: ""
                        XmlPullParser.END_TAG -> when (parser.name?.lowercase()) {
                            "series_animedb_id", "manga_mangadb_id" -> { malId = text.toIntOrNull() ?: 0 }
                            "series_title", "manga_title" -> { title = text }
                            "my_tags" -> { if (text.isNotEmpty()) userTagsList = text.split(",").map { it.trim() } }
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
            entries
        }
    }

    fun log(message: String) {
        viewModelScope.launch {
            val current = _logs.value.toMutableList()
            current.add(0, message)
            _logs.value = current.take(1000)
            repository.logInfo("app", message)
        }
    }

    suspend fun downloadImages(entry: AnimeEntry) {
        log("📥 Starting download for: ${entry.title}")
        // TODO: Implement actual download logic here
    }
}
