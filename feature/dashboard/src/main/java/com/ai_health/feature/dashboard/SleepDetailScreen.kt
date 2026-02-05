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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.ai_health.core.domain.model.SleepSessionRec
import com.ai_health.core.domain.model.SleepStageRec
import com.ai_health.core.domain.model.SleepStageType
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SleepDetailScreen(
    sessions: List<SleepSessionRec>,
    analyses: Map<String, SleepQualityResult>,
    initialSessionId: String?,
    onBack: () -> Unit
) {
    // Find the initial page index based on the selected session
    val initialPage = if (initialSessionId != null) {
        sessions.indexOfFirst { it.id == initialSessionId }.coerceAtLeast(0)
    } else {
        0
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { sessions.size }
    )
    
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
        if (sessions.isEmpty()) {
            // Empty state
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { page ->
                val session = sessions[page]
                val analysis = analyses[session.id]
                
                if (analysis != null) {
                    SleepDetailContent(
                        session = session,
                        analysis = analysis
                    )
                } else {
                    // Loading or error state for this session
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
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
