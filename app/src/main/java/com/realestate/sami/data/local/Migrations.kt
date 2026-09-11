package com.realestate.sami.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * فاز ۵.۲ — نسخه ۳ به ۴: افزودن ستون `minAdjustableDeposit` به جدول `properties` (برای نوار
 * لغزنده‌ی تعدیل رهن↔اجاره)، و تبدیل مقدار قدیمی `dealType`/`desiredDealType` = "MORTGAGE" به
 * "RENT" (چون رهن کامل و اجاره در ایران یک فرآیندند و دیگه به‌عنوان نوع معامله‌ی جدا وجود نداره).
 *
 * این یک migration واقعیه، نه destructive — چون این اپ الان روی گوشی‌های واقعی با داده‌ی واقعی
 * در حال استفاده‌ست، پاک کردن کامل دیتابیس محلی موقع آپدیت (fallbackToDestructiveMigration)
 * می‌تونست هر تغییری که هنوز sync نشده رو از بین ببره. این migration فقط یک ستون nullable
 * اضافه می‌کنه و دو تا UPDATE ساده می‌زنه — هیچ جدولی بازسازی نمی‌شه، پس خطری برای داده‌ی
 * موجود نداره.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE properties ADD COLUMN minAdjustableDeposit INTEGER DEFAULT NULL")
        db.execSQL("UPDATE properties SET dealType = 'RENT' WHERE dealType = 'MORTGAGE'")
        db.execSQL("UPDATE clients SET desiredDealType = 'RENT' WHERE desiredDealType = 'MORTGAGE'")
    }
}

/**
 * فاز ۵.۳ — نسخه ۴ به ۵: افزودن ستون‌های `isExchangeable`/`exchangePreferredType`/`exchangeNote`
 * به جدول `properties`، و تبدیل مقدار قدیمی `dealType` = "EXCHANGE" به "SALE" + isExchangeable=1
 * (چون معاوضه دیگه نوع معامله‌ی جدا نیست، یک پرچم روی فروشه). مشابه MIGRATION_3_4، این هم فقط
 * ستون اضافه می‌کنه و مقدار آپدیت می‌کنه — بدون بازسازی جدول و بدون خطر برای داده‌ی موجود.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE properties ADD COLUMN isExchangeable INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN exchangePreferredType TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN exchangeNote TEXT DEFAULT NULL")
        db.execSQL("UPDATE properties SET dealType = 'SALE', isExchangeable = 1 WHERE dealType = 'EXCHANGE'")
    }
}
