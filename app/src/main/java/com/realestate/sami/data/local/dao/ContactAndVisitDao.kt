package com.realestate.sami.data.local.dao

import androidx.room.*
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.RelatedType
import com.realestate.sami.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ContactLogEntity): Long

    @Update
    suspend fun update(log: ContactLogEntity)

    @Delete
    suspend fun delete(log: ContactLogEntity)

    /** soft-delete: برای همگام‌سازی حذف بین دستگاه‌ها (فعلاً بدون UI مربوطه، فقط برای آینده). */
    @Query("UPDATE contact_logs SET isDeleted = 1, updatedAt = :deletedAt, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM contact_logs WHERE relatedId = :relatedId AND relatedType = :type AND isDeleted = 0 ORDER BY contactDate DESC")
    fun getForEntity(relatedId: Long, type: RelatedType): Flow<List<ContactLogEntity>>

    @Query("SELECT * FROM contact_logs WHERE followUpDate IS NOT NULL AND isFollowUpDone = 0 AND isDeleted = 0 ORDER BY followUpDate ASC")
    fun getPendingFollowUps(): Flow<List<ContactLogEntity>>

    /** یک‌بار (نه Flow) برای بررسی دوره‌ای در Worker یادآوری فاز ۵. */
    @Query("SELECT * FROM contact_logs WHERE followUpDate IS NOT NULL AND isFollowUpDone = 0 AND isDeleted = 0 ORDER BY followUpDate ASC")
    suspend fun getPendingFollowUpsOnce(): List<ContactLogEntity>

    /** علامت‌گذاری یک پیگیری به‌عنوان انجام‌شده (فاز ۵: بعد از نمایش نوتیفیکیشن یا لمس دستی کاربر). */
    @Query("UPDATE contact_logs SET isFollowUpDone = 1, updatedAt = :doneAt, isSynced = 0 WHERE id = :id")
    suspend fun markFollowUpDone(id: Long, doneAt: Long = System.currentTimeMillis())

    /** همه‌ی رکوردها شامل tombstone های حذف‌شده — فقط برای منطق sync، نه UI. */
    @Query("SELECT * FROM contact_logs")
    suspend fun getAllIncludingDeleted(): List<ContactLogEntity>
}

@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)

    @Delete
    suspend fun delete(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE propertyId = :propertyId ORDER BY visitDate DESC")
    fun getForProperty(propertyId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE clientId = :clientId ORDER BY visitDate DESC")
    fun getForClient(clientId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE visitDate BETWEEN :startOfDay AND :endOfDay ORDER BY visitDate ASC")
    fun getForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<VisitEntity>>

    /** همه‌ی بازدیدها — برای داشبورد آماری فاز ۵ (شمارش معاملات موفق و غیره). */
    @Query("SELECT * FROM visits ORDER BY visitDate DESC")
    fun getAll(): Flow<List<VisitEntity>>

    /** بازدیدهای پیش‌رو از این لحظه به بعد — برای کارت «بازدیدهای این هفته» در داشبورد. */
    @Query("SELECT * FROM visits WHERE visitDate >= :fromMillis ORDER BY visitDate ASC")
    fun getUpcoming(fromMillis: Long): Flow<List<VisitEntity>>
}
