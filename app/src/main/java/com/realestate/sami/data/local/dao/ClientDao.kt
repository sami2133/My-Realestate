package com.realestate.sami.data.local.dao

import androidx.room.*
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.ClientStatus
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyType
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    /** حذف فیزیکی — فقط برای موارد داخلی (مثلاً پاک‌سازی tombstone قدیمی)؛ برای حذف عادی از UI از softDelete استفاده کن. */
    @Delete
    suspend fun delete(client: ClientEntity)

    /** soft-delete: رکورد فیزیکی حذف نمی‌شود تا حذف بین دستگاه‌ها sync شود؛ در همه‌ی لیست‌ها مخفی می‌شود. */
    @Query("UPDATE clients SET isDeleted = 1, updatedAt = :deletedAt, isSynced = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM clients WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): ClientEntity?

    @Query("SELECT * FROM clients WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ClientEntity>>

    /** همه‌ی رکوردها شامل tombstone های حذف‌شده — فقط برای منطق sync، نه UI. */
    @Query("SELECT * FROM clients")
    suspend fun getAllIncludingDeleted(): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE status = :status AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getByStatus(status: ClientStatus): Flow<List<ClientEntity>>

    @Query(
        """
        SELECT * FROM clients
        WHERE isDeleted = 0
          AND (:query = '' OR fullName LIKE '%' || :query || '%' OR desiredRegion LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        """
    )
    fun search(query: String): Flow<List<ClientEntity>>

    /**
     * تطبیق معکوس: وقتی یک ملک جدید ثبت می‌شود، متقاضیان سازگار با آن را پیدا می‌کند.
     */
    @Query(
        """
        SELECT * FROM clients
        WHERE isDeleted = 0
          AND status = 'SEARCHING'
          AND desiredPropertyType = :propertyType
          AND desiredDealType = :dealType
          AND (minArea IS NULL OR minArea <= :area)
          AND (maxArea IS NULL OR maxArea >= :area)
          AND (minRooms IS NULL OR minRooms <= :rooms)
          AND (maxRooms IS NULL OR maxRooms >= :rooms)
          AND (maxTotalPrice IS NULL OR :totalPrice IS NULL OR maxTotalPrice >= :totalPrice)
          AND (maxDepositPrice IS NULL OR :depositPrice IS NULL OR maxDepositPrice >= :depositPrice)
          AND (maxRentPrice IS NULL OR :rentPrice IS NULL OR maxRentPrice >= :rentPrice)
          AND (needsParking = 0 OR :hasParking = 1)
          AND (needsStorage = 0 OR :hasStorage = 1)
          AND (needsElevator = 0 OR :hasElevator = 1)
        ORDER BY createdAt DESC
        """
    )
    fun findMatchingClients(
        propertyType: PropertyType,
        dealType: DealType,
        area: Double,
        rooms: Int,
        totalPrice: Long?,
        depositPrice: Long?,
        rentPrice: Long?,
        hasParking: Boolean,
        hasStorage: Boolean,
        hasElevator: Boolean
    ): Flow<List<ClientEntity>>
}
