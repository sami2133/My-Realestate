package com.realestate.sami.sync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class DriveFile(val id: String, val name: String)

/** توکن دسترسی رد شده (منقضی/باطل) — فراخوان باید توکن رو invalidate و دوباره تلاش کنه. */
class DriveAuthException(code: Int) : IOException("توکن دسترسی به Drive نامعتبر یا منقضی است (HTTP $code)")

/** پوشه/فایل با این شناسه پیدا نشد یا کاربر بهش دسترسی نداره — برای پیام مخصوص «join by ID». */
class DriveNotFoundException(message: String) : IOException(message)

/**
 * کلاینت سبک برای Google Drive REST API v3 (بدون کتابخانه‌ی سنگین google-api-client)
 * فقط عملیات لازم برای همگام‌سازی: پیدا/ساخت پوشه، آپلود/آپدیت/دانلود فایل JSON.
 */
@Singleton
class DriveApiClient @Inject constructor() {

    private val http = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    private fun authHeader(token: String) = "Bearer $token"

    /** بررسی مرکزی پاسخ: ۴۰۱/۴۰۳ با توکن نامعتبر را از بقیه خطاها جدا می‌کند تا SyncManager بتونه retry کنه. */
    private fun requireSuccess(resp: okhttp3.Response, actionDescription: String) {
        if (resp.isSuccessful) return
        if (resp.code == 401) throw DriveAuthException(resp.code)
        throw IOException("$actionDescription ناموفق بود: ${resp.code}")
    }

    /** پوشه‌ی تیمی را با نام مشخص در ریشه‌ی Drive پیدا می‌کند؛ اگر نبود می‌سازد. */
    suspend fun ensureTeamFolder(token: String): String = withContext(Dispatchers.IO) {
        val existing = findByName(token, DriveConstants.TEAM_FOLDER_NAME, DriveConstants.FOLDER_MIME_TYPE, parentId = null)
        if (existing != null) return@withContext existing.id
        createFolder(token, DriveConstants.TEAM_FOLDER_NAME)
    }

    private fun createFolder(token: String, name: String): String {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("mimeType", DriveConstants.FOLDER_MIME_TYPE)
        }
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_API_BASE}/files")
            .header("Authorization", authHeader(token))
            .post(body.toString().toRequestBody(json))
            .build()
        http.newCall(request).execute().use { resp ->
            requireSuccess(resp, "ساخت پوشه‌ی تیمی")
            val result = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
            return result.get("id").asString
        }
    }

    /** یک زیرپوشه با نام مشخص را داخل پوشه‌ی والد پیدا می‌کند؛ اگر نبود می‌سازد (مثلاً پوشه‌ی «images»). */
    suspend fun ensureSubfolder(token: String, parentFolderId: String, name: String): String =
        withContext(Dispatchers.IO) {
            val existing = findByName(token, name, DriveConstants.FOLDER_MIME_TYPE, parentId = parentFolderId)
            existing?.id ?: createFolderIn(token, name, parentFolderId)
        }

    private fun createFolderIn(token: String, name: String, parentId: String): String {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("mimeType", DriveConstants.FOLDER_MIME_TYPE)
            add("parents", com.google.gson.JsonArray().apply { add(parentId) })
        }
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_API_BASE}/files")
            .header("Authorization", authHeader(token))
            .post(body.toString().toRequestBody(json))
            .build()
        http.newCall(request).execute().use { resp ->
            requireSuccess(resp, "ساخت زیرپوشه‌ی تصاویر")
            val result = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
            return result.get("id").asString
        }
    }

    /** فایل با نام مشخص را داخل یک پوشه (یا در ریشه اگر parentId=null) پیدا می‌کند. */
    suspend fun findFileInFolder(token: String, folderId: String, fileName: String): DriveFile? =
        withContext(Dispatchers.IO) { findByName(token, fileName, null, folderId) }

    private fun findByName(token: String, name: String, mimeType: String?, parentId: String?): DriveFile? {
        val escapedName = name.replace("'", "\\'")
        val queryParts = mutableListOf("name = '$escapedName'", "trashed = false")
        if (mimeType != null) queryParts.add("mimeType = '$mimeType'")
        if (parentId != null) queryParts.add("'$parentId' in parents")
        val q = URLEncoder.encode(queryParts.joinToString(" and "), "UTF-8")

        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_API_BASE}/files?q=$q&fields=files(id,name)&spaces=drive")
            .header("Authorization", authHeader(token))
            .get()
            .build()
        http.newCall(request).execute().use { resp ->
            requireSuccess(resp, "جستجوی فایل در Drive")
            val files = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject.getAsJsonArray("files")
            if (files == null || files.size() == 0) return null
            val first = files[0].asJsonObject
            return DriveFile(first.get("id").asString, first.get("name").asString)
        }
    }

    /** محتوای یک فایل متنی (JSON) را دانلود می‌کند؛ اگر فایل وجود نداشت null برمی‌گرداند. */
    suspend fun downloadFileContent(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_API_BASE}/files/$fileId?alt=media")
            .header("Authorization", authHeader(token))
            .get()
            .build()
        http.newCall(request).execute().use { resp ->
            if (resp.code == 404) return@withContext null
            requireSuccess(resp, "دانلود فایل از Drive")
            resp.body?.string()
        }
    }

    /**
     * محتوای JSON را در پوشه‌ی داده‌شده آپلود می‌کند؛ اگر فایلی با همین نام از قبل بود، آپدیتش می‌کند
     * وگرنه فایل جدید می‌سازد. شناسه‌ی نهایی فایل را برمی‌گرداند.
     */
    suspend fun uploadOrUpdateJson(token: String, folderId: String, fileName: String, content: String): String =
        withContext(Dispatchers.IO) {
            val existing = findByName(token, fileName, null, folderId)
            if (existing != null) {
                updateFileContent(token, existing.id, content)
                existing.id
            } else {
                createFileWithContent(token, folderId, fileName, content)
            }
        }

    private fun updateFileContent(token: String, fileId: String, content: String) {
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_UPLOAD_BASE}/files/$fileId?uploadType=media")
            .header("Authorization", authHeader(token))
            .patch(content.toRequestBody(json))
            .build()
        http.newCall(request).execute().use { resp ->
            requireSuccess(resp, "آپدیت فایل روی Drive")
        }
    }

    private fun createFileWithContent(token: String, folderId: String, fileName: String, content: String): String {
        val metadata = JsonObject().apply {
            addProperty("name", fileName)
            add("parents", com.google.gson.JsonArray().apply { add(folderId) })
        }
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(MultipartBody.Part.create(metadata.toString().toRequestBody(json)))
            .addPart(MultipartBody.Part.create(content.toRequestBody(json)))
            .build()
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_UPLOAD_BASE}/files?uploadType=multipart")
            .header("Authorization", authHeader(token))
            .post(multipart)
            .build()
        http.newCall(request).execute().use { resp ->
            requireSuccess(resp, "ساخت فایل روی Drive")
            val result = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
            return result.get("id").asString
        }
    }

    /**
     * یک فایل باینری (عکس) را به‌عنوان فایل جدید داخل پوشه آپلود می‌کند و شناسه‌ی فایل ساخته‌شده
     * را برمی‌گرداند. برخلاف [uploadOrUpdateJson]، هربار یک فایل تازه می‌سازد چون هر عکس یک‌بار
     * آپلود می‌شود و بعد از آن دیگر تغییر نمی‌کند (immutable).
     */
    suspend fun uploadBinaryFile(
        token: String,
        folderId: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        val metadata = JsonObject().apply {
            addProperty("name", fileName)
            add("parents", com.google.gson.JsonArray().apply { add(folderId) })
        }
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(MultipartBody.Part.create(metadata.toString().toRequestBody(json)))
            .addPart(MultipartBody.Part.create(bytes.toRequestBody(mimeType.toMediaType())))
            .build()
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_UPLOAD_BASE}/files?uploadType=multipart")
            .header("Authorization", authHeader(token))
            .post(multipart)
            .build()
        http.newCall(request).execute().use { resp ->
            requireSuccess(resp, "آپلود عکس روی Drive")
            val result = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
            result.get("id").asString
        }
    }

    /** محتوای باینری یک فایل (عکس) را دانلود می‌کند؛ اگر فایل دیگر روی Drive وجود نداشت null برمی‌گرداند. */
    suspend fun downloadBinaryFile(token: String, fileId: String): ByteArray? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_API_BASE}/files/$fileId?alt=media")
            .header("Authorization", authHeader(token))
            .get()
            .build()
        http.newCall(request).execute().use { resp ->
            if (resp.code == 404) return@withContext null
            requireSuccess(resp, "دانلود عکس از Drive")
            resp.body?.bytes()
        }
    }

    /**
     * متادیتای یک پوشه را با شناسه‌اش می‌گیرد — برای اعتبارسنجی «پیوستن به تیم با Folder ID»
     * قبل از این‌که کاربر مطمئن بشه ID درست وارد کرده و واقعاً به اون پوشه دسترسی داره.
     * اگر پوشه وجود نداشت یا کاربر دسترسی نداشت، DriveNotFoundException پرتاب می‌کند.
     */
    suspend fun getFolderMetadata(token: String, folderId: String): DriveFile = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${DriveConstants.DRIVE_API_BASE}/files/$folderId?fields=id,name,mimeType,trashed")
            .header("Authorization", authHeader(token))
            .get()
            .build()
        http.newCall(request).execute().use { resp ->
            if (resp.code == 404) throw DriveNotFoundException("پوشه‌ای با این شناسه پیدا نشد یا به آن دسترسی نداری")
            requireSuccess(resp, "بررسی پوشه‌ی تیمی")
            val result = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
            if (result.get("trashed")?.asBoolean == true) {
                throw DriveNotFoundException("این پوشه در Drive حذف شده (در سطل زباله) است")
            }
            if (result.get("mimeType")?.asString != DriveConstants.FOLDER_MIME_TYPE) {
                throw DriveNotFoundException("شناسه‌ی وارد‌شده مربوط به یک پوشه نیست")
            }
            DriveFile(result.get("id").asString, result.get("name")?.asString.orEmpty())
        }
    }
}
