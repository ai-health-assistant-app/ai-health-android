package com.ai_health.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Premium Color System for Health & Sleep Tracker
 * 
 * Deep Midnight Aesthetic with vibrant accents.
 * Designed for dark mode with glassmorphic surfaces.
 */

// MARK: - Midnight Base Palette
val MidnightDarkest = Color(0xFF0A0E1A)     // Deepest navy, almost black
val MidnightDark = Color(0xFF0F172A)        // Deep slate navy
val MidnightBase = Color(0xFF1E293B)        // Base midnight blue
val MidnightLight = Color(0xFF334155)       // Lighter slate
val MidnightLighter = Color(0xFF475569)     // Even lighter slate

// MARK: - Accent Colors
val NeonBlue = Color(0xFF3B82F6)            // Vibrant blue
val NeonBlueBright = Color(0xFF60A5FA)      // Brighter blue
val MintGreen = Color(0xFF10B981)           // Fresh mint green
val MintGreenBright = Color(0xFF34D399)     // Lighter mint
val SunsetOrange = Color(0xFFF59E0B)        // Warm sunset orange
val SunsetOrangeBright = Color(0xFFFBBF24)  // Bright amber
val PurpleGlow = Color(0xFF8B5CF6)          // Purple accent
val PurpleGlowBright = Color(0xFFA78BFA)    // Lighter purple

// MARK: - Text Colors
val TextPrimary = Color(0xFFFFFFFF)         // Pure white for headlines
val TextSecondary = Color(0xFFCBD5E1)       // Light gray-blue for body
val TextTertiary = Color(0xFF94A3B8)        // Muted gray-blue for labels
val TextDisabled = Color(0xFF64748B)        // Disabled text

// MARK: - Semantic Colors
val SuccessGreen = Color(0xFF10B981)        // Success states
val SuccessGreenLight = Color(0xFF34D399)
val WarningAmber = Color(0xFFF59E0B)        // Warning states
val WarningAmberLight = Color(0xFFFBBF24)
val ErrorRed = Color(0xFFEF4444)            // Error states
val ErrorRedLight = Color(0xFFF87171)
val InfoBlue = Color(0xFF3B82F6)            // Info states
val InfoBlueLight = Color(0xFF60A5FA)

// MARK: - Glass Surface Colors (Semi-transparent)
val GlassLight = Color(0x1AFFFFFF)          // 10% white for subtle glass
val GlassMedium = Color(0x33FFFFFF)         // 20% white for medium glass
val GlassDark = Color(0x1A1E293B)           // 10% midnight for dark glass
val GlassDarkMedium = Color(0x331E293B)     // 20% midnight for darker glass

// MARK: - Border Colors
val BorderSubtle = Color(0x1AFFFFFF)        // Very subtle border
val BorderMedium = Color(0x33FFFFFF)        // Medium border
val BorderBright = Color(0x4DFFFFFF)        // Brighter border (30%)
val BorderGlow = Color(0x66FFFFFF)          // Glowing border (40%)

// MARK: - Gradients

/**
 * Deep midnight gradient for app background.
 * From darkest navy at top to slightly lighter at bottom.
 */
val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        MidnightDarkest,
        MidnightDark,
        Color(0xFF1A2332)  // Slightly blue-tinted
    )
)

/**
 * Subtle gradient for cards and surfaces.
 */
val CardGradient = Brush.verticalGradient(
    colors = listOf(
        MidnightBase.copy(alpha = 0.6f),
        MidnightLight.copy(alpha = 0.4f)
    )
)

/**
 * Vibrant blue to purple gradient for accents.
 * Perfect for progress bars, highlights, and CTAs.
 */
val AccentGradient = Brush.horizontalGradient(
    colors = listOf(
        NeonBlue,
        PurpleGlow
    )
)

/**
 * Alternative accent gradient: Mint to blue.
 */
val AccentGradientAlt = Brush.horizontalGradient(
    colors = listOf(
        MintGreen,
        NeonBlue
    )
)

/**
 * Sunset gradient: Orange to pink/purple.
 */
val SunsetGradient = Brush.horizontalGradient(
    colors = listOf(
        SunsetOrange,
        Color(0xFFEC4899) // Pink
    )
)

/**
 * Glow gradient for highlights and emphasis.
 * Creates a radial glow effect.
 */
val GlowGradient = Brush.radialGradient(
    colors = listOf(
        NeonBlueBright.copy(alpha = 0.3f),
        Color.Transparent
    )
)

/**
 * Glass surface gradient with subtle highlight.
 */
val GlassGradient = Brush.verticalGradient(
    colors = listOf(
        GlassMedium,
        GlassLight
    )
)

// MARK: - Chart/Data Visualization Colors
val ChartBlue = NeonBlue
val ChartGreen = MintGreen
val ChartOrange = SunsetOrange
val ChartPurple = PurpleGlow
val ChartPink = Color(0xFFEC4899)
val ChartYellow = Color(0xFFFBBF24)

// MARK: - Sleep Stage Colors (for sleep tracking visualizations)
val SleepDeep = Color(0xFF3730A3)           // Deep indigo for deep sleep
val SleepLight = Color(0xFF60A5FA)          // Light blue for light sleep
val SleepREM = Color(0xFF8B5CF6)            // Purple for REM
val SleepAwake = Color(0xFFF59E0B)          // Amber for awake periods