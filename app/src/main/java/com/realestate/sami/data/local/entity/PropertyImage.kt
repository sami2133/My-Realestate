package com.realestate.sami.data.local.entity

/**
 * یک تصویر ملک.
 *
 * [localUri] مسیر/URI قابل‌نمایش روی همین دستگاه است — یا چون خودِ همین دستگاه عکس را از
 * گالری انتخاب کرده، یا چون در یک sync قبلی از Drive دانلود و در حافظه‌ی محلی کش شده.
 * وقتی این تصویر از دستگاه دیگری آمده و هنوز دانلود نشده، این مقدار null است.
 *
 * [driveFileId] شناسه‌ی فایل روی Google Drive است؛ تا وقتی این تصویر sync نشده null است،
 * بعد از اولین آپلود پر می‌شود و بقیه‌ی دستگاه‌های تیم با همین شناسه آن را دانلود می‌کنند.
 */
data class PropertyImage(
    val localUri: String? = null,
    val driveFileId: String? = null
)
