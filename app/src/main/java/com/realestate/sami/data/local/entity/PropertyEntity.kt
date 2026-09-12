package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PropertyType { APARTMENT, VILLA, LAND, COMMERCIAL, OFFICE }
/**
 * [MORTGAGE] («رهن کامل») دیگر به‌عنوان یک نوع معامله‌ی جدا قابل‌انتخاب نیست — چون در ایران
 * رهن کامل و اجاره دقیقاً یک فرآیند هستند و فقط با معادله‌ی تبدیل رهن↔اجاره به هم تبدیل می‌شوند
 * (فاز ۵.۲). این مقدار فقط برای سازگاری با رکوردهای قدیمی (محلی یا روی Drive) نگه داشته شده تا
 * `DealType.valueOf("MORTGAGE")` خطا ندهد؛ هر جای دیگر کد باید با آن مثل [RENT] رفتار کند.
 *
 * [EXCHANGE] («مبادله») هم دیگر یک نوع معامله‌ی جدا نیست — چون در عمل همیشه زیرمجموعه‌ی خرید و
 * فروش است (ملکی که برای فروش گذاشته شده، گاهی مالک حاضر به معاوضه هم هست). حالا این یک پرچم
 * روی [DealType.SALE] است: [PropertyEntity.isExchangeable] (فاز ۵.۳). این مقدار فقط برای
 * سازگاری با رکوردهای قدیمی نگه داشته شده؛ هر جای دیگر کد باید با آن مثل [SALE] + isExchangeable
 * رفتار کند.
 */
enum class DealType { SALE, RENT, MORTGAGE, EXCHANGE }
enum class PropertyStatus { AVAILABLE, RESERVED, SOLD_OR_RENTED, ARCHIVED }

// ===== فاز ۵.۵ — ویژگی‌های تکمیلی مشخصات ملک (بر اساس نوع ملک) =====

/** نوع سند مالکیت — برای همه‌ی انواع ملک کاربرد دارد. */
enum class DeedType { SINGLE_PAGE, MANGOLEH, AGREEMENT, UNDER_CONSTRUCTION, SIX_DANG, ENDOWMENT, OTHER }

/** وضعیت اشتراک آب/برق/گاز — برای آپارتمان، ویلایی و زمین. */
enum class UtilityStatus { AVAILABLE, NOT_AVAILABLE, OBTAINABLE }

/** کلاس ساختمان (استاندارد رایج در آگهی‌های اداری) — برای آپارتمان و اداری. */
enum class BuildingClass { A, B, C }

/** سیستم سرمایش/گرمایش — برای آپارتمان و اداری. */
enum class HeatingCoolingSystem { PACKAGE, RADIATOR, SPLIT, FAN_COIL, CENTRAL, OTHER }

// --- مخصوص آپارتمان ---
enum class UnitDirection { NORTH, SOUTH, EAST, WEST, TWO_SIDED }
enum class UnitCondition { NEW, RENOVATED, LIVED_IN, UNOCCUPIED }
enum class FlooringType { CERAMIC, PARQUET, MOSAIC, STONE, OTHER }
enum class FacadeType { STONE, BRICK, COMPOSITE, OTHER }

// --- مخصوص زمین ---
enum class LandUse { RESIDENTIAL, COMMERCIAL, OFFICE, AGRICULTURAL, INDUSTRIAL, GARDEN }
enum class LandPosition { NORTH, SOUTH, TWO_SIDED, CORNER_THREE, CORNER_FOUR }
enum class LandSlope { FLAT, SLOPED }

// --- مخصوص تجاری ---
enum class CommercialPosition { BASEMENT, GROUND, UPPER_FLOOR }
/** موقعیت نسبت به گذر — برای تجاری و اداری. */
enum class StreetPosition { MAIN_STREET, SIDE_STREET }

/**
 * ملکی که یک "معرف" (مالک یا واسطه) به دفتر معرفی کرده است.
 */
@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // اطلاعات معرف / مالک ملک
    val ownerName: String,
    val ownerPhone: String,
    val ownerNote: String? = null,

    // مشخصات ملک
    val propertyType: PropertyType,
    val dealType: DealType,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val area: Double,                // متراژ (متر مربع)
    val rooms: Int,
    val buildingAge: Int? = null,    // سن بنا به سال
    val floor: Int? = null,
    val totalFloors: Int? = null,
    val hasParking: Boolean = false,
    val hasStorage: Boolean = false,
    val hasElevator: Boolean = false,
    val hasBalcony: Boolean = false,

    // قیمت‌گذاری (بسته به dealType پر می‌شود)
    val totalPrice: Long? = null,      // برای فروش
    val depositPrice: Long? = null,    // ودیعه/رهن
    val rentPrice: Long? = null,       // اجاره ماهانه

    /**
     * فاز ۵.۳ — فقط برای dealType=SALE معنا دارد: آیا مالک/معرف علاوه بر فروش نقدی، معاوضه را هم
     * می‌پذیرد. وقتی true باشد، در صفحه‌ی ثبت/جزئیات ملک می‌توان نوع ملک مدنظر برای معاوضه را هم
     * مشخص کرد ([exchangePreferredType], [exchangeNote]).
     */
    val isExchangeable: Boolean = false,
    /** نوع ملکی که برای معاوضه مدنظر است؛ null یعنی مالک به هر نوع ملکی باز است. */
    val exchangePreferredType: PropertyType? = null,
    /** توضیح تکمیلی معاوضه (مثلاً منطقه‌ی مدنظر، تفاوت نقدی قابل قبول و...). */
    val exchangeNote: String? = null,

    /**
     * فاز ۵.۲ — حداقل رهنی که مالک/معرف حاضر است در ازای افزایش اجاره بپذیرد (معادله‌ی موزون
     * رهن↔اجاره). null یعنی رهن این ملک ثابت است و قابل تعدیل نیست. وقتی مقدار دارد، در صفحه‌ی
     * ثبت/جزئیات ملک یک نوار لغزنده‌ی رهن↔اجاره نمایش داده می‌شود که بین این مقدار (حداقل) و
     * [depositPrice] (حداکثر، همان رهن پایه‌ی ثبت‌شده) قابل تنظیم است.
     */
    val minAdjustableDeposit: Long? = null,

    val description: String? = null,
    /** فاز ۵.۶ — یادداشت آزاد برای درج موارد شخصی/داخلی هر رکورد؛ جدا از [description] که برای نمایش به مشتری است. */
    val additionalNotes: String? = null,
    val images: List<PropertyImage> = emptyList(), // تصاویر ملک؛ هرکدام هم مسیر محلی (اگر روی این دستگاه موجود باشد) و هم شناسه‌ی فایل روی Drive (بعد از sync) را نگه می‌دارد
    val documentUris: String = "",     // اسکن سند/مدارک

    // ===== فاز ۵.۵ — مشخصات تکمیلی، بسته به نوع ملک (همه nullable و اختیاری) =====

    /** نوع سند — برای همه‌ی انواع ملک. */
    val deedType: DeedType? = null,

    // --- آپارتمان (و برخی مشترک با ویلایی/اداری) ---
    val unitDirection: UnitDirection? = null,
    val unitCondition: UnitCondition? = null,
    val heatingCoolingSystem: HeatingCoolingSystem? = null,
    val flooring: FlooringType? = null,
    val facade: FacadeType? = null,
    val bathroomCount: Int? = null,
    val buildingClass: BuildingClass? = null, // آپارتمان و اداری
    val hasPool: Boolean = false,
    val hasSauna: Boolean = false,
    val hasGym: Boolean = false,
    val hasLobby: Boolean = false,          // آپارتمان و اداری
    val hasSecurityGuard: Boolean = false,  // آپارتمان و اداری
    val hasVideoIntercom: Boolean = false,

    // --- آپارتمان، ویلایی و زمین: اشتراک آب/برق/گاز ---
    val waterStatus: UtilityStatus? = null,
    val electricityStatus: UtilityStatus? = null,
    val gasStatus: UtilityStatus? = null,

    // --- زمین ---
    val landUse: LandUse? = null,
    val frontageWidth: Double? = null, // طول بر (زمین) / عرض ویترین (تجاری) — یک مفهوم مشترک
    val streetWidth: Double? = null,
    val allowedDensity: Int? = null,   // تراکم مجاز (درصد)
    val allowedFloors: Int? = null,    // تعداد طبقات مجاز ساخت
    val landPosition: LandPosition? = null,
    val hasWall: Boolean = false,
    val hasBuildingPermit: Boolean = false,
    val landSlope: LandSlope? = null,

    // --- تجاری ---
    val keyMoney: Long? = null, // مبلغ سرقفلی — جدا از رهن/اجاره
    val commercialFloorPosition: CommercialPosition? = null,
    val ceilingHeight: Double? = null,
    val businessLicenseType: String? = null,
    val hasThreePhaseElectricity: Boolean = false,
    val hasRestroom: Boolean = false,
    val streetPosition: StreetPosition? = null, // تجاری و اداری

    // --- اداری ---
    val partitionCount: Int? = null,
    val hasFalseFloor: Boolean = false,
    val hasFalseCeiling: Boolean = false,
    val hasConferenceRoom: Boolean = false,

    val status: PropertyStatus = PropertyStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // برای همگام‌سازی ابری در فازهای بعدی
    val remoteId: String? = null,
    val isSynced: Boolean = false,

    /** soft-delete: به‌جای حذف فیزیکی، این پرچم ست می‌شود تا حذف بین دستگاه‌ها هم sync شود. */
    val isDeleted: Boolean = false
)
