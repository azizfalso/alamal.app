package com.alamal.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alamal.app.data.db.entity.RemoteWorkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteWorkDao {
    @Query("SELECT * FROM remote_works WHERE periodId IS NULL ORDER BY date DESC")
    fun observeCurrent(): Flow<List<RemoteWorkEntity>>

    @Query("SELECT * FROM remote_works ORDER BY date DESC")
    fun observeAll(): Flow<List<RemoteWorkEntity>>

    @Query("SELECT * FROM remote_works WHERE periodId IS NULL ORDER BY date ASC")
    suspend fun getCurrent(): List<RemoteWorkEntity>

    @Query("SELECT * FROM remote_works WHERE periodId = :periodId ORDER BY date ASC")
    suspend fun getByPeriod(periodId: Long): List<RemoteWorkEntity>

    @Query("SELECT * FROM remote_works ORDER BY date ASC")
    suspend fun getAllForBackup(): List<RemoteWorkEntity>

    @Query("SELECT * FROM remote_works WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RemoteWorkEntity?

    @Query("UPDATE remote_works SET periodId = :periodId WHERE periodId IS NULL")
    suspend fun assignCurrentToPeriod(periodId: Long)

    @Query("UPDATE remote_works SET periodId = NULL WHERE periodId = :periodId")
    suspend fun releaseFromPeriod(periodId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RemoteWorkEntity): Long

    @Update
    suspend fun update(item: RemoteWorkEntity)

    @Delete
    suspend fun delete(item: RemoteWorkEntity)
}
