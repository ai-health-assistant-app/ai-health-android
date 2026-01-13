package com.ai_health.assistant.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable screen that informs the user about the health permissions required by the application.
 * It provides a clear overview of the data types being accessed and allows the user to either
 * grant access or skip the step.
 *
 * @param onRequestPermissions Lambda callback invoked when the user clicks the "Grant Access" button.
 * @param onSkip Lambda callback invoked when the user chooses to "Skip for now".
 */
@Composable
fun HealthPermissionScreen(
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate 900
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Text(
                text = "Connect Health",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "We need access to your health data to provide AI-powered insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PermissionItem(Icons.Rounded.DirectionsRun, "Steps", "Activity tracking")
            PermissionItem(Icons.Rounded.Favorite, "Heart Rate", "Cardio health")
            PermissionItem(Icons.Rounded.FitnessCenter, "Workouts", "Exercise logs")
            PermissionItem(Icons.Rounded.LocalFireDepartment, "Calories", "Energy burn")
        }

        Column {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF0F172A)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = "Grant Access",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now",
                    color = Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * A layout component that represents a single permission entry.
 * Displays an icon alongside a title and a descriptive subtitle within a styled container.
 *
 * @param icon The graphical representation of the permission type.
 * @param title The primary label of the permission (e.g., "Steps").
 * @param subtitle A short explanation of the data's purpose (e.g., "Activity tracking").
 */
@Composable
fun PermissionItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E293B), MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
        }
    }
}
