package com.ai_health.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme

/**
 * Premium card component with multiple variants.
 * 
 * Variants:
 * - SOLID: Semi-transparent surface with subtle border
 * - OUTLINED: Transparent with prominent border
 * - GLASS: Glassmorphic effect with semi-transparency and hairline border
 */
enum class CardVariant {
    SOLID,
    OUTLINED,
    GLASS
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.SOLID,
    cornerRadius: Dp = AppDimensions.cornerRadiusMedium,
    contentPadding: Dp = AppDimensions.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    val cardModifier = modifier
        .clip(shape)
        .then(
            when (variant) {
                CardVariant.SOLID -> {
                    Modifier
                        .background(
                            color = AppTheme.colors.surfacePrimary.copy(alpha = 0.6f),
                            shape = shape
                        )
                        .border(
                            width = AppDimensions.borderThin,
                            color = AppTheme.colors.borderSubtle,
                            shape = shape
                        )
                }
                
                CardVariant.OUTLINED -> {
                    Modifier
                        .background(Color.Transparent)
                        .border(
                            width = AppDimensions.borderMedium,
                            color = AppTheme.colors.borderMedium,
                            shape = shape
                        )
                }
                
                CardVariant.GLASS -> {
                    Modifier
                        .background(
                            brush = AppTheme.gradients.glass,
                            shape = shape
                        )
                        .border(
                            width = AppDimensions.borderHairline,
                            color = AppTheme.colors.borderBright,
                            shape = shape
                        )
                }
            }
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(contentPadding)
    
    Column(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Variant with gradient border for extra premium feel.
 */
@Composable
fun AppCardGradientBorder(
    modifier: Modifier = Modifier,
    borderBrush: Brush = AppTheme.gradients.accent,
    borderWidth: Dp = AppDimensions.borderMedium,
    cornerRadius: Dp = AppDimensions.cornerRadiusMedium,
    backgroundColor: Color = AppTheme.colors.surfacePrimary.copy(alpha = 0.6f),
    contentPadding: Dp = AppDimensions.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    // Create layered effect: gradient border + background
    Box(
        modifier = modifier
            .clip(shape)
            .background(borderBrush, shape)
            .padding(borderWidth)
    ) {
        val cardModifier = Modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(contentPadding)
        
        Column(
            modifier = cardModifier,
            content = content
        )
    }
}
