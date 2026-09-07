package com.realestate.sami.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.realestate.sami.data.local.dao.ClientDao
import com.realestate.sami.data.local.dao.PropertyDao
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.PropertyEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    data class Success(val syncedAt: Long) : SyncResult()
    data class ConsentRequired(val intent: Intent) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

/** نتیجه‌ی پیوستن به یک پوشه‌ی تیمی موجود با شناسه‌ی Drive. */
sealed class JoinTeamResult {
    data class Success(val folderName: String) : JoinTeamResult()
    data class Failure(val message: String) : JoinTeamResult()
}

/**
 * موتور همگام‌سازی: داده‌ی محلی Room را با فایل‌های JSON مشترک روی یک پوشه‌ی Drive ادغام می‌کند.
 *
 * استراتژی ادغام: «آخرین ویرایش برنده است» (last-write-wins) بر اساس فیلد updatedAt،
 * و تطبیق رکوردها بین دستگاه‌ها از طریق remoteId (UUID) نه id محلی (که فقط داخل هر دستگاه معتبر است).
 *
 * حذف رکورد به‌صورت soft-delete (فیلد isDeleted) مدیریت می‌شود: رکورد حذف‌شده به‌صورت tombstone
 * در فایل مشترک باقی می‌ماند تا بقیه‌ی دستگاه‌ها هم آن را حذف کنند، و بعد از
 * [DriveConstants.TOMBSTONE_RETENTION_DAYS] روز از فایل مشترک پاک‌سازی می‌شود.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val propertyDao: PropertyDao,
    private val clientDao: ClientDao,
    private val authManager: GoogleAuthManager,
    private val driveApi: DriveApiClient,
    private val syncPrefs: SyncPreferences
) {
    private val gson = Gson()

    suspend fun syncNow(account: GoogleSignInAccount): SyncResult {
        val token = when (val tokenResult = authManager.getAccessToken(context, account)) {
            is AccessTokenResult.Success -> tokenResult.token
            is AccessTokenResult.ConsentRequired -> return SyncResult.ConsentRequired(tokenResult.intent)
            is AccessTokenResult.Failure -> return SyncResult.Failure(tokenResult.message)
        }

        return try {
            runSyncWithRetry(token, account)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "همگام‌سازی ناموفق بود")
        }
    }

    /** یک بار sync کامل رو اجرا می‌کنه؛ اگه توکن منقضی بود (DriveAuthException)، یه بار توکن رو تازه می‌کنه و دوباره تلاش می‌کنه. */
    private suspend fun runSyncWithRetry(token: String, account: GoogleSignInAccount, isRetry: Boolean = false): SyncResult {
        val folderId = syncPrefs.teamFolderId
        return try {
            val resolvedFolderId = folderId ?: driveApi.ensureTeamFolder(token).also {
                syncPrefs.teamFolderId = it
            }

            val propertiesError = runCatching { syncProperties(token, resolvedFolderId) }.exceptionOrNull()
            val clientsError = runCatching { syncClients(token, resolvedFolderId) }.exceptionOrNull()

            val errors = listOfNotNull(propertiesError, clientsError)
            // اگه هرکدوم از این دو خطای «توکن نامعتبر» بود و هنوز retry نکردیم، یه بار دیگه با توکن تازه امتحان کن
            if (!isRetry && errors.any { it is DriveAuthException }) {
                authManager.clearToken(context, token)
                val freshTokenResult = authManager.getAccessToken(context, account)
                val freshToken = when (freshTokenResult) {
                    is AccessTokenResult.Success -> freshTokenResult.token
                    is AccessTokenResult.ConsentRequired -> return SyncResult.ConsentRequired(freshTokenResult.intent)
                    is AccessTokenResult.Failure -> return SyncResult.Failure(freshTokenResult.message)
                }
                return runSyncWithRetry(freshToken, account, isRetry = true)
            }

            if (errors.isNotEmpty()) {
                val summary = buildString {
                    if (propertiesError != null) append("ملک‌ها: ${propertiesError.message}")
                    if (propertiesError != null && clientsError != null) append(" | ")
                    if (clientsError != null) append("متقاضیان: ${clientsError.message}")
                }
                // موفقیت جزئی: هرکدوم از دو نوع داده که موفق شد، همون موقع commit شده (partial sync عمدی، نه atomic)
                return SyncResult.Failure(summary)
            }

            val now = System.currentTimeMillis()
            syncPrefs.lastSyncedAt = now
            SyncResult.Success(now)
        } catch (e: DriveAuthException) {
            if (!isRetry) {
                authManager.clearToken(context, token)
                val freshTokenResult = authManager.getAccessToken(context, account)
                val freshToken = when (freshTokenResult) {
                    is AccessTokenResult.Success -> freshTokenResult.token
                    is AccessTokenResult.ConsentRequired -> return SyncResult.ConsentRequired(freshTokenResult.intent)
                    is AccessTokenResult.Failure -> return SyncResult.Failure(freshTokenResult.message)
                }
                runSyncWithRetry(freshToken, account, isRetry = true)
            } else {
                SyncResult.Failure("دسترسی به Drive رد شد؛ لطفاً یک‌بار خارج و دوباره با گوگل وارد شو")
            }
        }
    }

    /**
     * به یک پوشه‌ی تیمی موجود (که یک همکار قبلاً ساخته و با تو Share کرده) با شناسه‌ی مستقیم Drive می‌پیوندد،
     * به‌جای این‌که با جستجوی نام یک پوشه‌ی جدید و جدا بسازه (که ریسک دوشاخه‌شدن داده‌ی تیم رو داره).
     */
    suspend fun joinTeamFolder(account: GoogleSignInAccount, folderId: String): JoinTeamResult {
        val trimmedId = extractFolderId(folderId)
        if (trimmedId.isEmpty()) return JoinTeamResult.Failure("شناسه‌ی پوشه نمی‌تواند خالی باشد")

        val token = when (val tokenResult = authManager.getAccessToken(context, account)) {
            is AccessTokenResult.Success -> tokenResult.token
            is AccessTokenResult.ConsentRequired -> return JoinTeamResult.Failure("ابتدا نیاز به تایید دسترسی Drive است؛ یک‌بار «همگام‌سازی الان» را بزن")
            is AccessTokenResult.Failure -> return JoinTeamResult.Failure(tokenResult.message)
        }

        return try {
            val folder = driveApi.getFolderMetadata(token, trimmedId)
            syncPrefs.teamFolderId = folder.id
            JoinTeamResult.Success(folder.name)
        } catch (e: DriveNotFoundException) {
            JoinTeamResult.Failure(e.message ?: "پوشه پیدا نشد")
        } catch (e: Exception) {
            JoinTeamResult.Failure(e.message ?: "پیوستن به پوشه‌ی تیمی ناموفق بود")
        }
    }

    /**
     * کاربر ممکنه یا شناسه‌ی خام Drive رو پیست کنه یا کل لینک اشتراک‌گذاری‌شده رو
     * (مثل https://drive.google.com/drive/folders/XXXX?usp=sharing) — این تابع در هر دو حالت
     * فقط شناسه‌ی پوشه رو استخراج می‌کنه.
     */
    private fun extractFolderId(input: String): String {
        val trimmed = input.trim()
        val marker = "folders/"
        val markerIndex = trimmed.indexOf(marker)
        if (markerIndex == -1) return trimmed
        val afterMarker = trimmed.substring(markerIndex + marker.length)
        return afterMarker.substringBefore('?').substringBefore('/').trim()
    }

    // ---------- ملک‌ها ----------

    private suspend fun syncProperties(token: String, folderId: String) {
        val local = propertyDao.getAllIncludingDeleted().map { ensureRemoteId(it) }
        val existingFile = driveApi.findFileInFolder(token, folderId, DriveConstants.PROPERTIES_FILE_NAME)
        val remoteJson = existingFile?.let { driveApi.downloadFileContent(token, it.id) }
        val remote: List<PropertyEntity> = parseList(remoteJson)

        val localById = local.associateBy { it.remoteId }
        val remoteById = remote.associateBy { it.remoteId }
        val allKeys = localById.keys + remoteById.keys

        val merged = mutableListOf<PropertyEntity>()
        for (key in allKeys) {
            val localItem = localById[key]
            val remoteItem = remoteById[key]
            when {
                localItem != null && remoteItem == null -> merged += localItem
                localItem == null && remoteItem != null -> {
                    if (remoteItem.isDeleted) {
                        // این دستگاه هیچ‌وقت این رکورد رو نداشته و روی یه دستگاه دیگه حذف شده؛
                        // نیازی به insert کردنش نیست، فقط tombstone رو توی merged نگه می‌داریم تا در فایل مشترک بمونه.
                        merged += remoteItem
                    } else {
                        val inserted = propertyDao.insert(remoteItem.copy(id = 0, isSynced = true))
                        merged += remoteItem.copy(id = inserted)
                    }
                }
                localItem != null && remoteItem != null -> {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        val toSave = remoteItem.copy(id = localItem.id, isSynced = true)
                        propertyDao.update(toSave)
                        merged += toSave
                    } else {
                        merged += localItem
                    }
                }
            }
        }

        driveApi.uploadOrUpdateJson(
            token, folderId, DriveConstants.PROPERTIES_FILE_NAME, gson.toJson(gcTombstones(merged))
        )
    }

    private fun ensureRemoteId(property: PropertyEntity): PropertyEntity =
        if (property.remoteId != null) property else property.copy(remoteId = UUID.randomUUID().toString())

    // ---------- متقاضیان ----------

    private suspend fun syncClients(token: String, folderId: String) {
        val local = clientDao.getAllIncludingDeleted().map { ensureRemoteId(it) }
        val existingFile = driveApi.findFileInFolder(token, folderId, DriveConstants.CLIENTS_FILE_NAME)
        val remoteJson = existingFile?.let { driveApi.downloadFileContent(token, it.id) }
        val remote: List<ClientEntity> = parseList(remoteJson)

        val localById = local.associateBy { it.remoteId }
        val remoteById = remote.associateBy { it.remoteId }
        val allKeys = localById.keys + remoteById.keys

        val merged = mutableListOf<ClientEntity>()
        for (key in allKeys) {
            val localItem = localById[key]
            val remoteItem = remoteById[key]
            when {
                localItem != null && remoteItem == null -> merged += localItem
                localItem == null && remoteItem != null -> {
                    if (remoteItem.isDeleted) {
                        merged += remoteItem
                    } else {
                        val inserted = clientDao.insert(remoteItem.copy(id = 0, isSynced = true))
                        merged += remoteItem.copy(id = inserted)
                    }
                }
                localItem != null && remoteItem != null -> {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        val toSave = remoteItem.copy(id = localItem.id, isSynced = true)
                        clientDao.update(toSave)
                        merged += toSave
                    } else {
                        merged += localItem
                    }
                }
            }
        }

        driveApi.uploadOrUpdateJson(
            token, folderId, DriveConstants.CLIENTS_FILE_NAME, gson.toJson(gcTombstones(merged))
        )
    }

    private fun ensureRemoteId(client: ClientEntity): ClientEntity =
        if (client.remoteId != null) client else client.copy(remoteId = UUID.randomUUID().toString())

    // ---------- کمکی ----------

    /** tombstone هایی که به‌اندازه‌ی کافی قدیمی هستن (یعنی احتمالاً به همه‌ی دستگاه‌ها رسیدن) رو از فایل مشترک حذف می‌کنه. */
    private fun <T> gcTombstones(items: List<T>): List<T> {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DriveConstants.TOMBSTONE_RETENTION_DAYS)
        return items.filterNot { item ->
            when (item) {
                is PropertyEntity -> item.isDeleted && item.updatedAt < cutoff
                is ClientEntity -> item.isDeleted && item.updatedAt < cutoff
                else -> false
            }
        }
    }

    private inline fun <reified T> parseList(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, T::class.java).type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
