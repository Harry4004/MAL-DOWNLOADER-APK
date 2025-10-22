package com.harry.maldownloader.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** Minimal MAL v2 endpoints used as primary source */
interface MalApiService {
    @GET("anime/{id}?fields=id,title,main_picture,synopsis,mean,rank,media_type,status,genres,studios,nsfw,num_episodes,start_season{year,season}")
    suspend fun getAnime(@Path("id") id: Int): Response<MalAnimeResponse>

    @GET("manga/{id}?fields=id,title,main_picture,synopsis,mean,rank,media_type,status,genres,authors{first_name,last_name},nsfw,chapters,volumes")
    suspend fun getManga(@Path("id") id: Int): Response<MalMangaResponse>
}

data class MalAnimeResponse(val id: Int, val title: String?, val synopsis: String?, val mean: Double?, val status: String?, val media_type: String?, val num_episodes: Int?, val start_season: MalSeason?, val main_picture: MalImage?, val genres: List<MalName>?, val studios: List<MalName>?, val nsfw: String?)

data class MalMangaResponse(val id: Int, val title: String?, val synopsis: String?, val mean: Double?, val status: String?, val media_type: String?, val chapters: Int?, val volumes: Int?, val main_picture: MalImage?, val genres: List<MalName>?, val nsfw: String?)

data class MalSeason(val year: Int?, val season: String?)

data class MalImage(val medium: String?, val large: String?)

data class MalName(val name: String?)
