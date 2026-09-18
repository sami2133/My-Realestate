package com.realestate.sami.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * کپی کردن عکس انتخاب‌شده از Photo Picker اندروید به حافظه‌ی داخلیِ دائمی خودِ اپ.
 *
 * چرا لازمه: URI ای که Photo Picker (`PickVisualMedia`/`PickMultipleVisualMedia`) برمی‌گردونه
 * فقط برای طول عمر session جاری اپ معتبره — این یک محدودیت رسمی خودِ پیکر گوگله، نه باگ این اپ
 * و نه ربطی به آپدیت/دیپلوی نسخه‌های جدید داره. وقتی اپ کامل از حافظه پاک بشه (که معمولاً ظرف
 * چند ساعت تا یک روز توسط اندروید برای آزاد کردن رم اتفاق می‌افته) یا گوشی ریستارت بشه، اون URI
 * باطل می‌شه و دیگه قابل خوندن نیست — عکس «آسیب‌دیده» به‌نظر می‌رسه چون فایلش دیگه در دسترس اپ
 * نیست، نه اینکه واقعاً خراب شده باشه. برای همین بلافاصله بعد از انتخاب، بایت‌های عکس خونده و
 * در پوشه‌ی داخلی خودِ اپ (`filesDir/property_images`) که همیشه در دسترسه کپی می‌شه.
 */
fun Context.copyPickedImageToAppStorage(sourceUri: Uri): String? = try {
    val bytes = contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
    if (bytes == null) {
        null
    } else {
        val dir = File(filesDir, "property_images").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        Uri.fromFile(file).toString()
    }
} catch (e: Exception) {
    null
}

/**
 * نمایش یک تصویر ثبت‌شده با اپ گالری/نمایش‌گر پیش‌فرض گوشی، یا در صورت وجود چند اپ، با دیالوگ
 * انتخاب استاندارد اندروید که گزینه‌ی «فقط یک‌بار / همیشه» را هم دارد — یعنی کاربر می‌تواند از
 * همان‌جا یک اپ را به‌عنوان پیش‌فرض برای نمایش تصاویر تنظیم کند. (عمداً از Intent.createChooser
 * استفاده نشده، چون آن همیشه دیالوگ انتخاب را نشان می‌دهد و اجازه‌ی «همیشه» نمی‌دهد.)
 * فایل از طریق FileProvider به برنامه‌ی مقصد داده می‌شود (بدون نیاز به مجوز حافظه).
 */
fun Context.viewImageExternally(localUri: String): Boolean = try {
    val uri = Uri.parse(localUri)
    val viewableUri = if (uri.scheme == "file") {
        FileProvider.getUriForFile(this, "$packageName.fileprovider", File(uri.path!!))
    } else {
        uri
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(viewableUri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(intent)
    true
} catch (e: Exception) {
    false
}
