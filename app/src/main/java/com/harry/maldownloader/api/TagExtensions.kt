package com.harry.maldownloader.api

/**
 * Extension functions for converting MAL API responses to entry tags
 * These functions extract and format tags from MAL API responses
 */

/**
 * Converts MAL Anime Response to entry tags and synopsis
 */
fun MalAnimeResponse.toEntryTags(): Pair<List<String>, String?> {
    val tags = mutableSetOf<String>()
    
    // Basic information
    tags.add("Anime")
    tags.add("MAL-$id")
    
    // Media type
    media_type?.let { 
        tags.add("Type: $it")
        tags.add("Format: $it")
    }
    
    // Status
    status?.let { tags.add("Status: $it") }
    
    // Episode count
    num_episodes?.let { if (it > 0) tags.add("Episodes: $it") }
    
    // Season information
    start_season?.let { season ->
        season.year?.let { tags.add("Year: $it") }
        season.season?.let { tags.add("Season: ${it.replaceFirstChar { c -> c.uppercase() }}") }
    }
    
    // Genres
    genres?.forEach { genre ->
        genre.name?.let { name ->
            tags.add(name)
            tags.add("Genre: $name")
        }
    }
    
    // Studios
    studios?.forEach { studio ->
        studio.name?.let { name ->
            tags.add("Studio: $name")
            tags.add(name)
        }
    }
    
    // NSFW content
    when (nsfw) {
        "white" -> {
            // Safe content, no additional tags
        }
        "gray" -> {
            tags.add("Suggestive")
            tags.add("Ecchi")
        }
        "black" -> {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
            tags.add("Hentai")
        }
    }
    
    return Pair(tags.sorted(), synopsis)
}

/**
 * Converts MAL Manga Response to entry tags and synopsis
 */
fun MalMangaResponse.toEntryTags(): Pair<List<String>, String?> {
    val tags = mutableSetOf<String>()
    
    // Basic information
    tags.add("Manga")
    tags.add("MAL-$id")
    
    // Media type
    media_type?.let { 
        tags.add("Type: $it")
        tags.add("Format: $it")
    }
    
    // Status
    status?.let { tags.add("Status: $it") }
    
    // Chapter and volume count
    chapters?.let { if (it > 0) tags.add("Chapters: $it") }
    volumes?.let { if (it > 0) tags.add("Volumes: $it") }
    
    // Genres
    genres?.forEach { genre ->
        genre.name?.let { name ->
            tags.add(name)
            tags.add("Genre: $name")
        }
    }
    
    // NSFW content
    when (nsfw) {
        "white" -> {
            // Safe content, no additional tags
        }
        "gray" -> {
            tags.add("Suggestive")
            tags.add("Ecchi")
        }
        "black" -> {
            tags.add("Adult Content")
            tags.add("NSFW")
            tags.add("18+")
            tags.add("Hentai")
        }
    }
    
    return Pair(tags.sorted(), synopsis)
}

/**
 * Converts genres list to entry tags
 */
fun List<Genre>?.toEntryTags(): List<String> {
    if (this == null) return emptyList()
    return this.mapNotNull { it.name?.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

/**
 * Converts studios list to entry tags
 */
fun List<Studio>?.toStudioTags(): List<String> {
    if (this == null) return emptyList()
    return this.mapNotNull { it.name?.trim() }
        .filter { it.isNotBlank() }
        .map { "Studio: $it" }
        .distinct()
        .sorted()
}

/**
 * Converts themes list to entry tags
 */
fun List<Theme>?.toThemeTags(): List<String> {
    if (this == null) return emptyList()
    return this.mapNotNull { it.name?.trim() }
        .filter { it.isNotBlank() }
        .map { "Theme: $it" }
        .distinct()
        .sorted()
}

/**
 * Converts demographics list to entry tags
 */
fun List<Demographic>?.toDemographicTags(): List<String> {
    if (this == null) return emptyList()
    return this.mapNotNull { it.name?.trim() }
        .filter { it.isNotBlank() }
        .map { "Demographic: $it" }
        .distinct()
        .sorted()
}

/**
 * Converts authors list to entry tags
 */
fun List<Author>?.toAuthorTags(): List<String> {
    if (this == null) return emptyList()
    return this.mapNotNull { it.name?.trim() }
        .filter { it.isNotBlank() }
        .map { "Author: $it" }
        .distinct()
        .sorted()
}

/**
 * Global extension function for converting various lists to entry tags
 */
fun <T> List<T>?.toEntryTags(selector: (T) -> String?): List<String> {
    if (this == null) return emptyList()
    return this.mapNotNull { selector(it)?.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}