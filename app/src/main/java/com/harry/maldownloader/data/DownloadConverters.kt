package com.harry.maldownloader.data

import androidx.room.TypeConverter

class DownloadConverters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.joinToString(",")

    @TypeConverter
    fun toStringList(str: String?): List<String> =
        if (str.isNullOrBlank()) emptyList() else str.split(",").map { it.trim() }
}