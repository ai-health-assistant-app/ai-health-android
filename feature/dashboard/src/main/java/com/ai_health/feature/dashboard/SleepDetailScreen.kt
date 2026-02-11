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
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.components.AppBackground
import com.ai_health.ui.components.AppCard
import com.ai_health.ui.components.CardVariant
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.SleepDeep
import com.ai_health.ui.theme.SleepLight
import com.ai_health.ui.theme.SleepREM
import com.ai_health.ui.theme.SleepAwake

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
    
    AppBackground(contentPadding = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dettaglio Sonno", color = AppTheme.colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AppTheme.colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
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
                    color = AppTheme.colors.textTertiary
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
                        color = AppTheme.colors.accentBlue
                    )
                }
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
    // Generiamo il feedback al volo qui
    val insightText = remember(analysis) { SleepFeedbackGenerator.getInsight(analysis) }
    val headlineText = remember(analysis) { SleepFeedbackGenerator.getHeadline(analysis.totalScore) }

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

        // --- NUOVO BLOCCO FEEDBACK ---
        AppCard(
            variant = CardVariant.HIGHLIGHT,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.accentBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = insightText, // Usiamo il testo generato, non quello del DB
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Score Breakdown Section (if available)
        analysis.breakdown?.let { breakdown ->
            Text(
                text = "Composizione Punteggio",
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.textPrimary,
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
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                HrMetricsCard(metrics = metrics)
            }
        }

        // Timeline Chart
        Text(
            text = "Ipogramma",
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
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
            color = AppTheme.colors.textPrimary,
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
            color = AppTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$startStr - $endStr",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textTertiary
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
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Icon or visual indicator
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppTheme.colors.textDisabled
            )
            
            // Message
            Text(
                text = "Nessun dato sul sonno disponibile per questa notte",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textTertiary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Scorri per visualizzare le altre notti",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textDisabled,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SleepScoreCircle(score: Int) {
    val scoreColor = when {
        score >= 80 -> AppTheme.colors.success // Green
        score >= 60 -> AppTheme.colors.warning // Yellow
        else -> AppTheme.colors.error // Red
    }

    val trackColor = AppTheme.colors.surfaceSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            
            // Background Track
            drawArc(
                color = trackColor,
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
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Score",
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.colors.textTertiary
            )
        }
    }
}

@Composable
fun SleepTimelineChart(stages: List<SleepStageRec>, totalDuration: Duration) {
    if (totalDuration.isZero || stages.isEmpty()) {
        Text("No Data", color = AppTheme.colors.textTertiary)
        return
    }

    val totalMillis = totalDuration.toMillis().coerceAtLeast(1)

    // Container for the bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(AppTheme.colors.surfaceSecondary, RoundedCornerShape(8.dp))
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
                Text(label, color = AppTheme.colors.textTertiary, style = MaterialTheme.typography.labelSmall)
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

    AppCard(
        variant = CardVariant.NORMAL,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textTertiary)
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
            if (percentage != null) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
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
        SleepStageType.OUT_OF_BED -> AppTheme.colors.textDisabled
        SleepStageType.SLEEPING -> AppTheme.colors.accentBlue
        SleepStageType.UNKNOWN -> AppTheme.colors.textDisabled
    }
}

@Composable
fun ScoreBreakdownCard(breakdown: ScoreBreakdown) {
    AppCard(
        variant = CardVariant.NORMAL,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fattori del Riposo",
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.textPrimary
            )

            // 1. Architecture -> "Profondità del Sonno"
            // L'utente capisce che riguarda quanto bene ha dormito, non le fasi tecniche.
            ScoreProgressRow(
                label = "Profondità",
                score = breakdown.architectureScore,
                maxScore = 40.0,
                color = AppTheme.colors.accentBlue // Blu
            )

            // 2. Dipping -> "Recupero Fisico"
            // Se il dipping è basso, il recupero è basso. Niente panico medico.
            ScoreProgressRow(
                label = "Recupero Fisico",
                score = breakdown.dippingScore,
                maxScore = 30.0,
                color = AppTheme.colors.accentGreen // Verde
            )

            // 3. RHR -> "Rilassamento Cardiaco"
            ScoreProgressRow(
                label = "Rilassamento",
                score = breakdown.rhrScore,
                maxScore = 20.0,
                color = AppTheme.colors.accentOrange // Arancio
            )

            // 4. Timing Nadir -> "Allineamento Circadiano"
            // Suona sofisticato ma non patologico. Indica se sei andato a letto all'ora giusta.
            ScoreProgressRow(
                label = "Ritmo Circadiano",
                score = breakdown.timingScore,
                maxScore = 10.0,
                color = AppTheme.colors.accentPurple // Viola
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
                color = AppTheme.colors.textTertiary
            )
            Text(
                text = "%.0f / %.0f".format(score, maxScore),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { (score / maxScore).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = AppTheme.colors.surfaceSecondary,
            strokeCap = StrokeCap.Round
        )
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
    AppCard(
        variant = CardVariant.NORMAL,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Salute Cardiaca Notturna",
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.textPrimary
            )

            // ROW 1: I dati "Grezi" sicuri (BPM)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Usiamo la media notturna (RHR approssimativo)
                metrics.nocturnalHrAvg?.let { hr ->
                    HrMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Media Notturna",
                        value = "$hr",
                        unit = "bpm",
                        icon = null // Qui potresti mettere un'icona cuore
                    )
                }

                // Usiamo la minima assoluta (Lowest)
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

            // ROW 2: L'Interpretazione (Black Box)
            // Qui nascondiamo la logica complessa del Dipping/Nadir
            // Mostriamo solo se il "Motore" si è riposato bene.

            val dippingState = metrics.dippingPercent ?: 0.0

            // Logica di traduzione "Whoop Style"
            val (statusText, statusColor) = when {
                dippingState < 0 -> "Sotto Sforzo" to AppTheme.colors.error // Era Reverse Dipper
                dippingState < 10 -> "Recupero Parziale" to AppTheme.colors.warning // Era Non-Dipper
                else -> "Recupero Ottimale" to AppTheme.colors.success // Normal/Extreme
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surfaceSecondary, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stato del Sistema Nervoso",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textTertiary
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Un piccolo indicatore visivo minimalista
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, RoundedCornerShape(50))
                )
            }

            // Nota: Abbiamo rimosso completamente "Data Quality Score".
            // Se i dati fanno schifo, l'utente vedrà "Sotto Sforzo" o "Recupero Parziale",
            // che è comunque un consiglio valido (forse la band ha misurato male perché si muoveva troppo).
        }
    }
}


@Composable
private fun HrMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    icon: Any? // Placeholder per future icone
) {
    Column(
        modifier = modifier
            .background(AppTheme.colors.surfaceSecondary, RoundedCornerShape(8.dp)) // Grigio leggermente più chiaro
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$unit $label", // Es: "bpm Media Notturna"
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}