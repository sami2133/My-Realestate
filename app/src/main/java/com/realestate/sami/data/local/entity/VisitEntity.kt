package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VisitResult { PENDING, INTERESTED, NOT_INTERESTED, DEAL_CLOSED }

/**
 * قرار بازدید یک متقاضی از یک ملک؛ برای تقویم و پیگیری معامله.
 */
@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long,
    val clientId: Long,
    val visitDate: Long,
    val result: VisitResult = VisitResult.PENDING,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
