package com.orioooneee.lmuasister.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LmuColors = darkColorScheme(
    primary = Amber,
    onPrimary = Carbon,
    primaryContainer = AmberDim,
    onPrimaryContainer = Amber,
    secondary = Lime,
    onSecondary = Carbon,
    secondaryContainer = LimeDim,
    onSecondaryContainer = Lime,
    background = Carbon,
    onBackground = TextHigh,
    surface = Surface1,
    onSurface = TextHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMed,
    outline = Outline,
    outlineVariant = OutlineSoft,
    error = ClassHyper,
    onError = Carbon,
)

private val LmuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LmuTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    )
}

@Composable
fun LmuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LmuColors,
        shapes = LmuShapes,
        typography = LmuTypography,
        content = content,
    )
}
