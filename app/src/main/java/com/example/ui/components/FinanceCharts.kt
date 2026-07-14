package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Data Models for Charts ---
data class ChartData(
    val label: String,
    val value: Float,
    val color: Color
)

// --- 1. Donut Chart Component ---
@Composable
fun DonutChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    centerLabel: String = "Total",
    centerValue: String = "Rp 0"
) {
    val animateSweep = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animateSweep.animateTo(1f, animationSpec = tween(800))
    }

    val total = data.sumOf { it.value.toDouble() }.toFloat()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (total == 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    style = Stroke(width = 40f)
                )
            }
            Text(
                text = "Tidak Ada Data",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.forEach { item ->
                    val sweepAngle = (item.value / total) * 360f
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animateSweep.value,
                        useCenter = false,
                        style = Stroke(width = 44f, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            // Central Summary Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = centerLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = centerValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// --- 2. Line Area Chart (Bezier Curve) ---
@Composable
fun LineAreaChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gradientColors: List<Color> = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent)
) {
    val animatePercent = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatePercent.animateTo(1f, animationSpec = tween(1000))
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak Ada Data Tren", color = Color.Gray)
        }
        return
    }

    val maxVal = remember(data) { (data.maxOrNull() ?: 1f).coerceAtLeast(1f) }
    val minVal = remember(data) { data.minOrNull() ?: 0f }
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1).coerceAtLeast(1)

            val points = data.mapIndexed { index, value ->
                val x = index * spacing
                val ratio = (value - minVal) / range
                // Flip vertical axis + scaling
                val y = height - (ratio * (height - 60f)) - 30f
                Offset(x, y)
            }

            // Create Path for spline/bezier
            val path = Path()
            val fillPath = Path()

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y + (height - points[0].y) * (1f - animatePercent.value))
                fillPath.moveTo(points[0].x, height)
                fillPath.lineTo(points[0].x, points[0].y + (height - points[0].y) * (1f - animatePercent.value))

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    
                    // Control points for smooth bezier curve
                    val controlX1 = p0.x + (p1.x - p0.x) / 2
                    val controlY1 = p0.y + (p1.y - p0.y) * (1f - animatePercent.value)
                    val controlX2 = p0.x + (p1.x - p0.x) / 2
                    val controlY2 = p1.y + (p0.y - p1.y) * (1f - animatePercent.value)

                    val currY1 = p0.y + (height - p0.y) * (1f - animatePercent.value)
                    val currY2 = p1.y + (height - p1.y) * (1f - animatePercent.value)
                    val cy1 = currY1 + (currY2 - currY1) / 3
                    val cy2 = currY2 + (currY1 - currY2) / 3

                    path.cubicTo(
                        controlX1, currY1 + (currY2 - currY1) * 0.25f,
                        controlX2, currY1 + (currY2 - currY1) * 0.75f,
                        p1.x, currY2
                    )
                    fillPath.cubicTo(
                        controlX1, currY1 + (currY2 - currY1) * 0.25f,
                        controlX2, currY1 + (currY2 - currY1) * 0.75f,
                        p1.x, currY2
                    )
                }
                fillPath.lineTo(points.last().x, height)
                fillPath.close()

                // Draw gradient background under path
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Bezier Line
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Draw points on curve
                points.forEach { point ->
                    val animatedY = point.y + (height - point.y) * (1f - animatePercent.value)
                    drawCircle(
                        color = lineColor,
                        radius = 8f,
                        center = Offset(point.x, animatedY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(point.x, animatedY)
                    )
                }
            }
        }

        // Horizontal axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// --- 3. Bar Chart Component ---
@Composable
fun BarChart(
    data: List<ChartData>, // Grouped or single bars
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.secondary,
    compareData: List<ChartData>? = null, // Secondary side-by-side bar (e.g. Income vs Expense)
    compareBarColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatePercent = remember { Animatable(0f) }
    LaunchedEffect(data, compareData) {
        animatePercent.animateTo(1f, animationSpec = tween(800))
    }

    if (data.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak Ada Data Grafik", color = Color.Gray)
        }
        return
    }

    val maxVal1 = data.maxOfOrNull { it.value } ?: 1f
    val maxVal2 = compareData?.maxOfOrNull { it.value } ?: 0f
    val maxVal = maxOf(maxVal1, maxVal2, 1f)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height
            val itemCount = data.size
            val barSpacing = 24f
            val sectionWidth = width / itemCount

            for (i in 0 until itemCount) {
                val secStartX = i * sectionWidth
                val ratio1 = data[i].value / maxVal
                val h1 = ratio1 * (height - 40f) * animatePercent.value

                if (compareData != null && i < compareData.size) {
                    // Side-by-Side Dual Bars
                    val ratio2 = compareData[i].value / maxVal
                    val h2 = ratio2 * (height - 40f) * animatePercent.value
                    
                    val barWidth = (sectionWidth - barSpacing) / 2f
                    
                    // First Bar (Income)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(secStartX + barSpacing/2, height - h1),
                        size = Size(barWidth, h1),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Second Bar (Expense)
                    drawRoundRect(
                        color = compareBarColor,
                        topLeft = Offset(secStartX + barSpacing/2 + barWidth + 4f, height - h2),
                        size = Size(barWidth, h2),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                } else {
                    // Single Bar Chart
                    val barWidth = sectionWidth - barSpacing
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(secStartX + barSpacing/2, height - h1),
                        size = Size(barWidth, h1),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }

        // Horizontal axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp),
                    maxLines = 1
                )
            }
        }
    }
}
