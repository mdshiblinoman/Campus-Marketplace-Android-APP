package com.example.campusmarketplace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val MarketplaceColorScheme = lightColorScheme(
    primary = MarketplaceBlue,
    onPrimary = Color.White,
    primaryContainer = MarketplaceSky,
    onPrimaryContainer = MarketplaceBlueDark,
    secondary = MarketplaceBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9FF),
    onSecondaryContainer = MarketplaceInk,
    background = MarketplaceBackground,
    onBackground = MarketplaceInk,
    surface = MarketplaceSurface,
    onSurface = MarketplaceInk,
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = MarketplaceMuted,
    outline = Color(0xFFD5DDE9),
    error = Color(0xFFBA1A1A)
)

private val MarketplaceShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun CampusMarketplaceTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MarketplaceColorScheme,
        shapes = MarketplaceShapes,
        typography = Typography,
        content = content
    )
}