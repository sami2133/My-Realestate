package com.realestate.sami

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import com.realestate.sami.ui.navigation.AppNavigation
import com.realestate.sami.ui.theme.RealEstateConsultantTheme
import com.realestate.sami.util.LanguagePreferences
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // جهت چیدمان (راست‌به‌چپ/چپ‌به‌راست) بر اساس زبان انتخابی واقعی اپ تعیین می‌شود،
            // نه بر اساس زبان سیستم؛ فارسی همیشه راست‌به‌چپ است، انگلیسی چپ‌به‌راست.
            val currentLocale = ConfigurationCompat.getLocales(resources.configuration)[0]
                ?: Locale.getDefault()
            val layoutDirection = if (currentLocale.language == LanguagePreferences.LANGUAGE_FA) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
                RealEstateConsultantTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        NotificationPermissionRequester()
                        AppNavigation()
                    }
                }
            }
        }
    }
}

/**
 * فاز ۵: از اندروید ۱۳ (API 33) به بعد نمایش نوتیفیکیشن نیاز به مجوز runtime دارد؛
 * یک‌بار در اولین اجرا از کاربر درخواست می‌شود. اگر رد کند، بقیه‌ی اپ عادی کار می‌کند،
 * فقط یادآوری پیگیری نمایش داده نمی‌شود.
 */
@androidx.compose.runtime.Composable
private fun NotificationPermissionRequester() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
