package com.harry.maldownloader

import com.harry.maldownloader.api.*
import com.harry.maldownloader.utils.retryWithBackoff
import retrofit2.Response

suspend fun MalApiService.safeAnime(id: Int, clientId: String, fields: String? = null): Response<MalAnimeResponse> =
    if (fields == null) getAnime(id) else getAnimeWithFields(id, fields, clientId)

suspend fun MalApiService.safeManga(id: Int, clientId: String, fields: String? = null): Response<MalMangaResponse> =
    if (fields == null) getManga(id) else getMangaWithFields(id, fields, clientId)

suspend fun JikanApiService.retryAnimeFull(id: Int): Response<AnimeResponse> =
    retryWithBackoff { getAnimeFull(id) }

suspend fun JikanApiService.retryMangaFull(id: Int): Response<MangaResponse> =
    retryWithBackoff { getMangaFull(id) }
