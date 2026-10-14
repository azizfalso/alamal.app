package com.alamal.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alamal.app.data.db.entity.CreditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {
    @Query("SELECT * FROM credits WHERE periodId IS NULL ORDER BY date DESC")
    fun observeCurrent(): Flow<List<CreditEntity>>

    @Query("SELECT * FROM credits ORDER BY date DESC")
    fun observeAll(): Flow<List<CreditEntity>>

    @Query("SELECT * FROM credits WHERE periodId IS NULL ORDER BY date ASC")
    suspend fun getCurrent(): List<CreditEntity>

    @Query("SELECT * FROM credits WHERE periodId = :periodId ORDER BY date ASC")
    suspend fun getByPeriod(periodId: Long): List<CreditEntity>

    @Query("SELECT * FROM credits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CreditEntity?

    @Query("UPDATE credits SET periodId = :periodId WHERE periodId IS NULL")
    suspend fun assignCurrentToPeriod(periodId: Long)

    @Query("UPDATE credits SET periodId = NULL WHERE periodId = :periodId")
    suspend fun releaseFromPeriod(periodId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CreditEntity): Long

    @Update
    suspend fun update(item: CreditEntity)

    @Delete
    suspend fun delete(item: CreditEntity)
}
