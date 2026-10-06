package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportHistoryDao {
    @Query("SELECT * FROM export_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ExportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExportHistoryEntity): Long

    @Delete
    suspend fun delete(entry: ExportHistoryEntity)

    @Query("DELETE FROM export_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM export_history")
    suspend fun clearAll()
}
