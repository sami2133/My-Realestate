package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PropertyType { APARTMENT, VILLA, LAND, COMMERCIAL, OFFICE }
enum class DealType { SALE, RENT, MORTGAGE, EXCHANGE }
enum class PropertyStatus { AVAILABLE, RESERVED, SOLD_OR_RENTED, ARCHIVED }

/**
 * ملکی که یک "معرف" (مالک یا واسطه) به دفتر معرفی کرده است.
 */
@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // اطلاعات معرف / مالک ملک
    val ownerName: String,
    val ownerPhone: String,
    val ownerNote: String? = null,

    // مشخصات ملک
    val propertyType: PropertyType,
    val dealType: DealType,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val area: Double,                // متراژ (متر مربع)
    val rooms: Int,
    val buildingAge: Int? = null,    // سن بنا به سال
    val floor: Int? = null,
    val totalFloors: Int? = null,
    val hasParking: Boolean = false,
    val hasStorage: Boolean = false,
    val hasElevator: Boolean = false,
    val hasBalcony: Boolean = false,

    // قیمت‌گذاری (بسته به dealType پر می‌شود)
    val totalPrice: Long? = null,      // برای فروش
    val depositPrice: Long? = null,    // ودیعه/رهن
    val rentPrice: Long? = null,       // اجاره ماهانه

    val description: String? = null,
    val imageUris: String = "",        // لیست مسیر عکس‌ها با کاما جدا شده
    val documentUris: String = "",     // اسکن سند/مدارک

    val status: PropertyStatus = PropertyStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // برای همگام‌سازی ابری در فازهای بعدی
    val remoteId: String? = null,
    val isSynced: Boolean = false
)
