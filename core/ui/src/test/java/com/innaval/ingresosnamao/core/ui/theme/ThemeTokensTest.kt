package com.innaval.ingresosnamao.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {
    @Test fun textAndControlColorsMeetContrastInBothThemes() {
        verifyColors(LightColors, LightExtraColors)
        verifyColors(DarkColors, DarkExtraColors)
    }

    @Test fun spacingAndTypographyUseScalableTokens() {
        assertEquals(listOf(4.dp, 8.dp, 16.dp, 24.dp, 32.dp),
            Spacing().let { listOf(it.xs, it.sm, it.md, it.lg, it.xl) })
        assertEquals(16.sp, IngressosTypography.bodyLarge.fontSize)
        assertEquals(24.sp, IngressosTypography.headlineSmall.fontSize)
    }

    private fun verifyColors(colors: ColorScheme, extras: ExtraColors) {
        val textPairs = listOf(
            colors.onPrimary to colors.primary,
            colors.onPrimaryContainer to colors.primaryContainer,
            colors.onSecondary to colors.secondary,
            colors.onSecondaryContainer to colors.secondaryContainer,
            colors.onError to colors.error,
            colors.onErrorContainer to colors.errorContainer,
            colors.onSurface to colors.surface,
            colors.onSurface to colors.surfaceContainerHighest,
            colors.onSurfaceVariant to colors.surface,
            colors.onBackground to colors.background,
            colors.onSurfaceVariant to colors.surfaceVariant,
            colors.primary to colors.surface,
            colors.secondary to colors.surface,
            colors.error to colors.surface,
            extras.success to colors.surface,
            extras.onSuccess to extras.success,
            extras.onSuccessContainer to extras.successContainer,
        )
        textPairs.forEach { (foreground, background) ->
            val ratio = contrast(foreground, background)
            assertTrue("$foreground on $background has contrast $ratio", ratio >= 4.5)
        }
        assertTrue("Outline contrast must be at least 3:1", contrast(colors.outline, colors.surface) >= 3.0)
    }

    private fun contrast(first: Color, second: Color): Double {
        val a = first.luminance().toDouble()
        val b = second.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}
