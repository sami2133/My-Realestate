package com.realestate.sami.notification

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * فاز ۵ — رد شناسه‌ی پیگیری‌هایی که قبلاً برایشان نوتیفیکیشن نمایش داده شده، تا با هر بار اجرای
 * دوره‌ای [FollowUpReminderWorker] یک یادآوری دوباره و دوباره نمایش داده نشود.
 */
@Singleton
class NotificationPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    fun isAlreadyNotified(contactLogId: Long): Boolean =
        prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.contains(contactLogId.toString()) == true

    fun markNotified(contactLogId: Long) {
        val current = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(contactLogId.toString())
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, current).apply()
    }

    companion object {
        private const val KEY_NOTIFIED_IDS = "notified_follow_up_ids"
    }
}
