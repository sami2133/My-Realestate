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
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    data class Success(val syncedAt: Long) : SyncResult()
    data class ConsentRequired(val intent: Intent) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

/**
 * موتور همگام‌سازی: داده‌ی محلی Room را با فایل‌های JSON مشترک روی یک پوشه‌ی Drive ادغام می‌کند.
 *
 * استراتژی ادغام: «آخرین ویرایش برنده است» (last-write-wins) بر اساس فیلد updatedAt،
 * و تطبیق رکوردها بین دستگاه‌ها از طریق remoteId (UUID) نه id محلی (که فقط داخل هر دستگاه معتبر است).
 *
 * محدودیت شناخته‌شده‌ی نسخه‌ی فعلی: حذف رکوردها بین دستگاه‌ها همگام نمی‌شود (فقط افزودن/ویرایش).
 * برای همگام‌سازی حذف، باید فیلد isDeleted (soft-delete) به موجودیت‌ها اضافه شود.
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
        val tokenResult = authManager.getAccessToken(context, account)
        val token = when (tokenResult) {
            is AccessTokenResult.Success -> tokenResult.token
            is AccessTokenResult.ConsentRequired -> return SyncResult.ConsentRequired(tokenResult.intent)
            is AccessTokenResult.Failure -> return SyncResult.Failure(tokenResult.message)
        }

        return try {
            val folderId = syncPrefs.teamFolderId ?: driveApi.ensureTeamFolder(token).also {
                syncPrefs.teamFolderId = it
            }

            syncProperties(token, folderId)
            syncClients(token, folderId)

            val now = System.currentTimeMillis()
            syncPrefs.lastSyncedAt = now
            SyncResult.Success(now)
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "همگام‌سازی ناموفق بود")
        }
    }

    // ---------- ملک‌ها ----------

    private suspend fun syncProperties(token: String, folderId: String) {
        val local = propertyDao.getAll().first().map { ensureRemoteId(it) }
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
                    val inserted = propertyDao.insert(remoteItem.copy(id = 0, isSynced = true))
                    merged += remoteItem.copy(id = inserted)
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

        driveApi.uploadOrUpdateJson(token, folderId, DriveConstants.PROPERTIES_FILE_NAME, gson.toJson(merged))
    }

    private fun ensureRemoteId(property: PropertyEntity): PropertyEntity =
        if (property.remoteId != null) property else property.copy(remoteId = UUID.randomUUID().toString())

    // ---------- متقاضیان ----------

    private suspend fun syncClients(token: String, folderId: String) {
        val local = clientDao.getAll().first().map { ensureRemoteId(it) }
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
                    val inserted = clientDao.insert(remoteItem.copy(id = 0, isSynced = true))
                    merged += remoteItem.copy(id = inserted)
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

        driveApi.uploadOrUpdateJson(token, folderId, DriveConstants.CLIENTS_FILE_NAME, gson.toJson(merged))
    }

    private fun ensureRemoteId(client: ClientEntity): ClientEntity =
        if (client.remoteId != null) client else client.copy(remoteId = UUID.randomUUID().toString())

    // ---------- کمکی ----------

    private inline fun <reified T> parseList(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, T::class.java).type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
