package com.realestate.sami.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    // نسخه ۲: افزودن ستون isDeleted برای پشتیبانی از soft-delete (sync حذف رکورد).
    // نسخه ۳: افزودن remoteId/relatedRemoteId/updatedAt/isSynced/isDeleted به ContactLogEntity
    // (برای sync تاریخچه‌ی تماس) و تبدیل PropertyEntity.imageUris (رشته‌ی ساده) به
    // PropertyEntity.images (لیست PropertyImage با پشتیبانی از sync عکس روی Drive).
    // نسخه ۴ (فاز ۵.۲): افزودن PropertyEntity.minAdjustableDeposit؛ و ادغام DealType.MORTGAGE
    // در DealType.RENT (رهن کامل و اجاره در ایران یک فرآیندند). این تبدیل با یک migration واقعی
    // انجام می‌شه (به data/local/Migrations.kt نگاه کن) نه destructive — چون اپ در حال استفاده‌ی
    // واقعیه و نباید داده‌ی sync‌نشده‌ی کسی پاک بشه. SyncManager هم هنگام دانلود از Drive مقدار
    // MORTGAGE قدیمی رو به RENT تبدیل می‌کنه، برای سازگاری با فایل‌های JSON قدیمی‌تر.
    version = 4,
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
    }
}
