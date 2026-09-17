package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoastDao {
    @Query("SELECT * FROM roast_history ORDER BY timestamp DESC")
    fun getAllRoasts(): Flow<List<RoastEntity>>

    @Query("SELECT * FROM roast_history WHERE id = :id LIMIT 1")
    suspend fun getRoastById(id: String): RoastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoast(roast: RoastEntity)

    @Query("DELETE FROM roast_history WHERE id = :id")
    suspend fun deleteRoast(id: String)
}
