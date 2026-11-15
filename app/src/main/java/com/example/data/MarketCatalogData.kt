package com.example.data

import com.example.model.CohortMetric
import com.example.model.MarketCatalogApp
import com.example.model.PlatformForecastPoint
import com.example.model.TopStoreBenchmark
import kotlin.math.pow

object MarketCatalogData {

    val abandonedAppsCatalog: List<MarketCatalogApp> = listOf(
        MarketCatalogApp(
            id = "cat_scanlite",
            name = "ScanLite PDF Classic",
            packageName = "com.legacytools.scanlitepdf",
            store = "Google Play",
            category = "Productivity",
            downloads = 2_400_000L,
            rating = 3.1,
            monthsDormant = 38L,
            opportunityScore = 94L,
            estMonthlyProfitUsd = 14_200.0,
            recommendedSubPriceUsd = 6.99,
            abandonmentSignal = "Last updated 2023; crashes on Android 15 Scoped Storage; 1,400+ requests for dark mode & batch OCR.",
            redesignNotes = "Rebuild with Material 3 edge-to-edge UI, Photo Picker + CameraX document auto-crop, and offline ML Kit OCR.",
            marketingHook = "Private, zero-watermark PDF scanner with 2-second batch capture"
        ),
        MarketCatalogApp(
            id = "cat_sleeploop",
            name = "DeepSleep Ambient Mixer",
            packageName = "net.indieaudio.sleeploop",
            store = "Google Play",
            category = "Health & Fitness",
            downloads = 1_850_000L,
            rating = 3.4,
            monthsDormant = 31L,
            opportunityScore = 91L,
            estMonthlyProfitUsd = 11_800.0,
            recommendedSubPriceUsd = 5.99,
            abandonmentSignal = "Abandoned since early 2024; broken background audio fade-out; one-time $0.99 IAP never converted.",
            redesignNotes = "Modernize with spatial audio presets, adaptive sleep timer widget, and gentle sunrise alarm integration.",
            marketingHook = "Custom rain & brown-noise soundscapes built for deep REM cycles"
        ),
        MarketCatalogApp(
            id = "cat_invoicenow",
            name = "QuickInvoice Freelance",
            packageName = "com.bizcraft.quickinvoice",
            store = "Google Play",
            category = "Business",
            downloads = 980_000L,
            rating = 3.2,
            monthsDormant = 29L,
            opportunityScore = 89L,
            estMonthlyProfitUsd = 16_500.0,
            recommendedSubPriceUsd = 9.99,
            abandonmentSignal = "Holo-era UI; lacks recurring invoice reminders and tax export; high willingness to pay among contractors.",
            redesignNotes = "Clean financial-grade Material 3 ledger, instant PDF & CSV tax export, and recurring client templates.",
            marketingHook = "Send branded contractor invoices & get paid faster in 30 seconds"
        ),
        MarketCatalogApp(
            id = "cat_mileagelog",
            name = "TripTrack Mileage Vault",
            packageName = "io.roadlog.triptrack",
            store = "Google Play",
            category = "Auto & Vehicles",
            downloads = 760_000L,
            rating = 2.9,
            monthsDormant = 42L,
            opportunityScore = 87L,
            estMonthlyProfitUsd = 9_600.0,
            recommendedSubPriceUsd = 7.99,
            abandonmentSignal = "Broken CSV tax summary and dated forms; 62% of recent reviews beg for IRS-ready annual export.",
            redesignNotes = "Streamlined trip categorization swipe cards, multi-vehicle logbooks, and 1-tap accountant CSV bundles.",
            marketingHook = "Effortless tax-deductible mileage log with instant CSV export"
        ),
        MarketCatalogApp(
            id = "cat_focusdeck",
            name = "FocusDeck Pomodoro Pro",
            packageName = "app.zenstudy.focusdeck",
            store = "Google Play",
            category = "Productivity",
            downloads = 1_320_000L,
            rating = 3.5,
            monthsDormant = 26L,
            opportunityScore = 85L,
            estMonthlyProfitUsd = 8_400.0,
            recommendedSubPriceUsd = 4.99,
            abandonmentSignal = "No cloud sync across devices; static timer without streak analytics or Calendar-style weekly heatmaps.",
            redesignNotes = "Introduce visual focus heatmaps, ambient focus audio, and cross-device Firestore streak syncing.",
            marketingHook = "Visual focus sprints & habit heatmaps for deep-work builders"
        ),
        MarketCatalogApp(
            id = "cat_qrinventory",
            name = "StockBin QR Inventory",
            packageName = "com.warehouselite.stockbin",
            store = "Galaxy Store",
            category = "Business",
            downloads = 540_000L,
            rating = 3.3,
            monthsDormant = 33L,
            opportunityScore = 84L,
            estMonthlyProfitUsd = 10_900.0,
            recommendedSubPriceUsd = 12.99,
            abandonmentSignal = "Legacy barcode engine stalls on modern Galaxy devices; zero low-stock automated alerts.",
            redesignNotes = "High-speed batch barcode scanning, low-stock threshold alerts, and multi-warehouse CSV export.",
            marketingHook = "Instant warehouse & retail stock counts from any Android phone"
        ),
        MarketCatalogApp(
            id = "cat_guitartune",
            name = "Chromatic Pro Tuner",
            packageName = "org.stringlab.chrometune",
            store = "Google Play",
            category = "Music & Audio",
            downloads = 3_100_000L,
            rating = 3.6,
            monthsDormant = 44L,
            opportunityScore = 83L,
            estMonthlyProfitUsd = 7_900.0,
            recommendedSubPriceUsd = 3.99,
            abandonmentSignal = "Unmaintained since Android 11; cluttered banner layout; missing alternate tunings and chord trainer.",
            redesignNotes = "Precision strobe needle UI, custom capo/drop tunings library, and gamified ear-training streaks.",
            marketingHook = "Studio-grade chromatic tuner + interactive chord mastery"
        ),
        MarketCatalogApp(
            id = "cat_hydroalert",
            name = "AquaCue Hydration Log",
            packageName = "com.wellnessbit.aquacue",
            store = "Amazon Appstore",
            category = "Health & Fitness",
            downloads = 620_000L,
            rating = 3.2,
            monthsDormant = 28L,
            opportunityScore = 79L,
            estMonthlyProfitUsd = 5_400.0,
            recommendedSubPriceUsd = 3.49,
            abandonmentSignal = "Outdated notification channels fail on newer Android tablets; missing electrolyte & caffeine tracking.",
            redesignNotes = "Adaptive tablet + phone quick-log widgets, beverage hydration factors, and weekly goal streaks.",
            marketingHook = "Smart hydration & electrolyte balance tailored to your daily routine"
        ),
        MarketCatalogApp(
            id = "cat_receiptvault",
            name = "SnapReceipt Expense",
            packageName = "com.fintrack.snapreceipt",
            store = "Google Play",
            category = "Finance",
            downloads = 890_000L,
            rating = 3.0,
            monthsDormant = 35L,
            opportunityScore = 90L,
            estMonthlyProfitUsd = 13_400.0,
            recommendedSubPriceUsd = 8.99,
            abandonmentSignal = "Deprecated OCR library fails on thermal receipts; users willing to pay monthly for clean tax categories.",
            redesignNotes = "Gemini multimodal receipt parsing, category budget alerts, and accountant-ready CSV summaries.",
            marketingHook = "AI receipt capture & monthly expense intelligence in one tap"
        ),
        MarketCatalogApp(
            id = "cat_unitflow",
            name = "OmniUnit Converter Plus",
            packageName = "com.mathcraft.omniunit",
            store = "Galaxy Store",
            category = "Tools",
            downloads = 1_550_000L,
            rating = 3.7,
            monthsDormant = 30L,
            opportunityScore = 76L,
            estMonthlyProfitUsd = 4_800.0,
            recommendedSubPriceUsd = 2.99,
            abandonmentSignal = "Stale currency API keys and cramped keypad; high organic search traffic still arriving daily.",
            redesignNotes = "Split-screen live engineering & currency calculator, offline favorites, and foldable-ready adaptive layout.",
            marketingHook = "Instant engineering, cooking & live FX converter without clutter"
        )
    )

    val topStoreBenchmarks: List<TopStoreBenchmark> = listOf(
        TopStoreBenchmark(
            id = "bench_capcut",
            rank = 1,
            name = "CapCut Video Studio",
            store = "Google Play",
            category = "Video Players & Editors",
            segment = "Top Downloads",
            downloads = 1_000_000_000L,
            estMonthlyRevenueUsd = 14_500_000.0,
            monetizationModel = "Freemium + Tiered Pro Monthly ($9.99) & Annual ($74.99)",
            designPattern = "Template-first home feed with 1-tap preview & inline Pro badge upsell at export",
            keyTakeaway = "Let users reach their 'aha moment' before showing the subscription paywall."
        ),
        TopStoreBenchmark(
            id = "bench_duolingo",
            rank = 2,
            name = "Duolingo Language",
            store = "Google Play",
            category = "Education",
            segment = "Top Downloads",
            downloads = 500_000_000L,
            estMonthlyRevenueUsd = 38_000_000.0,
            monetizationModel = "7-Day Free Trial + Individual ($12.99/mo) & Family Annual Plan",
            designPattern = "Gamified streak path, progressive micro-celebrations, and loss-aversion streak freeze",
            keyTakeaway = "Daily streak mechanics + annual family anchoring drive industry-leading D30 retention."
        ),
        TopStoreBenchmark(
            id = "bench_canva",
            rank = 3,
            name = "Canva Design & AI",
            store = "Google Play",
            category = "Art & Design",
            segment = "Top Downloads",
            downloads = 500_000_000L,
            estMonthlyRevenueUsd = 29_000_000.0,
            monetizationModel = "Freemium + Pro Monthly ($14.99) + Team Seats",
            designPattern = "Search-led visual template gallery with contextual 'Try Pro free' on premium assets",
            keyTakeaway = "Contextual paywalls triggered on high-intent actions outperform generic launch walls by 3.2x."
        ),
        TopStoreBenchmark(
            id = "bench_spotify",
            rank = 4,
            name = "Spotify Audio Stream",
            store = "Google Play",
            category = "Music & Audio",
            segment = "Top Downloads",
            downloads = 1_000_000_000L,
            estMonthlyRevenueUsd = 62_000_000.0,
            monetizationModel = "Multi-tier Subscriptions (Individual, Duo, Family) + Web Billing",
            designPattern = "Dynamic personalized shelves, instant mini-player persistence, and seamless offline toggle",
            keyTakeaway = "Multi-tier household & team plans lift ARPU without raising single-user friction."
        ),
        TopStoreBenchmark(
            id = "bench_calm",
            rank = 1,
            name = "Calm Sleep & Meditation",
            store = "Google Play",
            category = "Health & Fitness",
            segment = "Top Paid / Grossing",
            downloads = 100_000_000L,
            estMonthlyRevenueUsd = 18_400_000.0,
            monetizationModel = "Annual-first ($69.99/yr) with 7-Day Trial & Lifetime anchor",
            designPattern = "Personalized 3-step goal quiz onboarding feeding directly into a tailored trial offer",
            keyTakeaway = "Onboarding personalization quizzes increase trial start rates by 40%+ on utility/wellness apps."
        ),
        TopStoreBenchmark(
            id = "bench_camscanner",
            rank = 2,
            name = "CamScanner PDF Pro",
            store = "Google Play",
            category = "Productivity",
            segment = "Top Paid / Grossing",
            downloads = 500_000_000L,
            estMonthlyRevenueUsd = 15_200_000.0,
            monetizationModel = "Weekly ($4.99), Monthly ($9.99), & Annual ($49.99) + Business Seats",
            designPattern = "Immediate camera-first launch with clean batch tray and instant OCR/watermark upgrade prompt",
            keyTakeaway = "Direct proof for why abandoned scanner & utility apps monetize strongly with modern UX."
        ),
        TopStoreBenchmark(
            id = "bench_notion",
            rank = 3,
            name = "Notion Workspace",
            store = "Google Play",
            category = "Productivity",
            segment = "Top Paid / Grossing",
            downloads = 50_000_000L,
            estMonthlyRevenueUsd = 12_800_000.0,
            monetizationModel = "Freemium + Plus ($10/mo) + AI Add-on ($8/mo)",
            designPattern = "Modular block editor, clean monochrome hierarchy, and starter workspace templates",
            keyTakeaway = "Starter templates eliminate blank-screen drop-off and accelerate Day-1 activation."
        ),
        TopStoreBenchmark(
            id = "bench_poweramp",
            rank = 4,
            name = "Poweramp Equalizer Pro",
            store = "Google Play",
            category = "Music & Audio",
            segment = "Top Paid / Grossing",
            downloads = 50_000_000L,
            estMonthlyRevenueUsd = 2_100_000.0,
            monetizationModel = "Paid Unlocker ($6.99) + Feature Pack Add-ons",
            designPattern = "Tactile high-precision rotary controls, dark obsidian surface, and instant audio preset A/B",
            keyTakeaway = "Power-user audio/tool utilities earn 4.7+ stars when UI feels tactile and lag-free."
        ),
        TopStoreBenchmark(
            id = "bench_samsung_notes_pro",
            rank = 5,
            name = "Noteshelf Stylus Studio",
            store = "Galaxy Store",
            category = "Productivity",
            segment = "Top Paid / Grossing",
            downloads = 12_000_000L,
            estMonthlyRevenueUsd = 1_450_000.0,
            monetizationModel = "Paid Upfront ($4.99) + Club Annual Sync ($19.99/yr)",
            designPattern = "Adaptive tablet split-pane shelf, low-latency ink toolbar, and structured binder tabs",
            keyTakeaway = "Galaxy Store tablet optimization commands lower competition and strong paid conversion."
        ),
        TopStoreBenchmark(
            id = "bench_amazon_luna_companion",
            rank = 6,
            name = "KineMaster Pro Studio",
            store = "Amazon Appstore",
            category = "Video Players & Editors",
            segment = "Top Paid / Grossing",
            downloads = 150_000_000L,
            estMonthlyRevenueUsd = 4_900_000.0,
            monetizationModel = "Monthly ($8.99) & Annual ($44.99) Cross-Store Subscription",
            designPattern = "Timeline precision scrubbing + instant aspect-ratio switcher for Fire & Android tablets",
            keyTakeaway = "Republishing modernized Android apps to Galaxy Store & Amazon adds 12-18% incremental MRR."
        )
    )

    val engagementCohorts: List<CohortMetric> = listOf(
        CohortMetric("Day 1", 0.58f, 0.34f),
        CohortMetric("Day 7", 0.39f, 0.19f),
        CohortMetric("Day 30", 0.27f, 0.11f),
        CohortMetric("Day 90", 0.21f, 0.06f)
    )

    fun buildForecastSeries(
        currentMrrUsd: Double,
        avgConversionPct: Double,
        avgChurnPct: Double
    ): List<PlatformForecastPoint> {
        val baseMrr = if (currentMrrUsd > 0) currentMrrUsd else 6_400.0
        val netMonthlyGrowth = ((avgConversionPct * 2.4 - avgChurnPct) / 100.0).coerceIn(0.03, 0.22)
        val months = listOf("Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan+", "Feb+", "Mar+", "Apr+")
        return months.mapIndexed { idx, label ->
            if (idx <= 4) {
                val factor = (1.0 + netMonthlyGrowth).pow(idx - 4)
                val actual = (baseMrr * factor).coerceAtLeast(500.0)
                PlatformForecastPoint(
                    monthLabel = label,
                    actualMrrUsd = actual,
                    projectedMrrUsd = actual,
                    retentionRatePct = (74.0 + idx * 2.1).coerceAtMost(95.0)
                )
            } else {
                val stepsAhead = idx - 4
                val projected = baseMrr * (1.0 + netMonthlyGrowth).pow(stepsAhead)
                PlatformForecastPoint(
                    monthLabel = label,
                    actualMrrUsd = null,
                    projectedMrrUsd = projected,
                    retentionRatePct = (83.0 + stepsAhead * 1.4 - (avgChurnPct * 0.4)).coerceIn(60.0, 96.0)
                )
            }
        }
    }
}
