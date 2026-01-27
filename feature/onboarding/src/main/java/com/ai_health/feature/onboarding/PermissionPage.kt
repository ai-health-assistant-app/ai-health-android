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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionPage(
    step: OnboardingStep,
    permissionStatus: PermissionStatus?,
    onGrantClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dashboard color scheme
    val backgroundColor = Color(0xFF0F172A)
    val cardColor = Color(0xFF1E293B)
    val accentColor = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF94A3B8)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = cardColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    modifier = Modifier.size(120.dp),
                    tint = if (permissionStatus == PermissionStatus.Granted) {
                        accentColor
                    } else {
                        textSecondary
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Status indicator
                AnimatedVisibility(
                    visible = permissionStatus == PermissionStatus.Granted,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission Granted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accentColor
                        )
                    }
                }

                // Buttons
                if (permissionStatus != PermissionStatus.Granted) {
                    Button(
                        onClick = onGrantClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = backgroundColor
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Grant Permission",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onSkipClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}
