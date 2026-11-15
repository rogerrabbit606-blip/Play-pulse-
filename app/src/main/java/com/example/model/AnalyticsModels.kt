package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TrackedOpportunity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val packageName: String = "",
    val store: String = "Google Play",
    val category: String = "Productivity",
    val downloads: Long = 0L,
    val rating: Double = 0.0,
    val monthsDormant: Long = 0L,
    val opportunityScore: Long = 0L,
    val estMonthlyProfitUsd: Double = 0.0,
    val recommendedSubPriceUsd: Double = 4.99,
    val status: String = "scouted",
    val redesignNotes: String = "",
    val marketingHook: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val platform: String = "Google Play",
    val billingCycle: String = "monthly",
    val priceUsd: Double = 4.99,
    val trialDays: Long = 7L,
    val conversionRatePct: Double = 4.2,
    val monthlyChurnPct: Double = 3.8,
    val activeSubscribers: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val monthlyEquivalentRevenueUsd: Double
        get() {
            if (!isActive) return 0.0
            val monthlyUnit = when (billingCycle) {
                "weekly" -> priceUsd * 4.33
                "annual" -> priceUsd / 12.0
                else -> priceUsd
            }
            return monthlyUnit * activeSubscribers
        }
}

@IgnoreExtraProperties
data class PaymentGatewayConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val providerName: String = "Google Play Billing v7",
    val skuId: String = "",
    val mode: String = "sandbox",
    val webhookConfigured: Boolean = false,
    val gracePeriodEnabled: Boolean = true,
    val isConnected: Boolean = false,
    val payoutReady: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AlertRule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val metricKey: String = "opportunity_score",
    val operator: String = "gt",
    val threshold: Double = 80.0,
    val isEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedReportConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val datasetType: String = "opportunities",
    val storeFilter: String = "All",
    val minOpportunityScore: Long = 0L,
    val selectedColumns: List<String> = listOf("Name", "Store", "Category", "Downloads", "OpportunityScore", "EstMonthlyProfitUsd"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AiStrategyBrief(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetName: String = "",
    val platform: String = "Google Play",
    val competitiveMoatScore: Long = 0L,
    val monetizationStrategy: String = "",
    val redesignPriorities: String = "",
    val marketingPlaybook: String = "",
    val retentionHooks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MarketCatalogApp(
    val id: String,
    val name: String,
    val packageName: String,
    val store: String,
    val category: String,
    val downloads: Long,
    val rating: Double,
    val monthsDormant: Long,
    val opportunityScore: Long,
    val estMonthlyProfitUsd: Double,
    val recommendedSubPriceUsd: Double,
    val abandonmentSignal: String,
    val redesignNotes: String,
    val marketingHook: String
)

data class TopStoreBenchmark(
    val id: String,
    val rank: Int,
    val name: String,
    val store: String,
    val category: String,
    val segment: String, // "Top Downloads" or "Top Paid / Grossing"
    val downloads: Long,
    val estMonthlyRevenueUsd: Double,
    val monetizationModel: String,
    val designPattern: String,
    val keyTakeaway: String
)

data class TriggeredAlert(
    val ruleId: String,
    val title: String,
    val detail: String,
    val severity: String // "opportunity", "warning", "milestone"
)

data class PlatformForecastPoint(
    val monthLabel: String,
    val actualMrrUsd: Double?,
    val projectedMrrUsd: Double,
    val retentionRatePct: Double
)

data class CohortMetric(
    val label: String,
    val portfolioPct: Float,
    val marketBenchmarkPct: Float
)

data class AiScoutedOpportunity(
    val name: String,
    val packageHint: String,
    val store: String,
    val category: String,
    val estimatedDownloads: Long,
    val legacyRating: Double,
    val monthsDormant: Long,
    val opportunityScore: Long,
    val estMonthlyProfitUsd: Double,
    val recommendedSubPriceUsd: Double,
    val redesignNotes: String,
    val marketingHook: String
)
