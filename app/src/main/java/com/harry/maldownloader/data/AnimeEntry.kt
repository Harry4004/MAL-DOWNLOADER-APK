package com.harry.maldownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_entries")
data class AnimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val malId: Int = 0,
    val title: String = "",
    val type: String = "",
    val userTags: List<String> = emptyList(),

    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val synopsis: String? = null,
    val score: Float? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val episodesWatched: Int? = null,
    val totalEpisodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val imageUrl: String? = null,
    val imagePath: String? = null,
    val malUrl: String? = null,
    val allTags: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val studio: String? = null,
    val source: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isHentai: Boolean = false
)