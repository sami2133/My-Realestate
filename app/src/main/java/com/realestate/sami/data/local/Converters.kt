package com.realestate.sami.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.realestate.sami.data.local.entity.*

/**
 * Room به‌صورت پیش‌فرض enum های ساده رو به‌عنوان String ذخیره می‌کنه،
 * اما برای اطمینان از سازگاری در آینده (مثلاً migration) این‌ها رو صریح تعریف می‌کنیم.
 */
class Converters {
    private val gson = Gson()

    /** لیست تصاویر ملک به‌صورت JSON در یک ستون متنی ذخیره می‌شود. */
    @TypeConverter
    fun fromPropertyImages(value: List<PropertyImage>): String = gson.toJson(value)

    @TypeConverter
    fun toPropertyImages(value: String): List<PropertyImage> {
        if (value.isBlank()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, PropertyImage::class.java).type
        return gson.fromJson(value, type) ?: emptyList()
    }
    @TypeConverter
    fun fromPropertyType(value: PropertyType): String = value.name
    @TypeConverter
    fun toPropertyType(value: String): PropertyType = PropertyType.valueOf(value)

    @TypeConverter
    fun fromDealType(value: DealType): String = value.name
    @TypeConverter
    fun toDealType(value: String): DealType = DealType.valueOf(value)

    @TypeConverter
    fun fromPropertyStatus(value: PropertyStatus): String = value.name
    @TypeConverter
    fun toPropertyStatus(value: String): PropertyStatus = PropertyStatus.valueOf(value)

    @TypeConverter
    fun fromClientStatus(value: ClientStatus): String = value.name
    @TypeConverter
    fun toClientStatus(value: String): ClientStatus = ClientStatus.valueOf(value)

    @TypeConverter
    fun fromRelatedType(value: RelatedType): String = value.name
    @TypeConverter
    fun toRelatedType(value: String): RelatedType = RelatedType.valueOf(value)

    @TypeConverter
    fun fromVisitResult(value: VisitResult): String = value.name
    @TypeConverter
    fun toVisitResult(value: String): VisitResult = VisitResult.valueOf(value)

    // ===== فاز ۶ — کانورترهای enum های مشخصات تکمیلی ملک (فرم بازطراحی‌شده) =====
    // همه‌ی این فیلدها روی PropertyEntity nullable هستن؛ Room خودش null را با همین کانورترهای
    // غیر-nullable مدیریت می‌کنه.

    @TypeConverter
    fun fromDeedType(value: DeedType): String = value.name
    @TypeConverter
    fun toDeedType(value: String): DeedType = DeedType.valueOf(value)

    @TypeConverter
    fun fromBuildingClass(value: BuildingClass): String = value.name
    @TypeConverter
    fun toBuildingClass(value: String): BuildingClass = BuildingClass.valueOf(value)

    @TypeConverter
    fun fromCabinetMaterial(value: CabinetMaterial): String = value.name
    @TypeConverter
    fun toCabinetMaterial(value: String): CabinetMaterial = CabinetMaterial.valueOf(value)

    @TypeConverter
    fun fromFlooringType(value: FlooringType): String = value.name
    @TypeConverter
    fun toFlooringType(value: String): FlooringType = FlooringType.valueOf(value)

    @TypeConverter
    fun fromWallCovering(value: WallCovering): String = value.name
    @TypeConverter
    fun toWallCovering(value: String): WallCovering = WallCovering.valueOf(value)

    @TypeConverter
    fun fromCeilingCovering(value: CeilingCovering): String = value.name
    @TypeConverter
    fun toCeilingCovering(value: String): CeilingCovering = CeilingCovering.valueOf(value)

    @TypeConverter
    fun fromCoolingSystem(value: CoolingSystem): String = value.name
    @TypeConverter
    fun toCoolingSystem(value: String): CoolingSystem = CoolingSystem.valueOf(value)

    @TypeConverter
    fun fromHeatingSystem(value: HeatingSystem): String = value.name
    @TypeConverter
    fun toHeatingSystem(value: String): HeatingSystem = HeatingSystem.valueOf(value)

    @TypeConverter
    fun fromLandUse(value: LandUse): String = value.name
    @TypeConverter
    fun toLandUse(value: String): LandUse = LandUse.valueOf(value)

    @TypeConverter
    fun fromLandPosition(value: LandPosition): String = value.name
    @TypeConverter
    fun toLandPosition(value: String): LandPosition = LandPosition.valueOf(value)

    @TypeConverter
    fun fromLandSlope(value: LandSlope): String = value.name
    @TypeConverter
    fun toLandSlope(value: String): LandSlope = LandSlope.valueOf(value)

    @TypeConverter
    fun fromCommercialUsage(value: CommercialUsage): String = value.name
    @TypeConverter
    fun toCommercialUsage(value: String): CommercialUsage = CommercialUsage.valueOf(value)

    @TypeConverter
    fun fromCommercialPosition(value: CommercialPosition): String = value.name
    @TypeConverter
    fun toCommercialPosition(value: String): CommercialPosition = CommercialPosition.valueOf(value)

    @TypeConverter
    fun fromStreetPosition(value: StreetPosition): String = value.name
    @TypeConverter
    fun toStreetPosition(value: String): StreetPosition = StreetPosition.valueOf(value)
}
