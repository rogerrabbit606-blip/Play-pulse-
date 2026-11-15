package com.example.data

import com.example.model.SubscriptionPlan
import com.example.model.TrackedOpportunity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvReportGeneratorTest {

    @Test
    fun generateOpportunitiesCsv_filtersByStoreAndScoreAndEscapesCommas() {
        val tracked = listOf(
            TrackedOpportunity(
                id = "t1",
                name = "Invoice, Pro",
                packageName = "com.test.invoice",
                store = "Google Play",
                category = "Business",
                downloads = 120_000L,
                rating = 3.2,
                monthsDormant = 30L,
                opportunityScore = 92L,
                estMonthlyProfitUsd = 8500.0,
                recommendedSubPriceUsd = 9.99,
                status = "scouted",
                redesignNotes = "Clean UI",
                marketingHook = "Fast, private invoicing"
            )
        )

        val preview = CsvReportGenerator.generate(
            datasetType = "opportunities",
            storeFilter = "Google Play",
            minOpportunityScore = 85L,
            selectedColumns = listOf("Name", "Store", "OpportunityScore", "MarketingHook"),
            trackedApps = tracked,
            catalogApps = MarketCatalogData.abandonedAppsCatalog,
            benchmarks = MarketCatalogData.topStoreBenchmarks,
            plans = emptyList()
        )

        assertEquals(listOf("Name", "Store", "OpportunityScore", "MarketingHook"), preview.headers)
        assertTrue(preview.rows.isNotEmpty())
        assertTrue(preview.csvContent.contains("\"Invoice, Pro\""))
        assertTrue(preview.csvContent.contains("\"Fast, private invoicing\""))
    }

    @Test
    fun generateSubscriptionsCsv_includesActiveMonthlyEquivalentRevenue() {
        val plans = listOf(
            SubscriptionPlan(
                id = "p1",
                name = "Annual Anchor",
                platform = "Google Play",
                billingCycle = "annual",
                priceUsd = 48.0,
                trialDays = 7L,
                conversionRatePct = 6.0,
                monthlyChurnPct = 2.0,
                activeSubscribers = 100L,
                isActive = true
            )
        )

        val preview = CsvReportGenerator.generate(
            datasetType = "subscriptions",
            storeFilter = "All",
            minOpportunityScore = 0L,
            selectedColumns = listOf("PlanName", "Platform", "MonthlyEquivalentRevenueUsd"),
            trackedApps = emptyList(),
            catalogApps = emptyList(),
            benchmarks = emptyList(),
            plans = plans
        )

        assertEquals(1, preview.rows.size)
        assertEquals("400.00", preview.rows.first()[2])
    }
}
