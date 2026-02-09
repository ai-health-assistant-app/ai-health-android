package com.ai_health.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_health.core.domain.model.SleepQualityResult
import com.ai_health.core.domain.model.ScoreBreakdown
import com.ai_health.core.domain.model.SleepMetrics
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepStageRec
import com.ai_health.core.domain.model.SleepStageType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SleepDetailScreen(
    sleepNights: List<SleepNightData>,
    initialDate: LocalDate?,
    isLoadingMore: Boolean,
    onPageChanged: (currentPage: Int, totalPages: Int) -> Unit,
    onBack: () -> Unit
) {
    // Find the initial page index based on the selected date
    val initialPage = if (initialDate != null) {
        sleepNights.indexOfFirst { it.date == initialDate }.coerceAtLeast(0)
    } else {
        0
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { sleepNights.size }
    )
    
    // Monitor page changes for lazy loading
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage, sleepNights.size)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Sonno", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { padding ->
        if (sleepNights.isEmpty()) {
            // Empty state - no nights loaded at all
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun dato sul sonno disponibile",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                reverseLayout = true, // Older nights on left, newer on right
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { page ->
                val nightData = sleepNights[page]
                
                if (nightData.session != null && nightData.analysis != null) {
                    // Night with data - show the full detail
                    SleepDetailContent(
                        session = nightData.session,
                        analysis = nightData.analysis
                    )
                } else {
                    // Night without data - show empty state with date
                    EmptyNightScreen(date = nightData.date)
                }
            }
            
            // Show loading indicator at the bottom when loading more
            if (isLoadingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
}


@Composable
private fun SleepDetailContent(
    session: SleepSessionRec,
    analysis: SleepQualityResult
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        SleepDateHeader(session.startTime, session.endTime)

        // Sleep Score Indicator
        SleepScoreCircle(score = analysis.totalScore)

        // Feedback
        Text(
            text = analysis.feedback,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .padding(16.dp)
        )

        // Score Breakdown Section (if available)
        analysis.breakdown?.let { breakdown ->
            Text(
                text = "Composizione Punteggio",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            ScoreBreakdownCard(breakdown = breakdown)
        }

        // HR Metrics Section (if available)
        analysis.metrics?.let { metrics ->
            if (metrics.nocturnalHrAvg != null) {
                Text(
                    text = "Analisi Cardiaca",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                )
                HrMetricsCard(metrics = metrics)
            }
        }

        // Timeline Chart
        Text(
            text = "Ipogramma",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )
        SleepTimelineChart(
            stages = session.stages,
            totalDuration = Duration.between(session.startTime, session.endTime)
        )

        // Stats Grid
        Text(
            text = "Statistiche Fasi",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )
        SleepStageStatsGrid(analysis = analysis)
    }
}

@Composable
fun SleepDateHeader(start: Instant, end: Instant) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ITALIAN).withZone(ZoneId.systemDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN).withZone(ZoneId.systemDefault())

    val dateStr = dateFormatter.format(start)
    val startStr = timeFormatter.format(start)
    val endStr = timeFormatter.format(end)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Notte del $dateStr",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$startStr - $endStr",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun EmptyNightScreen(date: LocalDate) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN)
    val dateStr = dateFormatter.format(date)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date display
            Text(
                text = "Notte del $dateStr",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Icon or visual indicator
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF64748B)
            )
            
            // Message
            Text(
                text = "Nessun dato sul sonno disponibile per questa notte",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Scorri per visualizzare le altre notti",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SleepScoreCircle(score: Int) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF4ADE80) // Green
        score >= 60 -> Color(0xFFFACC15) // Yellow
        else -> Color(0xFFEF4444) // Red
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            
            // Background Track
            drawArc(
                color = Color(0xFF334155),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Progress Arc
            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = (360f * (score / 100f)),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun SleepTimelineChart(stages: List<SleepStageRec>, totalDuration: Duration) {
    if (totalDuration.isZero || stages.isEmpty()) {
        Text("No Data", color = Color.Gray)
        return
    }

    val totalMillis = totalDuration.toMillis().coerceAtLeast(1)

    // Container for the bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(2.dp) // Optional padding inside container
    ) {
        stages.forEach { stage ->
            val stageDuration = Duration.between(stage.startTime, stage.endTime).toMillis()
            val weight = (stageDuration.toFloat() / totalMillis.toFloat()).coerceAtLeast(0.001f)
            
            val color = getStageColor(SleepStageType.fromInt(stage.stage))

            Box(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
    
    // Legend (Optional but good for UX)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf(
            SleepStageType.DEEP to "Deep",
            SleepStageType.REM to "REM",
            SleepStageType.LIGHT to "Light",
            SleepStageType.AWAKE to "Awake"
        ).forEach { (type, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).background(getStageColor(type), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SleepStageStatsGrid(analysis: SleepQualityResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "Profondo",
                duration = analysis.deepSleepDuration,
                percentage = analysis.deepSleepPercentage,
                color = getStageColor(SleepStageType.DEEP)
            )
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "REM",
                duration = analysis.remSleepDuration,
                percentage = analysis.remSleepPercentage,
                color = getStageColor(SleepStageType.REM)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "Leggero",
                duration = analysis.lightSleepDuration,
                // Percentage not in result model explicitly for Light/Awake in prompt, calculate or hide?
                // Prompt: "se disponibile nel analysisResult". 
                // Model has deepSleepPercentage, remSleepPercentage. 
                // We can imply light/awake via duration calc if needed, or just show duration.
                percentage = null, 
                color = getStageColor(SleepStageType.LIGHT)
            )
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "Sveglio",
                duration = analysis.awakeDuration,
                percentage = null,
                color = getStageColor(SleepStageType.AWAKE)
            )
        }
    }
}

@Composable
fun StageStatCard(
    modifier: Modifier = Modifier,
    title: String,
    duration: Duration,
    percentage: Int?,
    color: Color
) {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val timeStr = "${hours}h ${minutes}m"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (percentage != null) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

fun getStageColor(stage: SleepStageType): Color {
    return when (stage) {
        SleepStageType.DEEP -> Color(0xFF1E3A8A) // Dark Blue
        SleepStageType.REM -> Color(0xFF7C3AED) // Purple
        SleepStageType.LIGHT -> Color(0xFF38BDF8) // Light Blue
        SleepStageType.AWAKE -> Color(0xFFF97316) // Orange
        SleepStageType.OUT_OF_BED -> Color(0xFF94A3B8) // Gray
        SleepStageType.SLEEPING -> Color(0xFF3B82F6) // Generic Blue
        SleepStageType.UNKNOWN -> Color.Gray
    }
}

@Composable
fun ScoreBreakdownCard(breakdown: ScoreBreakdown) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScoreProgressRow(
                label = "Architettura",
                score = breakdown.architectureScore,
                maxScore = 40.0,
                color = Color(0xFF3B82F6)
            )
            ScoreProgressRow(
                label = "HR Dipping",
                score = breakdown.dippingScore,
                maxScore = 30.0,
                color = Color(0xFF10B981)
            )
            ScoreProgressRow(
                label = "RHR",
                score = breakdown.rhrScore,
                maxScore = 20.0,
                color = Color(0xFFF97316)
            )
            ScoreProgressRow(
                label = "Timing Nadir",
                score = breakdown.timingScore,
                maxScore = 10.0,
                color = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun ScoreProgressRow(
    label: String,
    score: Double,
    maxScore: Double,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = "%.0f / %.0f".format(score, maxScore),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (score / maxScore).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = Color(0xFF374151),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun HrMetricsCard(metrics: com.ai_health.core.domain.model.SleepMetrics) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dipping Row
            metrics.dippingPercent?.let { dip ->
                val dipColor = when {
                    dip < 0 -> Color(0xFFEF4444) // Rosso - Reverse
                    dip < 10 -> Color(0xFFFBBF24) // Giallo - Non-dipper
                    dip <= 20 -> Color(0xFF10B981) // Verde - Normal
                    else -> Color(0xFF3B82F6) // Blu - Extreme
                }
                val dipLabel = when {
                    dip < 0 -> "Reverse Dipper ⚠️"
                    dip < 10 -> "Non-Dipper"
                    dip <= 20 -> "Normal Dipper ✓"
                    else -> "Extreme Dipper"
                }
                HrMetricRow(
                    label = "HR Dipping",
                    value = "%.1f%%".format(dip),
                    subtitle = dipLabel,
                    color = dipColor
                )
            }

            // HR Averages
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                metrics.nocturnalHrAvg?.let { hr ->
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "HR Notturna",
                        value = "$hr",
                        unit = "bpm"
                    )
                }
                metrics.daytimeHrAvg?.let { hr ->
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "HR Diurna",
                        value = "$hr",
                        unit = "bpm"
                    )
                }
            }

            // Nadir
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                metrics.lowestNocturnalHr?.let { hr ->
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "HR Minima",
                        value = "$hr",
                        unit = "bpm"
                    )
                }
                metrics.hrNadirOffsetPercent?.let { pos ->
                    val posLabel = when (pos) {
                        in 40..65 -> "Ottimale"
                        in 30..75 -> "Buono"
                        else -> "Da migliorare"
                    }
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Posizione Nadir",
                        value = "$pos%",
                        unit = posLabel
                    )
                }
            }

            // Data Quality
            Text(
                text = "Qualità dati: ${"%.0f".format(metrics.dataQualityScore * 100)}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun HrMetricRow(
    label: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = color)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HrMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String
) {
    Column(
        modifier = modifier
            .background(Color(0xFF374151), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(text = unit, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
    }
}
