package com.harry.maldownloader

import com.harry.maldownloader.api.*
import retrofit2.Response

fun MalAnimeResponse.toEntryTags(): Pair<List<String>, String?> {
    val tags = mutableListOf<String>()
    tags.add("Anime"); tags.add("MAL-$id")
    media_type?.let { tags.add("Format: ${it}") }
    status?.let { tags.add("Status: ${it}") }
    start_season?.year?.let { tags.add("Year: ${it}") }
    genres?.forEach { g -> g.name?.let { n -> tags.add(n); tags.add("Genre: $n") } }
    return tags.distinct().sorted() to synopsis
}

fun MalMangaResponse.toEntryTags(): Pair<List<String>, String?> {
    val tags = mutableListOf<String>()
    tags.add("Manga"); tags.add("MAL-$id")
    media_type?.let { tags.add("Format: ${it}") }
    status?.let { tags.add("Status: ${it}") }
    genres?.forEach { g -> g.name?.let { n -> tags.add(n); tags.add("Genre: $n") } }
    return tags.distinct().sorted() to synopsis
}
