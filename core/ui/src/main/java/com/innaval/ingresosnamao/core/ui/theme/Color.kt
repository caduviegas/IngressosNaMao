package com.innaval.ingresosnamao.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** Raw palette. Never referenced by features; use MaterialTheme.colorScheme or IngressosTheme.extras. */
internal object Palette {
    val Brand = Color(0xFFE53935)
    val BrandDark = Color(0xFFFF8A80)
    val Accent = Color(0xFF6A1B9A)
    val AccentDark = Color(0xFFCE93D8)
    val Success = Color(0xFF2E7D32)
    val SuccessDark = Color(0xFF81C784)
}

internal val LightColors = lightColorScheme(primary = Palette.Brand, secondary = Palette.Accent)
internal val DarkColors = darkColorScheme(primary = Palette.BrandDark, secondary = Palette.AccentDark)
