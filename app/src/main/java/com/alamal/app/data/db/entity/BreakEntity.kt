package com.alamal.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "breaks",
    foreignKeys = [
        ForeignKey(
            entity = WorkDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workDayId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("workDayId")]
)
data class BreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workDayId: Long,
    val startTime: Long,
    val endTime: Long,
    val note: String? = null,
)
