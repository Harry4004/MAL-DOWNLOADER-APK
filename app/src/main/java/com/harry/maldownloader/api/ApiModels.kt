package com.harry.maldownloader.api

// Jikan API response models
data class JikanAnimeResponse(
    val data: AnimeData?
)

data class JikanMangaResponse(
    val data: MangaData?
)

data class AnimeData(
    val mal_id: Int,
    val title: String?,
    val title_english: String?,
    val title_japanese: String?,
    val type: String?,
    val source: String?,
    val episodes: Int?,
    val status: String?,
    val airing: Boolean?,
    val aired: Aired?,
    val duration: String?,
    val rating: String?,
    val score: Double?,
    val scored_by: Int?,
    val rank: Int?,
    val popularity: Int?,
    val members: Int?,
    val favorites: Int?,
    val synopsis: String?,
    val background: String?,
    val season: String?,
    val year: Int?,
    val broadcast: Broadcast?,
    val producers: List<Producer>?,
    val licensors: List<Licensor>?,
    val studios: List<Studio>?,
    val genres: List<Genre>?,
    val explicit_genres: List<Genre>?,
    val themes: List<Theme>?,
    val demographics: List<Demographic>?,
    val relations: List<Relation>?,
    val theme: ThemeMusic?,
    val external: List<External>?,
    val streaming: List<Streaming>?,
    val images: Images?
)

data class MangaData(
    val mal_id: Int,
    val title: String?,
    val title_english: String?,
    val title_japanese: String?,
    val type: String?,
    val chapters: Int?,
    val volumes: Int?,
    val status: String?,
    val publishing: Boolean?,
    val published: Published?,
    val score: Double?,
    val scored_by: Int?,
    val rank: Int?,
    val popularity: Int?,
    val members: Int?,
    val favorites: Int?,
    val synopsis: String?,
    val background: String?,
    val authors: List<Author>?,
    val serializations: List<Serialization>?,
    val genres: List<Genre>?,
    val explicit_genres: List<Genre>?,
    val themes: List<Theme>?,
    val demographics: List<Demographic>?,
    val relations: List<Relation>?,
    val external: List<External>?,
    val images: Images?
)

data class Aired(
    val from: String?,
    val to: String?,
    val prop: AiredProp?
)

data class AiredProp(
    val from: DateProp?,
    val to: DateProp?
)

data class DateProp(
    val day: Int?,
    val month: Int?,
    val year: Int?
)

data class Published(
    val from: String?,
    val to: String?,
    val prop: PublishedProp?
)

data class PublishedProp(
    val from: DateProp?,
    val to: DateProp?
)

data class Broadcast(
    val day: String?,
    val time: String?,
    val timezone: String?,
    val string: String?
)

data class Producer(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Licensor(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Studio(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Author(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Serialization(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Genre(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Theme(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Demographic(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class Relation(
    val relation: String?,
    val entry: List<RelationEntry>?
)

data class RelationEntry(
    val mal_id: Int?,
    val type: String?,
    val name: String?,
    val url: String?
)

data class ThemeMusic(
    val openings: List<String>?,
    val endings: List<String>?
)

data class External(
    val name: String?,
    val url: String?
)

data class Streaming(
    val name: String?,
    val url: String?
)

data class Images(
    val jpg: ImageJpg?,
    val webp: ImageWebp?
)

data class ImageJpg(
    val image_url: String?,
    val small_image_url: String?,
    val large_image_url: String?
)

data class ImageWebp(
    val image_url: String?,
    val small_image_url: String?,
    val large_image_url: String?
)