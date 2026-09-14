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

        // الگوریتم استاندارد ۳۳-ساله‌ی تقویم جلالی (بیرشک) — همان الگوریتمی که کتابخانه‌های
        // معروف jalaali-js/moment-jalaali از آن استفاده می‌کنند. فرمول قبلی این فایل یک
        // ثابتِ اشتباه («355666+...») داشت که باعث می‌شد سال/ماه تا حدود یک تا دو سال جابه‌جا
        // محاسبه شود (مثلاً امروز را «۲۵ بهمن ۱۴۰۴» به‌جای تاریخ درست نشان می‌داد).
        private val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
            1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )

        private fun div(a: Int, b: Int): Int = a / b // تقسیم صحیح به‌سمت صفر، مثل جاوااسکریپت
        private fun mod(a: Int, b: Int): Int = a - div(a, b) * b

        private data class JalCalResult(val leap: Int, val gy: Int, val march: Int)

        private fun jalCal(jy: Int): JalCalResult {
            val gy = jy + 621
            var leapJ = -14
            var jp = breaks[0]
            var jump = 0
            for (i in 1 until breaks.size) {
                val jm = breaks[i]
                jump = jm - jp
                if (jy < jm) break
                leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
                jp = jm
            }
            var n = jy - jp
            leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
            if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1

            val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
            val march = 20 + leapJ - leapG

            if (jump - n < 6) n = n - jump + div(jump, 33) * 33
            var leap = mod(mod(n + 1, 33) - 1, 4)
            if (leap == -1) leap = 4
            return JalCalResult(leap, gy, march)
        }

        private fun g2d(gy: Int, gm: Int, gd: Int): Int {
            var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
                    div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
            d -= div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) - 752
            return d
        }

        fun fromGregorian(gy: Int, gm: Int, gd: Int): PersianDate {
            val jdn = g2d(gy, gm, gd)
            var jy = gy - 621
            var r = jalCal(jy)
            var jdn1f = g2d(r.gy, 3, r.march)
            var k = jdn - jdn1f
            val jm: Int
            val jd: Int
            if (k >= 0) {
                if (k <= 185) {
                    jm = 1 + div(k, 31)
                    jd = mod(k, 31) + 1
                    return PersianDate(jy, jm, jd)
                }
                k -= 186
            } else {
                jy -= 1
                k += 179
                if (r.leap == 1) k += 1
            }
            jm = 7 + div(k, 30)
            jd = mod(k, 30) + 1
            return PersianDate(jy, jm, jd)
        }
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
