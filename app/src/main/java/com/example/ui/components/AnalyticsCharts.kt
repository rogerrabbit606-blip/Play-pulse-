package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CohortMetric
import com.example.model.PlatformForecastPoint
import com.example.model.SubscriptionPlan
import java.util.Locale
import kotlin.math.max

@Composable
fun MrrForecastLineChart(
    points: List<PlatformForecastPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIdx by remember(points.size) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrElse(selectedIdx) { points.last() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val maxMrr = remember(points) {
        max(1000.0, points.maxOf { max(it.actualMrrUsd ?: 0.0, it.projectedMrrUsd) } * 1.15)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selected.actualMrrUsd != null) "Recorded MRR (${selected.monthLabel})" else "AI Forecast (${selected.monthLabel})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "$%,.0f / mo",
                        selected.actualMrrUsd ?: selected.projectedMrrUsd
                    ),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (selected.actualMrrUsd != null) primaryColor else secondaryColor
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = String.format(Locale.US, "Retention %.0f%%", selected.retentionRatePct),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 12f
            val padBottom = 12f
            val chartH = h - padTop - padBottom

            // Horizontal guide lines
            for (i in 0..3) {
                val y = padTop + (chartH / 3f) * i
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f
                )
            }

            val stepX = if (points.size > 1) w / (points.size - 1) else w

            fun yFor(value: Double): Float {
                val ratio = (value / maxMrr).toFloat().coerceIn(0f, 1f)
                return padTop + chartH * (1f - ratio)
            }

            val actualIndices = points.indices.filter { points[it].actualMrrUsd != null }
            val forecastPath = Path()
            val areaPath = Path()

            points.forEachIndexed { index, pt ->
                val x = index * stepX
                val y = yFor(pt.projectedMrrUsd)
                if (index == 0) {
                    forecastPath.moveTo(x, y)
                    areaPath.moveTo(x, padTop + chartH)
                    areaPath.lineTo(x, y)
                } else {
                    forecastPath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }
            areaPath.lineTo((points.size - 1) * stepX, padTop + chartH)
            areaPath.close()

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f),
                        Color.Transparent
                    )
                )
            )

            // Draw projected dashed segment
            drawPath(
                path = forecastPath,
                color = secondaryColor,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            // Draw solid actual segment
            if (actualIndices.size >= 2) {
                val actualPath = Path()
                actualIndices.forEachIndexed { pos, idx ->
                    val x = idx * stepX
                    val y = yFor(points[idx].actualMrrUsd ?: 0.0)
                    if (pos == 0) actualPath.moveTo(x, y) else actualPath.lineTo(x, y)
                }
                drawPath(
                    path = actualPath,
                    color = primaryColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            // Nodes
            points.forEachIndexed { index, pt ->
                val x = index * stepX
                val y = yFor(pt.actualMrrUsd ?: pt.projectedMrrUsd)
                val dotColor = if (pt.actualMrrUsd != null) primaryColor else secondaryColor
                if (index == selectedIdx) {
                    drawCircle(
                        color = dotColor.copy(alpha = 0.25f),
                        radius = 16f,
                        center = Offset(x, y)
                    )
                }
                drawCircle(
                    color = surfaceColor,
                    radius = 8f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = dotColor,
                    radius = 5.5f,
                    center = Offset(x, y)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIdx
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendDot(color = primaryColor, label = "Recorded MRR")
            ChartLegendDot(color = secondaryColor, label = "AI Predictive MRR")
        }
    }
}

@Composable
fun CohortRetentionBarChart(
    cohorts: List<CohortMetric>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.tertiary
    val benchmark = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
        ) {
            val w = size.width
            val h = size.height
            val slotW = w / cohorts.size.coerceAtLeast(1)
            val barW = (slotW * 0.24f).coerceAtMost(38f)
            val gap = 8f

            cohorts.forEachIndexed { idx, metric ->
                val centerX = slotW * idx + slotW / 2f
                val hPortfolio = (h * metric.portfolioPct.coerceIn(0.05f, 1f))
                val hBench = (h * metric.marketBenchmarkPct.coerceIn(0.05f, 1f))

                drawRoundRect(
                    color = primary,
                    topLeft = Offset(centerX - barW - gap / 2f, h - hPortfolio),
                    size = Size(barW, hPortfolio),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = benchmark,
                    topLeft = Offset(centerX + gap / 2f, h - hBench),
                    size = Size(barW, hBench),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            cohorts.forEach { metric ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = metric.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(metric.portfolioPct * 100).toInt()}% vs ${(metric.marketBenchmarkPct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartLegendDot(color = primary, label = "Redesigned App Target")
            ChartLegendDot(color = benchmark, label = "Legacy Store Avg")
        }
    }
}

@Composable
fun PlatformRevenueDonutChart(
    plans: List<SubscriptionPlan>,
    modifier: Modifier = Modifier
) {
    val gPlayColor = MaterialTheme.colorScheme.primary
    val galaxyColor = MaterialTheme.colorScheme.secondary
    val webColor = MaterialTheme.colorScheme.tertiary

    val slices = remember(plans) {
        val active = plans.filter { it.isActive }
        val gPlay = active.filter { it.platform == "Google Play" }.sumOf { it.monthlyEquivalentRevenueUsd }
        val galaxy = active.filter { it.platform == "Galaxy Store" }.sumOf { it.monthlyEquivalentRevenueUsd }
        val web = active.filter { it.platform == "Web Billing" }.sumOf { it.monthlyEquivalentRevenueUsd }
        listOf(
            Triple("Google Play", gPlay, gPlayColor),
            Triple("Galaxy Store", galaxy, galaxyColor),
            Triple("Web Billing", web, webColor)
        )
    }

    val total = slices.sumOf { it.second }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier.size(116.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(116.dp)) {
                val strokeWidth = 20f
                val safeTotal = if (total > 0.0) total else 1.0
                var startAngle = -90f
                if (total <= 0.0) {
                    drawArc(
                        color = gPlayColor.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                } else {
                    slices.forEach { (_, value, color) ->
                        if (value > 0.0) {
                            val sweep = ((value / safeTotal) * 360.0).toFloat()
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 4f).coerceAtLeast(4f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweep
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MRR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "$%,.0f", total),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { (label, value, color) ->
                val pct = if (total > 0.0) ((value / total) * 100.0).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(color = color, label = label)
                    Text(
                        text = String.format(Locale.US, "$%,.0f (%d%%)", value, pct),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
