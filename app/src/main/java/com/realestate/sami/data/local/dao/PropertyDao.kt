package com.realestate.sami.data.local.dao

import androidx.room.*
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(property: PropertyEntity): Long

    @Update
    suspend fun update(property: PropertyEntity)

    @Delete
    suspend fun delete(property: PropertyEntity)

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun getById(id: Long): PropertyEntity?

    @Query("SELECT * FROM properties ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: PropertyStatus): Flow<List<PropertyEntity>>

    @Query(
        """
        SELECT * FROM properties
        WHERE (:query = '' OR address LIKE '%' || :query || '%' OR ownerName LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        """
    )
    fun search(query: String): Flow<List<PropertyEntity>>

    /**
     * موتور تطبیق: برای یک متقاضی مشخص، ملک‌های سازگار را برمی‌گرداند.
     * پارامترها معمولاً از روی مشخصات ClientEntity پر می‌شوند.
     */
    @Query(
        """
        SELECT * FROM properties
        WHERE status = 'AVAILABLE'
          AND propertyType = :propertyType
          AND dealType = :dealType
          AND (:minArea IS NULL OR area >= :minArea)
          AND (:maxArea IS NULL OR area <= :maxArea)
          AND (:minRooms IS NULL OR rooms >= :minRooms)
          AND (:maxRooms IS NULL OR rooms <= :maxRooms)
          AND (:maxTotalPrice IS NULL OR totalPrice IS NULL OR totalPrice <= :maxTotalPrice)
          AND (:maxDepositPrice IS NULL OR depositPrice IS NULL OR depositPrice <= :maxDepositPrice)
          AND (:maxRentPrice IS NULL OR rentPrice IS NULL OR rentPrice <= :maxRentPrice)
          AND (:needsParking = 0 OR hasParking = 1)
          AND (:needsStorage = 0 OR hasStorage = 1)
          AND (:needsElevator = 0 OR hasElevator = 1)
        ORDER BY createdAt DESC
        """
    )
    fun findMatchingProperties(
        propertyType: com.realestate.sami.data.local.entity.PropertyType,
        dealType: com.realestate.sami.data.local.entity.DealType,
        minArea: Double?,
        maxArea: Double?,
        minRooms: Int?,
        maxRooms: Int?,
        maxTotalPrice: Long?,
        maxDepositPrice: Long?,
        maxRentPrice: Long?,
        needsParking: Boolean,
        needsStorage: Boolean,
        needsElevator: Boolean
    ): Flow<List<PropertyEntity>>
}
