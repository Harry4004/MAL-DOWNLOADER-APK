package com.harry.maldownloader.data

data class AnimeEntry(
    var malId: Int = 0,
    var title: String = "",
    val type: String = "",
    var userTags: List<String> = emptyList(),

    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val synopsis: String? = null,
    val score: Float? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val imageUrl: String? = null,
    val allTags: List<String> = emptyList(),
    val isHentai: Boolean = false
)