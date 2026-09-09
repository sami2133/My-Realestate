package com.realestate.sami.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.realestate.sami.MainActivity
import com.realestate.sami.R

/**
 * فاز ۵ — نوتیفیکیشن پیگیری: وقتی followUpDate یک ContactLogEntity فرا می‌رسد، یک یادآوری محلی
 * (بدون نیاز به سرور) نمایش داده می‌شود.
 */
object NotificationHelper {

    const val CHANNEL_ID = "follow_up_reminders"
    private const val REQUEST_CODE_BASE = 5000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_followup_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_followup_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    /**
     * نمایش یادآوری برای یک پیگیری مشخص. اگر مجوز POST_NOTIFICATIONS (اندروید ۱۳ به بالا) داده
     * نشده باشد، سیستم خودش نوتیفیکیشن را نادیده می‌گیرد؛ برای همین با try/catch محافظت شده.
     */
    fun showFollowUpReminder(context: Context, contactLogId: Long, relatedName: String, note: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_BASE + contactLogId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.notification_followup_title, relatedName))
            .setContentText(note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(contactLogId.toInt(), notification)
        } catch (_: SecurityException) {
            // مجوز POST_NOTIFICATIONS داده نشده؛ بی‌صدا رد می‌شود، اپ در بقیه بخش‌ها عادی کار می‌کند.
        }
    }
}
