package com.realestate.sami.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.realestate.sami.data.local.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * پشتیبان‌گیری/بازیابیِ کامل داده‌های محلی اپ. چون همه‌ی جدول‌ها (ملک‌ها، مشتری‌ها، تماس‌ها،
 * بازدیدها) در یک فایل SQLite واحد هستن، به‌جای سریالایز کردن دستیِ هر Entity به JSON (که با هر
 * تغییر ساختار جدول باید هماهنگ نگه داشته بشه)، ساده‌ترین و مطمئن‌ترین راه، پشتیبان‌گیری از خودِ
 * فایل دیتابیسه.
 */
object BackupManager {

    private const val BACKUP_MIME_TYPE = "application/octet-stream"

    /**
     * یک کپیِ timestamp-دار از فایل دیتابیس در cache می‌سازه (برای اشتراک‌گذاری با FileProvider).
     * قبل از کپی، WAL رو کامل روی فایل اصلی merge می‌کنه تا کپی همیشه کامل و سالم باشه — وگرنه
     * ممکنه بخشی از آخرین تغییرات که هنوز در فایل WAL جداگانه‌ان، در کپی نباشن.
     */
    fun createBackup(context: Context, db: AppDatabase): File? {
        return try {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) return null

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val backupFile = File(exportDir, "realestate_backup_$timestamp.db")
            dbFile.copyTo(backupFile, overwrite = true)
            backupFile
        } catch (e: Exception) {
            null
        }
    }

    fun shareBackup(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        PropertyExporter.shareFile(context, uri, BACKUP_MIME_TYPE)
    }

    /**
     * فایلِ پشتیبانِ انتخاب‌شده توسط کاربر رو جایگزین دیتابیس فعلی می‌کنه. چون Room همین حالا یک
     * اتصال باز به فایل قدیمی داره، جایگزینی امن فقط با بستن کامل اتصال ممکنه — برای همین بعد از
     * بازیابیِ موفق، UI باید کل اپ رو (با [restartApp]) دوباره باز کنه تا Room با فایل جدید از نو
     * ساخته بشه.
     */
    fun restoreBackup(context: Context, db: AppDatabase, sourceUri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val tempFile = File(dbFile.parentFile, "${dbFile.name}.restore_tmp")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return false

            // اعتبارسنجی سریع: فایل انتخاب‌شده باید واقعاً یک دیتابیس SQLite باشه (هدر استاندارد
            // ۱۶ بایتیِ "SQLite format 3\u0000")، نه یک فایل دلخواه با پسوند اشتباه.
            // readNBytes روی API < 33 وجود نداره، پس با یک بافر ثابت دستی می‌خونیم.
            val header = ByteArray(16)
            tempFile.inputStream().use { stream ->
                var offset = 0
                while (offset < header.size) {
                    val read = stream.read(header, offset, header.size - offset)
                    if (read == -1) break
                    offset += read
                }
            }
            val isValidSqlite = header.decodeToString().startsWith("SQLite format 3")
            if (!isValidSqlite) {
                tempFile.delete()
                return false
            }

            db.close()
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            // فایل‌های WAL/SHM قدیمی که با دیتابیس جایگزین‌شده هماهنگ نیستن رو پاک کن.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** چون Room بعد از [restoreBackup] با فایل قدیمی در حافظه مونده، اپ باید کامل از نو باز بشه. */
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
