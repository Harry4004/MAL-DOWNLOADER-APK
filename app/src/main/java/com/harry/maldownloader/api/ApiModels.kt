package com.harry.maldownloader.api

// No need for @Serializable; we use standard data classes for Moshi/Gson/Retrofit

data class AnimeResponse(
    val data: AnimeData? = null
)

data class AnimeData(
    val mal_id: Int = 0,
    val title: String? = null,
    val title_english: String? = null,
    val title_japanese: String? = null,
    val type: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val score: Double? = null,
    val synopsis: String? = null,
    val year: Int? = null,
    val season: String? = null,
    val source: String? = null,
    val rating: String? = null,
    val images: AnimeImages? = null,
    val genres: List<Genre>? = null,
    val studios: List<Studio>? = null,
    val themes: List<Theme>? = null,
    val demographics: List<Demographic>? = null,
    val aired: Aired? = null,
    val duration: String? = null,
    val approved: Boolean? = null
)

data class AnimeImages(
    val jpg: ImageFormat? = null,
    val webp: ImageFormat? = null
)

data class ImageFormat(
    val image_url: String? = null,
    val small_image_url: String? = null,
    val large_image_url: String? = null
)

data class Genre(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class Studio(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class Theme(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class Demographic(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class Aired(
    val from: String? = null,
    val to: String? = null,
    val prop: AiredProp? = null,
    val string: String? = null
)

data class AiredProp(
    val from: DateProp? = null,
    val to: DateProp? = null
)

data class DateProp(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null
)

data class MangaResponse(
    val data: MangaData? = null
)

data class MangaData(
    val mal_id: Int = 0,
    val title: String? = null,
    val title_english: String? = null,
    val title_japanese: String? = null,
    val type: String? = null,
    val status: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val score: Double? = null,
    val synopsis: String? = null,
    val year: Int? = null,
    val images: AnimeImages? = null,
    val genres: List<Genre>? = null,
    val themes: List<Theme>? = null,
    val demographics: List<Demographic>? = null,
    val authors: List<Author>? = null,
    val serializations: List<Serialization>? = null,
    val published: Published? = null,
    val approved: Boolean? = null
)

data class Author(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class Serialization(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

data class Published(
    val from: String? = null,
    val to: String? = null,
    val prop: AiredProp? = null,
    val string: String? = null
)
