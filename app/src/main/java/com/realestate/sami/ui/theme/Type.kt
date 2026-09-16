package com.realestate.sami.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.realestate.sami.R

/**
 * فونت این پروژه «وزیرمتن» (Vazirmatn) است — یک فونت فارسی مدرن، هندسی و بسیار خوانا که این
 * روزها استاندارد اپ‌های فارسی باکیفیت است.
 *
 * چون محیط توسعه‌ی اولیه‌ی این پروژه به اینترنت دسترسی نداشت، به‌جای باندل‌کردن فایل‌های .ttf
 * داخل اپ، از API «فونت‌های دانلودی» گوگل استفاده شده: فونت اولین بار که دستگاه کاربر به
 * اینترنت وصله (از طریق Google Play Services) دانلود و کش می‌شه — بدون افزایش حجم فایل APK و
 * بدون نیاز به دانلود دستی فایل فونت توسط توسعه‌دهنده.
 * (values/font_certs.xml گواهی‌های لازم برای تایید هویت این سرویس رو نگه می‌داره.)
 *
 * روی دستگاه‌هایی که Google Play Services ندارند (بعضی گوشی‌های چینی خاص)، این API به‌صورت
 * خودکار به فونت پیش‌فرض سیستم برمی‌گرده — یعنی اپ کرش نمی‌کنه، فقط فونت سفارشی نمایش داده نمی‌شه.
 */
private val vazirmatnProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val vazirmatn = GoogleFont("Vazirmatn")

private val AppFontFamily = FontFamily(
    Font(googleFont = vazirmatn, fontProvider = vazirmatnProvider, weight = FontWeight.Normal),
    Font(googleFont = vazirmatn, fontProvider = vazirmatnProvider, weight = FontWeight.Medium),
    Font(googleFont = vazirmatn, fontProvider = vazirmatnProvider, weight = FontWeight.SemiBold),
    Font(googleFont = vazirmatn, fontProvider = vazirmatnProvider, weight = FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)
