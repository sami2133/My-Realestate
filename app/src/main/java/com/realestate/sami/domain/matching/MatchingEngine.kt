package com.realestate.sami.domain.matching

import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.repository.ClientRepository
import com.realestate.sami.data.repository.PropertyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * منطق تطبیق دوطرفه:
 *  - وقتی ملک جدیدی ثبت می‌شود -> کدام متقاضیان به آن می‌خورند؟
 *  - وقتی متقاضی جدیدی ثبت می‌شود -> کدام ملک‌های موجود به او می‌خورند؟
 */
@Singleton
class MatchingEngine @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val clientRepository: ClientRepository
) {
    /** برای یک ملک تازه ثبت‌شده، لیست متقاضیان سازگار را برمی‌گرداند. */
    fun matchesForProperty(property: PropertyEntity): Flow<List<ClientEntity>> =
        clientRepository.findMatchingClients(property)

    /** برای یک متقاضی تازه ثبت‌شده، لیست ملک‌های سازگار را برمی‌گرداند. */
    fun matchesForClient(client: ClientEntity): Flow<List<PropertyEntity>> =
        propertyRepository.findMatchingProperties(
            propertyType = client.desiredPropertyType,
            dealType = client.desiredDealType,
            minArea = client.minArea,
            maxArea = client.maxArea,
            minRooms = client.minRooms,
            maxRooms = client.maxRooms,
            maxTotalPrice = client.maxTotalPrice,
            maxDepositPrice = client.maxDepositPrice,
            maxRentPrice = client.maxRentPrice,
            needsParking = client.needsParking,
            needsStorage = client.needsStorage,
            needsElevator = client.needsElevator
        )
}
