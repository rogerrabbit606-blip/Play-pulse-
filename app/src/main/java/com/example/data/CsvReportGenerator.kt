package com.example.data

import com.example.model.SubscriptionPlan
import com.example.model.TopStoreBenchmark
import com.example.model.TrackedOpportunity
import java.util.Locale

data class ReportTablePreview(
    val headers: List<String>,
    val rows: List<List<String>>,
    val csvContent: String
)

object CsvReportGenerator {

    val columnsByDataset: Map<String, List<String>> = mapOf(
        "opportunities" to listOf(
            "Name", "PackageName", "Store", "Category", "Downloads",
            "Rating", "MonthsDormant", "OpportunityScore",
            "EstMonthlyProfitUsd", "RecommendedSubPriceUsd", "Status", "MarketingHook"
        ),
        "benchmarks" to listOf(
            "Rank", "Name", "Store", "Segment", "Category",
            "Downloads", "EstMonthlyRevenueUsd", "MonetizationModel", "DesignPattern"
        ),
        "subscriptions" to listOf(
            "PlanName", "Platform", "BillingCycle", "PriceUsd",
            "TrialDays", "ConversionRatePct", "MonthlyChurnPct",
            "ActiveSubscribers", "MonthlyEquivalentRevenueUsd", "IsActive"
        ),
        "executive_kpi" to listOf(
            "KPI", "Value", "PlatformScope", "StatusNote"
        )
    )

    fun generate(
        datasetType: String,
        storeFilter: String,
        minOpportunityScore: Long,
        selectedColumns: List<String>,
        trackedApps: List<TrackedOpportunity>,
        catalogApps: List<com.example.model.MarketCatalogApp>,
        benchmarks: List<TopStoreBenchmark>,
        plans: List<SubscriptionPlan>
    ): ReportTablePreview {
        val availableCols = columnsByDataset[datasetType].orEmpty()
        val headers = selectedColumns.filter { it in availableCols }.ifEmpty { availableCols.take(5) }

        val rows: List<List<String>> = when (datasetType) {
            "opportunities" -> {
                val combined = buildList {
                    addAll(trackedApps)
                    val trackedPackages = trackedApps.map { it.packageName }.toSet()
                    catalogApps.filter { it.packageName !in trackedPackages }.forEach { cat ->
                        add(
                            TrackedOpportunity(
                                id = cat.id,
                                name = cat.name,
                                packageName = cat.packageName,
                                store = cat.store,
                                category = cat.category,
                                downloads = cat.downloads,
                                rating = cat.rating,
                                monthsDormant = cat.monthsDormant,
                                opportunityScore = cat.opportunityScore,
                                estMonthlyProfitUsd = cat.estMonthlyProfitUsd,
                                recommendedSubPriceUsd = cat.recommendedSubPriceUsd,
                                status = "catalog_candidate",
                                redesignNotes = cat.redesignNotes,
                                marketingHook = cat.marketingHook
                            )
                        )
                    }
                }.filter { app ->
                    (storeFilter == "All" || app.store == storeFilter) &&
                        app.opportunityScore >= minOpportunityScore
                }.sortedByDescending { it.opportunityScore }

                combined.map { app ->
                    headers.map { col ->
                        when (col) {
                            "Name" -> app.name
                            "PackageName" -> app.packageName
                            "Store" -> app.store
                            "Category" -> app.category
                            "Downloads" -> app.downloads.toString()
                            "Rating" -> String.format(Locale.US, "%.1f", app.rating)
                            "MonthsDormant" -> app.monthsDormant.toString()
                            "OpportunityScore" -> app.opportunityScore.toString()
                            "EstMonthlyProfitUsd" -> String.format(Locale.US, "%.2f", app.estMonthlyProfitUsd)
                            "RecommendedSubPriceUsd" -> String.format(Locale.US, "%.2f", app.recommendedSubPriceUsd)
                            "Status" -> app.status
                            "MarketingHook" -> app.marketingHook
                            else -> ""
                        }
                    }
                }
            }

            "benchmarks" -> {
                benchmarks.filter { storeFilter == "All" || it.store == storeFilter }
                    .map { b ->
                        headers.map { col ->
                            when (col) {
                                "Rank" -> b.rank.toString()
                                "Name" -> b.name
                                "Store" -> b.store
                                "Segment" -> b.segment
                                "Category" -> b.category
                                "Downloads" -> b.downloads.toString()
                                "EstMonthlyRevenueUsd" -> String.format(Locale.US, "%.2f", b.estMonthlyRevenueUsd)
                                "MonetizationModel" -> b.monetizationModel
                                "DesignPattern" -> b.designPattern
                                else -> ""
                            }
                        }
                    }
            }

            "subscriptions" -> {
                plans.filter {
                    storeFilter == "All" || it.platform == storeFilter ||
                        (storeFilter == "Google Play" && it.platform == "Google Play")
                }.map { p ->
                    headers.map { col ->
                        when (col) {
                            "PlanName" -> p.name
                            "Platform" -> p.platform
                            "BillingCycle" -> p.billingCycle
                            "PriceUsd" -> String.format(Locale.US, "%.2f", p.priceUsd)
                            "TrialDays" -> p.trialDays.toString()
                            "ConversionRatePct" -> String.format(Locale.US, "%.1f", p.conversionRatePct)
                            "MonthlyChurnPct" -> String.format(Locale.US, "%.1f", p.monthlyChurnPct)
                            "ActiveSubscribers" -> p.activeSubscribers.toString()
                            "MonthlyEquivalentRevenueUsd" -> String.format(Locale.US, "%.2f", p.monthlyEquivalentRevenueUsd)
                            "IsActive" -> p.isActive.toString()
                            else -> ""
                        }
                    }
                }
            }

            else -> {
                val totalMrr = plans.sumOf { it.monthlyEquivalentRevenueUsd }
                val totalSubs = plans.sumOf { it.activeSubscribers }
                val avgScore = if (trackedApps.isNotEmpty()) {
                    trackedApps.map { it.opportunityScore }.average()
                } else 0.0
                val potentialProfit = trackedApps.sumOf { it.estMonthlyProfitUsd }
                val kpiRows = listOf(
                    listOf("Active Portfolio MRR (USD)", String.format(Locale.US, "%.2f", totalMrr), storeFilter, "Recurring Subscriptions"),
                    listOf("Projected Annual Run Rate (ARR)", String.format(Locale.US, "%.2f", totalMrr * 12.0), storeFilter, "12-Month Run Rate"),
                    listOf("Active Paid Subscribers", totalSubs.toString(), storeFilter, "Cross-Platform"),
                    listOf("Tracked Opportunities Count", trackedApps.size.toString(), storeFilter, "Score >= $minOpportunityScore"),
                    listOf("Avg Opportunity Score", String.format(Locale.US, "%.1f", avgScore), storeFilter, "Out of 100"),
                    listOf("Tracked Revival Profit Potential (USD/mo)", String.format(Locale.US, "%.2f", potentialProfit), storeFilter, "Post-Redesign Estimate")
                )
                kpiRows.map { row ->
                    headers.map { col ->
                        when (col) {
                            "KPI" -> row[0]
                            "Value" -> row[1]
                            "PlatformScope" -> row[2]
                            "StatusNote" -> row[3]
                            else -> ""
                        }
                    }
                }
            }
        }

        val csvBuilder = StringBuilder()
        csvBuilder.appendLine(headers.joinToString(",") { escapeCsv(it) })
        rows.forEach { r ->
            csvBuilder.appendLine(r.joinToString(",") { escapeCsv(it) })
        }

        return ReportTablePreview(
            headers = headers,
            rows = rows,
            csvContent = csvBuilder.toString()
        )
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
