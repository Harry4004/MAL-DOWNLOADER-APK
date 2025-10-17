package com.harry.maldownloader.api

import com.harry.maldownloader.data.AnimeEntry
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface JikanApiService {
    @GET("anime/{id}/full")
    suspend fun getAnimeFull(@Path("id") id: Int): Response<AnimeResponse>
}

data class AnimeResponse(
    val data: AnimeData?
)

data class AnimeData(
    val images: Images?,
    val title: String?  // For fallback if needed
)

data class Images(
    val jpg: JpgImage?
)

data class JpgImage(
    val image_url: String?
)
