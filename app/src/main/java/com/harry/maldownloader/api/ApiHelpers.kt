package com.harry.maldownloader.api

import com.harry.maldownloader.utils.retryWithBackoff
import retrofit2.Response

suspend fun JikanApiService.retryAnimeFull(id: Int): Response<AnimeResponse> =
    retryWithBackoff { getAnimeFull(id) }

suspend fun JikanApiService.retryMangaFull(id: Int): Response<MangaResponse> =
    retryWithBackoff { getMangaFull(id) }

suspend fun JikanApiService.retryAnime(id: Int): Response<AnimeResponse> =
    retryWithBackoff { getAnime(id) }

suspend fun JikanApiService.retryManga(id: Int): Response<MangaResponse> =
    retryWithBackoff { getManga(id) }