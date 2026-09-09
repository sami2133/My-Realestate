package com.realestate.sami.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.realestate.sami.data.local.dao.ClientDao
import com.realestate.sami.data.local.dao.ContactLogDao
import com.realestate.sami.data.local.dao.PropertyDao
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.ContactLogEntity
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyImage
import com.realestate.sami.data.local.entity.RelatedType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
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
 *
 * علاوه بر ملک‌ها و متقاضیان، این کلاس دو نوع داده‌ی دیگر را هم sync می‌کند:
 * - تاریخچه‌ی تماس (`ContactLogEntity`): چون به‌جای id محلی به `relatedRemoteId` (remoteId
 *   پایدار ملک/متقاضی) وصل می‌شود، فقط بعد از merge موفق ملک‌ها و متقاضیان قابل sync است.
 * - تصاویر ملک: هر عکس به‌صورت یک فایل باینری مستقل در زیرپوشه‌ی «images» آپلود می‌شود؛
 *   عکس‌های محلیِ آپلودنشده آپلود، و عکس‌های ریموتیِ دانلودنشده دانلود و در حافظه‌ی محلی کش می‌شوند.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val propertyDao: PropertyDao,
    private val clientDao: ClientDao,
    private val contactLogDao: ContactLogDao,
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
            val imagesFolderId = driveApi.ensureSubfolder(token, resolvedFolderId, DriveConstants.IMAGES_FOLDER_NAME)

            val propertiesResult = runCatching { syncProperties(token, resolvedFolderId, imagesFolderId) }
            val clientsResult = runCatching { syncClients(token, resolvedFolderId) }

            // تاریخچه‌ی تماس فقط وقتی sync می‌شه که ملک‌ها و متقاضیان با موفقیت merge شده باشن،
            // چون برای وصل‌کردن هر لاگ به ملک/متقاضی درست، به remoteId نهاییِ اون‌ها نیاز داره.
            val contactLogsResult = if (propertiesResult.isSuccess && clientsResult.isSuccess) {
                runCatching {
                    syncContactLogs(token, resolvedFolderId, propertiesResult.getOrThrow(), clientsResult.getOrThrow())
                }
            } else {
                Result.success(Unit)
            }

            val errors = listOfNotNull(
                propertiesResult.exceptionOrNull(),
                clientsResult.exceptionOrNull(),
                contactLogsResult.exceptionOrNull()
            )

            // اگه هرکدوم خطای «توکن نامعتبر» بود و هنوز retry نکردیم، یه بار دیگه با توکن تازه امتحان کن
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
                // موفقیت جزئی: هرکدوم از انواع داده که موفق شد، همون موقع commit شده (partial sync عمدی، نه atomic)
                val parts = mutableListOf<String>()
                propertiesResult.exceptionOrNull()?.let { parts += "ملک‌ها: ${it.message}" }
                clientsResult.exceptionOrNull()?.let { parts += "متقاضیان: ${it.message}" }
                contactLogsResult.exceptionOrNull()?.let { parts += "تاریخچه تماس: ${it.message}" }
                return SyncResult.Failure(parts.joinToString(" | "))
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

    // ---------- ملک‌ها (متن + تصاویر) ----------

    private suspend fun syncProperties(token: String, folderId: String, imagesFolderId: String): List<PropertyEntity> {
        // نکته‌ی مهم: remoteId تازه‌تولیدشده باید همین‌جا در Room ذخیره بشه، وگرنه دفعه‌ی بعدِ sync
        // یک remoteId دیگه براش تولید می‌شه و رکورد به‌جای update، به‌عنوان آیتم جدید insert میشه (تکراری).
        val local = propertyDao.getAllIncludingDeleted().map { original ->
            val withRemoteId = ensureRemoteId(original)
            if (withRemoteId.remoteId != original.remoteId) propertyDao.update(withRemoteId)
            withRemoteId
        }
        val existingFile = driveApi.findFileInFolder(token, folderId, DriveConstants.PROPERTIES_FILE_NAME)
        val remoteJson = existingFile?.let { driveApi.downloadFileContent(token, it.id) }
        val remote: List<PropertyEntity> = parsePropertyList(remoteJson)

        val localById = local.associateBy { it.remoteId }
        val remoteById = remote.associateBy { it.remoteId }
        val allKeys = localById.keys + remoteById.keys

        val merged = mutableListOf<PropertyEntity>()
        for (key in allKeys) {
            val localItem = localById[key]
            val remoteItem = remoteById[key]
            val resolved: PropertyEntity? = when {
                localItem != null && remoteItem == null -> localItem
                localItem == null && remoteItem != null -> {
                    if (remoteItem.isDeleted) {
                        // این دستگاه هیچ‌وقت این رکورد رو نداشته و روی یه دستگاه دیگه حذف شده؛
                        // نیازی به insert کردنش نیست، فقط tombstone رو توی merged نگه می‌داریم تا در فایل مشترک بمونه.
                        remoteItem
                    } else {
                        val inserted = propertyDao.insert(remoteItem.copy(id = 0, isSynced = true))
                        remoteItem.copy(id = inserted)
                    }
                }
                localItem != null && remoteItem != null -> {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        val toSave = remoteItem.copy(id = localItem.id, isSynced = true)
                        propertyDao.update(toSave)
                        toSave
                    } else {
                        localItem
                    }
                }
                else -> null
            }
            if (resolved != null) merged += resolved
        }

        // مرحله‌ی دوم: عکس‌ها جدا از بقیه‌ی فیلدها resolve می‌شن — چون آپلود/دانلود فایل باینری
        // زمان می‌بره و نباید منطق merge متنی بالا رو پیچیده کنه.
        val withImagesResolved = merged.map { property ->
            val resolvedImages = resolvePropertyImages(token, imagesFolderId, property.images)
            if (resolvedImages == property.images) {
                property
            } else {
                val updated = property.copy(images = resolvedImages)
                if (updated.id != 0L) propertyDao.update(updated)
                updated
            }
        }

        driveApi.uploadOrUpdateJson(
            token, folderId, DriveConstants.PROPERTIES_FILE_NAME, gson.toJson(gcTombstones(withImagesResolved))
        )
        return withImagesResolved
    }

    /** برای هر عکس: اگر فقط محلیه (آپلود نشده) آپلودش می‌کنه؛ اگر فقط ریموته (دانلود نشده) دانلود و کشش می‌کنه. */
    private suspend fun resolvePropertyImages(
        token: String,
        imagesFolderId: String,
        images: List<PropertyImage>
    ): List<PropertyImage> = images.map { image ->
        when {
            image.driveFileId == null && image.localUri != null -> {
                val uploadedId = runCatching { uploadImage(token, imagesFolderId, image.localUri) }.getOrNull()
                if (uploadedId != null) image.copy(driveFileId = uploadedId) else image
            }
            image.driveFileId != null && image.localUri == null -> {
                val cachedPath = runCatching { downloadAndCacheImage(token, image.driveFileId) }.getOrNull()
                if (cachedPath != null) image.copy(localUri = cachedPath) else image
            }
            else -> image
        }
    }

    private suspend fun uploadImage(token: String, imagesFolderId: String, localUri: String): String {
        val bytes = context.contentResolver.openInputStream(Uri.parse(localUri))?.use { it.readBytes() }
            ?: throw IOException("خواندن فایل عکس ممکن نشد")
        val fileName = "img_${UUID.randomUUID()}.jpg"
        return driveApi.uploadBinaryFile(token, imagesFolderId, fileName, bytes, DriveConstants.DEFAULT_IMAGE_MIME_TYPE)
    }

    private suspend fun downloadAndCacheImage(token: String, driveFileId: String): String {
        val bytes = driveApi.downloadBinaryFile(token, driveFileId)
            ?: throw IOException("عکس دیگر روی Drive موجود نیست")
        val dir = File(context.filesDir, "synced_images").apply { mkdirs() }
        val file = File(dir, "$driveFileId.jpg")
        file.writeBytes(bytes)
        return Uri.fromFile(file).toString()
    }

    private fun ensureRemoteId(property: PropertyEntity): PropertyEntity =
        if (property.remoteId != null) property else property.copy(remoteId = UUID.randomUUID().toString())

    // ---------- متقاضیان ----------

    private suspend fun syncClients(token: String, folderId: String): List<ClientEntity> {
        // همون نکته‌ی remoteId که در syncProperties توضیح داده شد — اینجا هم باید persist بشه.
        val local = clientDao.getAllIncludingDeleted().map { original ->
            val withRemoteId = ensureRemoteId(original)
            if (withRemoteId.remoteId != original.remoteId) clientDao.update(withRemoteId)
            withRemoteId
        }
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
            val resolved: ClientEntity? = when {
                localItem != null && remoteItem == null -> localItem
                localItem == null && remoteItem != null -> {
                    if (remoteItem.isDeleted) {
                        remoteItem
                    } else {
                        val inserted = clientDao.insert(remoteItem.copy(id = 0, isSynced = true))
                        remoteItem.copy(id = inserted)
                    }
                }
                localItem != null && remoteItem != null -> {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        val toSave = remoteItem.copy(id = localItem.id, isSynced = true)
                        clientDao.update(toSave)
                        toSave
                    } else {
                        localItem
                    }
                }
                else -> null
            }
            if (resolved != null) merged += resolved
        }

        driveApi.uploadOrUpdateJson(
            token, folderId, DriveConstants.CLIENTS_FILE_NAME, gson.toJson(gcTombstones(merged))
        )
        return merged
    }

    private fun ensureRemoteId(client: ClientEntity): ClientEntity =
        if (client.remoteId != null) client else client.copy(remoteId = UUID.randomUUID().toString())

    // ---------- تاریخچه تماس ----------

    /**
     * چون [ContactLogEntity.relatedId] فقط یک id محلی Room است، هر لاگ باید از طریق
     * [ContactLogEntity.relatedRemoteId] (که برابر remoteId پایدار ملک/متقاضیِ مرتبط است) بین
     * دستگاه‌ها ردیابی شود. این تابع بعد از این فراخوانی می‌شود که [syncProperties] و
     * [syncClients] رکوردهایشان را نهایی کرده‌اند، تا remoteId های لازم برای این نگاشت آماده باشد.
     */
    private suspend fun syncContactLogs(
        token: String,
        folderId: String,
        mergedProperties: List<PropertyEntity>,
        mergedClients: List<ClientEntity>
    ) {
        val propertyRemoteIdByLocalId = mergedProperties.associate { it.id to it.remoteId }
        val clientRemoteIdByLocalId = mergedClients.associate { it.id to it.remoteId }
        val propertyLocalIdByRemoteId = mergedProperties.mapNotNull { p -> p.remoteId?.let { it to p.id } }.toMap()
        val clientLocalIdByRemoteId = mergedClients.mapNotNull { c -> c.remoteId?.let { it to c.id } }.toMap()

        fun localIdToRemoteId(relatedId: Long, type: RelatedType): String? = when (type) {
            RelatedType.PROPERTY -> propertyRemoteIdByLocalId[relatedId]
            RelatedType.CLIENT -> clientRemoteIdByLocalId[relatedId]
        }

        fun remoteIdToLocalId(relatedRemoteId: String, type: RelatedType): Long? = when (type) {
            RelatedType.PROPERTY -> propertyLocalIdByRemoteId[relatedRemoteId]
            RelatedType.CLIENT -> clientLocalIdByRemoteId[relatedRemoteId]
        }

        // قبل از merge، هر لاگِ محلی که هنوز remoteId/relatedRemoteId ندارد را پر می‌کنیم.
        val local = contactLogDao.getAllIncludingDeleted().map { original ->
            var log = original
            var changed = false
            if (log.remoteId == null) {
                log = log.copy(remoteId = UUID.randomUUID().toString())
                changed = true
            }
            if (log.relatedRemoteId == null) {
                localIdToRemoteId(log.relatedId, log.relatedType)?.let {
                    log = log.copy(relatedRemoteId = it)
                    changed = true
                }
            }
            if (changed) contactLogDao.update(log)
            log
        }
        // لاگ‌هایی که هنوز relatedRemoteId ندارن (یعنی ملک/متقاضی‌شون هنوز remoteId نگرفته) رو این دور
        // sync نمی‌کنیم؛ دور بعد که ملک/متقاضی remoteId گرفت خودش حل می‌شه.
        val syncable = local.filter { it.relatedRemoteId != null }

        val existingFile = driveApi.findFileInFolder(token, folderId, DriveConstants.CONTACT_LOGS_FILE_NAME)
        val remoteJson = existingFile?.let { driveApi.downloadFileContent(token, it.id) }
        val remote: List<ContactLogEntity> = parseList(remoteJson)

        val localById = syncable.associateBy { it.remoteId }
        val remoteById = remote.associateBy { it.remoteId }
        val allKeys = localById.keys + remoteById.keys

        val merged = mutableListOf<ContactLogEntity>()
        for (key in allKeys) {
            val localItem = localById[key]
            val remoteItem = remoteById[key]
            val resolved: ContactLogEntity? = when {
                localItem != null && remoteItem == null -> localItem
                localItem == null && remoteItem != null -> {
                    val relatedRemoteId = remoteItem.relatedRemoteId
                    val localRelatedId = relatedRemoteId?.let { remoteIdToLocalId(it, remoteItem.relatedType) }
                    when {
                        remoteItem.isDeleted -> remoteItem
                        localRelatedId == null -> {
                            // ملک/متقاضی مرتبط هنوز روی این دستگاه وجود نداره؛ خودِ لاگ رو در فایل مشترک
                            // نگه می‌داریم (تا از دست نره) ولی این دور محلی insert نمی‌کنیم.
                            remoteItem
                        }
                        else -> {
                            val inserted = contactLogDao.insert(
                                remoteItem.copy(id = 0, relatedId = localRelatedId, isSynced = true)
                            )
                            remoteItem.copy(id = inserted, relatedId = localRelatedId)
                        }
                    }
                }
                localItem != null && remoteItem != null -> {
                    if (remoteItem.updatedAt > localItem.updatedAt) {
                        val toSave = remoteItem.copy(id = localItem.id, relatedId = localItem.relatedId, isSynced = true)
                        contactLogDao.update(toSave)
                        toSave
                    } else {
                        localItem
                    }
                }
                else -> null
            }
            if (resolved != null) merged += resolved
        }

        driveApi.uploadOrUpdateJson(
            token, folderId, DriveConstants.CONTACT_LOGS_FILE_NAME, gson.toJson(gcTombstones(merged))
        )
    }

    // ---------- کمکی ----------

    /** tombstone هایی که به‌اندازه‌ی کافی قدیمی هستن (یعنی احتمالاً به همه‌ی دستگاه‌ها رسیدن) رو از فایل مشترک حذف می‌کنه. */
    private fun <T> gcTombstones(items: List<T>): List<T> {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DriveConstants.TOMBSTONE_RETENTION_DAYS)
        return items.filterNot { item ->
            when (item) {
                is PropertyEntity -> item.isDeleted && item.updatedAt < cutoff
                is ClientEntity -> item.isDeleted && item.updatedAt < cutoff
                is ContactLogEntity -> item.isDeleted && item.updatedAt < cutoff
                else -> false
            }
        }
    }

    private inline fun <reified T> parseList(json: String?): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        val type = TypeToken.getParameterized(List::class.java, T::class.java).type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /**
     * پارس کردن مخصوص ملک‌ها: فایل sami_properties.json روی Drive ممکن است رکوردهایی از یک
     * نسخه‌ی قدیمی‌تر اپ داشته باشد که هنوز فیلد `images` (لیست تصاویر، اضافه‌شده در فاز ۲) را
     * نداشتن. Gson برای فیلدهای Kotlin غیر-nullable که در JSON غایبند، به‌جای استفاده از مقدار
     * پیش‌فرض کلاس (emptyList())، مقدار null واقعی می‌گذارد و null-safety کاتلین را دور می‌زند؛
     * بعد هر `.copy()` روی همچین رکوردی بلافاصله با
     * «Parameter specified as non-null is null: parameter images» کرش می‌کند.
     * برای همین قبل از map شدن به PropertyEntity، فیلدهای غایب/null را در خودِ JSON پر می‌کنیم.
     */
    private fun parsePropertyList(json: String?): List<PropertyEntity> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = com.google.gson.JsonParser.parseString(json).asJsonArray
            array.forEach { element ->
                val obj = element.asJsonObject
                if (!obj.has("images") || obj.get("images").isJsonNull) {
                    obj.add("images", com.google.gson.JsonArray())
                }
                if (!obj.has("documentUris") || obj.get("documentUris").isJsonNull) {
                    obj.addProperty("documentUris", "")
                }
            }
            val type = TypeToken.getParameterized(List::class.java, PropertyEntity::class.java).type
            gson.fromJson<List<PropertyEntity>>(array, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
