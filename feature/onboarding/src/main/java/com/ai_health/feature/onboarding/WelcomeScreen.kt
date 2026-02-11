package com.ai_health.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_health.ui.components.AppBackground
import com.ai_health.ui.components.AppButton
import com.ai_health.ui.components.ButtonVariant
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme

/**
 * Composable function that renders the application's welcome screen.
 * This component serves as the primary entry point for the onboarding experience,
 * establishing the visual identity and communicating the core value proposition
 * of the AI Health Assistant to the user.
 *
 * @param onGetStarted A lambda callback executed when the user interacts with the "Get Started"
 * action, typically triggering a navigation event to the next step of the onboarding flow.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimensions.space6)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Health\nAssistant",
                    style = MaterialTheme.typography.displayMedium,
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 50.sp
                )
                Spacer(modifier = Modifier.height(AppDimensions.space6))
                Text(
                    text = "Unleash the power of AI to analyze your health data and give you personalized insights.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            AppButton(
                text = "Get Started",
                onClick = onGetStarted,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = AppDimensions.space6)
            )
        }
    }
}
