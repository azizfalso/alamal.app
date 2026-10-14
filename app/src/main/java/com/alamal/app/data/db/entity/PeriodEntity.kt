package com.alamal.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startDate: Long,
    val endDate: Long,
    val carriedBalance: Double,
    val totalWages: Double,
    val totalRemote: Double,
    val totalCredits: Double,
    val amountDue: Double,
    val amountReceived: Double,
    val remaining: Double,
    val note: String? = null,
    val archivedAt: Long = System.currentTimeMillis(),
)
