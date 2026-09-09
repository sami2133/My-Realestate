package com.realestate.sami.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

/**
 * فاز ۵ — اتصال VisitEntity به تقویم سیستم.
 *
 * به‌جای نوشتن مستقیم روی جدول تقویم (که نیاز به مجوز READ_CALENDAR/WRITE_CALENDAR دارد)،
 * از ACTION_INSERT روی CalendarContract.Events.CONTENT_URI استفاده می‌کنیم: برنامه تقویم پیش‌فرض
 * گوشی (گوگل کلندر یا هر برنامه دیگر) باز می‌شود، فرم از قبل پر شده و کاربر فقط دکمه‌ی «ذخیره» را
 * می‌زند. این یعنی هیچ مجوز runtime لازم نیست و رویداد در همان تقویمی ثبت می‌شود که کاربر خودش
 * انتخاب می‌کند (شخصی/کاری/گوگل/…)، دقیقاً مثل تماس مستقیم (ACTION_DIAL) که در بقیه اپ استفاده شده.
 */
fun buildCalendarInsertIntent(
    title: String,
    description: String? = null,
    location: String? = null,
    beginMillis: Long,
    endMillis: Long = beginMillis + 30 * 60 * 1000L, // پیش‌فرض ۳۰ دقیقه طول بازدید
    reminderMinutesBefore: Int = 60
): Intent =
    Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        putExtra(CalendarContract.Events.TITLE, title)
        if (!description.isNullOrBlank()) putExtra(CalendarContract.Events.DESCRIPTION, description)
        if (!location.isNullOrBlank()) putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        putExtra(CalendarContract.Events.HAS_ALARM, true)
        putExtra(
            Intent.EXTRA_EMAIL, // برخی برنامه‌های تقویم فقط از این کلید برای یادآوری استفاده می‌کنند؛ بی‌ضرر است اگر نادیده گرفته شود
            ""
        )
        putExtra("beginTime", beginMillis)
        putExtra("endTime", endMillis)
        putExtra("reminderMinutes", reminderMinutesBefore)
    }

/** باز کردن برنامه تقویم با فرم رویداد از قبل پر شده؛ اگر هیچ برنامه‌ی تقویمی نصب نباشد بی‌صدا نادیده گرفته می‌شود. */
fun Context.openCalendarToAddVisit(
    title: String,
    description: String? = null,
    location: String? = null,
    beginMillis: Long
) {
    val intent = buildCalendarInsertIntent(title, description, location, beginMillis)
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    }
}
