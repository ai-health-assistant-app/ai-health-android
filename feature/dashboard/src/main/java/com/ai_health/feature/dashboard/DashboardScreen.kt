package com.ai_health.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome // La scintilla ✨
import androidx.compose.material.icons.rounded.ChatBubbleOutline // Il fumetto 💬
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // Added for biomimetic animations
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_health.ui.components.*
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    // AGGIUNGI QUESTI DUE PARAMETRI
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onMetricClick: (String) -> Unit,
    onChatClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    val onRefresh = { viewModel.refreshData() }

    // Calculate derived metrics
    val sleepScore = state.sleepQualityAnalysis?.totalScore ?: 0
    val readinessText = when {
        sleepScore >= 80 -> "Batterie cariche. Ottimo giorno per allenarsi."
        sleepScore >= 60 -> "Giornata stabile. Mantieni il ritmo."
        sleepScore > 0 -> "Riposo consigliato. Non esagerare oggi."
        else -> "Nessun dato sul sonno. Indossa il dispositivo stanotte."
    }

    // Calculate steps progress (Goal: 10,000 steps fallback)
    val stepsValue = state.stepsFormatted.replace(",", "").replace(".", "").replace(" ", "").toIntOrNull() ?: 0
    val stepsGoal = 10000
    val stepsProgress = (stepsValue.toFloat() / stepsGoal).coerceIn(0f, 1f)

    AppBackground(contentPadding = false) {
        PullToRefreshBox(
            modifier = Modifier.statusBarsPadding(), // Handle top insets
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    containerColor = AppTheme.colors.surfacePrimary,
                    color = AppTheme.colors.accentBlue,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimensions.space4),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.space4),
                contentPadding = PaddingValues(
                    bottom = 32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                // 1. Header (Greeting + Date)
                item {
                    DashboardHeader()
                }

                // 2. Activity Section (Visual)
                item {
                    Text(
                        "Attività di oggi",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )
                    ActivityProgressCard(
                        steps = state.stepsFormatted,
                        distance = state.distanceFormatted,
                        calories = state.caloriesFormatted,
                        progress = stepsProgress,
                        onClick = { onMetricClick("steps") }
                    )
                }

                // 3. Vitals Grid (Asymmetrical)
                item {
                    Text(
                        "Parametri Vitali",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.space3)) {
                        // Left Column (HR)
                        MetricMiniCard(
                            title = "Battito",
                            value = state.heartRateFormatted,
                            unit = "bpm",
                            icon = Icons.Rounded.Favorite,
                            iconColor = AppTheme.colors.error,
                            animationType = IconAnimationType.PULSE,
                            modifier = Modifier.weight(1f),
                            onClick = { onMetricClick("hr") }
                        )

                        // Right Column (Sleep Duration)
                        MetricMiniCard(
                            title = "Sonno",
                            value = state.sleepTimeFormatted,
                            unit = "ore",
                            icon = Icons.Rounded.Bedtime,
                            iconColor = AppTheme.colors.accentPurple,
                            animationType = IconAnimationType.ROCK,
                            modifier = Modifier.weight(1f),
                            onClick = { onMetricClick("sleep") }
                        )
                    }
                }

                // 4. Extra (Oxygen)
                item {
                    MetricWideRow(
                        title = "Ossigenazione (SpO2)",
                        value = state.oxygenFormatted,
                        icon = Icons.Rounded.Air,
                        animationType = IconAnimationType.FLOAT,
                        onClick = { onMetricClick("ox") }
                    )
                }
                // 5. Hero Section: Daily Focus
                item {
                    Text(
                        "Assistente Digitale",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )
                    DailyFocusCard(
                        readinessText = readinessText,
                        // PASSA GLI SCOPE QUI
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onChatClick
                    )
                }
            }
        }
    }
}

// --- DEDICATED COMPONENTS ---

@Composable
fun DashboardHeader() {
    val dateStr = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN))
    }

    val hour = java.time.LocalTime.now().hour
    val greeting = when(hour) {
        in 5..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = dateStr.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.colors.textTertiary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$greeting Angelo!",
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DailyFocusCard(
    readinessText: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    with(sharedTransitionScope) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // ... (Parte Messaggio AI: Avatar + Bubble - RIMANE UGUALE A PRIMA) ...
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AppTheme.colors.accentPurple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        /*modifier = Modifier
                            .size(20.dp)
                            .biomimeticAnimation(IconAnimationType.TWINKLE)*/
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = AppTheme.colors.surfacePrimary,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 4.dp
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Health Coach",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.accentPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Ciao Angelo! Ho notato che il tuo sonno è stato ottimo, ma hai camminato poco. Analizziamo?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            // ... (Fine Parte Messaggio) ...

            Spacer(modifier = Modifier.height(12.dp))

            // --- L'AREA INPUT CHE SI ANIMA ---
            // Deve essere identica a quella della ChatScreen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Altezza identica alla chat
                    // 1. Modifier dell'animazione
                    .sharedElement(
                        rememberSharedContentState(key = "chat_input_area"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    // 2. Modifier visivi (sfondo e click) applicati DOPO sharedElement
                    .clip(CircleShape) // Importante per il ripple effect
                    .background(AppTheme.colors.surfacePrimary) // Colore identico alla chat
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp), // Padding interno
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Scrivi un messaggio...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textTertiary,
                        modifier = Modifier.weight(1f)
                    )

                    // Icona Send finta (visiva)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AppTheme.colors.accentBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityProgressCard(
    steps: String,
    distance: String,
    calories: String,
    progress: Float,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Rounded.DirectionsWalk,
                    contentDescription = null,
                    tint = AppTheme.colors.accentBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Passi", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Text(steps, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
            }

            // Visual Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = AppTheme.colors.accentBlue,
                trackColor = AppTheme.colors.surfacePrimary
            )

            // Secondary Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$distance km", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
                Text("$calories kcal", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    animationType: IconAnimationType = IconAnimationType.NONE,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(18.dp)
                        .biomimeticAnimation(animationType, value) // Pass value for BPM
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = "$unit $title",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun MetricWideRow(
    title: String,
    value: String,
    icon: ImageVector,
    animationType: IconAnimationType = IconAnimationType.NONE, // Add animation support
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, contentPadding = 16.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = AppTheme.colors.textTertiary,
                    modifier = Modifier.biomimeticAnimation(animationType)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.textSecondary)
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        }
    }
}

// --- BIOMIMETIC ANIMATIONS ---

enum class IconAnimationType {
    NONE, PULSE, ROCK, FLOAT, TWINKLE
}

@Composable
fun Modifier.biomimeticAnimation(
    type: IconAnimationType,
    value: String = "0" // Used for BPM dynamic calculation
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "biomimetic_${type.name}")

    return when (type) {
        IconAnimationType.PULSE -> {
            // Pulse Logic: Calculate duration based on BPM
            val bpm = value.toIntOrNull()?.coerceAtLeast(40) ?: 60
            val durationMillis = (60000 / bpm).coerceAtLeast(300) // limit max speed

            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis / 2, easing = FastOutSlowInEasing), // Systole
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            this.scale(scale)
        }
        IconAnimationType.ROCK -> {
            // Rock Logic: Sleep icon gently rocking
            val rotation by infiniteTransition.animateFloat(
                initialValue = -20f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rock_rotation"
            )
            this.graphicsLayer { rotationZ = rotation }
        }
        IconAnimationType.FLOAT -> {
            // Breathing Logic: Slow expansion/contraction for Oxygen (Respiro)
            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.20f, // Espansione leggera (10%), meno aggressiva del cuore
                animationSpec = infiniteRepeatable(
                    // 3000ms = 3 secondi per inspirare, 3 per espirare (ciclo tot 6s).
                    // Molto rilassante, tipico di un respiro profondo.
                    animation = tween(3000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "breath_scale"
            )

            // Applica la scala uniformemente
            this.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        }
        IconAnimationType.TWINKLE -> {
            // Twinkle Logic: AI Magic
            val scale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "twinkle_scale"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "twinkle_rotation"
            )
            this
                .scale(scale)
                .graphicsLayer { rotationZ = rotation }
        }
        IconAnimationType.NONE -> this
    }
}