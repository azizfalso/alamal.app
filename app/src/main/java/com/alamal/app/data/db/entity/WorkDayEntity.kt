package com.alamal.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A regular work day (clock-in + clock-out + optional breaks).
 * periodId is null while the day belongs to the current (open) period.
 */
@Entity(tableName = "work_days")
data class WorkDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val periodId: Long? = null,
    val clockIn: Long,
    val clockOut: Long? = null,
    val hourlyRate: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
