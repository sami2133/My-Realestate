package com.realestate.sami.data.local.dao

import androidx.room.*
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.RelatedType
import com.realestate.sami.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ContactLogEntity): Long

    @Update
    suspend fun update(log: ContactLogEntity)

    @Delete
    suspend fun delete(log: ContactLogEntity)

    @Query("SELECT * FROM contact_logs WHERE relatedId = :relatedId AND relatedType = :type ORDER BY contactDate DESC")
    fun getForEntity(relatedId: Long, type: RelatedType): Flow<List<ContactLogEntity>>

    @Query("SELECT * FROM contact_logs WHERE followUpDate IS NOT NULL AND isFollowUpDone = 0 ORDER BY followUpDate ASC")
    fun getPendingFollowUps(): Flow<List<ContactLogEntity>>
}

@Dao
interface VisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity)

    @Delete
    suspend fun delete(visit: VisitEntity)

    @Query("SELECT * FROM visits WHERE propertyId = :propertyId ORDER BY visitDate DESC")
    fun getForProperty(propertyId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE clientId = :clientId ORDER BY visitDate DESC")
    fun getForClient(clientId: Long): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE visitDate BETWEEN :startOfDay AND :endOfDay ORDER BY visitDate ASC")
    fun getForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<VisitEntity>>
}
