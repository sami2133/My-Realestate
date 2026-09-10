package com.realestate.sami.data.repository

import com.realestate.sami.data.local.dao.PropertyDao
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * تنها نقطه دسترسی به داده‌های ملک. فعلاً فقط از Room (آفلاین) می‌خواند.
 * در فاز همگام‌سازی ابری، این کلاس مسئول ترکیب داده محلی و ریموت (Firebase) خواهد بود.
 */
@Singleton
class PropertyRepository @Inject constructor(
    private val propertyDao: PropertyDao
) {
    fun getAll(): Flow<List<PropertyEntity>> = propertyDao.getAll()

    fun getByStatus(status: PropertyStatus): Flow<List<PropertyEntity>> =
        propertyDao.getByStatus(status)

    fun search(query: String): Flow<List<PropertyEntity>> = propertyDao.search(query)

    suspend fun getById(id: Long): PropertyEntity? = propertyDao.getById(id)

    suspend fun save(property: PropertyEntity): Long {
        val toSave = property.copy(updatedAt = System.currentTimeMillis())
        return propertyDao.insert(toSave)
    }

    /** soft-delete: به‌جای حذف فیزیکی، رکورد را tombstone می‌کند تا حذف بین دستگاه‌های تیم هم sync شود. */
    suspend fun delete(property: PropertyEntity) = propertyDao.softDelete(property.id)

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
    ): Flow<List<PropertyEntity>> = propertyDao.findMatchingProperties(
        propertyType, dealType, minArea, maxArea, minRooms, maxRooms,
        maxTotalPrice, maxDepositPrice, maxRentPrice,
        needsParking, needsStorage, needsElevator
    )
}
