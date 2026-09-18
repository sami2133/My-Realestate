package com.realestate.sami.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldPrimary,
    secondary = SlateBlue,
    secondaryContainer = SlateBlueContainer,
    tertiary = GoldAccent,
    tertiaryContainer = GoldAccentContainer,
    background = WarmBackground,
    surface = SurfaceWhite,
    onBackground = InkText,
    onSurface = InkText
)

// نکته‌ی مهم: قبلاً این پالت فقط primary/secondary/tertiary رو ست می‌کرد و نقش‌های
// «Container» (primaryContainer/secondaryContainer/tertiaryContainer) رو خالی می‌ذاشت.
// وقتی این نقش‌ها ست نشن، Compose به‌جای رنگ‌های تعریف‌شده‌ی این پروژه، از پالت پیش‌فرض
// Material Baseline (که بنفشه) استفاده می‌کنه — دقیقاً همون جعبه‌ی بنفشِ دکمه‌ی
// «افزودن عکس» در حالت تاریک. همه‌ی نقش‌ها رو صریح ست می‌کنیم تا این نشتی رنگی از بین بره.
private val DarkColors = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = InkTextDark,
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = EmeraldPrimaryDark,
    secondary = SlateBlue,
    onSecondary = InkTextDark,
    secondaryContainer = SlateBlueContainer.copy(alpha = 0.24f),
    onSecondaryContainer = InkTextDark,
    tertiary = GoldAccentDark,
    onTertiary = InkText,
    tertiaryContainer = GoldAccent,
    onTertiaryContainer = GoldAccentDark,
    background = WarmBackgroundDark,
    surface = SurfaceDark,
    onBackground = InkTextDark,
    onSurface = InkTextDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = InkTextDark
)

// شکل‌های اختصاصی: گوشه‌های نرم اما نه یکسان روی همه چیز — سلسله‌مراتب بصری دارد
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RealEstateConsultantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
