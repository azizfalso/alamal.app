package com.alamal.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alamal.app.data.db.entity.WorkDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM work_days WHERE periodId IS NULL ORDER BY clockIn DESC")
    fun observeCurrent(): Flow<List<WorkDayEntity>>

    @Query("SELECT * FROM work_days WHERE periodId IS NULL AND clockOut IS NULL ORDER BY clockIn DESC LIMIT 1")
    fun observeOpen(): Flow<WorkDayEntity?>

    @Query("SELECT * FROM work_days WHERE periodId IS NULL AND clockOut IS NULL ORDER BY clockIn DESC LIMIT 1")
    suspend fun getOpen(): WorkDayEntity?

    @Query("SELECT * FROM work_days WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(day: WorkDayEntity): Long

    @Update
    suspend fun update(day: WorkDayEntity)

    @Delete
    suspend fun delete(day: WorkDayEntity)
}
