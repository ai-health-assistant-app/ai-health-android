package com.ai_health.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ai_health.ui.components.AppButton
import com.ai_health.ui.components.AppCard
import com.ai_health.ui.components.ButtonVariant
import com.ai_health.ui.components.CardVariant
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme

@Composable
fun PermissionPage(
    step: OnboardingStep,
    permissionStatus: PermissionStatus?,
    onGrantClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
            .padding(AppDimensions.space6),
        contentAlignment = Alignment.Center
    ) {
        AppCard(
            variant = CardVariant.NORMAL,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentPadding = AppDimensions.space8
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    modifier = Modifier.size(120.dp),
                    tint = if (permissionStatus == PermissionStatus.Granted) {
                        AppTheme.colors.accentBlue
                    } else {
                        AppTheme.colors.textSecondary
                    }
                )

                Spacer(modifier = Modifier.height(AppDimensions.space8))

                // Title
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(AppDimensions.space4))

                // Description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = AppDimensions.space2)
                )

                Spacer(modifier = Modifier.height(AppDimensions.space8))

                // Status indicator
                AnimatedVisibility(
                    visible = permissionStatus == PermissionStatus.Granted,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = AppDimensions.space4)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = AppTheme.colors.accentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.space2))
                        Text(
                            text = "Permission Granted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.accentBlue
                        )
                    }
                }

                // Buttons
                if (permissionStatus != PermissionStatus.Granted) {
                    AppButton(
                        text = "Grant Permission",
                        onClick = onGrantClick,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.space3))

                    TextButton(
                        onClick = onSkipClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
