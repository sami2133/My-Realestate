package com.realestate.sami.ui.viewmodel

/** گزینه‌های ترتیب نمایش لیست ملک‌ها. */
enum class PropertySortOption {
    NEWEST,
    OLDEST,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    AREA_LARGE_TO_SMALL,
    AREA_SMALL_TO_LARGE,
    ROOMS_MOST_TO_FEWEST,
    ROOMS_FEWEST_TO_MOST,
    BUILDING_AGE_NEWEST,
    BUILDING_AGE_OLDEST
}

/** گزینه‌های ترتیب نمایش لیست متقاضیان. */
enum class ClientSortOption {
    NEWEST,
    OLDEST,
    NAME_A_TO_Z,
    BUDGET_HIGH_TO_LOW
}
