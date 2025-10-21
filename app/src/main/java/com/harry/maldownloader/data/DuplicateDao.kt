package com.harry.maldownloader.data

import androidx.room.*

@Dao
interface DuplicateDao {
    @Query("SELECT * FROM duplicate_hashes WHERE hash = :hash")
    suspend fun findDuplicate(hash: String): DuplicateHash?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHash(hash: DuplicateHash)

    @Delete
    suspend fun deleteHash(hash: DuplicateHash)
    
    @Query("DELETE FROM duplicate_hashes WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldHashes(cutoffTime: Long)
    
    @Query("DELETE FROM duplicate_hashes")
    suspend fun clearAllHashes()
}