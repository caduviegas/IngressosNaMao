package com.innaval.ingresosnamao.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val IngressosTypography = Typography(
    displayLarge = type(57, 64),
    displayMedium = type(45, 52),
    displaySmall = type(36, 44),
    headlineLarge = type(32, 40),
    headlineMedium = type(28, 36),
    headlineSmall = type(24, 32),
    titleLarge = type(22, 28),
    titleMedium = type(16, 24, FontWeight.Medium),
    titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 20),
    bodySmall = type(12, 16),
    labelLarge = type(14, 20, FontWeight.Medium),
    labelMedium = type(12, 16, FontWeight.Medium),
    labelSmall = type(11, 16, FontWeight.Medium),
)

private fun type(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)
