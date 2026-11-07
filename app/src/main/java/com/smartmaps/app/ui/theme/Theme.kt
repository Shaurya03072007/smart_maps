package com.smartmaps.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Smart Maps Color System
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

// Primary â€” vibrant indigo
val Primary = Color(0xFF4F6BF4)
val PrimaryDark = Color(0xFF3A54D9)
val OnPrimary = Color(0xFFFFFFFF)

// Secondary â€” teal
val Secondary = Color(0xFF00BFA5)
val OnSecondary = Color(0xFFFFFFFF)

// Tertiary â€” warm amber
val Tertiary = Color(0xFFFFAB40)
val OnTertiary = Color(0xFF1A0000)

// â”€â”€ Dark palette â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
val DarkBackground = Color(0xFF0D1117)
val DarkBackgroundCard = Color(0xFF161C24)
val DarkBackgroundElevated = Color(0xFF1E2532)
val DarkOnBackground = Color(0xFFECEFF4)
val DarkSurface = Color(0xFF161C24)
val DarkSurfaceVariant = Color(0xFF1E2532)
val DarkOnSurface = Color(0xFFECEFF4)
val DarkOnSurfaceVariant = Color(0xFFB0BAC8)
val DarkPrimaryContainer = Color(0xFF1A2575)
val DarkOnPrimaryContainer = Color(0xFFBEC6FF)
val DarkSecondaryContainer = Color(0xFF003731)
val DarkOnSecondaryContainer = Color(0xFFB2F2E9)
val DarkOutline = Color(0xFF3A4455)
val DarkOutlineVariant = Color(0xFF2A3140)

// â”€â”€ Light palette â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
val LightBackground = Color(0xFFF7F8FA)
val LightBackgroundCard = Color(0xFFFFFFFF)
val LightBackgroundElevated = Color(0xFFF0F2F5)
val LightOnBackground = Color(0xFF1A1C20)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F2F5)
val LightOnSurface = Color(0xFF1A1C20)
val LightOnSurfaceVariant = Color(0xFF6B7280)
val LightPrimaryContainer = Color(0xFFDDE2FF)
val LightOnPrimaryContainer = Color(0xFF001A6E)
val LightSecondaryContainer = Color(0xFFCCF5EE)
val LightOnSecondaryContainer = Color(0xFF003731)
val LightOutline = Color(0xFFD1D5DB)
val LightOutlineVariant = Color(0xFFE5E7EB)
val LightErrorContainer = Color(0xFFFFDAD6)

// Status colors (shared)
val ColorAvailable = Color(0xFF4CAF50)
val ColorUnavailable = Color(0xFFEF5350)
val ColorWarning = Color(0xFFFFAB40)
val ColorVerified = Color(0xFF29B6F6)

// Error
val ErrorColor = Color(0xFFEF5350)
val OnError = Color(0xFFFFFFFF)

// Category colors
val CategoryColors = mapOf(
    "home_services" to Color(0xFF4F6BF4),
    "appliance_repair" to Color(0xFF7B52AB),
    "vehicle" to Color(0xFFEF5350),
    "technology" to Color(0xFF00BFA5),
    "beauty_wellness" to Color(0xFFE91E63),
    "healthcare" to Color(0xFF4CAF50),
    "education" to Color(0xFFFF9800),
    "events" to Color(0xFF5C6BC0),
    "professional" to Color(0xFF607D8B),
    "logistics" to Color(0xFF795548),
    "construction" to Color(0xFF9E9E9E),
    "pet" to Color(0xFF8D6E63),
    "agriculture" to Color(0xFF558B2F),
    "emergency" to Color(0xFFB71C1C)
)

// â”€â”€ Color Schemes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = ErrorColor,
    onError = OnError,
    errorContainer = Color(0xFF3D0000),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorColor,
    onError = OnError,
    errorContainer = LightErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

// â”€â”€ Theme Composable â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun SmartMapsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmartMapsTypography,
        content = content
    )
}

