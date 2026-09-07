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

private val DarkColors = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = InkText,
    secondary = SlateBlue,
    tertiary = GoldAccentDark,
    background = WarmBackgroundDark,
    surface = SurfaceDark,
    onBackground = InkTextDark,
    onSurface = InkTextDark
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
