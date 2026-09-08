package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RelatedType { PROPERTY, CLIENT }

/**
 * تاریخچه تماس/پیگیری برای یک ملک یا یک متقاضی.
 *
 * برای همگام‌سازی تیمی (فاز ۴.۱): [relatedId] فقط یک شناسه‌ی محلی Room است و بین دستگاه‌های
 * مختلف یکسان نیست، پس برای وصل‌کردن این لاگ به ملک/متقاضی درست روی دستگاه‌های دیگر، از
 * [relatedRemoteId] (که همان remoteId پایدار آن ملک/متقاضی است) استفاده می‌شود.
 */
@Entity(tableName = "contact_logs")
data class ContactLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val relatedId: Long,
    val relatedType: RelatedType,
    val contactDate: Long = System.currentTimeMillis(),
    val note: String,
    val followUpDate: Long? = null,   // تاریخ یادآوری پیگیری بعدی
    val isFollowUpDone: Boolean = false,

    // برای همگام‌سازی تیمی
    val remoteId: String? = null,
    val relatedRemoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,

    /** soft-delete: به‌جای حذف فیزیکی، این پرچم ست می‌شود تا حذف بین دستگاه‌ها هم sync شود. */
    val isDeleted: Boolean = false
)
