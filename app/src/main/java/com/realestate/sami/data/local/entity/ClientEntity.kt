package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ClientStatus { SEARCHING, PAUSED, MATCHED, CLOSED }

/**
 * متقاضی‌ای که دنبال ملکی با مشخصات خاص است.
 */
@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // اطلاعات متقاضی
    val fullName: String,
    val phone: String,
    val note: String? = null,

    // مشخصات ملک مورد نظر
    val desiredPropertyType: PropertyType,
    val desiredDealType: DealType,
    val desiredRegion: String,          // منطقه/محله مورد نظر (متنی، برای جستجو)
    val regionLatitude: Double? = null, // مرکز محدوده مورد نظر روی نقشه (اختیاری)
    val regionLongitude: Double? = null,
    val searchRadiusKm: Double? = null, // شعاع جستجو حول نقطه بالا

    val minArea: Double? = null,
    val maxArea: Double? = null,
    val minRooms: Int? = null,
    val maxRooms: Int? = null,

    val maxTotalPrice: Long? = null,
    val maxDepositPrice: Long? = null,
    val maxRentPrice: Long? = null,

    val needsParking: Boolean = false,
    val needsStorage: Boolean = false,
    val needsElevator: Boolean = false,

    val status: ClientStatus = ClientStatus.SEARCHING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val remoteId: String? = null,
    val isSynced: Boolean = false
)
