package com.realestate.sami.util

/** نتیجه‌ی محاسبه‌ی حق‌العمل: جمع کل + سهم هر طرف (خریدار/فروشنده یا موجر/مستاجر). همیشه نصف-نصف. */
data class CommissionResult(
    val total: Long,
    val partyAShare: Long, // خریدار (فروش) / مستاجر (اجاره)
    val partyBShare: Long  // فروشنده (فروش) / موجر (اجاره)
)

/**
 * پیاده‌سازی نرخ‌نامه‌ی حق‌العمل مشاور املاک. همه‌ی مبالغ ورودی/خروجی به تومان‌اند.
 * تمام اعداد نرخ‌نامه از [CommissionTariffPreferences] خوانده می‌شوند تا در آینده (با تغییر
 * نرخ‌نامه‌ی اتحادیه) فقط کافیست کاربر از صفحه‌ی تنظیمات عدد جدید را وارد کند — کد نیازی به
 * تغییر ندارد.
 */
object CommissionCalculator {

    /**
     * حق‌العمل فروش/معاوضه — پلکانی روی [dealAmount]:
     * هر بازه فقط روی همان بخش از مبلغ که داخل آن بازه است حساب می‌شود (نه کل مبلغ)، درست مثل
     * نمونه‌ی نرخ‌نامه: مثلاً برای معامله‌ی ۵۰ میلیارد ریالی (۵ میلیارد تومان)، ۲۰ میلیارد ریال
     * اول با نرخ ۱٪، ۲۰ میلیارد بعدی با نرخ ۰٫۸٪ و ۱۰ میلیارد باقی‌مانده با نرخ ۰٫۶٪ حساب می‌شود؛
     * این سه رقم با هم جمع می‌شوند و نتیجه، حق‌العمل کل معامله است.
     */
    fun calculateSaleCommission(
        dealAmount: Long,
        tariff: CommissionTariffPreferences
    ): CommissionResult {
        if (dealAmount <= 0L) return CommissionResult(0, 0, 0)

        val t1 = tariff.saleThreshold1
        val t2 = tariff.saleThreshold2
        val t3 = tariff.saleThreshold3

        var remaining = dealAmount
        var totalExact = 0.0

        val seg1 = minOf(remaining, t1)
        totalExact += seg1 * tariff.saleRate1 / 100.0
        remaining -= seg1

        if (remaining > 0) {
            val seg2 = minOf(remaining, t2 - t1)
            totalExact += seg2 * tariff.saleRate2 / 100.0
            remaining -= seg2
        }
        if (remaining > 0) {
            val seg3 = minOf(remaining, t3 - t2)
            totalExact += seg3 * tariff.saleRate3 / 100.0
            remaining -= seg3
        }
        if (remaining > 0) {
            totalExact += remaining * tariff.saleRate4 / 100.0
        }

        val total = totalExact.toLong()
        return total.splitEvenly()
    }

    /**
     * حق‌العمل اجاره/رهن. طبق نرخ‌نامه، رهن ابتدا به «معادل اجاره‌ی ماهانه» تبدیل می‌شود (با نرخ
     * تبدیل موجود در [RentPreferences.conversionPercent] — که پیش‌فرضش را می‌توان روی همان ۲٫۵٪
     * نرخ‌نامه‌ی رسمی گذاشت)، بعد کل (اجاره‌ی واقعی + معادلِ رهن) در نرخ حق‌العمل اجاره
     * ([CommissionTariffPreferences.rentCommissionPercent] — پیش‌فرض یک‌سوم) ضرب می‌شود.
     */
    fun calculateRentCommission(
        monthlyRent: Long,
        depositAmount: Long,
        tariff: CommissionTariffPreferences,
        rentConversionPercent: Float
    ): CommissionResult {
        val depositAsRentEquivalent = (depositAmount * rentConversionPercent / 100.0)
        val totalMonthlyEquivalent = monthlyRent + depositAsRentEquivalent
        val total = (totalMonthlyEquivalent * tariff.rentCommissionPercent / 100.0).toLong()
        return total.splitEvenly()
    }

    private fun Long.splitEvenly(): CommissionResult {
        val half = this / 2
        return CommissionResult(total = this, partyAShare = half, partyBShare = this - half)
    }
}
