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

/**
 * فاز ۵.۵ — نسخه ۵ به ۶: افزودن مشخصات تکمیلیِ بسته به نوع ملک (نوع سند، جهت/وضعیت واحد،
 * کلاس ساختمان، امتیازات آب/برق/گاز، مشخصات زمین، سرقفلی و مشخصات تجاری/اداری و…). همه‌ی
 * ستون‌های جدید nullable یا با مقدار پیش‌فرض false/0 هستن، پس هیچ رکورد موجودی خراب نمی‌شه —
 * فقط ستون‌های جدید خالی اضافه می‌شن، بدون بازسازی جدول.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // مشترک بین چند نوع ملک
        db.execSQL("ALTER TABLE properties ADD COLUMN deedType TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN waterStatus TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN electricityStatus TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN gasStatus TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN buildingClass TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN heatingCoolingSystem TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasLobby INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasSecurityGuard INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN streetPosition TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN frontageWidth REAL DEFAULT NULL")

        // آپارتمان
        db.execSQL("ALTER TABLE properties ADD COLUMN unitDirection TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN unitCondition TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN flooring TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN facade TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN bathroomCount INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasPool INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasSauna INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasGym INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasVideoIntercom INTEGER NOT NULL DEFAULT 0")

        // زمین
        db.execSQL("ALTER TABLE properties ADD COLUMN landUse TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN streetWidth REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN allowedDensity INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN allowedFloors INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN landPosition TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasWall INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasBuildingPermit INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN landSlope TEXT DEFAULT NULL")

        // تجاری
        db.execSQL("ALTER TABLE properties ADD COLUMN keyMoney INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN commercialFloorPosition TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN ceilingHeight REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN businessLicenseType TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasThreePhaseElectricity INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasRestroom INTEGER NOT NULL DEFAULT 0")

        // اداری
        db.execSQL("ALTER TABLE properties ADD COLUMN partitionCount INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasFalseFloor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasFalseCeiling INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE properties ADD COLUMN hasConferenceRoom INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * فاز ۵.۶ — نسخه ۶ به ۷: افزودن ستون `additionalNotes` (یادداشت آزاد برای موارد شخصی هر رکورد،
 * جدا از توضیحات نمایش‌داده‌شده به مشتری). یک migration جداگانه (نه ویرایش MIGRATION_5_6) چون
 * اون migration ممکنه قبلاً روی گوشی‌هایی اجرا شده باشه؛ migration‌های قبلی نباید بعد از انتشار
 * تغییر کنن.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE properties ADD COLUMN additionalNotes TEXT DEFAULT NULL")
    }
}
