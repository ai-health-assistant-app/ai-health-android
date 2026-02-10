package com.ai_health.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Custom theme extension for Health & Sleep Tracker.
 * Provides access to premium design tokens beyond Material3.
 */
@Immutable
data class AppColors(
    // Background
    val backgroundPrimary: Color = MidnightDarkest,
    val backgroundSecondary: Color = MidnightDark,
    val backgroundTertiary: Color = MidnightBase,
    
    // Surface
    val surfacePrimary: Color = MidnightBase,
    val surfaceSecondary: Color = MidnightLight,
    val surfaceGlass: Color = GlassMedium,
    
    // Text
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextTertiary,
    val textDisabled: Color = TextDisabled,
    
    // Accent
    val accentBlue: Color = NeonBlue,
    val accentGreen: Color = MintGreen,
    val accentOrange: Color = SunsetOrange,
    val accentPurple: Color = PurpleGlow,
    
    // Semantic
    val success: Color = SuccessGreen,
    val warning: Color = WarningAmber,
    val error: Color = ErrorRed,
    val info: Color = InfoBlue,
    
    // Border
    val borderSubtle: Color = BorderSubtle,
    val borderMedium: Color = BorderMedium,
    val borderBright: Color = BorderBright
)

@Immutable
data class AppGradients(
    val background: Brush = BackgroundGradient,
    val card: Brush = CardGradient,
    val accent: Brush = AccentGradient,
    val accentAlt: Brush = AccentGradientAlt,
    val sunset: Brush = SunsetGradient,
    val glow: Brush = GlowGradient,
    val glass: Brush = GlassGradient
)

@Immutable
data class AppShapes(
    val small: RoundedCornerShape = RoundedCornerShape(AppDimensions.cornerRadiusSmall),
    val medium: RoundedCornerShape = RoundedCornerShape(AppDimensions.cornerRadiusMedium),
    val large: RoundedCornerShape = RoundedCornerShape(AppDimensions.cornerRadiusLarge),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(AppDimensions.cornerRadiusExtraLarge),
    val full: RoundedCornerShape = RoundedCornerShape(AppDimensions.cornerRadiusFull)
)

/**
 * Complete theme configuration object.
 * Access via AppTheme.colors, AppTheme.gradients, etc.
 */
@Immutable
data class AppThemeConfig(
    val colors: AppColors = AppColors(),
    val gradients: AppGradients = AppGradients(),
    val dimensions: AppDimensions = AppDimensions,
    val shapes: AppShapes = AppShapes()
)

// CompositionLocal for custom theme
private val LocalAppTheme = staticCompositionLocalOf { AppThemeConfig() }

/**
 * Access point for custom theme tokens.
 * Usage: AppTheme.colors.accentBlue
 */
object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppTheme.current.colors
    
    val gradients: AppGradients
        @Composable
        get() = LocalAppTheme.current.gradients
    
    val dimensions: AppDimensions
        @Composable
        get() = LocalAppTheme.current.dimensions
    
    val shapes: AppShapes
        @Composable
        get() = LocalAppTheme.current.shapes
}

/**
 * Material3 ColorScheme mapped to our premium colors.
 */
private val AppDarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    onPrimary = TextPrimary,
    primaryContainer = MidnightBase,
    onPrimaryContainer = NeonBlueBright,
    
    secondary = PurpleGlow,
    onSecondary = TextPrimary,
    secondaryContainer = MidnightBase,
    onSecondaryContainer = PurpleGlowBright,
    
    tertiary = MintGreen,
    onTertiary = MidnightDarkest,
    tertiaryContainer = MidnightBase,
    onTertiaryContainer = MintGreenBright,
    
    background = MidnightDarkest,
    onBackground = TextPrimary,
    
    surface = MidnightBase,
    onSurface = TextPrimary,
    surfaceVariant = MidnightLight,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = MidnightBase,
    onErrorContainer = ErrorRedLight,
    
    outline = BorderMedium,
    outlineVariant = BorderSubtle
)

/**
 * Material3 Shapes using our design system.
 */
private val AppShapesM3 = Shapes(
    extraSmall = RoundedCornerShape(AppDimensions.cornerRadiusSmall),
    small = RoundedCornerShape(AppDimensions.cornerRadiusSmall),
    medium = RoundedCornerShape(AppDimensions.cornerRadiusMedium),
    large = RoundedCornerShape(AppDimensions.cornerRadiusLarge),
    extraLarge = RoundedCornerShape(AppDimensions.cornerRadiusExtraLarge)
)

/**
 * Main theme composable for Health & Sleep Tracker.
 * 
 * Replaces AssistantTheme with premium dark design system.
 * Dynamic color is disabled by default to ensure consistent branding.
 * 
 * @param darkTheme Forces dark theme (light mode not implemented yet)
 * @param dynamicColor Enable Android 12+ dynamic colors (disabled by default)
 * @param content Your app content
 */
@Composable
fun HealthAppTheme(
    darkTheme: Boolean = true, // Always dark mode
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> AppDarkColorScheme
    }
    
    val appTheme = AppThemeConfig()
    
    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapesM3,
            content = content
        )
    }
}

/**
 * LEGACY: Keep for backward compatibility.
 * @deprecated Use HealthAppTheme instead
 */
@Deprecated(
    message = "Use HealthAppTheme instead for premium design system",
    replaceWith = ReplaceWith("HealthAppTheme(darkTheme, dynamicColor, content)")
)
@Composable
fun AssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    HealthAppTheme(darkTheme, dynamicColor, content)
}