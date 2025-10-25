package com.harry.maldownloader

import com.harry.maldownloader.api.MalApiService
import com.harry.maldownloader.api.fetchAnimeEnriched
import com.harry.maldownloader.api.fetchMangaEnriched

suspend fun MainViewModel.tryMalApi(entry: com.harry.maldownloader.data.AnimeEntry): com.harry.maldownloader.data.AnimeEntry? {
    return try {
        log("🌐 Attempting MAL API enrichment for: ${entry.title}")
        val clientId = MainApplication.MAL_CLIENT_ID
        val enriched = when (entry.type) {
            "anime" -> malApi.fetchAnimeEnriched(entry.malId, clientId)
            "manga" -> malApi.fetchMangaEnriched(entry.malId, clientId)
            else -> null
        }
        enriched?.let { (tags, synopsis) ->
            val merged = entry.copy(
                synopsis = synopsis ?: entry.synopsis,
                allTags = (entry.allTags + tags).distinct().sorted(),
                tags = (entry.tags + tags).distinct().sorted()
            )
            merged
        }
    } catch (e: Exception) {
        log("⚠️ MAL API failed for ${entry.title}: ${e.message}")
        null
    }
}
