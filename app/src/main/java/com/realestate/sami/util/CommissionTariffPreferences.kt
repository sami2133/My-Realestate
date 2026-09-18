package com.realestate.sami.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * روش محاسبه‌ی حق‌العمل فروش:
 * - [MARGINAL] پلکانی/مرزی: هر بازه فقط روی همان بخش از مبلغ که داخل آن بازه است حساب می‌شود
 *   (شبیه پلکان مالیاتی).
 * - [FLAT] غیرپلکانی: کل مبلغ معامله با نرخِ همان بازه‌ای که مبلغ در آن قرار می‌گیرد ضرب می‌شود —
 *   طبق متن نرخ‌نامه‌ی ابلاغی («بالاتر از X تا Y، فلان درصد از دو طرف»)، خوانشِ رایج همین حالت است.
 */
enum class CommissionCalculationMode { MARGINAL, FLAT }

/**
 * نرخ‌نامه‌ی حق‌العمل مشاور املاک — طبق نرخ‌نامه‌ی ابلاغی اتحادیه (نمونه‌ی پیوست‌شده توسط کاربر).
 *
 * تمام مبالغ زیر «تومان» ذخیره می‌شوند (چون [PropertyEntity.totalPrice] و بقیه‌ی قیمت‌های اپ هم
 * به تومان‌اند)، در حالی که نرخ‌نامه‌ی رسمی به «ریال» نوشته شده. مقادیر پیش‌فرض همان اعداد
 * نرخ‌نامه تقسیم‌بر ۱۰ هستند (مثلاً «۲۰،۰۰۰،۰۰۰،۰۰۰ ریال» در پیش‌فرض شده «۲،۰۰۰،۰۰۰،۰۰۰ تومان»).
 *
 * منطق فروش پلکانی (مرزی) است — یعنی نرخ هر بازه فقط روی همان بخش از مبلغ اعمال می‌شود، نه کل
 * مبلغ (شبیه پلکان مالیاتی). سهم خریدار و فروشنده در هر بازه همیشه مساوی (نصف-نصف) است.
 *
 * چون ممکن است این نرخ‌نامه در آینده توسط اتحادیه تغییر کند، تمام ۷ عدد (۳ سقف + ۴ نرخ) از
 * صفحه‌ی «تنظیمات» توسط کاربر قابل ویرایش‌اند؛ نیازی به آپدیت اپ نیست.
 *
 * نرخ حق‌العمل اجاره/رهن از همین کلاس + [RentPreferences.conversionPercent] (برای معادل‌سازی
 * رهن به اجاره، طبق همان بند نرخ‌نامه) در [CommissionCalculator] استفاده می‌شود.
 */
@Singleton
class CommissionTariffPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("commission_tariff_prefs", Context.MODE_PRIVATE)

    // ---- بازه‌های فروش (سقف هر بازه، به تومان) ----
    var saleThreshold1: Long
        get() = prefs.getLong(KEY_SALE_THRESHOLD_1, DEFAULT_SALE_THRESHOLD_1)
        set(value) = prefs.edit().putLong(KEY_SALE_THRESHOLD_1, value).apply()

    var saleThreshold2: Long
        get() = prefs.getLong(KEY_SALE_THRESHOLD_2, DEFAULT_SALE_THRESHOLD_2)
        set(value) = prefs.edit().putLong(KEY_SALE_THRESHOLD_2, value).apply()

    var saleThreshold3: Long
        get() = prefs.getLong(KEY_SALE_THRESHOLD_3, DEFAULT_SALE_THRESHOLD_3)
        set(value) = prefs.edit().putLong(KEY_SALE_THRESHOLD_3, value).apply()

    // ---- نرخ هر بازه (درصدِ کل معامله در همان بازه؛ نصفش خریدار، نصفش فروشنده) ----
    var saleRate1: Float
        get() = prefs.getFloat(KEY_SALE_RATE_1, DEFAULT_SALE_RATE_1)
        set(value) = prefs.edit().putFloat(KEY_SALE_RATE_1, value).apply()

    var saleRate2: Float
        get() = prefs.getFloat(KEY_SALE_RATE_2, DEFAULT_SALE_RATE_2)
        set(value) = prefs.edit().putFloat(KEY_SALE_RATE_2, value).apply()

    var saleRate3: Float
        get() = prefs.getFloat(KEY_SALE_RATE_3, DEFAULT_SALE_RATE_3)
        set(value) = prefs.edit().putFloat(KEY_SALE_RATE_3, value).apply()

    var saleRate4: Float
        get() = prefs.getFloat(KEY_SALE_RATE_4, DEFAULT_SALE_RATE_4)
        set(value) = prefs.edit().putFloat(KEY_SALE_RATE_4, value).apply()

    // ---- اجاره: کل حق‌العمل = این درصد از (اجاره‌ی ماهانه + معادلِ رهن) — نصف موجر، نصف مستاجر ----
    var rentCommissionPercent: Float
        get() = prefs.getFloat(KEY_RENT_COMMISSION_PERCENT, DEFAULT_RENT_COMMISSION_PERCENT)
        set(value) = prefs.edit().putFloat(KEY_RENT_COMMISSION_PERCENT, value).apply()

    /** روش محاسبه‌ی فروش: پلکانی یا غیرپلکانی. پیش‌فرض غیرپلکانی (خوانش رایج از متن نرخ‌نامه). */
    var calculationMode: CommissionCalculationMode
        get() = CommissionCalculationMode.valueOf(
            prefs.getString(KEY_CALCULATION_MODE, DEFAULT_CALCULATION_MODE.name) ?: DEFAULT_CALCULATION_MODE.name
        )
        set(value) = prefs.edit().putString(KEY_CALCULATION_MODE, value.name).apply()

    companion object {
        private const val KEY_SALE_THRESHOLD_1 = "sale_threshold_1"
        private const val KEY_SALE_THRESHOLD_2 = "sale_threshold_2"
        private const val KEY_SALE_THRESHOLD_3 = "sale_threshold_3"
        private const val KEY_SALE_RATE_1 = "sale_rate_1"
        private const val KEY_SALE_RATE_2 = "sale_rate_2"
        private const val KEY_SALE_RATE_3 = "sale_rate_3"
        private const val KEY_SALE_RATE_4 = "sale_rate_4"
        private const val KEY_RENT_COMMISSION_PERCENT = "rent_commission_percent"
        private const val KEY_CALCULATION_MODE = "commission_calculation_mode"

        // ریال‌های نرخ‌نامه ÷ ۱۰ = تومان
        const val DEFAULT_SALE_THRESHOLD_1 = 2_000_000_000L   // تا ۲۰ میلیارد ریال
        const val DEFAULT_SALE_THRESHOLD_2 = 4_000_000_000L   // تا ۴۰ میلیارد ریال
        const val DEFAULT_SALE_THRESHOLD_3 = 6_000_000_000L   // تا ۶۰ میلیارد ریال

        const val DEFAULT_SALE_RATE_1 = 1.0f   // ۱٪ (نیم‌-نیم)
        const val DEFAULT_SALE_RATE_2 = 0.8f   // ۸ دهم درصد (۴دهم-۴دهم)
        const val DEFAULT_SALE_RATE_3 = 0.6f   // ۶ دهم درصد (۳دهم-۳دهم)
        const val DEFAULT_SALE_RATE_4 = 0.5f   // نیم درصد (۲۵صدم-۲۵صدم)

        const val DEFAULT_RENT_COMMISSION_PERCENT = 100f / 3f // یک‌سوم اجاره (یک‌ششم - یک‌ششم)
        val DEFAULT_CALCULATION_MODE = CommissionCalculationMode.FLAT
    }
}
