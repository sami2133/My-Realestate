package com.realestate.sami.sync

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
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
            // اگه توکن یا کلیدهای Picker تنظیم نشده باشن (مثلاً local.properties هنوز پر نشده یا
            // سکرت‌های CI به مرحله‌ی build پاس داده نشدن)، به‌جای کرش یا لغوِ کاملاً بی‌صدا،
            // دلیل رو هم توی نتیجه می‌ذاریم تا صفحه‌ی قبلی بتونه پیام مناسب نشون بده.
            setResult(
                RESULT_CANCELED,
                Intent().putExtra(EXTRA_CANCEL_REASON, REASON_CONFIG_MISSING)
            )
            finish()
            return
        }

        // ویجت Google Picker برای بعضی درخواست‌های داخلیش (به دامنه‌های دیگه‌ی گوگل) به کوکی
        // نیاز داره. Android WebView به‌صورت پیش‌فرض کوکی‌های Third-Party رو مسدود می‌کنه که
        // بدون این خط، گوگل به‌جای Picker صفحه‌ی «Can't access your Google Account» نشون می‌ده.
        CookieManager.getInstance().setAcceptCookie(true)

        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
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
        /** روی نتیجه‌ی RESULT_CANCELED ست می‌شه تا مشخص کنه لغو به‌خاطر لغو دستی کاربره یا مشکل تنظیمات. */
        const val EXTRA_CANCEL_REASON = "extra_cancel_reason"
        const val REASON_CONFIG_MISSING = "config_missing"
    }
}
