package com.realestate.sami.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.realestate.sami.data.local.dao.ClientDao
import com.realestate.sami.data.local.dao.ContactLogDao
import com.realestate.sami.data.local.dao.PropertyDao
import com.realestate.sami.data.local.dao.VisitDao
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.VisitEntity

@Database(
    entities = [
        PropertyEntity::class,
        ClientEntity::class,
        ContactLogEntity::class,
        VisitEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun clientDao(): ClientDao
    abstract fun contactLogDao(): ContactLogDao
    abstract fun visitDao(): VisitDao

    companion object {
        const val DATABASE_NAME = "realestate_consultant.db"
    }
}
