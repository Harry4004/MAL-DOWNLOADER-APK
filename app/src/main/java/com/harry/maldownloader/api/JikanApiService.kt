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
}

data class AnimeResponse(
    val data: AnimeData?
)

data class MangaResponse(
    val data: MangaData?
)

data class AnimeData(
    val mal_id: Int,
    val title: String?,
    val title_english: String?,
    val title_japanese: String?,
    val images: Images?,
    val synopsis: String?,
    val score: Double?,
    val status: String?,
    val rating: String?,
    val source: String?,
    val type: String?,
    val episodes: Int?,
    val aired: Aired?,
    val season: String?,
    val year: Long?,
    val studios: List<Studio>?,
    val producers: List<Producer>?,
    val licensors: List<Licensor>?,
    val genres: List<Genre>?,
    val explicit_genres: List<Genre>?,
    val themes: List<Genre>?,
    val demographics: List<Genre>?,
    val relations: List<Relation>?,
    val external: List<External>?
)

data class MangaData(
    val mal_id: Int,
    val title: String?,
    val title_english: String?,
    val title_japanese: String?,
    val images: Images?,
    val synopsis: String?,
    val score: Double?,
    val status: String?,
    val type: String?,
    val chapters: Int?,
    val volumes: Int?,
    val published: Published?,
    val authors: List<Author>?,
    val serializations: List<Serialization>?,
    val genres: List<Genre>?,
    val explicit_genres: List<Genre>?,
    val themes: List<Genre>?,
    val demographics: List<Genre>?,
    val relations: List<Relation>?,
    val external: List<External>?
)

data class Images(
    val jpg: JpgImage?,
    val webp: WebpImage?
)

data class JpgImage(
    val image_url: String?,
    val small_image_url: String?,
    val large_image_url: String?
)

data class WebpImage(
    val image_url: String?,
    val small_image_url: String?,
    val large_image_url: String?
)

data class Aired(
    val from: String?,
    val to: String?,
    val prop: AiredProp?
)

data class Published(
    val from: String?,
    val to: String?,
    val prop: AiredProp?
)

data class AiredProp(
    val from: AiredDate?,
    val to: AiredDate?
)

data class AiredDate(
    val day: Int?,
    val month: Int?,
    val year: Int?
)

data class Studio(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Producer(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Licensor(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Author(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Serialization(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Genre(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Relation(
    val relation: String?,
    val entry: List<RelationEntry>?
)

data class RelationEntry(
    val mal_id: Int,
    val type: String?,
    val name: String?,
    val url: String?
)

data class External(
    val name: String?,
    val url: String?
)
