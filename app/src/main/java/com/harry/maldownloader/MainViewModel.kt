package com.harry.maldownloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Xml
import androidx.core.content.ContextCompat
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

    suspend fun processMalFile(context: Context, uri: Uri) {
        _isProcessing.value = true
        try {
            log("🚀 Starting MAL file processing with Client ID: ${MainApplication.MAL_CLIENT_ID.take(8)}...")
            val entries = withContext(Dispatchers.IO) { parseMalXml(context, uri) }
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
        // Try official MAL first (uses Client ID). Fallback to Jikan.
        runCatching {
            when (entry.type) {
                "anime" -> malApi.getAnime(entry.malId)
                "manga" -> malApi.getManga(entry.malId)
                else -> null
            }
        }.onSuccess { resp ->
            if (resp != null && resp.isSuccessful) {
                return@withContext when (entry.type) {
                    "anime" -> resp.body()?.let { mapFromMalAnime(entry, it) }
                    "manga" -> resp.body()?.let { mapFromMalManga(entry, it) }
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
                    "anime" -> resp.body()?.data?.let { enrichAnimeEntry(entry, it) }
                    "manga" -> resp.body()?.data?.let { enrichMangaEntry(entry, it) }
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
            imageUrl = mal.main_picture?.large ?: mal.main_picture?.medium,
            allTags = tags.toList(),
            genres = mal.genres?.mapNotNull { it.name } ?: emptyList(),
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
                            "anime", "manga" -> if (malId > 0 && title.isNotEmpty()) entries.add(AnimeEntry(malId, title, currentType, userTagsList))
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

    suspend fun downloadImages(entry: AnimeEntry) { /* unchanged body in your repo */ }
}

class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
