package com.realestate.sami.util

/**
 * فرمت‌کننده مبلغ برای نمایش ایرانی: جداکننده هزارگان + واحد تومان + ارقام فارسی.
 * مبالغ در دیتابیس به تومان (Long) ذخیره می‌شوند.
 */
fun Long.toTomanDisplay(showUnit: Boolean = true): String {
    val grouped = String.format("%,d", this).replace(",", "٬")
    val withDigits = grouped.toPersianDigits()
    return if (showUnit) "$withDigits تومان" else withDigits
}

/**
 * برای مبالغ بزرگ، خلاصه‌سازی خوانا مثل «۲ میلیارد و ۳۵۰ میلیون تومان».
 * مناسب سرتیتر کارت ملک؛ برای مبالغ دقیق از toTomanDisplay استفاده شود.
 */
fun Long.toTomanShort(): String {
    val billion = 1_000_000_000L
    val million = 1_000_000L
    return when {
        this >= billion -> {
            val b = this / billion
            val remM = (this % billion) / million
            val text = if (remM > 0) "$b میلیارد و $remM میلیون" else "$b میلیارد"
            text.toPersianDigits() + " تومان"
        }
        this >= million -> {
            val m = this / million
            "$m میلیون".toPersianDigits() + " تومان"
        }
        else -> toTomanDisplay()
    }
}

/** حذف جداکننده و واحد، برای پارس کردن ورودی فرم به Long خام (ارقام فارسی/انگلیسی هر دو پشتیبانی می‌شوند). */
fun String.parseTomanInput(): Long? {
    val cleaned = this.toEnglishDigits().filter { it.isDigit() }
    return cleaned.toLongOrNull()
}

/** پارس کردن متراژ/تعداد با پشتیبانی از ارقام فارسی (مثلاً «۸۵» یا «85»). */
fun String.parseNumberInput(): Double? = this.toEnglishDigits().trim().toDoubleOrNull()
fun String.parseIntInput(): Int? = this.toEnglishDigits().trim().toIntOrNull()
