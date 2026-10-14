package com.alamal.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alamal.app.data.db.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY archivedAt DESC")
    fun observeAll(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods ORDER BY archivedAt DESC")
    suspend fun getAll(): List<PeriodEntity>

    @Query("SELECT * FROM periods ORDER BY archivedAt DESC LIMIT 1")
    suspend fun getLast(): PeriodEntity?

    @Query("SELECT * FROM periods WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PeriodEntity?

    @Query("SELECT COUNT(*) FROM periods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PeriodEntity): Long

    @Delete
    suspend fun delete(item: PeriodEntity)
}
