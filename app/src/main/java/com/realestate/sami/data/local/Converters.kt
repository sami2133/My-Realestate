package com.realestate.sami.data.local

import androidx.room.TypeConverter
import com.realestate.sami.data.local.entity.*

/**
 * Room به‌صورت پیش‌فرض enum های ساده رو به‌عنوان String ذخیره می‌کنه،
 * اما برای اطمینان از سازگاری در آینده (مثلاً migration) این‌ها رو صریح تعریف می‌کنیم.
 */
class Converters {
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
}
