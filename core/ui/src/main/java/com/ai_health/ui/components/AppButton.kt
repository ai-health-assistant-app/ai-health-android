package com.ai_health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme

/**
 * Premium button variants.
 */
enum class ButtonVariant {
    PRIMARY,    // Gradient background
    SECONDARY,  // Outlined with border
    TERTIARY    // Text only
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    customGradient: Brush? = null
) {
    val shape = RoundedCornerShape(AppDimensions.cornerRadiusMedium)
    
    val buttonModifier = when (variant) {
        ButtonVariant.PRIMARY -> {
            modifier
                .clip(shape)
                .background(
                    brush = customGradient ?: AppTheme.gradients.accent,
                    shape = shape
                )
                .defaultMinSize(minHeight = AppDimensions.buttonHeight)
        }
        
        ButtonVariant.SECONDARY -> {
            modifier
                .clip(shape)
                .background(Color.Transparent)
                .border(
                    width = AppDimensions.borderMedium,
                    color = AppTheme.colors.accentBlue,
                    shape = shape
                )
                .defaultMinSize(minHeight = AppDimensions.buttonHeight)
        }
        
        ButtonVariant.TERTIARY -> {
            modifier
                .clip(shape)
                .background(Color.Transparent)
                .defaultMinSize(minHeight = AppDimensions.buttonHeight)
        }
    }
    
    Box(
        modifier = buttonModifier
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(
                horizontal = AppDimensions.buttonPaddingHorizontal,
                vertical = AppDimensions.space3
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppTheme.colors.textPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.iconSizeMedium),
                        tint = if (variant == ButtonVariant.TERTIARY) {
                            AppTheme.colors.accentBlue
                        } else {
                            AppTheme.colors.textPrimary
                        }
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.space2))
                }
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when (variant) {
                        ButtonVariant.PRIMARY -> AppTheme.colors.textPrimary
                        ButtonVariant.SECONDARY -> AppTheme.colors.accentBlue
                        ButtonVariant.TERTIARY -> AppTheme.colors.accentBlue
                    }
                )
            }
        }
    }
}
