package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlayPulseTheme

/**
 * Immutable UI model describing a single Key Performance Indicator card.
 */
data class KpiMetricItem(
    val id: String,
    val title: String,
    val formattedValue: String,
    val deltaLabel: String,
    val supportingText: String,
    val trendPositive: Boolean,
    val isInverseMetric: Boolean = false,
    val sparklinePoints: List<Float> = emptyList(),
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String = "kpi_card_$id",
    val onClick: (() -> Unit)? = null
)

/**
 * Reusable responsive grid of Material 3 KPI cards that adapts between
 * a 2-column layout on compact phones and a 4-column layout on medium/expanded screens.
 */
@Composable
fun DashboardKpiGrid(
    items: List<KpiMetricItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 680.dp) 4 else 2
        val rows = items.chunked(columns)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        KpiMetricCard(
                            item = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Reusable Material 3 KPI card displaying a primary figure, directional delta badge,
 * supporting context, and an embedded micro-sparkline.
 */
@Composable
fun KpiMetricCard(
    item: KpiMetricItem,
    modifier: Modifier = Modifier
) {
    val isHealthy = if (item.isInverseMetric) !item.trendPositive else item.trendPositive
    val deltaColor = if (isHealthy) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val deltaIcon = if (item.trendPositive) {
        Icons.AutoMirrored.Filled.TrendingUp
    } else {
        Icons.AutoMirrored.Filled.TrendingDown
    }

    val cardShape = RoundedCornerShape(20.dp)
    val baseModifier = modifier
        .minimumInteractiveComponentSize()
        .clip(cardShape)
        .testTag(item.testTag)
        .semantics(mergeDescendants = true) {
            contentDescription = "${item.title}: ${item.formattedValue}, ${item.deltaLabel}, ${item.supportingText}"
        }
        .let { mod ->
            if (item.onClick != null) {
                mod.clickable(onClick = item.onClick)
            } else {
                mod
            }
        }

    ElevatedCard(
        modifier = baseModifier,
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Surface(
            color = Color.Transparent,
            shape = cardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = item.accentColor.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.accentColor,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = deltaColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = deltaIcon,
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = item.deltaLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = deltaColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = item.formattedValue,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.sparklinePoints.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    KpiSparkline(
                        points = item.sparklinePoints,
                        strokeColor = item.accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiSparkline(
    points: List<Float>,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    val minVal = points.minOrNull() ?: 0f
    val maxVal = points.maxOrNull() ?: 1f
    val span = (maxVal - minVal).takeIf { it > 0.0001f } ?: 1f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padY = 4f
        val drawH = (h - padY * 2f).coerceAtLeast(4f)
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { idx, value ->
            val x = idx * stepX
            val normalized = (value - minVal) / span
            val y = padY + drawH * (1f - normalized)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo((points.size - 1) * stepX, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    strokeColor.copy(alpha = 0.28f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )

        val lastX = (points.size - 1) * stepX
        val lastNorm = (points.last() - minVal) / span
        val lastY = padY + drawH * (1f - lastNorm)
        drawCircle(
            color = strokeColor,
            radius = 5.5f,
            center = Offset(lastX, lastY)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardKpiGridPreviewDark() {
    PlayPulseTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardKpiGrid(
                modifier = Modifier.padding(16.dp),
                items = previewKpiItems()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardKpiGridPreviewLight() {
    PlayPulseTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardKpiGrid(
                modifier = Modifier.padding(16.dp),
                items = previewKpiItems()
            )
        }
    }
}

@Composable
private fun previewKpiItems(): List<KpiMetricItem> = listOf(
    KpiMetricItem(
        id = "mrr",
        title = "Monthly Revenue",
        formattedValue = "$8,740",
        deltaLabel = "+12.4%",
        supportingText = "ARR $104.9K",
        trendPositive = true,
        sparklinePoints = listOf(5.2f, 5.8f, 6.4f, 7.1f, 7.9f, 8.7f),
        icon = Icons.Filled.AttachMoney,
        accentColor = MaterialTheme.colorScheme.primary
    ),
    KpiMetricItem(
        id = "growth",
        title = "User Growth",
        formattedValue = "+10.6%",
        deltaLabel = "1.3K subs",
        supportingText = "4.8M tracked DL",
        trendPositive = true,
        sparklinePoints = listOf(6.1f, 7.0f, 7.5f, 8.9f, 9.8f, 10.6f),
        icon = Icons.Filled.GroupAdd,
        accentColor = MaterialTheme.colorScheme.tertiary
    ),
    KpiMetricItem(
        id = "churn",
        title = "Sub Churn Rate",
        formattedValue = "2.8%",
        deltaLabel = "-0.4%",
        supportingText = "Target < 4.0%",
        trendPositive = false,
        isInverseMetric = true,
        sparklinePoints = listOf(4.2f, 3.9f, 3.6f, 3.3f, 3.0f, 2.8f),
        icon = Icons.Filled.Autorenew,
        accentColor = MaterialTheme.colorScheme.secondary
    ),
    KpiMetricItem(
        id = "upside",
        title = "Revival Upside",
        formattedValue = "$34.4K",
        deltaLabel = "3 apps",
        supportingText = "Avg score 91",
        trendPositive = true,
        sparklinePoints = listOf(14f, 18f, 22f, 26f, 31f, 34.4f),
        icon = Icons.Filled.RocketLaunch,
        accentColor = MaterialTheme.colorScheme.primary
    )
)
