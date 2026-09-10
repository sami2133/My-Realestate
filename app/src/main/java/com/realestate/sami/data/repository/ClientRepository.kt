package com.realestate.sami.data.repository

import com.realestate.sami.data.local.dao.ClientDao
import com.realestate.sami.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao
) {
    fun getAll(): Flow<List<ClientEntity>> = clientDao.getAll()

    fun getByStatus(status: ClientStatus): Flow<List<ClientEntity>> =
        clientDao.getByStatus(status)

    fun search(query: String): Flow<List<ClientEntity>> = clientDao.search(query)

    suspend fun getById(id: Long): ClientEntity? = clientDao.getById(id)

    suspend fun save(client: ClientEntity): Long {
        val toSave = client.copy(updatedAt = System.currentTimeMillis())
        return clientDao.insert(toSave)
    }

    /** soft-delete: به‌جای حذف فیزیکی، رکورد را tombstone می‌کند تا حذف بین دستگاه‌های تیم هم sync شود. */
    suspend fun delete(client: ClientEntity) = clientDao.softDelete(client.id)

    fun findMatchingClients(property: PropertyEntity): Flow<List<ClientEntity>> =
        clientDao.findMatchingClients(
            propertyType = property.propertyType,
            dealType = property.dealType,
            area = property.area,
            rooms = property.rooms,
            totalPrice = property.totalPrice,
            depositPrice = property.depositPrice,
            rentPrice = property.rentPrice,
            hasParking = property.hasParking,
            hasStorage = property.hasStorage,
            hasElevator = property.hasElevator
        )
}
