package com.innaval.ingresosnamao.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

/** Tokens that Material3's ColorScheme doesn't cover. */
@Immutable
data class ExtraColors(val success: Color)

internal val LocalSpacing = staticCompositionLocalOf { Spacing() }
internal val LocalExtraColors = staticCompositionLocalOf { ExtraColors(Palette.Success) }
