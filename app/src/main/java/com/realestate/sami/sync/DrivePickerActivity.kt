package com.realestate.sami.sync

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.realestate.sami.BuildConfig

/**
 * اکتیویتی سبک که ویجت Google Picker (که فقط به‌صورت جاوااسکریپت وب موجوده و SDK
 * بومی اندرویدی نداره) رو داخل یک WebView بارگذاری می‌کنه، تا کاربر بتونه یک پوشه‌ی
 * Drive رو که با اکانتش به اشتراک گذاشته شده انتخاب کنه.
 *
 * چرا این لازمه: با اسکوپ drive.file، اپ فقط به فایل‌هایی دسترسی داره که خودش ساخته
 * یا کاربر صریحاً از طریق Picker انتخاب کرده. صرفِ Share شدن یک پوشه توسط همکار،
 * بدون این مرحله، برای درخواست‌های REST API این اپ کافی نیست.
 */
class DrivePickerActivity : Activity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = intent.getStringExtra(EXTRA_ACCESS_TOKEN)
        if (token.isNullOrBlank() || BuildConfig.DRIVE_PICKER_API_KEY.isBlank() || BuildConfig.DRIVE_APP_ID.isBlank()) {
            // اگه توکن یا کلیدهای Picker تنظیم نشده باشن (مثلاً local.properties هنوز پر نشده)،
            // به‌جای کرش، فقط لغو می‌کنیم تا صفحه‌ی قبلی پیام مناسب نشون بده.
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(PickerBridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val js = "init('${token.escapeJs()}', '${BuildConfig.DRIVE_PICKER_API_KEY.escapeJs()}', '${BuildConfig.DRIVE_APP_ID.escapeJs()}')"
                    view.evaluateJavascript(js, null)
                }
            }
            loadUrl("file:///android_asset/drive_picker.html")
        }
        setContentView(webView)
    }

    private fun String.escapeJs(): String = replace("\\", "\\\\").replace("'", "\\'")

    /** پل جاوااسکریپت → کاتلین؛ متدهاش از داخل drive_picker.html صدا زده می‌شن. */
    inner class PickerBridge {
        @JavascriptInterface
        fun onPicked(id: String, name: String) {
            runOnUiThread {
                val result = Intent().apply {
                    putExtra(EXTRA_RESULT_FOLDER_ID, id)
                    putExtra(EXTRA_RESULT_FOLDER_NAME, name)
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }

        @JavascriptInterface
        fun onCancel() {
            runOnUiThread {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_RESULT_FOLDER_ID = "extra_result_folder_id"
        const val EXTRA_RESULT_FOLDER_NAME = "extra_result_folder_name"
    }
}
