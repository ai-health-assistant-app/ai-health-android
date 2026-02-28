package com.ai_health.feature.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.ai_health.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepDetailScreen(
    sleepNights: List<SleepNightData>,
    initialDate: LocalDate?,
    isLoadingMore: Boolean,
    onPageChanged: (currentPage: Int, totalPages: Int) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Sonno") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            SleepDetailInlineContent(
                sleepNights = sleepNights,
                initialDate = initialDate,
                isLoadingMore = isLoadingMore,
                onPageChanged = onPageChanged
            )
        }
    }
}

/**
 * Reusable content composable for sleep detail.
 * Can be used standalone inside the Dashboard's expanded card overlay.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SleepDetailInlineContent(
    sleepNights: List<SleepNightData>,
    initialDate: LocalDate?,
    isLoadingMore: Boolean,
    onPageChanged: (currentPage: Int, totalPages: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = if (initialDate != null) {
        sleepNights.indexOfFirst { it.date == initialDate }.coerceAtLeast(0)
    } else {
        0
    }

    if (sleepNights.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nessun dato sul sonno disponibile",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary
            )
        }
    } else {
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { sleepNights.size }
        )

        LaunchedEffect(pagerState.currentPage) {
            onPageChanged(pagerState.currentPage, sleepNights.size)
        }

        Box(modifier = modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val nightData = sleepNights[page]

                if (nightData.session != null && nightData.analysis != null) {
                    SleepDetailContent(
                        session = nightData.session,
                        analysis = nightData.analysis
                    )
                } else {
                    EmptyNightScreen(date = nightData.date)
                }
            }

            if (isLoadingMore) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = NeonBlue
                    )
                }
            }
        }
    }
}


@Composable
fun SleepDetailContent(
    session: SleepSessionRec,
    analysis: SleepQualityResult
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        SleepDateHeader(session.startTime, session.endTime)

        // Sleep Score Gauge (with breakdown inside, like HR screen)
        SleepScoreGaugeCard(score = analysis.totalScore, breakdown = analysis.breakdown)

        // HR Metrics Section
        analysis.metrics?.let { metrics ->
            if (metrics.nocturnalHrAvg != null) {
                SleepSectionHeader(title = "Analisi Cardiaca", icon = Icons.Default.Favorite)
                HrMetricsCard(metrics = metrics)
            }
        }

        // Timeline
        SleepSectionHeader(title = "Ipogramma", icon = Icons.Default.Timeline)
        SleepTimelineChart(
            stages = session.stages,
            totalDuration = Duration.between(session.startTime, session.endTime)
        )

        // Stage Stats
        SleepSectionHeader(title = "Statistiche Fasi", icon = Icons.Default.PieChart)
        SleepStageStatsGrid(analysis = analysis)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SleepDateHeader(start: Instant, end: Instant) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ITALIAN).withZone(ZoneId.systemDefault())
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN).withZone(ZoneId.systemDefault())

    val dateStr = dateFormatter.format(start)
    val startStr = timeFormatter.format(start)
    val endStr = timeFormatter.format(end)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Notte del $dateStr",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$startStr - $endStr",
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
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
            Text(
                text = "Notte del $dateStr",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextDisabled
            )
            
            Text(
                text = "Nessun dato sul sonno disponibile per questa notte",
                style = MaterialTheme.typography.bodyLarge,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Scorri per visualizzare le altre notti",
                style = MaterialTheme.typography.bodyMedium,
                color = TextDisabled,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SleepScoreGaugeCard(score: Int, breakdown: ScoreBreakdown?) {
    val scoreColor = when {
        score >= 80 -> SuccessGreen
        score >= 60 -> WarningAmber
        else -> ErrorRed
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedProgress.animateTo(
            targetValue = score / 100f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    val levelText = when {
        score >= 85 -> "Eccellente"
        score >= 70 -> "Buono"
        score >= 50 -> "Sufficiente"
        else -> "Da migliorare"
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
                text = "Qualità del Sonno",
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
                        color = scoreColor,
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
                        text = score.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        ),
                        color = scoreColor
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
                color = scoreColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = levelText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Breakdown bars (inside the card, like HR screen)
            breakdown?.let { bd ->
                Spacer(modifier = Modifier.height(16.dp))
                SleepBreakdownRow(breakdown = bd)
            }
        }
    }
}

@Composable
fun SleepTimelineChart(stages: List<SleepStageRec>, totalDuration: Duration) {
    if (totalDuration.isZero || stages.isEmpty()) {
        Text("No Data", color = TextTertiary)
        return
    }

    val totalMillis = totalDuration.toMillis().coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MidnightLight, RoundedCornerShape(8.dp))
            .padding(2.dp)
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
                Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall)
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
                color = SleepDeep
            )
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "REM",
                duration = analysis.remSleepDuration,
                percentage = analysis.remSleepPercentage,
                color = SleepREM
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "Leggero",
                duration = analysis.lightSleepDuration,
                percentage = null, 
                color = SleepLight
            )
            StageStatCard(
                modifier = Modifier.weight(1f),
                title = "Sveglio",
                duration = analysis.awakeDuration,
                percentage = null,
                color = SleepAwake
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
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MidnightBase.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (percentage != null) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun getStageColor(stage: SleepStageType): Color {
    return when (stage) {
        SleepStageType.DEEP -> SleepDeep
        SleepStageType.REM -> SleepREM
        SleepStageType.LIGHT -> SleepLight
        SleepStageType.AWAKE -> SleepAwake
        SleepStageType.OUT_OF_BED -> TextDisabled
        SleepStageType.SLEEPING -> NeonBlue
        SleepStageType.UNKNOWN -> TextDisabled
    }
}

@Composable
private fun SleepBreakdownRow(breakdown: ScoreBreakdown) {
    val components = listOf(
        Triple("Profondità", breakdown.architectureScore, 40.0),
        Triple("Recupero Fisico", breakdown.dippingScore, 30.0),
        Triple("Rilassamento", breakdown.rhrScore, 20.0),
        Triple("Ritmo Circadiano", breakdown.timingScore, 10.0)
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
                    modifier = Modifier.width(120.dp)
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



object SleepFeedbackGenerator {
    fun getInsight(analysis: SleepQualityResult): String {
        val metrics = analysis.metrics
        val score = analysis.totalScore

        // 1. Controllo Prioritario: Stress Fisiologico (Dipping basso)
        // Se il dipping è sotto il 10%, è il segnale più forte di "giornata no".
        if (metrics?.dippingPercent != null && metrics.dippingPercent!! < 10.0) {
            return "Il tuo recupero fisiologico è stato parziale stanotte. Questo accade spesso dopo pasti pesanti, alcol o attività fisica intensa serale. Oggi punta su idratazione e attività leggera."
        }

        // 2. Controllo Prioritario: Sonno Profondo insufficiente
        // Assumiamo che meno del 10-15% sia poco (o meno di 45 min)
        if (analysis.deepSleepDuration.toMinutes() < 45) {
            return "La fase di rigenerazione profonda è stata breve. Per favorirla stasera, prova a oscurare completamente la stanza e mantenere una temperatura fresca (intorno ai 19°C)."
        }

        // 3. Controllo Prioritario: Troppi Risvegli (Frammentazione)
        // Usiamo awakeDuration come proxy se non hai un indice di frammentazione
        if (analysis.awakeDuration.toMinutes() > 60) {
            return "Hai avuto diverse interruzioni del sonno. Evita schermi e luci blu nell'ora prima di dormire per aiutare la produzione di melatonina."
        }

        // 4. Fallback basato sul Punteggio Totale (Se non ci sono problemi specifici evidenti)
        return when {
            score >= 85 -> "Prestazione eccellente! I tuoi biometri indicano che sei pronto per affrontare una giornata impegnativa o un allenamento intenso."
            score >= 70 -> "Buon equilibrio generale. Il tuo corpo ha recuperato le energie necessarie. Mantieni questa routine."
            score >= 50 -> "Recupero sufficiente, ma c'è margine di miglioramento. Cerca di andare a letto alla stessa ora stasera per regolarizzare il ritmo."
            else -> "Il corpo chiede tregua. I dati suggeriscono affaticamento accumulato. Prioritizza il riposo stasera."
        }
    }

    fun getHeadline(score: Int): String {
        return when {
            score >= 90 -> "Batterie Cariche ⚡"
            score >= 75 -> "Ben Riposato 🔋"
            score >= 60 -> "Stabile ⚖️"
            else -> "Serve Ricarica 🔌"
        }
    }
}

@Composable
fun HrMetricsCard(metrics: com.ai_health.core.domain.model.SleepMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MidnightBase.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Salute Cardiaca Notturna",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                metrics.nocturnalHrAvg?.let { hr ->
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Media Notturna",
                        value = "$hr",
                        unit = "bpm",
                        icon = null
                    )
                }

                metrics.lowestNocturnalHr?.let { hr ->
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Minima Assoluta",
                        value = "$hr",
                        unit = "bpm",
                        icon = null
                    )
                }
            }

            val dippingState = metrics.dippingPercent ?: 0.0

            val (statusText, statusColor) = when {
                dippingState < 0 -> "Sotto Sforzo" to ErrorRed
                dippingState < 10 -> "Recupero Parziale" to WarningAmber
                else -> "Recupero Ottimale" to SuccessGreen
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightLight, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stato del Sistema Nervoso",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, RoundedCornerShape(50))
                )
            }
        }
    }
}


@Composable
private fun HrMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    icon: Any?
) {
    Column(
        modifier = modifier
            .background(MidnightLight, RoundedCornerShape(8.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$unit $label",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// Section Header (matching HR screen style)
// =============================================================================

@Composable
private fun SleepSectionHeader(title: String, icon: ImageVector) {
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