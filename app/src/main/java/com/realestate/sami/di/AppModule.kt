package com.realestate.sami.di

import android.content.Context
import androidx.room.Room
import com.realestate.sami.data.local.AppDatabase
import com.realestate.sami.data.local.MIGRATION_3_4
import com.realestate.sami.data.local.MIGRATION_4_5
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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            // fallbackToDestructiveMigration فقط برای گوشی‌هایی که به‌هر دلیلی هنوز خیلی عقب‌تر
            // (نسخه ۱ یا ۲) موندن اجرا می‌شه — که آپدیت‌شون هم قبلاً همین‌طور destructive بوده.
            // برای مسیر واقعی و رایج (نسخه ۳ -> ۴ -> ۵) از MIGRATION_3_4 و MIGRATION_4_5 بالا
            // استفاده می‌شه و هیچ داده‌ای پاک نمی‌شه.
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
