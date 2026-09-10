package com.realestate.sami.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * فاز ۵.۲ — نرخ تبدیل رهن↔اجاره (درصد ماهانه)، قابل‌شخصی‌سازی چون بین بنگاه‌ها/مناطق/زمان‌ها فرق
 * می‌کنه. پیش‌فرض ۳٪ (نرخ «متعارف» بازار)؛ نرخ «رسمی/قانونی» معمولاً ۲.۵٪ است — کاربر از تنظیمات
 * (صفحه‌ی گزارش‌ها) هر عددی که خودش می‌خواد رو می‌تونه ست کنه.
 */
@Singleton
class RentPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("rent_conversion_prefs", Context.MODE_PRIVATE)

    var conversionPercent: Float
        get() = prefs.getFloat(KEY_CONVERSION_PERCENT, DEFAULT_CONVERSION_PERCENT)
        set(value) = prefs.edit().putFloat(KEY_CONVERSION_PERCENT, value).apply()

    companion object {
        private const val KEY_CONVERSION_PERCENT = "rent_conversion_percent"
        const val DEFAULT_CONVERSION_PERCENT = 3f
    }
}
