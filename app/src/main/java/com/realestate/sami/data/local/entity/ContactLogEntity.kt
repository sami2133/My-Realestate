package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RelatedType { PROPERTY, CLIENT }

/**
 * تاریخچه تماس/پیگیری برای یک ملک یا یک متقاضی.
 */
@Entity(tableName = "contact_logs")
data class ContactLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val relatedId: Long,
    val relatedType: RelatedType,
    val contactDate: Long = System.currentTimeMillis(),
    val note: String,
    val followUpDate: Long? = null,   // تاریخ یادآوری پیگیری بعدی
    val isFollowUpDone: Boolean = false
)
