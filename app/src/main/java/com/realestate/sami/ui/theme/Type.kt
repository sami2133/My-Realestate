package com.realestate.sami.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.realestate.sami.R

/**
 * فونت پیش‌فرض این پروژه «وزیرمتن» (Vazirmatn) در نظر گرفته شده — یک فونت فارسی مدرن،
 * هندسی و بسیار خوانا که این روزها استاندارد اپ‌های فارسی باکیفیت است.
 *
 * چون این محیط توسعه به اینترنت دسترسی ندارد، فایل‌های فونت به‌صورت باینری اضافه نشده‌اند.
 * برای فعال‌سازی فونت واقعی:
 *   ۱. از https://fonts.google.com/specimen/Vazirmatn فایل‌های Regular/Medium/Bold/SemiBold رو دانلود کن
 *   ۲. بریز داخل app/src/main/res/font/ با نام‌های:
 *      vazirmatn_regular.ttf, vazirmatn_medium.ttf, vazirmatn_semibold.ttf, vazirmatn_bold.ttf
 *   ۳. بلوک کامنت‌شده پایین همین فایل رو از حالت کامنت خارج کن و بلوک فعلی (System Default) رو حذف کن
 *
 * تا قبل از اون، اپ با فونت پیش‌فرض سیستم (که در اندروید مدرن به‌خوبی از فارسی پشتیبانی می‌کنه) اجرا می‌شه.
 */
private val AppFontFamily = FontFamily.SansSerif

/*
// --- نسخه نهایی با فونت وزیرمتن — بعد از اضافه کردن فایل‌های .ttf از حالت کامنت خارج کن ---
private val AppFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)
*/

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
