package com.alamal.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_works")
data class RemoteWorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val periodId: Long? = null,
    val date: Long,
    val amount: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
