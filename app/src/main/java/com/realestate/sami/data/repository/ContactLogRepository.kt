package com.realestate.sami.data.repository

import com.realestate.sami.data.local.dao.ContactLogDao
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.RelatedType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactLogRepository @Inject constructor(
    private val dao: ContactLogDao
) {
    fun getForEntity(relatedId: Long, type: RelatedType): Flow<List<ContactLogEntity>> =
        dao.getForEntity(relatedId, type)

    fun getPendingFollowUps(): Flow<List<ContactLogEntity>> = dao.getPendingFollowUps()

    suspend fun add(log: ContactLogEntity) = dao.insert(log)

    suspend fun update(log: ContactLogEntity) = dao.update(log)

    suspend fun delete(log: ContactLogEntity) = dao.delete(log)
}
