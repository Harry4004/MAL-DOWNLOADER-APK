package com.harry.maldownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "duplicate_hashes")
data class DuplicateHash(
    @PrimaryKey val hash: String,
    val filePath: String,
    val downloadId: String,
    val timestamp: Long = System.currentTimeMillis()
)