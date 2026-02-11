package com.ai_health.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized spacing and sizing system for the app.
 * Follows a 4dp baseline grid for consistency.
 */
object AppDimensions {
    
    // MARK: - Spacing Scale
    val space0: Dp = 0.dp
    val space1: Dp = 4.dp
    val space2: Dp = 8.dp
    val space3: Dp = 12.dp
    val space4: Dp = 16.dp
    val space5: Dp = 20.dp
    val space6: Dp = 24.dp
    val space8: Dp = 32.dp
    val space10: Dp = 40.dp
    val space12: Dp = 48.dp
    val space16: Dp = 64.dp
    val space20: Dp = 80.dp
    val space24: Dp = 96.dp
    val space32: Dp = 128.dp
    
    // MARK: - Corner Radius
    val cornerRadiusSmall: Dp = 8.dp
    val cornerRadiusMedium: Dp = 16.dp
    val cornerRadiusLarge: Dp = 24.dp
    val cornerRadiusExtraLarge: Dp = 32.dp
    val cornerRadiusFull: Dp = 9999.dp // For pills/circles
    
    // MARK: - Border Widths
    val borderHairline: Dp = 0.5.dp
    val borderThin: Dp = 1.dp
    val borderMedium: Dp = 2.dp
    val borderThick: Dp = 4.dp
    
    // MARK: - Blur Radius (for glassmorphic effects)
    val blurSmall: Dp = 8.dp
    val blurMedium: Dp = 16.dp
    val blurLarge: Dp = 24.dp
    
    // MARK: - Component Specific
    val cardPadding: Dp = space4
    val cardElevation: Dp = 4.dp
    val buttonHeight: Dp = 48.dp
    val buttonPaddingHorizontal: Dp = space6
    val iconSizeSmall: Dp = 16.dp
    val iconSizeMedium: Dp = 24.dp
    val iconSizeLarge: Dp = 32.dp
}
