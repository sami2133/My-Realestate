package com.realestate.sami.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** ذخیره‌ی وضعیت همگام‌سازی: شناسه‌ی پوشه‌ی تیمی و آخرین زمان sync موفق. */
@Singleton
class SyncPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    var teamFolderId: String?
        get() = prefs.getString(KEY_FOLDER_ID, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_ID, value).apply()

    var lastSyncedAt: Long?
        get() = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value ?: 0L).apply()

    /** وقتی روشنه، sync دوره‌ای پس‌زمینه فقط روی Wi-Fi اجرا می‌شود (نه دیتای موبایل). پیش‌فرض خاموش. */
    var autoSyncWifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    /** وقتی روشنه، هنگام sync به‌جای نام/شماره‌ی واقعی مالک/معرف، نام/شماره‌ی تماس خودِ این مشاور
     *  (myDisplayName/myDisplayPhone) برای بقیه‌ی اعضای تیم فرستاده می‌شود؛ اطلاعات واقعی مالک
     *  فقط روی همین دستگاه (که آن را ثبت کرده) باقی می‌ماند. پیش‌فرض خاموش. */
    var protectOwnerContact: Boolean
        get() = prefs.getBoolean(KEY_PROTECT_OWNER_CONTACT, false)
        set(value) = prefs.edit().putBoolean(KEY_PROTECT_OWNER_CONTACT, value).apply()

    /** نام و شماره‌ی تماسی که وقتی [protectOwnerContact] روشن است، به‌جای مالک واقعی به بقیه‌ی
     *  اعضای تیم نشان داده می‌شود (معمولاً نام/شماره‌ی خودِ همین مشاور). */
    var myDisplayName: String
        get() = prefs.getString(KEY_MY_DISPLAY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MY_DISPLAY_NAME, value).apply()

    var myDisplayPhone: String
        get() = prefs.getString(KEY_MY_DISPLAY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MY_DISPLAY_PHONE, value).apply()

    companion object {
        private const val KEY_FOLDER_ID = "team_folder_id"
        private const val KEY_LAST_SYNC = "last_synced_at"
        private const val KEY_WIFI_ONLY = "auto_sync_wifi_only"
        private const val KEY_PROTECT_OWNER_CONTACT = "protect_owner_contact"
        private const val KEY_MY_DISPLAY_NAME = "my_display_name"
        private const val KEY_MY_DISPLAY_PHONE = "my_display_phone"
    }
}
