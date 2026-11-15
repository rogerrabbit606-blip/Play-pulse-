package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MarketCatalogData
import com.example.model.AlertRule
import com.example.model.TriggeredAlert
import com.example.ui.AsyncStatus
import com.example.ui.PlayPulseUiState
import com.example.ui.components.CohortRetentionBarChart
import com.example.ui.components.DashboardKpiGrid
import com.example.ui.components.KpiMetricItem
import com.example.ui.components.MetricPill
import com.example.ui.components.MrrForecastLineChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatCurrencyCompact
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: PlayPulseUiState,
    forecastStreamText: String,
    forecastStreamStatus: AsyncStatus,
    userEmail: String?,
    onStreamForecast: () -> Unit,
    onCreateAlertRule: (String, String, String, Double) -> Unit,
    onToggleAlertRule: (String, Boolean) -> Unit,
    onDeleteAlertRule: (String) -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToMonetize: () -> Unit,
    onSignOut: () -> Unit
) {
    var showNewAlertDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PlayPulse Command",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = userEmail ?: "Live Firestore Portfolio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Reusable Material 3 KPI Strip (Monthly Revenue, User Growth %, Churn Rate, Revival Upside)
        item {
            val mrrSparkline = uiState.forecastSeries
                .take(6)
                .map { (it.actualMrrUsd ?: it.projectedMrrUsd).toFloat() }
                .ifEmpty { listOf(4200f, 5100f, 5900f, 6800f, 7400f) }

            val churnHealthy = uiState.avgChurnPct <= 4.5
            val kpiItems = listOf(
                KpiMetricItem(
                    id = "monthly_revenue",
                    title = "Monthly Revenue",
                    formattedValue = formatCurrencyCompact(uiState.totalMrrUsd),
                    deltaLabel = String.format(Locale.US, "+%.1f%%", uiState.userGrowthPct),
                    supportingText = "ARR ${formatCurrencyCompact(uiState.projectedArrUsd)}",
                    trendPositive = true,
                    sparklinePoints = mrrSparkline,
                    icon = Icons.Filled.AttachMoney,
                    accentColor = MaterialTheme.colorScheme.primary,
                    testTag = "kpi_monthly_revenue",
                    onClick = onNavigateToMonetize
                ),
                KpiMetricItem(
                    id = "user_growth",
                    title = "User Growth",
                    formattedValue = String.format(Locale.US, "+%.1f%%", uiState.userGrowthPct),
                    deltaLabel = "${formatCompactCount(uiState.totalActiveSubscribers)} subs",
                    supportingText = "${formatCompactCount(uiState.totalTrackedDownloads)} tracked DL",
                    trendPositive = true,
                    sparklinePoints = listOf(4.2f, 5.1f, 6.0f, 7.2f, uiState.userGrowthPct.toFloat()),
                    icon = Icons.Filled.GroupAdd,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    testTag = "kpi_user_growth",
                    onClick = onNavigateToRadar
                ),
                KpiMetricItem(
                    id = "churn_rate",
                    title = "Sub Churn Rate",
                    formattedValue = String.format(Locale.US, "%.1f%%", uiState.avgChurnPct),
                    deltaLabel = if (churnHealthy) "Healthy" else "High Risk",
                    supportingText = "Conv ${String.format(Locale.US, "%.1f%%", uiState.avgConversionPct)} avg",
                    trendPositive = !churnHealthy,
                    isInverseMetric = true,
                    sparklinePoints = listOf(4.8f, 4.4f, 4.0f, 3.6f, uiState.avgChurnPct.toFloat()),
                    icon = Icons.Filled.Autorenew,
                    accentColor = if (churnHealthy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    testTag = "kpi_churn_rate",
                    onClick = onNavigateToMonetize
                ),
                KpiMetricItem(
                    id = "revival_upside",
                    title = "Revival Upside",
                    formattedValue = "${formatCurrencyCompact(uiState.revivalProfitPotentialUsd)}/mo",
                    deltaLabel = "${uiState.trackedApps.size} apps",
                    supportingText = "Avg score ${uiState.avgOpportunityScore}/100",
                    trendPositive = true,
                    sparklinePoints = listOf(10f, 16f, 22f, 28f, uiState.revivalProfitPotentialUsd.toFloat().coerceAtLeast(1f)),
                    icon = Icons.Filled.RocketLaunch,
                    accentColor = MaterialTheme.colorScheme.primary,
                    testTag = "kpi_revival_upside",
                    onClick = onNavigateToRadar
                )
            )
            DashboardKpiGrid(items = kpiItems)
        }

        // Quick Action Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToRadar,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_scout_radar_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Scout Abandoned Apps")
                }
                FilledTonalButton(
                    onClick = onNavigateToMonetize,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_sub_model_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Scale Subscriptions")
                }
            }
        }

        // Growth Trend & Predictive MRR Chart Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "MRR Growth & AI Forecast",
                        subtitle = "Tap any month node to inspect projected retention & revenue"
                    )
                    Spacer(Modifier.height(14.dp))
                    MrrForecastLineChart(points = uiState.forecastSeries)
                }
            }
        }

        // User Engagement & Retention Cohort Chart
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "User Engagement Cohorts",
                        subtitle = "Redesigned M3 + Subscription UX vs. Legacy Play Store average"
                    )
                    Spacer(Modifier.height(14.dp))
                    CohortRetentionBarChart(cohorts = MarketCatalogData.engagementCohorts)
                }
            }
        }

        // Gemini AI Streaming Predictive Insights
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Predictive Portfolio Insights",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = onStreamForecast,
                            enabled = forecastStreamStatus !is AsyncStatus.Loading,
                            modifier = Modifier.testTag("run_ai_forecast_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (forecastStreamStatus is AsyncStatus.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Analyze")
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    when {
                        forecastStreamStatus is AsyncStatus.Error -> {
                            Text(
                                text = forecastStreamStatus.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        forecastStreamText.isNotBlank() -> {
                            Text(
                                text = forecastStreamText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        else -> {
                            Text(
                                text = "Run Gemini AI over your live MRR, churn rate, and claimed abandoned apps to receive a 90-day ROI trajectory and immediate paywall optimization.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Automated Alerts Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Automated KPI Alerts",
                        subtitle = "Live threshold triggers synced across your portfolio",
                        trailing = {
                            FilledTonalButton(
                                onClick = { showNewAlertDialog = true },
                                modifier = Modifier.testTag("add_alert_rule_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddAlert,
                                    contentDescription = "New alert rule",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Rule")
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    AnimatedVisibility(visible = uiState.triggeredAlerts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.triggeredAlerts.forEach { alert ->
                                TriggeredAlertBanner(alert)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    if (uiState.alertRules.isEmpty()) {
                        Text(
                            text = "No alert rules configured yet. Add a rule to monitor dormancy, MRR milestones, or churn spikes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.alertRules.forEach { rule ->
                                AlertRuleRow(
                                    rule = rule,
                                    onToggle = { enabled -> onToggleAlertRule(rule.id, enabled) },
                                    onDelete = { onDeleteAlertRule(rule.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewAlertDialog) {
        CreateAlertRuleDialog(
            onDismiss = { showNewAlertDialog = false },
            onSave = { title, metric, op, threshold ->
                onCreateAlertRule(title, metric, op, threshold)
                showNewAlertDialog = false
            }
        )
    }
}

@Composable
private fun TriggeredAlertBanner(alert: TriggeredAlert) {
    val accent = when (alert.severity) {
        "warning" -> MaterialTheme.colorScheme.error
        "milestone" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = alert.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlertRuleRow(
    rule: AlertRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricPill(
                        label = "${rule.metricKey} ${if (rule.operator == "gt") ">" else "<"} ${rule.threshold.toInt()}",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateAlertRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var title by remember { mutableStateOf("Breakout Opportunity Score > 88") }
    var metricKey by remember { mutableStateOf("opportunity_score") }
    var operator by remember { mutableStateOf("gt") }
    var thresholdText by remember { mutableStateOf("88") }

    val metrics = listOf(
        "opportunity_score" to "Opp Score",
        "dormancy_months" to "Dormant Mo",
        "mrr_usd" to "MRR ($)",
        "churn_pct" to "Churn (%)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Automated Alert Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alert Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Metric", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    metrics.take(2).forEach { (key, label) ->
                        FilterChip(
                            selected = metricKey == key,
                            onClick = { metricKey = key },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    metrics.drop(2).forEach { (key, label) ->
                        FilterChip(
                            selected = metricKey == key,
                            onClick = { metricKey = key },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = operator == "gt",
                        onClick = { operator = "gt" },
                        label = { Text("Greater than (>)") }
                    )
                    FilterChip(
                        selected = operator == "lt",
                        onClick = { operator = "lt" },
                        label = { Text("Less than (<)") }
                    )
                }
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("Threshold Value") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdText.toDoubleOrNull() ?: 80.0
                    if (title.isNotBlank()) {
                        onSave(title, metricKey, operator, threshold)
                    }
                }
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
