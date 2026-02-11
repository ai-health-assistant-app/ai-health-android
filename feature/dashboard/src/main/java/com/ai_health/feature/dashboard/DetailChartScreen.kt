package com.ai_health.feature.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai_health.ui.components.*
import com.ai_health.ui.theme.AppDimensions
import com.ai_health.ui.theme.AppTheme
import com.ai_health.ui.theme.ChartOrange
import com.ai_health.ui.theme.ChartPink
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailChartScreen(
    metricType: String,
    data: List<ChartDataPoint>,
    onBack: () -> Unit
) {
    // State per il selettore temporale
    var selectedTimeRange by remember { mutableStateOf("1G") }

    // State per l'interazione col grafico (punto selezionato col dito)
    var selectedPoint by remember { mutableStateOf<ChartDataPoint?>(null) }

    // Calcoli statistici
    val values = remember(data) { data.map { it.value } }
    val minVal = values.minOrNull()?.roundToInt() ?: 0
    val maxVal = values.maxOrNull()?.roundToInt() ?: 0
    val avgVal = if (values.isNotEmpty()) values.average().roundToInt() else 0

    // Configurazione Colori
    val color = when (metricType) {
        "steps" -> AppTheme.colors.accentBlue
        "hr" -> ChartPink
        "cal" -> ChartOrange
        "ox" -> AppTheme.colors.accentGreen
        else -> AppTheme.colors.accentPurple
    }

    val title = when (metricType) {
        "hr" -> "Battito Cardiaco"
        "ox" -> "Ossigenazione"
        "steps" -> "Passi"
        else -> "Dettaglio Metrica"
    }

    // Formatter per l'orario del punto selezionato
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = AppTheme.colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.backgroundPrimary)
            )
        },
        containerColor = AppTheme.colors.backgroundPrimary
    ) { padding ->
        AppBackground(contentPadding = false, modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimensions.space4)
            ) {
                // 1. TIME RANGE SELECTOR
                TimeRangeSelector(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { selectedTimeRange = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. CHART CARD INTERATTIVA
                AppCard(
                    variant = CardVariant.NORMAL,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header con Valore Puntuale (se selezionato) o Media
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (selectedPoint != null) "Valore Puntuale" else "Andamento",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = if (selectedPoint != null)
                                        "${selectedPoint!!.value.roundToInt()} ${if(metricType=="hr") "bpm" else ""}"
                                    else "Avg: $avgVal",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary
                                )
                            }

                            // Mostra l'orario se stiamo toccando il grafico
                            if (selectedPoint != null) {
                                Surface(
                                    color = AppTheme.colors.surfaceSecondary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = timeFormatter.format(Instant.ofEpochMilli(selectedPoint!!.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // IL NUOVO GRAFICO INTERATTIVO
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            InteractiveHealthChart(
                                data = data,
                                graphColor = color,
                                onPointSelected = { point -> selectedPoint = point }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.space4))

                // 3. STATISTICHE
                if (metricType == "hr") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Min",
                            value = "$minVal",
                            unit = "bpm",
                            icon = Icons.Rounded.ArrowDownward,
                            iconColor = AppTheme.colors.accentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Max",
                            value = "$maxVal",
                            unit = "bpm",
                            icon = Icons.Rounded.ArrowUpward,
                            iconColor = AppTheme.colors.error,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Riposo",
                            value = "${minVal + 4}",
                            unit = "bpm",
                            icon = Icons.Rounded.Favorite,
                            iconColor = AppTheme.colors.accentPurple,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// --- NUOVI COMPONENTI ---

@Composable
fun TimeRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit
) {
    val ranges = listOf("1G", "1S", "1M")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfacePrimary, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ranges.forEach { range ->
            val isSelected = range == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AppTheme.colors.accentBlue.copy(alpha = 0.15f) else Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures { onRangeSelected(range) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AppTheme.colors.accentBlue else AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun InteractiveHealthChart(
    data: List<ChartDataPoint>,
    graphColor: Color,
    onPointSelected: (ChartDataPoint?) -> Unit
) {
    if (data.isEmpty()) return

    // Pre-calcolo range Y
    val values = data.map { it.value }
    val minY = (values.minOrNull() ?: 0.0) * 0.9
    val maxY = (values.maxOrNull() ?: 100.0) * 1.1
    val rangeY = (maxY - minY).coerceAtLeast(1.0)

    // Animazione di entrata del grafico
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    // Variabile per tracciare la posizione del tocco (offset X nel componente Canvas)
    var touchX by remember { mutableStateOf<Float?>(null) }
    
    // Per calcolare l'indice selezionato e notificare il parent, abbiamo bisogno della larghezza del grafico.
    // Usiamo BoxWithConstraints per ottenere la larghezza disponibile.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val chartWidthPx = constraints.maxWidth.toFloat()

        // Effect per calcolare e notificare il punto selezionato quando touchX o data cambiano
        LaunchedEffect(touchX, data, chartWidthPx) {
            val tx = touchX
            if (tx != null && chartWidthPx > 0) {
                 val pointWidth = chartWidthPx / (data.size - 1).coerceAtLeast(1)
                 val index = (tx / pointWidth).roundToInt().coerceIn(0, data.size - 1)
                 onPointSelected(data[index])
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> touchX = offset.x },
                        onDragEnd = {},
                        onDragCancel = {}
                    ) { change, _ ->
                        touchX = change.position.x
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset -> touchX = offset.x },
                        onTap = { offset -> touchX = offset.x }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val pointWidth = width / (data.size - 1).coerceAtLeast(1)

            // 1. Disegna Griglia Sfondo
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = height * (i.toFloat() / gridLines)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Costruisci il Path
            val path = Path()
            val fillPath = Path()

            data.forEachIndexed { index, point ->
                val x = index * pointWidth
                val normalizedY = (point.value - minY) / rangeY
                val y = height - (normalizedY.toFloat() * height * animationProgress.value)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == data.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // 3. Gradiente
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(graphColor.copy(alpha = 0.3f), graphColor.copy(alpha = 0.0f)),
                    startY = 0f,
                    endY = height
                )
            )

            // 4. Linea
            drawPath(
                path = path,
                color = graphColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 5. Cursore
            touchX?.let { tx ->
                val index = (tx / pointWidth).roundToInt().coerceIn(0, data.size - 1)
                val selectedData = data[index]
                val x = index * pointWidth
                val normalizedY = (selectedData.value - minY) / rangeY
                val y = height - (normalizedY.toFloat() * height)

                drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(graphColor, radius = 4.dp.toPx(), center = Offset(x, y))
                
                drawLine(
                    color = graphColor.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        contentPadding = 12.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        }
    }
}