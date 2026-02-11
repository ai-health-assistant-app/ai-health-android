package com.ai_health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme

/**
 * Premium background component with gradient support.
 * 
 * Variants:
 * - Gradient (default): Deep midnight gradient
 * - Solid: Single color background
 * - Custom: Provide your own Brush
 */
enum class BackgroundVariant {
    GRADIENT,
    SOLID,
    CUSTOM
}

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    variant: BackgroundVariant = BackgroundVariant.GRADIENT,
    customBrush: Brush? = null,
    customColor: Color? = null,
    contentPadding: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundModifier = when (variant) {
        BackgroundVariant.GRADIENT -> {
            modifier.background(customBrush ?: AppTheme.gradients.background)
        }
        BackgroundVariant.SOLID -> {
            modifier.background(customColor ?: AppTheme.colors.backgroundPrimary)
        }
        BackgroundVariant.CUSTOM -> {
            if (customBrush != null) {
                modifier.background(customBrush)
            } else if (customColor != null) {
                modifier.background(customColor)
            } else {
                // Fallback to default gradient
                modifier.background(AppTheme.gradients.background)
            }
        }
    }
    
    Box(
        modifier = backgroundModifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = if (contentPadding) {
                Modifier.padding(AppDimensions.space4)
            } else {
                Modifier
            },
            content = content
        )
    }
}
