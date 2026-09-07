package com.realestate.sami.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** ذخیره‌ی وضعیت همگام‌سازی: شناسه‌ی پوشه‌ی تیمی و آخرین زمان sync موفق. */
@Singleton
class SyncPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    var teamFolderId: String?
        get() = prefs.getString(KEY_FOLDER_ID, null)
        set(value) = prefs.edit().putString(KEY_FOLDER_ID, value).apply()

    var lastSyncedAt: Long?
        get() = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 }
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value ?: 0L).apply()

    companion object {
        private const val KEY_FOLDER_ID = "team_folder_id"
        private const val KEY_LAST_SYNC = "last_synced_at"
    }
}
