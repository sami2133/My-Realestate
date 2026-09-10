package com.realestate.sami.ui.viewmodel

import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
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
    val maxPrice: Long? = null
) {
    val isActive: Boolean
        get() = propertyType != null || dealType != null || minPrice != null || maxPrice != null
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
    return true
}
