package com.realestate.sami

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.realestate.sami.notification.FollowUpReminderWorker
import com.realestate.sami.notification.NotificationHelper
import com.realestate.sami.sync.SyncPreferences
import com.realestate.sami.sync.SyncWorker
import com.realestate.sami.util.LanguagePreferences
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RealEstateApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncPreferences: SyncPreferences

    @Inject
    lateinit var languagePreferences: LanguagePreferences

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // زبان اپ (فارسی/انگلیسی) مستقل از زبان سیستم است؛ همون زبانی که آخرین‌بار در تنظیمات
        // انتخاب شده (یا فارسی به‌صورت پیش‌فرض) رو از همین ابتدای اجرا اعمال می‌کنیم.
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languagePreferences.languageTag)
        )

        // زمان‌بندی همگام‌سازی دوره‌ای تیمی (هر ۳۰ دقیقه، فقط وقتی اینترنت وصل است، یا فقط Wi-Fi اگر کاربر فعال کرده باشد)
        SyncWorker.schedulePeriodic(this, wifiOnly = syncPreferences.autoSyncWifiOnly)

        // فاز ۵: کانال نوتیفیکیشن پیگیری + زمان‌بندی بررسی دوره‌ای یادآوری‌های سررسیده
        NotificationHelper.createChannel(this)
        FollowUpReminderWorker.schedulePeriodic(this)
    }
}
