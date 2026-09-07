package com.realestate.sami.sync

/** ثابت‌های مربوط به همگام‌سازی تیمی روی Google Drive (فاز ۴). */
object DriveConstants {
    /** Scope کامل Drive — چون تیم کوچک (۲ تا ۵ نفر) در حالت Testing در Google Cloud Console می‌ماند،
     *  نیازی به تایید (verification) گوگل نیست؛ فقط باید ایمیل هر عضو تیم را به‌عنوان test user اضافه کنید. */
    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive"
    const val DRIVE_SCOPE_OAUTH = "oauth2:$DRIVE_SCOPE"

    /** نام پوشه‌ی مشترک تیمی که در Drive هر عضو ساخته/پیدا می‌شود. */
    const val TEAM_FOLDER_NAME = "SamiRealEstate-TeamSync"

    const val PROPERTIES_FILE_NAME = "sami_properties.json"
    const val CLIENTS_FILE_NAME = "sami_clients.json"

    const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    const val JSON_MIME_TYPE = "application/json"

    const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
    const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
}
