package com.realestate.sami.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.realestate.sami.data.local.dao.ClientDao
import com.realestate.sami.data.local.dao.ContactLogDao
import com.realestate.sami.data.local.dao.PropertyDao
import com.realestate.sami.data.local.dao.VisitDao
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.VisitEntity

@Database(
    entities = [
        PropertyEntity::class,
        ClientEntity::class,
        ContactLogEntity::class,
        VisitEntity::class
    ],
    // نسخه ۱ = baseline (طرح فاز ۶). چون این اپ الان روی دستگاه‌های واقعی نصب شده و کاربرها
    // داده‌ی واقعی (ملک/متقاضی) دارن، از این‌جا به بعد دیگه نمی‌شه به fallbackToDestructiveMigration
    // تکیه کرد — هر تغییر ساختاری باید با یک Migration واقعی (نه destructive) بیاد، وگرنه با آپدیت
    // اپ روی دستگاه‌های کاربرها، کل داده‌ی محلی‌شون پاک می‌شه. نسخه ۲: افزودن ستون lastEditedBy
    // به properties و clients (فاز sync — نام آخرین عضو تیمی که رکورد را ویرایش کرده).
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun clientDao(): ClientDao
    abstract fun contactLogDao(): ContactLogDao
    abstract fun visitDao(): VisitDao

    companion object {
        const val DATABASE_NAME = "realestate_consultant.db"

        /** فقط اضافه‌کردن دو ستون nullable — بدون تغییر/حذف داده‌ی موجود. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE properties ADD COLUMN lastEditedBy TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE clients ADD COLUMN lastEditedBy TEXT DEFAULT NULL")
            }
        }
    }
}
