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

/**
 * کلاینت سبک برای Google Drive REST API v3 (بدون کتابخانه‌ی سنگین google-api-client)
 * فقط عملیات لازم برای همگام‌سازی: پیدا/ساخت پوشه، آپلود/آپدیت/دانلود فایل JSON.
 */
@Singleton
class DriveApiClient @Inject constructor() {

    private val http = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    private fun authHeader(token: String) = "Bearer $token"

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
            if (!resp.isSuccessful) throw IOException("ساخت پوشه‌ی تیمی ناموفق بود: ${resp.code}")
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
            if (!resp.isSuccessful) throw IOException("جستجوی فایل در Drive ناموفق بود: ${resp.code}")
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
            if (!resp.isSuccessful) throw IOException("دانلود فایل از Drive ناموفق بود: ${resp.code}")
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
            if (!resp.isSuccessful) throw IOException("آپدیت فایل روی Drive ناموفق بود: ${resp.code}")
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
            if (!resp.isSuccessful) throw IOException("ساخت فایل روی Drive ناموفق بود: ${resp.code}")
            val result = JsonParser.parseString(resp.body?.string().orEmpty()).asJsonObject
            return result.get("id").asString
        }
    }
}
