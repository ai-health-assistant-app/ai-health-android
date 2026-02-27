package com.ai_health.feature.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_health.core.domain.model.*
import com.ai_health.ui.theme.*
import kotlin.math.roundToInt

/**
 * Heart Rate Detail Screen showing all Biometric Engine metrics.
 *
 * Layout:
 * 1. Z-Score Alert Banner (conditional, top) 
 * 2. Readiness Score Gauge
 * 3. Readiness Breakdown
 * 4. Training Load section (TRIMP + CTL/ATL/TSB)
 * 5. Autonomic Health section (Dipping + Baevsky SI)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateDetailScreen(
    biometricReport: BiometricReport?,
    heartRateFormatted: String,
    isLoading: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometric Intelligence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGradient)
                .padding(padding)
        ) {
            if (isLoading || biometricReport == null) {
                // Loading state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonBlue,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isLoading) "Analisi biometrica in corso..." else "Dati insufficienti per l'analisi",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Z-Score Alert Banner
                    biometricReport.rhrAnomaly?.let { anomaly ->
                        if (anomaly.alertLevel != AlertLevel.GREEN) {
                            ZScoreAlertBanner(anomaly = anomaly)
                        }
                    }

                    // 2. Readiness Score Gauge
                    biometricReport.readiness?.let { readiness ->
                        ReadinessGaugeCard(readiness = readiness)
                    }

                    // 3. Current HR Summary
                    CurrentHrCard(heartRateFormatted = heartRateFormatted)

                    // 4. Training Load Section
                    SectionHeader(title = "Carico di Allenamento", icon = Icons.Default.FitnessCenter)

                    biometricReport.trimpResult?.let { trimp ->
                        TrimpCard(trimp = trimp)
                    }

                    biometricReport.fitnessFatigue?.let { ff ->
                        FitnessFatigueCards(fitnessFatigue = ff)
                    }

                    // 5. Autonomic Health Section
                    SectionHeader(title = "Salute Autonomica", icon = Icons.Default.MonitorHeart)

                    biometricReport.sleepDipping?.let { dipping ->
                        SleepDippingCard(dipping = dipping)
                    }

                    biometricReport.baevskyStress?.let { stress ->
                        BaevskyStressCard(stress = stress)
                    }

                    biometricReport.rhrAnomaly?.let { anomaly ->
                        RhrTrendCard(anomaly = anomaly)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// =============================================================================
// Z-Score Alert Banner
// =============================================================================

@Composable
private fun ZScoreAlertBanner(anomaly: RhrAnomalyResult) {
    val (bgColor, iconColor, icon, message) = when (anomaly.alertLevel) {
        AlertLevel.YELLOW -> listOf(
            WarningAmber.copy(alpha = 0.15f),
            WarningAmber,
            Icons.Default.Warning,
            "Attenzione: La tua FC a riposo è elevata (+%.1f deviazioni). Possibile stress, alcol o inizio infezione.".format(anomaly.zScore)
        )
        AlertLevel.RED -> listOf(
            ErrorRed.copy(alpha = 0.15f),
            ErrorRed,
            Icons.Default.Error,
            "Allarme: FC a riposo anormalmente alta (+%.1f σ). Alta probabilità di malattia o overtraining. Riposo consigliato.".format(anomaly.zScore)
        )
        else -> return
    }

    @Suppress("UNCHECKED_CAST")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor as Color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = null,
                tint = iconColor as Color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = message as String,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================================================
// Readiness Score Gauge
// =============================================================================

@Composable
private fun ReadinessGaugeCard(readiness: ReadinessResult) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(readiness.score) {
        animatedProgress.animateTo(
            targetValue = readiness.score / 100f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    val gaugeColor = when (readiness.level) {
        ReadinessLevel.GREEN -> SuccessGreen
        ReadinessLevel.YELLOW -> WarningAmber
        ReadinessLevel.RED -> ErrorRed
    }

    val levelText = when (readiness.level) {
        ReadinessLevel.GREEN -> "Alta intensità consentita"
        ReadinessLevel.YELLOW -> "Mantenimento / Recupero attivo"
        ReadinessLevel.RED -> "Riposo necessario"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MidnightBase.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Readiness Score",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Arc Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Background arc
                    drawArc(
                        color = MidnightLight,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = gaugeColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Score text inside gauge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${readiness.score}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        ),
                        color = gaugeColor
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Level label
            Surface(
                color = gaugeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = levelText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = gaugeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (readiness.isOverridden) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Punteggio limitato per anomalia RHR",
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown bars
            ReadinessBreakdownRow(readiness.breakdown)
        }
    }
}

@Composable
private fun ReadinessBreakdownRow(breakdown: ReadinessBreakdown) {
    val components = listOf(
        Triple("Sonno", breakdown.sleepComponent, 40.0),
        Triple("RHR", breakdown.rhrComponent, 30.0),
        Triple("Allenamento", breakdown.trainingComponent, 20.0),
        Triple("Volatilità", breakdown.volatilityComponent, 10.0)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        components.forEach { (label, value, max) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.width(80.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    // Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MidnightLight)
                    )
                    // Progress
                    val fraction = (value / max).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonBlue, PurpleGlow)
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%.0f".format(value),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// =============================================================================
// Current HR Card
// =============================================================================

@Composable
private fun CurrentHrCard(heartRateFormatted: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MidnightBase.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "Frequenza Cardiaca Media",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Text(
                    text = heartRateFormatted,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =============================================================================
// Section Header
// =============================================================================

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonBlueBright,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// =============================================================================
// TRIMP Card
// =============================================================================

@Composable
private fun TrimpCard(trimp: TrimpResult) {
    MetricCard(
        title = "Session TRIMP",
        value = "%.0f".format(trimp.trimp),
        subtitle = "Carico sessione (Banister)",
        detail = "Durata: %.0f min · HRR avg: %.0f%%".format(
            trimp.durationMinutes,
            trimp.avgHrReserveFraction * 100
        ),
        icon = Icons.Default.LocalFireDepartment,
        iconColor = SunsetOrange
    )
}

// =============================================================================
// Fitness-Fatigue Cards (CTL, ATL, TSB)
// =============================================================================

@Composable
private fun FitnessFatigueCards(fitnessFatigue: FitnessFatigueState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // CTL - Fitness
        CompactMetricCard(
            title = "CTL",
            label = "Fitness",
            value = "%.1f".format(fitnessFatigue.ctl),
            color = MintGreen,
            modifier = Modifier.weight(1f)
        )
        // ATL - Fatigue
        CompactMetricCard(
            title = "ATL",
            label = "Fatica",
            value = "%.1f".format(fitnessFatigue.atl),
            color = SunsetOrange,
            modifier = Modifier.weight(1f)
        )
        // TSB - Form
        val tsbColor = when {
            fitnessFatigue.tsb > 5 -> SuccessGreen
            fitnessFatigue.tsb > -10 -> WarningAmber
            else -> ErrorRed
        }
        CompactMetricCard(
            title = "TSB",
            label = "Forma",
            value = "%+.1f".format(fitnessFatigue.tsb),
            color = tsbColor,
            modifier = Modifier.weight(1f)
        )
    }

    // TSB Interpretation
    val tsbAdvice = when {
        fitnessFatigue.tsb > 5 -> "🟢 Fresco e pronto per la prestazione"
        fitnessFatigue.tsb > -10 -> "🟡 Fase di carico produttivo"
        fitnessFatigue.tsb > -30 -> "🟠 Carico elevato — monitora il recupero"
        else -> "🔴 Rischio overtraining — riposo consigliato"
    }
    Text(
        text = tsbAdvice,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun CompactMetricCard(
    title: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MidnightBase.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

// =============================================================================
// Sleep Dipping Card
// =============================================================================

@Composable
private fun SleepDippingCard(dipping: SleepDippingResult) {
    val (profileText, profileColor) = when (dipping.profile) {
        DippingProfile.REVERSE_DIPPER -> "Reverse Dipper ⚠️" to ErrorRed
        DippingProfile.NON_DIPPER -> "Non-Dipper" to WarningAmber
        DippingProfile.NORMAL_DIPPER -> "Normal Dipper ✓" to SuccessGreen
        DippingProfile.EXTREME_DIPPER -> "Extreme Dipper" to InfoBlue
    }

    MetricCard(
        title = "Sleep Dipping Ratio",
        value = "%.1f%%".format(dipping.dippingPercent),
        subtitle = profileText,
        detail = when (dipping.profile) {
            DippingProfile.REVERSE_DIPPER -> "La FC è salita durante il sonno — possibile stress cardiovascolare"
            DippingProfile.NON_DIPPER -> "Calo insufficiente (< 10%) — fattore di rischio cardiovascolare"
            DippingProfile.NORMAL_DIPPER -> "Calo fisiologico sano (10-20%) — ottimo recupero notturno"
            DippingProfile.EXTREME_DIPPER -> "Calo > 20% — monitora regolarmente"
        },
        icon = Icons.Default.Bedtime,
        iconColor = profileColor
    )
}

// =============================================================================
// Baevsky Stress Index Card
// =============================================================================

@Composable
private fun BaevskyStressCard(stress: BaevskyStressResult) {
    val stressLevel = when {
        stress.stressIndex < 50 -> "Basso" to SuccessGreen
        stress.stressIndex < 150 -> "Moderato" to WarningAmber
        stress.stressIndex < 500 -> "Elevato" to SunsetOrange
        else -> "Molto Elevato" to ErrorRed
    }

    MetricCard(
        title = "Indice di Stress (Baevsky)",
        value = "%.0f".format(stress.stressIndex),
        subtitle = "Livello: ${stressLevel.first}",
        detail = "Moda RR: %.3fs · Ampiezza: %.1f%% · Range: %.3fs".format(
            stress.modeRR,
            stress.modeAmplitude,
            stress.variationRange
        ),
        icon = Icons.Default.Psychology,
        iconColor = stressLevel.second
    )
}

// =============================================================================
// RHR Trend Card
// =============================================================================

@Composable
private fun RhrTrendCard(anomaly: RhrAnomalyResult) {
    val statusColor = when (anomaly.alertLevel) {
        AlertLevel.GREEN -> SuccessGreen
        AlertLevel.YELLOW -> WarningAmber
        AlertLevel.RED -> ErrorRed
    }

    MetricCard(
        title = "RHR Trend (Z-Score)",
        value = "%+.2f σ".format(anomaly.zScore),
        subtitle = "RHR oggi: %.0f bpm · Media 30d: %.0f bpm".format(anomaly.todayRhr, anomaly.mean30d),
        detail = "Deviazione std: %.1f bpm".format(anomaly.stdDev30d),
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        iconColor = statusColor
    )
}

// =============================================================================
// Reusable Metric Card
// =============================================================================

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    detail: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MidnightBase.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = iconColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}
