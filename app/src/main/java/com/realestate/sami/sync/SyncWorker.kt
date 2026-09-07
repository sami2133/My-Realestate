package com.realestate.sami.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * همگام‌سازی دوره‌ای پس‌زمینه؛ فقط وقتی اینترنت وصل است اجرا می‌شود.
 * اگر کاربر هنوز وارد گوگل نشده یا مجوز Drive رو نداده، بی‌سروصدا رد می‌شود (کاربر باید از صفحه‌ی
 * تنظیمات، یک‌بار sync دستی با consent انجام بده).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return Result.success()
        return when (syncManager.syncNow(account)) {
            is SyncResult.Success -> Result.success()
            is SyncResult.ConsentRequired -> Result.success() // نیاز به تعامل کاربر دارد؛ در sync دستی انجام می‌شود
            is SyncResult.Failure -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "team_sync_periodic"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
