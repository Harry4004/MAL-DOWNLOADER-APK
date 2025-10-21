package com.harry.maldownloader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeEntryDao {
    @Query("SELECT * FROM anime_entries ORDER BY title ASC")
    fun getAllEntries(): Flow<List<AnimeEntry>>

    @Query("SELECT * FROM anime_entries WHERE id = :id")
    suspend fun getEntryById(id: String): AnimeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: AnimeEntry)

    @Update
    suspend fun updateEntry(entry: AnimeEntry)

    @Delete
    suspend fun deleteEntry(entry: AnimeEntry)
    
    @Query("DELETE FROM anime_entries")
    suspend fun clearAllEntries()
}