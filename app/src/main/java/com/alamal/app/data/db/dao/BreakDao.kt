package com.alamal.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alamal.app.data.db.entity.BreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakDao {
    @Query("SELECT * FROM breaks WHERE workDayId = :workDayId ORDER BY startTime ASC")
    fun observeForDay(workDayId: Long): Flow<List<BreakEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BreakEntity): Long

    @Delete
    suspend fun delete(item: BreakEntity)
}
