package com.ai_health.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_health.ui.components.*
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.theme.BackgroundGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.BorderStroke

/**
 * Main Dashboard Screen — 3 layers:
 * 1. Base: Dashboard cards (top) + chat preview (bottom)
 * 2. Expanded card: Detail (top 88%) + input bar (bottom 12%)
 * 3. Fullscreen chat overlay (slides in from bottom, covers everything)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    chatViewModel: ChatViewModel,
    onMetricClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val onRefresh = { viewModel.refreshData() }
    val focusManager = LocalFocusManager.current

    // --- State ---
    var expandedCard by remember { mutableStateOf<String?>(null) }
    var isChatFullscreen by remember { mutableStateOf(false) }

    // --- BackHandler: fullscreen chat → expanded card → exit ---
    BackHandler(enabled = isChatFullscreen || expandedCard != null) {
        when {
            isChatFullscreen -> {
                isChatFullscreen = false
                focusManager.clearFocus()
            }
            expandedCard != null -> expandedCard = null
        }
    }

    // --- Animated weights (dashboard 52/48, expanded 88/12) ---
    val targetTopWeight = if (expandedCard != null) 0.88f else 0.52f
    val animatedTopWeight by animateFloatAsState(
        targetValue = targetTopWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "topWeight"
    )
    val animatedBottomWeight by animateFloatAsState(
        targetValue = 1f - targetTopWeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bottomWeight"
    )

    // Derived metrics
    val stepsValue = state.stepsFormatted.replace(",", "").replace(".", "").replace(" ", "").toIntOrNull() ?: 0
    val stepsProgress = (stepsValue.toFloat() / 10000f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        // ==========================================================
        // LAYER 1: Dashboard base (cards + chat preview / input bar)
        // ==========================================================
        AppBackground(contentPadding = false) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // --- TOP: Dashboard Cards OR Expanded Detail ---
                Box(modifier = Modifier.weight(animatedTopWeight)) {
                    Crossfade(
                        targetState = expandedCard,
                        animationSpec = tween(350),
                        label = "card_crossfade"
                    ) { currentExpandedCard ->
                        if (currentExpandedCard == null) {
                            DashboardCardsContent(
                                state = state,
                                pullToRefreshState = pullToRefreshState,
                                isRefreshing = isRefreshing,
                                onRefresh = onRefresh,
                                stepsProgress = stepsProgress,
                                onSettingsClick = onSettingsClick,
                                onMetricClick = onMetricClick,
                                onExpandCard = { expandedCard = it }
                            )
                        } else {
                            ExpandedDetailContent(
                                expandedCard = currentExpandedCard,
                                state = state,
                                viewModel = viewModel,
                                onClose = { expandedCard = null }
                            )
                        }
                    }
                }

                // --- BOTTOM: Chat preview (dashboard) or input bar (expanded) ---
                Surface(
                    modifier = Modifier
                        .weight(animatedBottomWeight)
                        .fillMaxWidth(),
                    color = AppTheme.colors.backgroundPrimary,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    if (expandedCard != null) {
                        // Expanded mode: just input bar
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            ChatInputBar(
                                onSend = { chatViewModel.sendMessage(it) },
                                onFocusGained = { isChatFullscreen = true },
                                key = isChatFullscreen
                            )
                        }
                    } else {
                        // Dashboard mode: full chat preview
                        ChatSheetContent(
                            viewModel = chatViewModel,
                            modifier = Modifier.fillMaxSize(),
                            onInputFocused = { isChatFullscreen = true }
                        )
                    }
                }
            }
        }

        // ==========================================================
        // LAYER 2: Fullscreen chat overlay (slides up from bottom)
        // ==========================================================
        AnimatedVisibility(
            visible = isChatFullscreen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                color = AppTheme.colors.backgroundPrimary
            ) {
                ChatSheetContent(
                    viewModel = chatViewModel,
                    modifier = Modifier.fillMaxSize(),
                    autoFocus = true
                )
            }
        }
    }
}

// ═══════════════════════════════════════════
//  Sub-composables (extracted for clarity)
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardCardsContent(
    state: DashboardUiState,
    pullToRefreshState: androidx.compose.material3.pulltorefresh.PullToRefreshState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    stepsProgress: Float,
    onSettingsClick: () -> Unit,
    onMetricClick: (String) -> Unit,
    onExpandCard: (String) -> Unit
) {
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
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
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item { DashboardHeader(onSettingsClick = onSettingsClick) }

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

            item {
                Text(
                    "Parametri Vitali",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.space3)) {
                    MetricMiniCard(
                        title = "Battito",
                        value = state.heartRateFormatted,
                        unit = "bpm",
                        icon = Icons.Rounded.Favorite,
                        iconColor = AppTheme.colors.error,
                        animationType = IconAnimationType.PULSE,
                        modifier = Modifier.weight(1f),
                        onClick = { onExpandCard("hr") }
                    )
                    MetricMiniCard(
                        title = "Sonno",
                        value = state.sleepTimeFormatted,
                        unit = "ore",
                        icon = Icons.Rounded.Bedtime,
                        iconColor = AppTheme.colors.accentPurple,
                        animationType = IconAnimationType.ROCK,
                        modifier = Modifier.weight(1f),
                        onClick = { onExpandCard("sleep") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedDetailContent(
    expandedCard: String,
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
    ) {
        // Title + close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (expandedCard) {
                    "hr" -> "Battito Cardiaco"
                    "sleep" -> "Dettaglio Sonno"
                    else -> ""
                },
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Chiudi", tint = AppTheme.colors.textPrimary)
            }
        }

        // Detail content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BackgroundGradient)
        ) {
            when (expandedCard) {
                "hr" -> HeartRateDetailContent(
                    biometricReport = state.biometricReport,
                    heartRateFormatted = state.heartRateFormatted,
                    oxygenFormatted = state.oxygenFormatted,
                    isLoading = state.isBiometricLoading
                )
                "sleep" -> {
                    val initialDate = state.selectedSleepSession?.let { session ->
                        session.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    SleepDetailInlineContent(
                        sleepNights = state.sleepNights,
                        initialDate = initialDate,
                        isLoadingMore = state.isLoadingMoreSleep,
                        onPageChanged = { currentPage, totalPages ->
                            viewModel.onPageChanged(currentPage, totalPages)
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  UI Components
// ═══════════════════════════════════════════

@Composable
fun DashboardHeader(onSettingsClick: () -> Unit) {
    val dateStr = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN))
    }
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11 -> "Buongiorno"
        in 12..17 -> "Buon pomeriggio"
        else -> "Buonasera"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateStr.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textTertiary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$greeting Angelo!",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            onClick = onSettingsClick,
            shape = CircleShape,
            color = AppTheme.colors.surfacePrimary,
            border = BorderStroke(1.dp, AppTheme.colors.surfaceSecondary.copy(alpha = 0.5f)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.accentBlue.copy(alpha = 0.1f))
                )
                Text(
                    text = "AQ",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.accentBlue,
                    fontWeight = FontWeight.Bold
                )
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

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = AppTheme.colors.accentBlue,
                trackColor = AppTheme.colors.surfacePrimary
            )

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
                        .biomimeticAnimation(animationType, value)
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

// ═══════════════════════════════════════════
//  Biomimetic Animations
// ═══════════════════════════════════════════

enum class IconAnimationType { NONE, PULSE, ROCK, FLOAT, TWINKLE }

@Composable
fun Modifier.biomimeticAnimation(type: IconAnimationType, value: String = "0"): Modifier {
    val transition = rememberInfiniteTransition(label = "bio_${type.name}")
    return when (type) {
        IconAnimationType.PULSE -> {
            val bpm = value.toIntOrNull()?.coerceAtLeast(40) ?: 60
            val dur = (60000 / bpm).coerceAtLeast(300)
            val s by transition.animateFloat(1f, 1.2f, infiniteRepeatable(tween(dur / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
            this.scale(s)
        }
        IconAnimationType.ROCK -> {
            val r by transition.animateFloat(-20f, 10f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "rock")
            this.graphicsLayer { rotationZ = r }
        }
        IconAnimationType.FLOAT -> {
            val s by transition.animateFloat(1f, 1.2f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
            this.graphicsLayer { scaleX = s; scaleY = s }
        }
        IconAnimationType.TWINKLE -> {
            val s by transition.animateFloat(1f, 1.15f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tw_s")
            val r by transition.animateFloat(-10f, 10f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse), label = "tw_r")
            this.scale(s).graphicsLayer { rotationZ = r }
        }
        IconAnimationType.NONE -> this
    }
}