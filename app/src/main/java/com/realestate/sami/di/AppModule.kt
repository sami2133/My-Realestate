package com.realestate.sami.di

import android.content.Context
import androidx.room.Room
import com.realestate.sami.data.local.AppDatabase
import com.realestate.sami.data.local.dao.ClientDao
import com.realestate.sami.data.local.dao.ContactLogDao
import com.realestate.sami.data.local.dao.PropertyDao
import com.realestate.sami.data.local.dao.VisitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // fallbackToDestructiveMigration فقط در توسعه اولیه مناسب است؛
            // پیش از انتشار باید migration واقعی نوشته شود.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePropertyDao(db: AppDatabase): PropertyDao = db.propertyDao()

    @Provides
    fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideContactLogDao(db: AppDatabase): ContactLogDao = db.contactLogDao()

    @Provides
    fun provideVisitDao(db: AppDatabase): VisitDao = db.visitDao()
}
