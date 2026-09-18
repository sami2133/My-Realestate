package com.realestate.sami.data.repository

import com.realestate.sami.data.local.dao.VisitDao
import com.realestate.sami.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * فاز ۵: لایه دسترسی به قرارهای بازدید (VisitEntity)، برای تقویم و داشبورد آماری.
 */
@Singleton
class VisitRepository @Inject constructor(
    private val visitDao: VisitDao
) {
    fun getForProperty(propertyId: Long): Flow<List<VisitEntity>> = visitDao.getForProperty(propertyId)

    fun getForClient(clientId: Long): Flow<List<VisitEntity>> = visitDao.getForClient(clientId)

    fun getAll(): Flow<List<VisitEntity>> = visitDao.getAll()

    fun getUpcoming(fromMillis: Long = System.currentTimeMillis()): Flow<List<VisitEntity>> =
        visitDao.getUpcoming(fromMillis)

    suspend fun add(visit: VisitEntity): Long = visitDao.insert(visit)

    suspend fun update(visit: VisitEntity) = visitDao.update(visit)

    suspend fun delete(visit: VisitEntity) = visitDao.delete(visit)
}
