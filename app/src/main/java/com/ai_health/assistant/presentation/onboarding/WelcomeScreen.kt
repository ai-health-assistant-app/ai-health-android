package com.ai_health.assistant.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B), // Slate 800
                        Color(0xFF334155)  // Slate 700
                    )
                )
            )
            .padding(24.dp)
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
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 50.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Unleash the power of AI to analyze your health data and give you personalized insights.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF94A3B8), // Slate 400
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8), // Sky 400
                contentColor = Color(0xFF0F172A)
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
