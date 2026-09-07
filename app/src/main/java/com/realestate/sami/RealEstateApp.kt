package com.realestate.sami

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.realestate.sami.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RealEstateApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // زمان‌بندی همگام‌سازی دوره‌ای تیمی (هر ۳۰ دقیقه، فقط وقتی اینترنت وصل است)
        SyncWorker.schedulePeriodic(this)
    }
}
