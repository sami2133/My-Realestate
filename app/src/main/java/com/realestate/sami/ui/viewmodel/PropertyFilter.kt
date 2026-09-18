package com.realestate.sami.ui.viewmodel

import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.DeedType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import com.realestate.sami.data.local.entity.PropertyType

/**
 * فیلترهای فعال روی لیست ملک‌ها. مقدار null یعنی «فیلتر نشده» برای آن فیلد.
 * برای بازه قیمت، چون قیمت بسته به نوع معامله در یکی از سه فیلد
 * (totalPrice / rentPrice / depositPrice) ذخیره می‌شود، «قیمت مؤثر» ملک
 * اولین مقدار غیر-null از بین این سه است.
 */
data class PropertyFilter(
    val propertyType: PropertyType? = null,
    val dealType: DealType? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val status: PropertyStatus? = null,
    val minArea: Double? = null,
    val maxArea: Double? = null,
    /** تعداد اتاق انتخاب‌شده. مقادیر ۱ تا ۳ یعنی «دقیقاً همین تعداد»؛ ۴ یعنی «۴ یا بیشتر». */
    val rooms: Int? = null,
    val minBuildingAge: Int? = null,
    val maxBuildingAge: Int? = null,
    val requireElevator: Boolean = false,
    val requireParking: Boolean = false,
    val requireStorage: Boolean = false,
    val deedType: DeedType? = null,
    val exchangeableOnly: Boolean = false
) {
    /**
     * فاز ۵.۴: dealType دیگه یه «فیلتر اختیاری» نیست — چون صفحه‌ی ملک‌ها الان همیشه رو یکی از دو
     * تب خرید-و-فروش/اجاره است، این مقدار همیشه ست شده. برای همین در تشخیص «آیا واقعاً فیلتری
     * روی نتایج اعمال شده» (برای پیام حالت خالی و نشان‌دادن نشان روی آیکون) دخالت داده نمی‌شه؛
     * بقیه‌ی فیلدها «اختیاری» در نظر گرفته می‌شن.
     */
    val isActive: Boolean
        get() = propertyType != null || minPrice != null || maxPrice != null ||
            status != null || minArea != null || maxArea != null || rooms != null ||
            minBuildingAge != null || maxBuildingAge != null ||
            requireElevator || requireParking || requireStorage ||
            deedType != null || exchangeableOnly
}

/** قیمتی که برای مقایسه با بازه‌ی فیلتر استفاده می‌شود. */
val PropertyEntity.effectivePrice: Long?
    get() = totalPrice ?: rentPrice ?: depositPrice

fun PropertyEntity.matches(filter: PropertyFilter): Boolean {
    if (filter.propertyType != null && propertyType != filter.propertyType) return false
    if (filter.dealType != null && dealType != filter.dealType) return false
    if (filter.minPrice != null || filter.maxPrice != null) {
        val price = effectivePrice ?: return false
        if (filter.minPrice != null && price < filter.minPrice) return false
        if (filter.maxPrice != null && price > filter.maxPrice) return false
    }
    if (filter.status != null && status != filter.status) return false
    if (filter.minArea != null && area < filter.minArea) return false
    if (filter.maxArea != null && area > filter.maxArea) return false
    if (filter.rooms != null) {
        val r = rooms ?: return false
        if (filter.rooms >= 4) {
            if (r < 4) return false
        } else if (r != filter.rooms) return false
    }
    if (filter.minBuildingAge != null || filter.maxBuildingAge != null) {
        val age = buildingAge ?: return false
        if (filter.minBuildingAge != null && age < filter.minBuildingAge) return false
        if (filter.maxBuildingAge != null && age > filter.maxBuildingAge) return false
    }
    if (filter.requireElevator && !hasElevator) return false
    if (filter.requireParking && !hasParking) return false
    if (filter.requireStorage && !hasStorage) return false
    if (filter.deedType != null && deedType != filter.deedType) return false
    if (filter.exchangeableOnly && !isExchangeable) return false
    return true
}
