package com.innaval.ingresosnamao.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Raw palette. Never referenced by features; use MaterialTheme.colorScheme or IngressosTheme.extras. */
internal object Palette {
    val Brand = Color(0xFFB3261E)
    val BrandDark = Color(0xFFFFB4AB)
    val Accent = Color(0xFF6A1B9A)
    val AccentDark = Color(0xFFE5B3FF)
    val Success = Color(0xFF2E7D32)
    val SuccessDark = Color(0xFF81C784)
}

internal val LightColors = lightColorScheme(
    primary = Palette.Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Palette.Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6D9FF),
    onSecondaryContainer = Color(0xFF2C0051),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A19),
)

internal val DarkColors = darkColorScheme(
    primary = Palette.BrandDark,
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Palette.AccentDark,
    onSecondary = Color(0xFF3A005B),
    secondaryContainer = Color(0xFF520078),
    onSecondaryContainer = Color(0xFFF6D9FF),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDD),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDD),
)

internal val LightExtraColors = ExtraColors(success = Palette.Success)
internal val DarkExtraColors = ExtraColors(
    success = Palette.SuccessDark,
    onSuccess = Color(0xFF00390B),
    successContainer = Color(0xFF145B21),
    onSuccessContainer = Color(0xFFA3F7A5),
)
