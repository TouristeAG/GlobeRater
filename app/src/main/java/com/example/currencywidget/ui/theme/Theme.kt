package com.example.currencywidget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

// Monochrome fallback (Android 8–11): ink on paper.
private val Light = lightColorScheme(
    primary = Color(0xFF111111), onPrimary = Color.White,
    primaryContainer = Color(0xFFDEDEDE), onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF3C3C3C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E4E4), onSecondaryContainer = Color(0xFF111111),
    tertiary = Color(0xFF3C3C3C), onTertiary = Color.White,
    background = Color(0xFFEFEFEF), onBackground = Color(0xFF111111),
    surface = Color(0xFFEFEFEF), onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE4E4E4), onSurfaceVariant = Color(0xFF7A7A7A),
    surfaceContainerLowest = Color(0xFFF7F7F7), surfaceContainerLow = Color(0xFFE9E9E9),
    surfaceContainer = Color(0xFFE4E4E4), surfaceContainerHigh = Color(0xFFDDDDDD),
    surfaceContainerHighest = Color(0xFFD5D5D5),
    outline = Color(0xFF9A9A9A), outlineVariant = Color(0xFFCFCFCF),
    error = Color(0xFFB3261E)
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF2F2F2), onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFC8C8C8), onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF2A2A2A), onSecondaryContainer = Color(0xFFF2F2F2),
    tertiary = Color(0xFFC8C8C8), onTertiary = Color(0xFF111111),
    background = Color(0xFF111111), onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF111111), onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF222222), onSurfaceVariant = Color(0xFF8C8C8C),
    surfaceContainerLowest = Color(0xFF0B0B0B), surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1E1E1E), surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
    outline = Color(0xFF5A5A5A), outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFF2B8B5)
)

private val Sans = FontFamily.SansSerif

// Same voice as the widgets: heavy, rounded Roboto ("sans-serif-black" / bold).
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 76.sp, lineHeight = 80.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.4.sp)
)

// Widget radii: keys 8dp, selectors 12dp, widget 16dp.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(20.dp)
)

/**
 * Re-maps a scheme to the widgets' look: pages sit on the tinted primary container,
 * text and strokes use primary, raised areas are slightly deeper tints of the same hue.
 */
private fun widgetStyled(base: ColorScheme): ColorScheme {
    val c = base.primaryContainer
    val p = base.primary
    fun tint(f: Float) = lerp(c, p, f)
    return base.copy(
        background = c, onBackground = p,
        surface = c, onSurface = p,
        surfaceVariant = tint(0.10f), onSurfaceVariant = tint(0.72f),
        surfaceContainerLowest = tint(0.02f), surfaceContainerLow = tint(0.05f),
        surfaceContainer = tint(0.09f), surfaceContainerHigh = tint(0.13f), surfaceContainerHighest = tint(0.18f),
        outline = p, outlineVariant = tint(0.28f),
        surfaceTint = p, inverseSurface = p, inverseOnSurface = c
    )
}

/**
 * Colours come from Material You (the wallpaper) on Android 12+. Older versions, which have
 * no system palette, keep the monochrome ink-on-paper scheme. Layout and type are the same.
 */
@Composable
fun CurrencyWidgetTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) Dark else Light
    }
    MaterialTheme(colorScheme = widgetStyled(scheme), typography = Typography, shapes = AppShapes, content = content)
}
