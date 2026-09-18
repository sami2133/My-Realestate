package com.realestate.sami.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * زبان نمایش برنامه (فارسی/انگلیسی)، مستقل از زبان سیستم‌عامل دستگاه — کاربر از صفحه‌ی تنظیمات
 * خودش انتخاب می‌کند. پیش‌فرض «فارسی» است (مطابق رفتار همیشگی اپ)، مگر این‌که کاربر صریحاً
 * انگلیسی را انتخاب کرده باشد. اعمال واقعی زبان (AppCompatDelegate.setApplicationLocales) در
 * RealEstateApp.onCreate و در SettingsViewModel انجام می‌شود؛ این کلاس فقط مقدار انتخابی را
 * ذخیره/بازیابی می‌کند.
 */
@Singleton
class LanguagePreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    var languageTag: String
        get() = prefs.getString(KEY_LANGUAGE_TAG, DEFAULT_LANGUAGE_TAG) ?: DEFAULT_LANGUAGE_TAG
        set(value) = prefs.edit().putString(KEY_LANGUAGE_TAG, value).apply()

    companion object {
        private const val KEY_LANGUAGE_TAG = "language_tag"
        const val DEFAULT_LANGUAGE_TAG = "fa"
        const val LANGUAGE_FA = "fa"
        const val LANGUAGE_EN = "en"
    }
}
