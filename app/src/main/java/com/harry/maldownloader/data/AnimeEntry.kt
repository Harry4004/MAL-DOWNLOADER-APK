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
    val isHentai: Boolean = false,
    
    // Enhanced descriptive fields from APIs
    val creator: String? = null,
    val descriptiveText: String? = null,
    val sourceUrl: String? = null,
    val descriptiveSubjects: List<String> = emptyList(),
    val translatedTitle: String? = null,
    val rating: String? = null,
    val year: Int? = null,
    val season: String? = null,
    val broadcast: String? = null,
    val duration: String? = null,
    val aired: String? = null,
    val producers: List<String> = emptyList(),
    val licensors: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val demographics: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val serializations: List<String> = emptyList()
)