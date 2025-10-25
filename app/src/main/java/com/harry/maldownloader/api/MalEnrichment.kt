package com.harry.maldownloader.api

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