package com.realestate.sami.sync

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.realestate.sami.BuildConfig

/**
 * اکتیویتی سبک که ویجت Google Picker (که فقط به‌صورت جاوااسکریپت وب موجوده و SDK
 * بومی اندرویدی نداره) رو داخل یک WebView بارگذاری می‌کنه، تا کاربر بتونه یک پوشه‌ی
 * Drive رو با مرور بصری پیدا و انتخاب کنه (به‌جای وارد کردن دستی شناسه‌ی پوشه).
 *
 * توجه: چون طبق معماری این اپ همه‌ی اعضای تیم با یک اکانت گوگل مشترک وارد می‌شن (نه اکانت
 * شخصی هرکس)، این Picker دیگه نقش «اعطای مجوز per-file» نداره — صرفاً یک میان‌بر UI برای
 * پیدا کردن شناسه‌ی پوشه‌ست؛ همون کار رو می‌شه با پیست‌کردن دستیِ لینک/شناسه هم انجام داد.
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

        // ویجت Google Picker با origin از نوع file:// کار نمی‌کنه (گوگل درخواست رو با ۴۰۳ رد
        // می‌کنه چون این origin رو معتبر نمی‌دونه)؛ برای همین asset رو از یک دامنه‌ی مجازی https
        // (appassets.androidplatform.net) سرو می‌کنیم تا از دید گوگل یک origin واقعی http/https ببینه.
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

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
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    val js = "init(" +
                        "'${token.escapeJs()}', " +
                        "'${BuildConfig.DRIVE_PICKER_API_KEY.escapeJs()}', " +
                        "'${BuildConfig.DRIVE_APP_ID.escapeJs()}'" +
                        ")"
                    view.evaluateJavascript(js, null)
                }
            }
            loadUrl("https://appassets.androidplatform.net/assets/drive_picker.html")
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
