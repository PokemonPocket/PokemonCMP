package com.tcgptrading.tcgptrading.core.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object PokemonTCGPTheme {
    val dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}

/**
 * The app theme. Light only, like the website: the palette is built around the cards,
 * and the card art reads best on the pale ground.
 */
@Composable
fun PokemonTCGPTheme(content: @Composable () -> Unit) {
    val body = lexendFontFamily
    val display = bricolageFontFamily

    val typography = Typography(
        // Page titles ("Wanted cards")
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.03).em),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.02).em),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em),
        // Section titles and card names
        titleLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
        titleMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp),
    )

    CompositionLocalProvider(LocalDimensions provides Dimensions()) {
        MaterialTheme(
            colorScheme = TcgColorScheme,
            typography = typography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}

private val LocalDimensions = staticCompositionLocalOf { Dimensions() }

private val TcgColorScheme = lightColorScheme(
    primary = TcgColors.Red,
    onPrimary = TcgColors.Surface,
    primaryContainer = TcgColors.RedSoft,
    onPrimaryContainer = TcgColors.RedInk,
    secondary = TcgColors.Ink,
    onSecondary = TcgColors.Surface,
    secondaryContainer = TcgColors.Chip,
    onSecondaryContainer = TcgColors.Ink,
    tertiary = TcgColors.Diamond,
    onTertiary = TcgColors.Surface,
    background = TcgColors.Ground,
    onBackground = TcgColors.Ink,
    surface = TcgColors.Surface,
    onSurface = TcgColors.Ink,
    surfaceVariant = TcgColors.Soft,
    onSurfaceVariant = TcgColors.Muted,
    surfaceContainer = TcgColors.Surface,
    surfaceContainerLow = TcgColors.Soft,
    surfaceContainerHigh = TcgColors.Surface,
    outline = TcgColors.Line,
    outlineVariant = TcgColors.LineSoft,
    error = TcgColors.RedInk,
    onError = TcgColors.Surface,
    errorContainer = TcgColors.RedTag,
    onErrorContainer = TcgColors.RedInk,
    scrim = TcgColors.Ink,
)
