package com.cinewala.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Netflix-style dark color scheme (always dark, never white)
private val NetflixColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = Color.White,
    secondary = NetflixRedDark,
    onSecondary = Color.White,
    tertiary = NetflixRedDark,
    background = NetflixBackground,
    onBackground = NetflixOnBackground,
    surface = NetflixSurface,
    onSurface = NetflixOnSurface,
    surfaceVariant = NetflixSurfaceVariant,
    onSurfaceVariant = NetflixOnSurfaceVariant,
    outline = NetflixOutline
)

@Composable
fun CineWalaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NetflixColorScheme,
        typography = Typography,
        content = content
    )
}