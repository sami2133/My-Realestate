package com.realestate.sami.sync

/** ثابت‌های مربوط به همگام‌سازی تیمی روی Google Drive (فاز ۴). */
object DriveConstants {
    /** Scope محدود drive.file (به‌جای drive کامل) — چون قرار است چند تیم/گروه مختلف از اپ منتشرشده
     *  استفاده کنند، دیگر نمی‌توان در حالت Testing با افزودن دستی هر کاربر ماند. drive.file جزو
     *  اسکوپ‌های حساس/محدود گوگل نیست، پس انتشار اپ (Publishing) بدون نیاز به بررسی امنیتی CASA
     *  ممکن است. محدودیتش: اپ فقط به فایل‌هایی دسترسی دارد که خودش ساخته یا کاربر با
     *  Google Picker صریحاً انتخاب کرده (به همین دلیل DrivePickerActivity اضافه شده است). */
    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val DRIVE_SCOPE_OAUTH = "oauth2:$DRIVE_SCOPE"

    /** نام پوشه‌ی مشترک تیمی که در Drive هر عضو ساخته/پیدا می‌شود. */
    const val TEAM_FOLDER_NAME = "SamiRealEstate-TeamSync"

    const val PROPERTIES_FILE_NAME = "sami_properties.json"
    const val CLIENTS_FILE_NAME = "sami_clients.json"
    const val CONTACT_LOGS_FILE_NAME = "sami_contact_logs.json"

    /** زیرپوشه‌ی داخل پوشه‌ی تیمی که عکس‌های ملک (فایل‌های باینری) در آن آپلود می‌شوند. */
    const val IMAGES_FOLDER_NAME = "images"

    const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    const val JSON_MIME_TYPE = "application/json"
    const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"

    const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
    const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"

    /**
     * رکوردهای soft-delete شده (tombstone) این تعداد روز در فایل JSON مشترک نگه داشته می‌شوند
     * تا فرصت کافی برای رسیدن پیام حذف به همه‌ی دستگاه‌های تیم باشد، بعد از آن پاک‌سازی می‌شوند
     * تا حجم فایل مشترک بی‌نهایت رشد نکند. دستگاهی که بیش از این مدت آفلاین بماند، ممکن است
     * حذف رو نبینه (محدودیت شناخته‌شده).
     */
    const val TOMBSTONE_RETENTION_DAYS = 90L
}
