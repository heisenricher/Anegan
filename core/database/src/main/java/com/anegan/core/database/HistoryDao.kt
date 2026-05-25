package com.anegan.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ConversionHistoryEntity>>

    @Insert
    suspend fun insertConversion(history: ConversionHistoryEntity)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()
}
