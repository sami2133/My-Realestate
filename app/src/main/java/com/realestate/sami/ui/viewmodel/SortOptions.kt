package com.realestate.sami.ui.viewmodel

/** گزینه‌های ترتیب نمایش لیست ملک‌ها. */
enum class PropertySortOption {
    NEWEST,
    OLDEST,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    AREA_LARGE_TO_SMALL
}

/** گزینه‌های ترتیب نمایش لیست متقاضیان. */
enum class ClientSortOption {
    NEWEST,
    OLDEST,
    NAME_A_TO_Z,
    BUDGET_HIGH_TO_LOW
}
