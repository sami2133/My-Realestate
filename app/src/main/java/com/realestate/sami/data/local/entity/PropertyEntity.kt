package com.realestate.sami.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * فاز ۶ — بازطراحی کامل مشخصات ملک بر اساس فرم‌های جدید (آپارتمان/ویلایی/تجاری/زمین).
 * نوع ملک «اداری» به‌عنوان نوع جدا حذف شد و به‌صورت یکی از گزینه‌های «کاربرد» ([CommercialUsage.OFFICE])
 * زیر نوع «تجاری» ادغام شد — چون در فرم جدید، تجاری/مجتمع‌تجاری/اداری/کارگاه/سوله همگی یک ساختار
 * فرم مشترک دارند و فقط کاربردشان فرق می‌کند.
 */
enum class PropertyType { APARTMENT, VILLA, LAND, COMMERCIAL }

/**
 * [MORTGAGE] («رهن کامل») دیگر به‌عنوان یک نوع معامله‌ی جدا قابل‌انتخاب نیست — چون در ایران
 * رهن کامل و اجاره دقیقاً یک فرآیند هستند و فقط با معادله‌ی تبدیل رهن↔اجاره به هم تبدیل می‌شوند.
 * این مقدار فقط برای سازگاری با رکوردهای قدیمی نگه داشته شده تا `DealType.valueOf("MORTGAGE")`
 * خطا ندهد؛ هر جای دیگر کد باید با آن مثل [RENT] رفتار کند.
 *
 * [EXCHANGE] («مبادله») هم دیگر یک نوع معامله‌ی جدا نیست، بلکه زیرمجموعه‌ی خرید و فروش است؛
 * این حالا یک پرچم روی [DealType.SALE] است: [PropertyEntity.isExchangeable].
 */
enum class DealType { SALE, RENT, MORTGAGE, EXCHANGE }
enum class PropertyStatus { AVAILABLE, RESERVED, SOLD_OR_RENTED, ARCHIVED }

/** نوع سند مالکیت — برای همه‌ی انواع ملک («در حال ساخت» فقط برای آپارتمان/ویلایی/تجاری معنا دارد). */
enum class DeedType { SINGLE_PAGE, MANGOLEH, UNDIVIDED, AGREEMENT, UNDER_CONSTRUCTION, ENDOWMENT }

/** کلاس ساختمان — آپارتمان، ویلایی و تجاری. */
enum class BuildingClass { A_PLUS, A, B, C }

// --- آشپزخانه: آپارتمان و ویلایی ---
enum class CabinetMaterial { HIGH_GLOSS, MDF, MEMBRANE, WOOD, METAL }

// --- کف‌پوش/دیوارپوش/سقف‌پوش: آپارتمان، ویلایی و تجاری ---
enum class FlooringType { CERAMIC, PARQUET, MOSAIC, STONE, CONCRETE, OTHER }
enum class WallCovering { PLASTER, WALLPAPER, PAINT, CERAMIC, CONCRETE }
enum class CeilingCovering { PLASTER, GYPSUM_BOARD, PAINT, SUSPENDED }

// --- سرمایش/گرمایش: آپارتمان، ویلایی و تجاری ---
enum class CoolingSystem { SPLIT, COOLER, CENTRAL }
enum class HeatingSystem { PACKAGE, WATER_HEATER, RADIATOR, FAN_COIL, CENTRAL }

// --- زمین ---
enum class LandUse { COMMERCIAL, RESIDENTIAL, GARDEN, AGRICULTURAL, INDUSTRIAL, UNPLANNED }
enum class LandPosition { NORTH, SOUTH, TWO_SIDED, CORNER_THREE, CORNER_FOUR }
enum class LandSlope { FLAT, SLOPED }

// --- تجاری (شامل اداری سابق) ---
/** کاربرد ملک تجاری — «اداری» دیگر نوع ملک جدا نیست، این‌جا یکی از کاربردهاست. */
enum class CommercialUsage { RETAIL, COMMERCIAL_COMPLEX, OFFICE, WORKSHOP, WAREHOUSE }
enum class CommercialPosition { BASEMENT, GROUND, UPPER_FLOOR }
enum class StreetPosition { MAIN_STREET, SIDE_STREET }

/**
 * ملکی که یک «معرف» (مالک یا واسطه) به دفتر معرفی کرده است.
 * ترتیب فیلدها عمداً با ترتیب نمایش فرم ثبت هم‌خوان است: معرف/مالک → عکس‌ها → مشخصات پایه →
 * مشخصات تکمیلی بسته به نوع ملک → توضیحات → قیمت‌گذاری.
 */
@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // ===== اطلاعات معرف / مالک ملک =====
    val ownerName: String,
    val ownerPhone: String,
    val ownerNote: String? = null,

    // ===== عکس‌ها و مدارک =====
    val images: List<PropertyImage> = emptyList(),
    val documentUris: String = "",

    // ===== مشخصات پایه (مشترک) =====
    val propertyType: PropertyType,
    val dealType: DealType,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,

    /** مساحت بنا (آپارتمان/تجاری) یا مساحت بنای ویلایی؛ برای زمین یعنی مساحت‌کل. */
    val area: Double,
    /** مساحت‌کل — فقط ویلایی (جدا از [area] که آن‌جا یعنی مساحت‌بنا). */
    val totalArea: Double? = null,
    /** مساحت بالکن — فقط تجاری. */
    val balconyArea: Double? = null,
    /** تعداد اطاق — آپارتمان/ویلایی/تجاری؛ زمین ندارد. */
    val rooms: Int? = null,

    val floor: Int? = null,
    val totalFloors: Int? = null,
    /** تعداد واحد در طبقه — فقط آپارتمان. */
    val unitsPerFloor: Int? = null,
    /** قدمت بنا — فقط آپارتمان و ویلایی. */
    val buildingAge: Int? = null,

    val deedType: DeedType? = null,
    /** کلاس ساختمان — آپارتمان، ویلایی و تجاری. */
    val buildingClass: BuildingClass? = null,

    // ===== آشپزخانه — آپارتمان و ویلایی =====
    val cabinetMaterial: CabinetMaterial? = null,
    val hasKitchenIsland: Boolean = false,  // جزیره
    val hasKitchenette: Boolean = false,    // مطبخ
    val hasBarbecue: Boolean = false,       // باربیکیو

    // ===== کف‌پوش / دیوارپوش / سقف‌پوش — آپارتمان، ویلایی، تجاری =====
    val flooring: FlooringType? = null,
    val wallCovering: WallCovering? = null,
    val ceilingCovering: CeilingCovering? = null,

    // ===== سرویس بهداشتی — آپارتمان و ویلایی (چندانتخابی) =====
    val hasIranianToilet: Boolean = false,
    val hasWesternToilet: Boolean = false,
    val hasJacuzzi: Boolean = false,

    // ===== سرمایش/گرمایش — آپارتمان، ویلایی، تجاری =====
    val coolingSystem: CoolingSystem? = null,
    val heatingSystem: HeatingSystem? = null,

    // ===== امکانات آپارتمان/ویلایی/تجاری (چندانتخابی) =====
    val hasStorage: Boolean = false,               // انباری
    val hasElevator: Boolean = false,               // آسانسور
    val hasParking: Boolean = false,                // پارکینگ
    val hasPrivateParkingPath: Boolean = false,     // مسیر پارکینگ اختصاصی
    val hasSharedParkingPath: Boolean = false,      // مسیر پارکینگ اشتراکی
    val hasAutomaticParkingDoor: Boolean = false,   // درب پارکینگ اتوماتیک
    val hasPrivateWater: Boolean = false,           // آب اختصاصی
    val hasSharedWater: Boolean = false,            // آب اشتراکی
    val hasPrivateElectricity: Boolean = false,     // برق اختصاصی
    val hasSharedElectricity: Boolean = false,      // برق اشتراکی
    val hasPrivateGas: Boolean = false,             // گاز اختصاصی
    val hasSharedGas: Boolean = false,              // گاز اشتراکی
    val hasBuiltInCloset: Boolean = false,          // کمد دیواری
    val hasVideoIntercom: Boolean = false,          // آیفون تصویری

    // ===== امکانات ساختمان — آپارتمان (و استخر/سالن ورزشی قابل‌استفاده برای ویلایی) =====
    val hasLobby: Boolean = false,
    val hasSecurityGuard: Boolean = false,
    val hasPool: Boolean = false,
    val hasGym: Boolean = false,
    val hasCourtyard: Boolean = false,   // محوطه حیاط

    // ===== ویلایی =====
    val yardArea: Double? = null,          // حیاط
    val terraceArea: Double? = null,       // تراس — آپارتمان و ویلایی
    val masterBedroomCount: Int? = null,   // خواب مستر
    val hasCaretaker: Boolean = false,     // سرایداری

    // ===== زمین =====
    val landUse: LandUse? = null,
    /** طول‌دهانه — زمین و تجاری (مفهوم مشترک؛ برای تجاری یعنی عرض ویترین). */
    val frontageWidth: Double? = null,
    val streetWidth: Double? = null,               // طول‌گذر — فقط زمین
    val buildingPermitArea: Double? = null,         // پروانه ساختمانی به‌مساحت
    val landPosition: LandPosition? = null,
    val landSlope: LandSlope? = null,
    val hasWall: Boolean = false,                   // محصور با دیوار
    val waterRightOwned: Boolean = false,           // امتیاز آب اختصاصی
    val waterRightObtainable: Boolean = false,      // امکان اخذ امتیاز آب
    val electricityRightOwned: Boolean = false,
    val electricityRightObtainable: Boolean = false,
    val gasRightOwned: Boolean = false,
    val gasRightObtainable: Boolean = false,

    // ===== تجاری (شامل اداری سابق) =====
    val commercialUsage: CommercialUsage? = null,           // کاربرد
    val commercialFloorPosition: CommercialPosition? = null,
    val streetPosition: StreetPosition? = null,
    val ceilingHeight: Double? = null,                       // طول‌ارتفاع
    val hasThreePhaseElectricity: Boolean = false,           // برق صنعتی
    val hasKitchen: Boolean = false,                          // آشپزخانه
    val hasRestroom: Boolean = false,                         // سرویس بهداشتی

    // ===== توضیحات تکمیلی (همیشه در انتها) =====
    val description: String? = null,
    val additionalNotes: String? = null,

    // ===== قیمت‌گذاری (همیشه در انتها) =====
    val totalPrice: Long? = null,       // برای فروش
    val depositPrice: Long? = null,     // ودیعه/رهن
    val rentPrice: Long? = null,        // اجاره ماهانه
    val isExchangeable: Boolean = false,
    val exchangePreferredType: PropertyType? = null,
    val exchangeNote: String? = null,
    /** حداقل رهنی که مالک حاضر است در ازای افزایش اجاره بپذیرد؛ null یعنی رهن ثابت است. */
    val minAdjustableDeposit: Long? = null,

    val status: PropertyStatus = PropertyStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // برای همگام‌سازی ابری
    val remoteId: String? = null,
    val isSynced: Boolean = false,

    /** نام دلخواهی که در تنظیمات sync («نام/شماره‌ی نمایشی من») ثبت شده و آخرین‌بار این رکورد را
     *  ثبت/ویرایش کرده است؛ برای تشخیص سریع «این رکورد آخرین‌بار توسط کدوم عضو تیم تغییر کرده».
     *  اگر آن تنظیم خالی باشد (یا sync تیمی استفاده نشود)، null باقی می‌ماند. */
    val lastEditedBy: String? = null,

    /** true یعنی مقادیر فعلی ownerName/ownerPhone/ownerNote این رکورد از نسخه‌ی ماسک‌شده‌ای
     *  آمده‌اند که یک همکار دیگر (نه خود این دستگاه) آپلود کرده — یعنی نام/شماره‌ی واقعیِ مالک
     *  نیستند، بلکه نام/شماره‌ی تماس همان همکار ثبت‌کننده‌اند. برای جلوگیری از این‌که merge بعدی
     *  این مقدار ماسک‌شده را روی اطلاعات واقعیِ دستگاهِ ثبت‌کننده‌ی اصلی بنویسد استفاده می‌شود.
     *  به‌صورت محلی همیشه false است مگر رکورد از Drive دانلود شده باشد. */
    val ownerContactIsMasked: Boolean = false,

    /** soft-delete: به‌جای حذف فیزیکی، این پرچم ست می‌شود تا حذف بین دستگاه‌ها هم sync شود. */
    val isDeleted: Boolean = false
)
