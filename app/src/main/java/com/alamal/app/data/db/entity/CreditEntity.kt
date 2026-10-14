package com.alamal.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Credit / advance. Stored as a positive amount but treated as negative in totals.
 * periodId null = current period.
 */
@Entity(tableName = "credits")
data class CreditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val periodId: Long? = null,
    val date: Long,
    val amount: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
