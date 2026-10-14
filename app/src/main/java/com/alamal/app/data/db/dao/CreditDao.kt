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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CreditEntity): Long

    @Update
    suspend fun update(item: CreditEntity)

    @Delete
    suspend fun delete(item: CreditEntity)
}
