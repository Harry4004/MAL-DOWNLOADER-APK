package com.harry.maldownloader

import com.harry.maldownloader.api.MalAnimeResponse
import com.harry.maldownloader.api.MalApiService
import com.harry.maldownloader.api.MalMangaResponse
import com.harry.maldownloader.api.toEntryTags
import retrofit2.Response

suspend fun MalApiService.fetchAnimeEnriched(id: Int, clientId: String): Pair<List<String>, String?>? {
    val resp: Response<MalAnimeResponse> = getAnime(id)
    if (!resp.isSuccessful) return null
    return resp.body()?.toEntryTags()
}

suspend fun MalApiService.fetchMangaEnriched(id: Int, clientId: String): Pair<List<String>, String?>? {
    val resp: Response<MalMangaResponse> = getManga(id)
    if (!resp.isSuccessful) return null
    return resp.body()?.toEntryTags()
}
