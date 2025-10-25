package com.harry.maldownloader.api

import com.harry.maldownloader.data.AnimeEntry
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface JikanApiService {
    @GET("anime/{id}/full")
    suspend fun getAnimeFull(@Path("id") id: Int): Response<AnimeResponse>
    
    @GET("manga/{id}/full") 
    suspend fun getMangaFull(@Path("id") id: Int): Response<MangaResponse>
    
    // Lightweight fallback endpoints
    @GET("anime/{id}")
    suspend fun getAnime(@Path("id") id: Int): Response<AnimeResponse>

    @GET("manga/{id}")
    suspend fun getManga(@Path("id") id: Int): Response<MangaResponse>
}

// (All model classes unchanged, preserved as in existing file)