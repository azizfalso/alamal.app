package com.alamal.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
