package com.apkpackager.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.apkpackager.R

// ── Yoinkins Color Palette (derived from pig mascot) ──

// Backgrounds (warm dark, not pure black)
private val Background = Color(0xFF1A1625)
private val YoinkinsSurface = Color(0xFF221E30)
private val YoinkinsSurfaceVariant = Color(0xFF2D2945)
private val YoinkinsSurfaceContainerHighest = Color(0xFF383350)

// Primary (pig's coral-pink)
private val Primary = Color(0xFFFF8FA3)
private val OnPrimary = Color(0xFF1A1625)
private val PrimaryContainer = Color(0xFF4A2035)
private val OnPrimaryContainer = Color(0xFFFFD1DC)

// Secondary (cyan from mascot's code lines)
private val Secondary = Color(0xFF7DD3E8)
private val OnSecondary = Color(0xFF1A1625)
private val SecondaryContainer = Color(0xFF1A3A45)
private val OnSecondaryContainer = Color(0xFFB8EAF6)

// Tertiary (gold from mascot's bracket symbols)
private val Tertiary = Color(0xFFF5C842)
private val OnTertiary = Color(0xFF1A1625)
private val TertiaryContainer = Color(0xFF3D3520)
private val OnTertiaryContainer = Color(0xFFFFE8A0)

// Error
private val YoinkinsError = Color(0xFFFF6B6B)
private val OnError = Color(0xFF1A1625)
private val ErrorContainer = Color(0xFF4A2020)
private val OnErrorContainer = Color(0xFFFFB3B3)

// Surface text
private val OnBackground = Color(0xFFEDE7F6)
private val OnSurface = Color(0xFFEDE7F6)
private val OnSurfaceVariant = Color(0xFFA09BB5)
private val Outline = Color(0xFF4A4660)
private val OutlineVariant = Color(0xFF332F48)

private val YoinkinsDarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = YoinkinsError,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = YoinkinsSurface,
    onSurface = OnSurface,
    surfaceVariant = YoinkinsSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceContainerHighest = YoinkinsSurfaceContainerHighest,
)

// ── Light Color Palette ──

private val LightBackground = Color(0xFFFFF5F7)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF2E8EC)
private val LightSurfaceContainerHighest = Color(0xFFEDE0E5)

private val LightPrimary = Color(0xFFC4506A)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFD9E0)
private val LightOnPrimaryContainer = Color(0xFF3F0018)

private val LightSecondary = Color(0xFF2D7D91)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCCEEF6)
private val LightOnSecondaryContainer = Color(0xFF0A2E38)

private val LightTertiary = Color(0xFF8A7000)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFFFE170)
private val LightOnTertiaryContainer = Color(0xFF2A2100)

private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)

private val LightOnBackground = Color(0xFF1A1625)
private val LightOnSurface = Color(0xFF1A1625)
private val LightOnSurfaceVariant = Color(0xFF564D58)
private val LightOutline = Color(0xFF8A8090)
private val LightOutlineVariant = Color(0xFFD4C8D4)

private val YoinkinsLightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

// ── Typography ──

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
)

private val YoinkinsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun YoinkinsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> YoinkinsDarkColors
        else -> YoinkinsLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YoinkinsTypography,
        content = content,
    )
}
