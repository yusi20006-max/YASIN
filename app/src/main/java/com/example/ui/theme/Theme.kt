package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class YasinExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val agentAccent: Color,
    val agentContainer: Color,
    val heroStart: Color,
    val heroEnd: Color
)

val LocalYasinColors = staticCompositionLocalOf {
    YasinExtendedColors(
        success = EmeraldActive,
        onSuccess = Color.White,
        successContainer = EmeraldContainer,
        agentAccent = VioletAgent,
        agentContainer = VioletContainer,
        heroStart = CobaltSurfaceElevated,
        heroEnd = CobaltNight
    )
}

private val YasinDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CobaltNight,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = SoftCyan,
    secondary = WarmAmber,
    onSecondary = CobaltNight,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = SoftAmber,
    tertiary = VioletAgent,
    onTertiary = CobaltNight,
    tertiaryContainer = VioletContainer,
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = CobaltNight,
    onBackground = InkPrimaryDark,
    surface = CobaltSurface,
    onSurface = InkPrimaryDark,
    surfaceVariant = CobaltSurfaceElevated,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainerHigh = CobaltSurfaceHighlight,
    outline = OutlineDark,
    error = CoralError,
    onError = CobaltNight,
    errorContainer = CoralErrorContainer,
    onErrorContainer = Color(0xFFFFE4E6)
)

private val YasinLightColorScheme = lightColorScheme(
    primary = DeepOcean,
    onPrimary = Color.White,
    primaryContainer = DeepOceanContainer,
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = DeepAmber,
    onSecondary = Color.White,
    secondaryContainer = DeepAmberContainer,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = AlabasterBg,
    onBackground = InkPrimaryLight,
    surface = AlabasterSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = AlabasterElevated,
    onSurfaceVariant = InkSecondaryLight,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = OutlineLight,
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337)
)

val YasinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun YasinHubTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) YasinDarkColorScheme else YasinLightColorScheme
    val extendedColors = if (darkTheme) {
        YasinExtendedColors(
            success = EmeraldActive,
            onSuccess = CobaltNight,
            successContainer = EmeraldContainer,
            agentAccent = VioletAgent,
            agentContainer = VioletContainer,
            heroStart = Color(0xFF132244),
            heroEnd = CobaltNight
        )
    } else {
        YasinExtendedColors(
            success = Color(0xFF059669),
            onSuccess = Color.White,
            successContainer = Color(0xFFD1FAE5),
            agentAccent = Color(0xFF7C3AED),
            agentContainer = Color(0xFFEDE9FE),
            heroStart = Color(0xFF0B132B),
            heroEnd = Color(0xFF1E3A5F)
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalYasinColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YasinTypography,
            shapes = YasinShapes,
            content = content
        )
    }
}
