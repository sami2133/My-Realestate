package com.realestate.sami

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.realestate.sami.notification.FollowUpReminderWorker
import com.realestate.sami.notification.NotificationHelper
import com.realestate.sami.sync.SyncPreferences
import com.realestate.sami.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RealEstateApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncPreferences: SyncPreferences

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // زمان‌بندی همگام‌سازی دوره‌ای تیمی (هر ۳۰ دقیقه، فقط وقتی اینترنت وصل است، یا فقط Wi-Fi اگر کاربر فعال کرده باشد)
        SyncWorker.schedulePeriodic(this, wifiOnly = syncPreferences.autoSyncWifiOnly)

        // فاز ۵: کانال نوتیفیکیشن پیگیری + زمان‌بندی بررسی دوره‌ای یادآوری‌های سررسیده
        NotificationHelper.createChannel(this)
        FollowUpReminderWorker.schedulePeriodic(this)
    }
}
