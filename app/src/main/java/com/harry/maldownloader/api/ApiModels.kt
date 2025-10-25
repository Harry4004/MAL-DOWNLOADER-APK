package com.harry.maldownloader.api

import kotlinx.serialization.Serializable

/**
 * Jikan API Response Models for MAL Downloader
 * These models define the structure of responses from the Jikan API
 */

// Anime Response Models
@Serializable
data class AnimeResponse(
    val data: AnimeData? = null
)

@Serializable
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

@Serializable
data class AnimeImages(
    val jpg: ImageFormat? = null,
    val webp: ImageFormat? = null
)

@Serializable
data class ImageFormat(
    val image_url: String? = null,
    val small_image_url: String? = null,
    val large_image_url: String? = null
)

@Serializable
data class Genre(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Studio(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Theme(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Demographic(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Aired(
    val from: String? = null,
    val to: String? = null,
    val prop: AiredProp? = null,
    val string: String? = null
)

@Serializable
data class AiredProp(
    val from: DateProp? = null,
    val to: DateProp? = null
)

@Serializable
data class DateProp(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null
)

// Manga Response Models
@Serializable
data class MangaResponse(
    val data: MangaData? = null
)

@Serializable
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

@Serializable
data class Author(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Serialization(
    val mal_id: Int = 0,
    val type: String? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class Published(
    val from: String? = null,
    val to: String? = null,
    val prop: AiredProp? = null,
    val string: String? = null
)

// Helper function for extracting tags
fun AnimeData.extractTags(): List<String> {
    val tags = mutableSetOf<String>()
    
    // Add basic information
    tags.add("Anime")
    tags.add("MAL-$mal_id")
    
    // Add type and status
    type?.let { tags.add("Type: $it") }
    status?.let { tags.add("Status: $it") }
    rating?.let { tags.add("Rating: $it") }
    source?.let { tags.add("Source: $it") }
    
    // Add seasonal information
    season?.let { tags.add("Season: ${it.replaceFirstChar { c -> c.uppercase() }}") }
    year?.let { tags.add("Year: $it") }
    
    // Add episode count
    episodes?.let { if (it > 0) tags.add("Episodes: $it") }
    
    // Add genres
    genres?.forEach { genre -> 
        genre.name?.let { name -> 
            tags.add(name)
            tags.add("Genre: $name")
        }
    }
    
    // Add studios
    studios?.forEach { studio ->
        studio.name?.let { name ->
            tags.add("Studio: $name")
            tags.add(name)
        }
    }
    
    // Add themes
    themes?.forEach { theme ->
        theme.name?.let { name ->
            tags.add("Theme: $name")
            tags.add(name)
        }
    }
    
    // Add demographics
    demographics?.forEach { demo ->
        demo.name?.let { name ->
            tags.add("Demographic: $name")
            tags.add(name)
        }
    }
    
    // Check for adult content
    val isHentai = genres?.any { it.name?.contains("hentai", true) == true } 
        ?: rating?.contains("Rx", true) ?: false
    if (isHentai) {
        tags.add("Adult Content")
        tags.add("NSFW")
        tags.add("18+")
    }
    
    return tags.sorted()
}

fun MangaData.extractTags(): List<String> {
    val tags = mutableSetOf<String>()
    
    // Add basic information
    tags.add("Manga")
    tags.add("MAL-$mal_id")
    
    // Add type and status
    type?.let { tags.add("Type: $it") }
    status?.let { tags.add("Status: $it") }
    
    // Add chapter and volume count
    chapters?.let { if (it > 0) tags.add("Chapters: $it") }
    volumes?.let { if (it > 0) tags.add("Volumes: $it") }
    
    // Add genres
    genres?.forEach { genre -> 
        genre.name?.let { name -> 
            tags.add(name)
            tags.add("Genre: $name")
        }
    }
    
    // Add authors
    authors?.forEach { author ->
        author.name?.let { name ->
            tags.add("Author: $name")
            tags.add(name)
        }
    }
    
    // Add themes
    themes?.forEach { theme ->
        theme.name?.let { name ->
            tags.add("Theme: $name")
            tags.add(name)
        }
    }
    
    // Add demographics
    demographics?.forEach { demo ->
        demo.name?.let { name ->
            tags.add("Demographic: $name")
            tags.add(name)
        }
    }
    
    // Check for adult content
    val isHentai = genres?.any { it.name?.contains("hentai", true) == true } ?: false
    if (isHentai) {
        tags.add("Adult Content")
        tags.add("NSFW")
        tags.add("18+")
    }
    
    return tags.sorted()
}