package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PaymentGatewayConfig
import com.example.model.SubscriptionPlan
import com.example.ui.PlayPulseUiState
import com.example.ui.components.MetricPill
import com.example.ui.components.PlatformRevenueDonutChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrencyCompact
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MonetizationScreen(
    uiState: PlayPulseUiState,
    onCreatePlan: (String, String, String, Double, Long, Double, Double, Long) -> Unit,
    onTogglePlan: (String, Boolean) -> Unit,
    onDeletePlan: (String) -> Unit,
    onUpdateGateway: (PaymentGatewayConfig) -> Unit
) {
    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var editingGateway by remember { mutableStateOf<PaymentGatewayConfig?>(null) }

    // Interactive ROI & Retention Simulator state
    var monthlyInstallsK by remember { mutableFloatStateOf(25f) }
    var trialConvPct by remember { mutableFloatStateOf(4.8f) }
    var monthlyChurnPct by remember { mutableFloatStateOf(3.5f) }
    var priceUsd by remember { mutableFloatStateOf(6.99f) }

    val simNewPaidPerMonth = (monthlyInstallsK * 1000f) * (trialConvPct / 100f)
    val simLtvMonths = (100f / monthlyChurnPct.coerceAtLeast(1f)).coerceAtMost(36f)
    val simSteadyStateMrr = simNewPaidPerMonth * priceUsd * (simLtvMonths * 0.45f)
    val simAnnualRunRate = simSteadyStateMrr * 12f
    val simLtvUsd = priceUsd * simLtvMonths

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Subscriptions & Gateways",
                subtitle = "Scale recurring revenue across platforms with same-day billing tools"
            )
        }

        // Cross-Platform Revenue Donut
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Platform Revenue Mix",
                        subtitle = String.format(
                            Locale.US,
                            "%d active subscribers across %d tiers",
                            uiState.totalActiveSubscribers,
                            uiState.subscriptionPlans.count { it.isActive }
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    PlatformRevenueDonutChart(plans = uiState.subscriptionPlans)
                }
            }
        }

        // Interactive ROI & Retention Simulator
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Subscription ROI Simulator",
                        subtitle = "Model pricing, conversion & churn impact on 12-month ARR"
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SimMetricBox(
                            label = "Est. Steady MRR",
                            value = formatCurrencyCompact(simSteadyStateMrr.toDouble()),
                            modifier = Modifier.weight(1f)
                        )
                        SimMetricBox(
                            label = "12-Mo Run Rate",
                            value = formatCurrencyCompact(simAnnualRunRate.toDouble()),
                            modifier = Modifier.weight(1f)
                        )
                        SimMetricBox(
                            label = "Sub LTV",
                            value = String.format(Locale.US, "$%.0f", simLtvUsd),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    SimulatorSlider(
                        label = "Monthly Installs",
                        valueText = "${monthlyInstallsK.roundToInt()}K",
                        value = monthlyInstallsK,
                        range = 5f..150f,
                        onValueChange = { monthlyInstallsK = it }
                    )
                    SimulatorSlider(
                        label = "Trial-to-Paid Conversion",
                        valueText = String.format(Locale.US, "%.1f%%", trialConvPct),
                        value = trialConvPct,
                        range = 1f..15f,
                        onValueChange = { trialConvPct = it }
                    )
                    SimulatorSlider(
                        label = "Monthly Churn Rate",
                        valueText = String.format(Locale.US, "%.1f%%", monthlyChurnPct),
                        value = monthlyChurnPct,
                        range = 1f..15f,
                        onValueChange = { monthlyChurnPct = it }
                    )
                    SimulatorSlider(
                        label = "Monthly Price Anchor",
                        valueText = String.format(Locale.US, "$%.2f", priceUsd),
                        value = priceUsd,
                        range = 1.99f..24.99f,
                        onValueChange = { priceUsd = it }
                    )
                }
            }
        }

        // Recurring Subscription Tiers
        item {
            SectionHeader(
                title = "Recurring Subscription Tiers",
                subtitle = "Manage live pricing models synced to Firestore",
                trailing = {
                    FilledTonalButton(
                        onClick = { showCreatePlanDialog = true },
                        modifier = Modifier.testTag("add_subscription_plan_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Tier")
                    }
                }
            )
        }

        items(uiState.subscriptionPlans, key = { "plan_${it.id}" }) { plan ->
            SubscriptionPlanCard(
                plan = plan,
                onToggle = { active -> onTogglePlan(plan.id, active) },
                onDelete = { onDeletePlan(plan.id) }
            )
        }

        // Same-Day Payment Gateways & Billing Readiness
        item {
            SectionHeader(
                title = "Same-Day Payment Gateways",
                subtitle = "Configure Billing SKUs, webhooks & grace-period recovery"
            )
        }

        items(uiState.paymentGateways, key = { "gw_${it.id}" }) { gateway ->
            PaymentGatewayCard(
                gateway = gateway,
                onToggleLive = {
                    val nextMode = if (gateway.mode == "live") "sandbox" else "live"
                    onUpdateGateway(gateway.copy(mode = nextMode, isConnected = true))
                },
                onConfigure = { editingGateway = gateway }
            )
        }
    }

    if (showCreatePlanDialog) {
        CreatePlanDialog(
            onDismiss = { showCreatePlanDialog = false },
            onSave = { name, platform, cycle, price, trial, conv, churn, subs ->
                onCreatePlan(name, platform, cycle, price, trial, conv, churn, subs)
                showCreatePlanDialog = false
            }
        )
    }

    editingGateway?.let { gw ->
        EditGatewayDialog(
            gateway = gw,
            onDismiss = { editingGateway = null },
            onSave = { updated ->
                onUpdateGateway(updated)
                editingGateway = null
            }
        )
    }
}

@Composable
private fun SimMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimulatorSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${plan.platform} • ${plan.billingCycle} • ${plan.trialDays}d free trial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = plan.isActive,
                    onCheckedChange = onToggle
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(
                    label = String.format(Locale.US, "$%.2f", plan.priceUsd),
                    color = MaterialTheme.colorScheme.primary
                )
                MetricPill(
                    label = "${plan.activeSubscribers} subs",
                    color = MaterialTheme.colorScheme.tertiary
                )
                MetricPill(
                    label = String.format(Locale.US, "Conv %.1f%% / Churn %.1f%%", plan.conversionRatePct, plan.monthlyChurnPct),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "Monthly Revenue: $%,.0f", plan.monthlyEquivalentRevenueUsd),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete plan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentGatewayCard(
    gateway: PaymentGatewayConfig,
    onToggleLive: () -> Unit,
    onConfigure: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = gateway.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SKU: ${gateway.skuId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MetricPill(
                    label = gateway.mode.uppercase(Locale.US),
                    color = if (gateway.mode == "live") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(
                    label = if (gateway.webhookConfigured) "Webhook Ready" else "Webhook Pending",
                    color = if (gateway.webhookConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                MetricPill(
                    label = if (gateway.gracePeriodEnabled) "Grace Period On" else "No Grace Period",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Configure")
                }
                Button(
                    onClick = onToggleLive,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (gateway.mode == "live") "Switch Sandbox" else "Go Live")
                }
            }
        }
    }
}

@Composable
private fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Long, Double, Double, Long) -> Unit
) {
    var name by remember { mutableStateOf("Pro Weekly Boost") }
    var platform by remember { mutableStateOf("Google Play") }
    var cycle by remember { mutableStateOf("monthly") }
    var priceText by remember { mutableStateOf("7.99") }
    var subsText by remember { mutableStateOf("250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription Tier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Platform", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Google Play", "Galaxy Store", "Web Billing").forEach { p ->
                        FilterChip(selected = platform == p, onClick = { platform = p }, label = { Text(p) })
                    }
                }
                Text("Billing Cycle", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("weekly", "monthly", "annual").forEach { c ->
                        FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (USD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subsText,
                    onValueChange = { subsText = it },
                    label = { Text("Projected Subscribers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = (priceText.toDoubleOrNull() ?: 6.99).coerceIn(0.99, 999.99)
                    val subs = (subsText.toLongOrNull() ?: 100L).coerceAtLeast(0L)
                    if (name.isNotBlank()) {
                        onSave(name, platform, cycle, price, 7L, 5.2, 3.2, subs)
                    }
                }
            ) { Text("Create Tier") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditGatewayDialog(
    gateway: PaymentGatewayConfig,
    onDismiss: () -> Unit,
    onSave: (PaymentGatewayConfig) -> Unit
) {
    var skuId by remember { mutableStateOf(gateway.skuId) }
    var webhook by remember { mutableStateOf(gateway.webhookConfigured) }
    var grace by remember { mutableStateOf(gateway.gracePeriodEnabled) }
    var payoutReady by remember { mutableStateOf(gateway.payoutReady) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(gateway.providerName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = skuId,
                    onValueChange = { skuId = it },
                    label = { Text("Subscription Product / SKU ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Server Receipt Webhook", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = webhook, onCheckedChange = { webhook = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Billing Grace Period (3d)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = grace, onCheckedChange = { grace = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payout Profile Verified", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = payoutReady, onCheckedChange = { payoutReady = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (skuId.isNotBlank()) {
                        onSave(
                            gateway.copy(
                                skuId = skuId.trim(),
                                webhookConfigured = webhook,
                                gracePeriodEnabled = grace,
                                payoutReady = payoutReady,
                                isConnected = webhook && payoutReady
                            )
                        )
                    }
                }
            ) { Text("Save Gateway") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
