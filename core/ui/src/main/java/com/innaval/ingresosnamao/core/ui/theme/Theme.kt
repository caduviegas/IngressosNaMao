package com.innaval.ingresosnamao.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun IngressosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extras = if (darkTheme) DarkExtraColors else LightExtraColors
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalExtraColors provides extras,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = IngressosTypography,
            shapes = IngressosShapes,
            content = content,
        )
    }
}

/** Access to non-Material tokens: IngressosTheme.spacing.md */
object IngressosTheme {
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val extras: ExtraColors
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current
}
