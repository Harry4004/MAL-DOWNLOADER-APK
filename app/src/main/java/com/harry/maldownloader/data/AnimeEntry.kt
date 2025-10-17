package com.harry.maldownloader.data

data class AnimeEntry(
    val seriesId: Int,
    val title: String,
    val type: String?,
    val episodesTotal: Int?,
    val userId: Int?,
    val episodesWatched: Int?,
    val startDate: String?,
    val finishDate: String?,
    val userScore: Int?,
    val dvd: String?,
    val storage: String?,
    val status: String?,
    val comments: String?,
    val timesWatched: Int?,
    val rewatchValue: String?,
    val tags: String?,
    val rewatching: Boolean?,
    val rewatchingEp: Int?
)
