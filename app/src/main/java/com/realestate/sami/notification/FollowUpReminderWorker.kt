package com.realestate.sami.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.realestate.sami.data.local.dao.ContactLogDao
import com.realestate.sami.data.local.entity.RelatedType
import com.realestate.sami.data.repository.ClientRepository
import com.realestate.sami.data.repository.PropertyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * فاز ۵ — نوتیفیکیشن پیگیری: هر ۶ ساعت بررسی می‌کند آیا followUpDate ای فرارسیده و هنوز به
 * کاربر یادآوری نشده؛ اگر بله، یک نوتیفیکیشن محلی نشان می‌دهد (بدون نیاز به سرور یا اینترنت).
 */
@HiltWorker
class FollowUpReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val contactLogDao: ContactLogDao,
    private val propertyRepository: PropertyRepository,
    private val clientRepository: ClientRepository,
    private val notificationPreferences: NotificationPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val dueLogs = contactLogDao.getPendingFollowUpsOnce()
            .filter { it.followUpDate != null && it.followUpDate <= now }
            .filter { !notificationPreferences.isAlreadyNotified(it.id) }

        for (log in dueLogs) {
            val relatedName = when (log.relatedType) {
                RelatedType.PROPERTY -> propertyRepository.getById(log.relatedId)?.address
                RelatedType.CLIENT -> clientRepository.getById(log.relatedId)?.fullName
            } ?: continue

            NotificationHelper.showFollowUpReminder(
                context = applicationContext,
                contactLogId = log.id,
                relatedName = relatedName,
                note = log.note
            )
            notificationPreferences.markNotified(log.id)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "follow_up_reminder_periodic"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder().build() // بدون نیاز به اینترنت؛ فقط دیتابیس محلی
            val request = PeriodicWorkRequestBuilder<FollowUpReminderWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
