package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiInsightsRepository
import com.example.data.MarketCatalogData
import com.example.data.PlayPulseRepository
import com.example.model.AiScoutedOpportunity
import com.example.model.AiStrategyBrief
import com.example.model.AlertRule
import com.example.model.MarketCatalogApp
import com.example.model.PaymentGatewayConfig
import com.example.model.PlatformForecastPoint
import com.example.model.SavedReportConfig
import com.example.model.SubscriptionPlan
import com.example.model.TrackedOpportunity
import com.example.model.TriggeredAlert
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "PlayPulseVM"

sealed interface AsyncStatus {
    data object Idle : AsyncStatus
    data object Loading : AsyncStatus
    data class Success(val message: String) : AsyncStatus
    data class Error(val message: String) : AsyncStatus
}

data class PlayPulseUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val trackedApps: List<TrackedOpportunity> = emptyList(),
    val subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    val paymentGateways: List<PaymentGatewayConfig> = emptyList(),
    val alertRules: List<AlertRule> = emptyList(),
    val savedReports: List<SavedReportConfig> = emptyList(),
    val strategyBriefs: List<AiStrategyBrief> = emptyList(),
    val totalMrrUsd: Double = 0.0,
    val projectedArrUsd: Double = 0.0,
    val totalActiveSubscribers: Long = 0L,
    val avgOpportunityScore: Int = 0,
    val totalTrackedDownloads: Long = 0L,
    val revivalProfitPotentialUsd: Double = 0.0,
    val avgConversionPct: Double = 4.8,
    val avgChurnPct: Double = 3.2,
    val userGrowthPct: Double = 8.3,
    val forecastSeries: List<PlatformForecastPoint> = emptyList(),
    val triggeredAlerts: List<TriggeredAlert> = emptyList()
)

class PlayPulseViewModel(
    private val repository: PlayPulseRepository,
    private val aiRepository: AiInsightsRepository = AiInsightsRepository(),
    private val currentUserId: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _scoutedApp = MutableStateFlow<AiScoutedOpportunity?>(null)
    val scoutedApp: StateFlow<AiScoutedOpportunity?> = _scoutedApp.asStateFlow()

    private val _scoutStatus = MutableStateFlow<AsyncStatus>(AsyncStatus.Idle)
    val scoutStatus: StateFlow<AsyncStatus> = _scoutStatus.asStateFlow()

    private val _strategyPreview = MutableStateFlow<AiStrategyBrief?>(null)
    val strategyPreview: StateFlow<AiStrategyBrief?> = _strategyPreview.asStateFlow()

    private val _strategyStatus = MutableStateFlow<AsyncStatus>(AsyncStatus.Idle)
    val strategyStatus: StateFlow<AsyncStatus> = _strategyStatus.asStateFlow()

    private val _forecastStreamText = MutableStateFlow("")
    val forecastStreamText: StateFlow<String> = _forecastStreamText.asStateFlow()

    private val _forecastStreamStatus = MutableStateFlow<AsyncStatus>(AsyncStatus.Idle)
    val forecastStreamStatus: StateFlow<AsyncStatus> = _forecastStreamStatus.asStateFlow()

    private var forecastJob: Job? = null

    private fun <T> safeStream(flow: kotlinx.coroutines.flow.Flow<List<T>>, label: String) =
        flow.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Stream error in $label", root)
            _statusBanner.value = root.message ?: "Failed to sync $label"
            emit(emptyList())
        }

    private val coreStreamsFlow = combine(
        safeStream(repository.observeTrackedApps(), "trackedApps"),
        safeStream(repository.observeSubscriptionPlans(), "subscriptionPlans"),
        safeStream(repository.observePaymentGateways(), "paymentGateways"),
        safeStream(repository.observeAlertRules(), "alertRules"),
        safeStream(repository.observeSavedReports(), "savedReports")
    ) { apps, plans, gateways, alerts, reports ->
        CoreData(apps, plans, gateways, alerts, reports)
    }

    val uiState: StateFlow<PlayPulseUiState> = combine(
        coreStreamsFlow,
        safeStream(repository.observeStrategyBriefs(), "strategyBriefs")
    ) { core, briefs ->
        val apps = core.apps.sortedByDescending { it.opportunityScore }
        val plans = core.plans
        val activePlans = plans.filter { it.isActive }
        val totalMrr = activePlans.sumOf { it.monthlyEquivalentRevenueUsd }
        val totalSubs = activePlans.sumOf { it.activeSubscribers }
        val avgConv = if (activePlans.isNotEmpty()) activePlans.map { it.conversionRatePct }.average() else 4.8
        val avgChurn = if (activePlans.isNotEmpty()) activePlans.map { it.monthlyChurnPct }.average() else 3.2
        val userGrowth = ((avgConv * 2.4 - avgChurn) + (apps.size * 0.6)).coerceIn(0.5, 45.0)
        val avgScore = if (apps.isNotEmpty()) apps.map { it.opportunityScore }.average().toInt() else 0
        val totalDownloads = apps.sumOf { it.downloads }
        val revivalProfit = apps.sumOf { it.estMonthlyProfitUsd }

        val forecast = MarketCatalogData.buildForecastSeries(
            currentMrrUsd = totalMrr,
            avgConversionPct = avgConv,
            avgChurnPct = avgChurn
        )

        val triggered = evaluateAlerts(
            rules = core.alerts,
            apps = apps,
            totalMrr = totalMrr,
            avgChurn = avgChurn
        )

        PlayPulseUiState(
            isLoading = false,
            trackedApps = apps,
            subscriptionPlans = plans,
            paymentGateways = core.gateways,
            alertRules = core.alerts,
            savedReports = core.reports,
            strategyBriefs = briefs,
            totalMrrUsd = totalMrr,
            projectedArrUsd = totalMrr * 12.0,
            totalActiveSubscribers = totalSubs,
            avgOpportunityScore = avgScore,
            totalTrackedDownloads = totalDownloads,
            revivalProfitPotentialUsd = revivalProfit,
            avgConversionPct = avgConv,
            avgChurnPct = avgChurn,
            userGrowthPct = userGrowth,
            forecastSeries = forecast,
            triggeredAlerts = triggered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PlayPulseUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.seedStarterWorkspaceIfEmpty()
                .onFailure { e ->
                    Log.w(TAG, "Starter seed skipped/failed for $currentUserId", e)
                }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    private fun evaluateAlerts(
        rules: List<AlertRule>,
        apps: List<TrackedOpportunity>,
        totalMrr: Double,
        avgChurn: Double
    ): List<TriggeredAlert> {
        val alerts = mutableListOf<TriggeredAlert>()
        rules.filter { it.isEnabled }.forEach { rule ->
            when (rule.metricKey) {
                "dormancy_months" -> {
                    val matches = MarketCatalogData.abandonedAppsCatalog.filter {
                        if (rule.operator == "gt") it.monthsDormant > rule.threshold else it.monthsDormant < rule.threshold
                    }
                    if (matches.isNotEmpty()) {
                        val top = matches.maxByOrNull { it.opportunityScore }!!
                        alerts += TriggeredAlert(
                            ruleId = rule.id,
                            title = rule.title,
                            detail = "${matches.size} dormant gems matched (top: ${top.name}, ${top.monthsDormant}mo idle)",
                            severity = "opportunity"
                        )
                    }
                }
                "opportunity_score" -> {
                    val matches = apps.filter {
                        if (rule.operator == "gt") it.opportunityScore > rule.threshold else it.opportunityScore < rule.threshold
                    }
                    if (matches.isNotEmpty()) {
                        alerts += TriggeredAlert(
                            ruleId = rule.id,
                            title = rule.title,
                            detail = "${matches.size} tracked app(s) crossed score threshold ${rule.threshold.toInt()}",
                            severity = "opportunity"
                        )
                    }
                }
                "churn_pct" -> {
                    val hit = if (rule.operator == "gt") avgChurn > rule.threshold else avgChurn < rule.threshold
                    if (hit) {
                        alerts += TriggeredAlert(
                            ruleId = rule.id,
                            title = rule.title,
                            detail = String.format(Locale.US, "Portfolio avg churn is %.1f%%", avgChurn),
                            severity = "warning"
                        )
                    }
                }
                "mrr_usd" -> {
                    val hit = if (rule.operator == "gt") totalMrr > rule.threshold else totalMrr < rule.threshold
                    if (hit) {
                        alerts += TriggeredAlert(
                            ruleId = rule.id,
                            title = rule.title,
                            detail = String.format(Locale.US, "Active MRR reached $%,.0f/mo", totalMrr),
                            severity = "milestone"
                        )
                    }
                }
                "downloads" -> {
                    val matches = apps.filter {
                        if (rule.operator == "gt") it.downloads > rule.threshold else it.downloads < rule.threshold
                    }
                    if (matches.isNotEmpty()) {
                        alerts += TriggeredAlert(
                            ruleId = rule.id,
                            title = rule.title,
                            detail = "${matches.size} portfolio app(s) matched download threshold",
                            severity = "opportunity"
                        )
                    }
                }
            }
        }
        return alerts
    }

    fun claimCatalogApp(cat: MarketCatalogApp) {
        viewModelScope.launch {
            val opp = TrackedOpportunity(
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
                status = "scouted",
                redesignNotes = cat.redesignNotes,
                marketingHook = cat.marketingHook
            )
            repository.trackOpportunity(opp, customId = cat.id)
                .onSuccess { _statusBanner.value = "Claimed ${cat.name} into portfolio." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to track app" }
        }
    }

    fun claimScoutedApp(scouted: AiScoutedOpportunity) {
        viewModelScope.launch {
            val opp = TrackedOpportunity(
                name = scouted.name,
                packageName = scouted.packageHint,
                store = scouted.store,
                category = scouted.category,
                downloads = scouted.estimatedDownloads,
                rating = scouted.legacyRating,
                monthsDormant = scouted.monthsDormant,
                opportunityScore = scouted.opportunityScore,
                estMonthlyProfitUsd = scouted.estMonthlyProfitUsd,
                recommendedSubPriceUsd = scouted.recommendedSubPriceUsd,
                status = "scouted",
                redesignNotes = scouted.redesignNotes,
                marketingHook = scouted.marketingHook
            )
            repository.trackOpportunity(opp)
                .onSuccess {
                    _scoutedApp.value = null
                    _statusBanner.value = "Added AI-scouted opportunity: ${scouted.name}"
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to save opportunity" }
        }
    }

    fun advanceAppStatus(appId: String, nextStatus: String) {
        viewModelScope.launch {
            repository.updateOpportunityStatus(appId, nextStatus)
                .onSuccess { _statusBanner.value = "Pipeline stage updated to $nextStatus" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun deleteTrackedApp(appId: String) {
        viewModelScope.launch {
            repository.deleteTrackedOpportunity(appId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun createSubscriptionPlan(
        name: String,
        platform: String,
        billingCycle: String,
        priceUsd: Double,
        trialDays: Long,
        conversionRatePct: Double,
        monthlyChurnPct: Double,
        activeSubscribers: Long
    ) {
        viewModelScope.launch {
            val plan = SubscriptionPlan(
                name = name.trim(),
                platform = platform,
                billingCycle = billingCycle,
                priceUsd = priceUsd,
                trialDays = trialDays,
                conversionRatePct = conversionRatePct,
                monthlyChurnPct = monthlyChurnPct,
                activeSubscribers = activeSubscribers,
                isActive = true
            )
            repository.createSubscriptionPlan(plan)
                .onSuccess { _statusBanner.value = "Added $platform subscription tier: $name" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Could not save plan" }
        }
    }

    fun toggleSubscriptionPlan(planId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleSubscriptionPlanActive(planId, active)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Toggle failed" }
        }
    }

    fun deleteSubscriptionPlan(planId: String) {
        viewModelScope.launch {
            repository.deleteSubscriptionPlan(planId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun updatePaymentGateway(gateway: PaymentGatewayConfig) {
        viewModelScope.launch {
            repository.upsertPaymentGateway(gateway)
                .onSuccess { _statusBanner.value = "Updated ${gateway.providerName} (${gateway.mode})" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Gateway update failed" }
        }
    }

    fun createAlertRule(title: String, metricKey: String, operator: String, threshold: Double) {
        viewModelScope.launch {
            repository.createAlertRule(
                AlertRule(
                    title = title.trim(),
                    metricKey = metricKey,
                    operator = operator,
                    threshold = threshold,
                    isEnabled = true
                )
            ).onSuccess { _statusBanner.value = "Automated alert rule created." }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Alert creation failed" }
        }
    }

    fun toggleAlertRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlertRule(ruleId, enabled)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Alert toggle failed" }
        }
    }

    fun deleteAlertRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteAlertRule(ruleId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun saveReportConfig(
        title: String,
        datasetType: String,
        storeFilter: String,
        minScore: Long,
        columns: List<String>
    ) {
        viewModelScope.launch {
            repository.saveReportConfig(
                SavedReportConfig(
                    title = title.trim(),
                    datasetType = datasetType,
                    storeFilter = storeFilter,
                    minOpportunityScore = minScore,
                    selectedColumns = columns
                )
            ).onSuccess { _statusBanner.value = "Saved custom report template: $title" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to save report" }
        }
    }

    fun deleteReportConfig(reportId: String) {
        viewModelScope.launch {
            repository.deleteReportConfig(reportId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun scoutAbandonedNiche(nichePrompt: String, store: String) {
        if (nichePrompt.isBlank()) {
            _scoutStatus.value = AsyncStatus.Error("Enter a niche or app keyword to scout.")
            return
        }
        viewModelScope.launch {
            _scoutStatus.value = AsyncStatus.Loading
            try {
                val result = aiRepository.scoutAbandonedNiche(nichePrompt.trim(), store)
                _scoutedApp.value = result
                _scoutStatus.value = AsyncStatus.Success("Opportunity scouted via Gemini AI")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _scoutStatus.value = AsyncStatus.Error(e.localizedMessage ?: "AI Market Scout failed.")
            }
        }
    }

    fun generateCompetitiveStrategy(
        targetName: String,
        platform: String,
        rating: Double,
        downloads: Long,
        monthsDormant: Long
    ) {
        viewModelScope.launch {
            _strategyStatus.value = AsyncStatus.Loading
            try {
                val brief = aiRepository.generateCompetitiveStrategy(
                    targetName = targetName,
                    platform = platform,
                    currentRating = rating,
                    downloads = downloads,
                    monthsDormant = monthsDormant
                )
                _strategyPreview.value = brief
                _strategyStatus.value = AsyncStatus.Success("Strategy brief ready")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _strategyStatus.value = AsyncStatus.Error(e.localizedMessage ?: "Strategy generation failed.")
            }
        }
    }

    fun saveStrategyBrief(brief: AiStrategyBrief) {
        viewModelScope.launch {
            repository.saveStrategyBrief(brief)
                .onSuccess { _statusBanner.value = "Saved playbook for ${brief.targetName}" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Save failed" }
        }
    }

    fun deleteStrategyBrief(briefId: String) {
        viewModelScope.launch {
            repository.deleteStrategyBrief(briefId)
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Delete failed" }
        }
    }

    fun streamPortfolioForecast() {
        val state = uiState.value
        val summary = buildString {
            appendLine("Active MRR: $${String.format(Locale.US, "%.0f", state.totalMrrUsd)}")
            appendLine("Active Subscribers: ${state.totalActiveSubscribers}")
            appendLine("Avg Conversion: ${String.format(Locale.US, "%.1f", state.avgConversionPct)}%, Avg Churn: ${String.format(Locale.US, "%.1f", state.avgChurnPct)}%")
            appendLine("Tracked Abandoned Apps: ${state.trackedApps.size}, Revival Profit Potential: $${String.format(Locale.US, "%.0f", state.revivalProfitPotentialUsd)}/mo")
            appendLine("Top Tracked: ${state.trackedApps.take(3).joinToString { "${it.name} (${it.store}, score ${it.opportunityScore})" }}")
        }

        forecastJob?.cancel()
        forecastJob = viewModelScope.launch {
            _forecastStreamStatus.value = AsyncStatus.Loading
            _forecastStreamText.value = ""
            try {
                aiRepository.streamPredictiveInsights(summary).collect { partial ->
                    _forecastStreamText.value = partial
                }
                _forecastStreamStatus.value = AsyncStatus.Success("Forecast complete")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _forecastStreamStatus.value = AsyncStatus.Error(e.localizedMessage ?: "Forecast failed.")
            }
        }
    }

    private data class CoreData(
        val apps: List<TrackedOpportunity>,
        val plans: List<SubscriptionPlan>,
        val gateways: List<PaymentGatewayConfig>,
        val alerts: List<AlertRule>,
        val reports: List<SavedReportConfig>
    )
}
