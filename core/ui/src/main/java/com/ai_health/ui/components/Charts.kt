package com.ai_health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChartDataPoint(
    val timestamp: Long,
    val value: Double
)

@Composable
fun SimpleLineChart(
    data: List<ChartDataPoint>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFF1E293B).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Dati insufficienti", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val values = data.map { it.value }
    val max = values.maxOrNull() ?: 1.0
    val min = values.minOrNull() ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF1E293B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacing = width / (data.size - 1)

                val path = Path()
                data.forEachIndexed { index, entity ->
                    val x = index * spacing
                    val y = height - ((entity.value - min) / range * height).toFloat()
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx())
                )
                
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
            }
        }
        
        // Time Axis
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeFormatter.format(Instant.ofEpochMilli(data.first().timestamp)),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = timeFormatter.format(Instant.ofEpochMilli(data.last().timestamp)),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<ChartDataPoint>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val hourlyData = data.groupBy { 
        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).hour 
    }.mapValues { it.value.sumOf { v -> v.value } }

    val sortedHours = hourlyData.keys.sorted()
    val max = hourlyData.values.maxOrNull() ?: 1.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF1E293B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            sortedHours.forEach { hour ->
                val value = hourlyData[hour] ?: 0.0
                val barHeight = (value / max * 110).dp
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
            }
        }

        // Hour labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sortedHours.forEach { hour ->
                Text(
                    text = "${hour}h",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
