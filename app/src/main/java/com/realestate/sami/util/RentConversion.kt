package com.realestate.sami.util

/**
 * فاز ۵.۲ — معادله‌ی موزون رهن↔اجاره، همون فرمولی که بنگاه‌های املاک ایران استفاده می‌کنن:
 * به ازای هر مقدار که رهن نسبت به رهن پایه (ثبت‌شده) کم بشه، اجاره به همون نسبت (درصد تبدیل
 * ماهانه) به اجاره‌ی پایه اضافه می‌شه. نرخ رسمی/قانونی معمولاً ۲.۵٪ و نرخ متعارف بازار ۳٪ است؛
 * چون این نرخ محل به محل و زمان به زمان فرق می‌کنه، در تنظیمات اپ قابل تغییره ([RentPreferences]).
 *
 * مثال: رهن پایه ۴۰۰ میلیون + اجاره پایه ۱ میلیون، با نرخ تبدیل ۳٪، اگر رهن به ۳۰۰ میلیون تغییر کنه:
 * اختلاف رهن = ۱۰۰ میلیون → افزایش اجاره = ۱۰۰,۰۰۰,۰۰۰ × ۳٪ = ۳,۰۰۰,۰۰۰ → اجاره‌ی جدید = ۴,۰۰۰,۰۰۰
 */
fun calculateAdjustedRent(baseDeposit: Long, baseRent: Long, adjustedDeposit: Long, conversionPercent: Float): Long {
    val depositDelta = baseDeposit - adjustedDeposit
    val rentDelta = depositDelta * conversionPercent / 100f
    return baseRent + rentDelta.toLong()
}

private val SLIDER_STEP_CANDIDATES_TOMAN = longArrayOf(
    500_000L, 1_000_000L, 2_000_000L, 5_000_000L, 10_000_000L,
    20_000_000L, 50_000_000L, 100_000_000L, 200_000_000L, 500_000_000L
)

/**
 * انتخاب یک گام «رند» برای نوار لغزنده‌ی تعدیل رهن، طوری که بین حدود ۱ تا ۴۰ پله‌ی قابل‌کنترل
 * ایجاد بشه — نه اونقدر ریز که لمس دقیقش سخت باشه، نه اونقدر درشت که نشه رو عدد دلخواه توقف کرد.
 */
fun pickSliderStepToman(spanToman: Long): Long {
    if (spanToman <= 0) return SLIDER_STEP_CANDIDATES_TOMAN.first()
    return SLIDER_STEP_CANDIDATES_TOMAN.firstOrNull { spanToman / it in 1..40 }
        ?: SLIDER_STEP_CANDIDATES_TOMAN.last()
}

/** نمایش تمیز یک عدد اعشاری درصد بدون صفرهای اضافه (مثلاً 3.0 -> "3"، 2.5 -> "2.5"). */
fun Float.toPlainPercentString(): String = this.toBigDecimal().stripTrailingZeros().toPlainString()
