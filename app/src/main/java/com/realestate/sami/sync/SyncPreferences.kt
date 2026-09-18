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

    /** برچسب دلخواهی که فقط داخل خودِ اپ (در UI) به‌جای/در کنار نام واقعی پوشه‌ی Drive نمایش
     *  داده می‌شود؛ مستقل از نام واقعی پوشه است و تغییرش هیچ درخواستی به Drive نمی‌فرستد —
     *  فقط برای تشخیص راحت‌تر «این کدوم تیم/دفتره» توی UI، مخصوصاً وقتی چند تیم مختلف هست. */
    var teamDisplayName: String
        get() = prefs.getString(KEY_TEAM_DISPLAY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TEAM_DISPLAY_NAME, value).apply()

    /**
     * شناسه‌ی Drive سه فایل مشترک، **کش‌شده بعد از اولین resolve موفق** در همین پوشه‌ی تیمی.
     * چرا لازمه: با اسکوپ drive.file، جست‌وجوی فایل با نام (`files.list`) روی فایلی که یک
     * دستگاه/عضو دیگر آن را ساخته، همیشه قابل‌اعتماد نیست؛ اگر هر بار sync دوباره با نام سرچ
     * کنیم و پیدا نکنیم، به‌جای update، یک نسخه‌ی موازی و تکراری ساخته می‌شود. با کش‌کردن شناسه‌ی
     * دقیق فایل همان لحظه که برای اولین‌بار resolve شد (یا در join، یا در اولین sync)، دفعات بعد
     * مستقیم با همان id به فایل درخواست می‌زنیم و به جست‌وجوی نام به‌عنوان fallback نیاز نداریم.
     */
    var propertiesFileId: String?
        get() = prefs.getString(KEY_PROPERTIES_FILE_ID, null)
        set(value) = prefs.edit().putString(KEY_PROPERTIES_FILE_ID, value).apply()

    var clientsFileId: String?
        get() = prefs.getString(KEY_CLIENTS_FILE_ID, null)
        set(value) = prefs.edit().putString(KEY_CLIENTS_FILE_ID, value).apply()

    var contactLogsFileId: String?
        get() = prefs.getString(KEY_CONTACT_LOGS_FILE_ID, null)
        set(value) = prefs.edit().putString(KEY_CONTACT_LOGS_FILE_ID, value).apply()

    /**
     * کل وضعیت مربوط به «تیم فعلی» را پاک می‌کند: شناسه‌ی پوشه، شناسه‌های کش‌شده‌ی فایل‌ها، و
     * برچسب دلخواه. برای «ساخت/پیوستن به یک پوشه‌ی جدید» (چه از طریق خروج دستی از تیم، چه هنگام
     * پیوستن به پوشه‌ای متفاوت از پوشه‌ی قبلی) باید همیشه صدا زده شود — وگرنه شناسه‌ی فایل‌های
     * کش‌شده‌ی تیم *قبلی* به‌اشتباه برای تیم *جدید* استفاده می‌شود.
     * توجه: داده‌های محلی (ملک‌ها/متقاضیان ثبت‌شده روی همین دستگاه) پاک نمی‌شوند — فقط اتصال
     * این دستگاه به تیم قطع می‌شود؛ کاربر می‌تواند بدون از‌دست‌دادن داده‌اش دوباره join کند.
     */
    fun clearTeamState() {
        prefs.edit()
            .remove(KEY_FOLDER_ID)
            .remove(KEY_TEAM_DISPLAY_NAME)
            .remove(KEY_PROPERTIES_FILE_ID)
            .remove(KEY_CLIENTS_FILE_ID)
            .remove(KEY_CONTACT_LOGS_FILE_ID)
            .remove(KEY_LAST_SYNC)
            .apply()
    }

    companion object {
        private const val KEY_FOLDER_ID = "team_folder_id"
        private const val KEY_LAST_SYNC = "last_synced_at"
        private const val KEY_WIFI_ONLY = "auto_sync_wifi_only"
        private const val KEY_PROTECT_OWNER_CONTACT = "protect_owner_contact"
        private const val KEY_MY_DISPLAY_NAME = "my_display_name"
        private const val KEY_MY_DISPLAY_PHONE = "my_display_phone"
        private const val KEY_TEAM_DISPLAY_NAME = "team_display_name"
        private const val KEY_PROPERTIES_FILE_ID = "properties_file_id"
        private const val KEY_CLIENTS_FILE_ID = "clients_file_id"
        private const val KEY_CONTACT_LOGS_FILE_ID = "contact_logs_file_id"
    }
}
