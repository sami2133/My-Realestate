package com.realestate.sami.util

import java.util.Calendar
import java.util.TimeZone

/**
 * مبدل سبک میلادی <-> شمسی (جلالی)، بدون وابستگی به کتابخانه خارجی.
 * الگوریتم استاندارد تقویم جلالی (بر پایه محاسبات دیرک روت / کاظمی).
 */
data class PersianDate(val year: Int, val month: Int, val day: Int) {
    companion object {
        private val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )

        /** تبدیل timestamp میلادی (میلی‌ثانیه) به تاریخ شمسی، با در نظر گرفتن منطقه زمانی تهران. */
        fun fromMillis(millis: Long): PersianDate {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
            cal.timeInMillis = millis
            return fromGregorian(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromGregorian(gy: Int, gm: Int, gd: Int): PersianDate {
            val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
                    ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
            if (gm > 2 && isGregorianLeap(gy)) days += 1

            var jy = -1595 + (33 * (days / 12053))
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                jy += (days - 1) / 365
                days = (days - 1) % 365
            }
            val jm: Int
            val jd: Int
            if (days < 186) {
                jm = 1 + (days / 31)
                jd = 1 + (days % 31)
            } else {
                jm = 7 + ((days - 186) / 30)
                jd = 1 + ((days - 186) % 30)
            }
            return PersianDate(jy, jm, jd)
        }

        private fun isGregorianLeap(year: Int): Boolean =
            (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun monthName(): String = monthNames[month - 1]

    /** مثل ۱۴۰۴/۰۷/۱۵ ولی با ارقام فارسی */
    fun formatNumeric(): String {
        val raw = "%04d/%02d/%02d".format(year, month, day)
        return raw.toPersianDigits()
    }

    /** مثل «۱۵ مهر ۱۴۰۴» */
    fun formatLong(): String = "${day.toString().toPersianDigits()} ${monthName()} ${year.toString().toPersianDigits()}"
}

/** تبدیل ارقام فارسی/عربی داخل یک رشته به ارقام انگلیسی — برای پارس کردن ورودی فرم‌ها. */
fun String.toEnglishDigits(): String {
    val fa = "۰۱۲۳۴۵۶۷۸۹"
    val ar = "٠١٢٣٤٥٦٧٨٩"
    val sb = StringBuilder(this)
    for (i in sb.indices) {
        val faIdx = fa.indexOf(sb[i])
        val arIdx = ar.indexOf(sb[i])
        when {
            faIdx != -1 -> sb[i] = ('0' + faIdx)
            arIdx != -1 -> sb[i] = ('0' + arIdx)
        }
    }
    return sb.toString()
}

/** تبدیل ارقام انگلیسی داخل یک رشته به ارقام فارسی. */
fun String.toPersianDigits(): String {
    val en = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val fa = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val sb = StringBuilder(this)
    for (i in sb.indices) {
        val idx = en.indexOf(sb[i])
        if (idx != -1) sb[i] = fa[idx]
    }
    return sb.toString()
}

fun Long.toPersianDateString(): String = PersianDate.fromMillis(this).formatLong()
fun Long.toPersianDateNumeric(): String = PersianDate.fromMillis(this).formatNumeric()
