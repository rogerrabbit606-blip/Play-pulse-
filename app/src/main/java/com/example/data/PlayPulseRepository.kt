package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AiStrategyBrief
import com.example.model.AlertRule
import com.example.model.PaymentGatewayConfig
import com.example.model.SavedReportConfig
import com.example.model.SubscriptionPlan
import com.example.model.TrackedOpportunity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PlayPulseRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeUserCollection(
        collectionName: String
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/$collectionName"
        emitAll(
            userDoc(uid).collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        T::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTrackedApps(): Flow<List<TrackedOpportunity>> =
        observeUserCollection("trackedApps")

    fun observeSubscriptionPlans(): Flow<List<SubscriptionPlan>> =
        observeUserCollection("subscriptionPlans")

    fun observePaymentGateways(): Flow<List<PaymentGatewayConfig>> =
        observeUserCollection("paymentGateways")

    fun observeAlertRules(): Flow<List<AlertRule>> =
        observeUserCollection("alertRules")

    fun observeSavedReports(): Flow<List<SavedReportConfig>> =
        observeUserCollection("savedReports")

    fun observeStrategyBriefs(): Flow<List<AiStrategyBrief>> =
        observeUserCollection("strategyBriefs")

    suspend fun seedStarterWorkspaceIfEmpty(): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val existingApps = userDoc(uid).collection("trackedApps").limit(1).get().await()
        val existingPlans = userDoc(uid).collection("subscriptionPlans").limit(1).get().await()
        if (!existingApps.isEmpty || !existingPlans.isEmpty) {
            return@runCatching false
        }

        val batch = db.batch()

        val starterApps = MarketCatalogData.abandonedAppsCatalog.take(3).mapIndexed { index, cat ->
            val status = when (index) {
                0 -> "redesigning"
                1 -> "launched"
                else -> "scouted"
            }
            val docRef = userDoc(uid).collection("trackedApps").document(cat.id)
            docRef to mapOf(
                "userId" to uid,
                "name" to cat.name,
                "packageName" to cat.packageName,
                "store" to cat.store,
                "category" to cat.category,
                "downloads" to cat.downloads,
                "rating" to cat.rating,
                "monthsDormant" to cat.monthsDormant,
                "opportunityScore" to cat.opportunityScore,
                "estMonthlyProfitUsd" to cat.estMonthlyProfitUsd,
                "recommendedSubPriceUsd" to cat.recommendedSubPriceUsd,
                "status" to status,
                "redesignNotes" to cat.redesignNotes,
                "marketingHook" to cat.marketingHook,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        starterApps.forEach { (ref, data) -> batch.set(ref, data) }

        val starterPlans = listOf(
            "plan_gplay_monthly" to mapOf(
                "userId" to uid,
                "name" to "Pro Monthly Pass",
                "platform" to "Google Play",
                "billingCycle" to "monthly",
                "priceUsd" to 6.99,
                "trialDays" to 7L,
                "conversionRatePct" to 5.4,
                "monthlyChurnPct" to 3.6,
                "activeSubscribers" to 640L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "plan_gplay_annual" to mapOf(
                "userId" to uid,
                "name" to "Pro Annual Anchor",
                "platform" to "Google Play",
                "billingCycle" to "annual",
                "priceUsd" to 47.99,
                "trialDays" to 7L,
                "conversionRatePct" to 6.8,
                "monthlyChurnPct" to 1.8,
                "activeSubscribers" to 510L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "plan_galaxy_pro" to mapOf(
                "userId" to uid,
                "name" to "Galaxy Tablet Studio",
                "platform" to "Galaxy Store",
                "billingCycle" to "monthly",
                "priceUsd" to 8.99,
                "trialDays" to 14L,
                "conversionRatePct" to 4.9,
                "monthlyChurnPct" to 2.9,
                "activeSubscribers" to 195L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        starterPlans.forEach { (id, data) ->
            batch.set(userDoc(uid).collection("subscriptionPlans").document(id), data)
        }

        val starterGateways = listOf(
            "gw_play_billing" to mapOf(
                "userId" to uid,
                "providerName" to "Google Play Billing v7",
                "skuId" to "playpulse_pro_sub_v1",
                "mode" to "live",
                "webhookConfigured" to true,
                "gracePeriodEnabled" to true,
                "isConnected" to true,
                "payoutReady" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "gw_stripe_web" to mapOf(
                "userId" to uid,
                "providerName" to "Stripe Web Checkout",
                "skuId" to "price_playpulse_annual_48",
                "mode" to "live",
                "webhookConfigured" to true,
                "gracePeriodEnabled" to true,
                "isConnected" to true,
                "payoutReady" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "gw_revenuecat" to mapOf(
                "userId" to uid,
                "providerName" to "RevenueCat Entitlements",
                "skuId" to "entitlement_pro_access",
                "mode" to "sandbox",
                "webhookConfigured" to false,
                "gracePeriodEnabled" to true,
                "isConnected" to false,
                "payoutReady" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        starterGateways.forEach { (id, data) ->
            batch.set(userDoc(uid).collection("paymentGateways").document(id), data)
        }

        val starterAlerts = listOf(
            "alert_dormancy_gem" to mapOf(
                "userId" to uid,
                "title" to "High-Profit Dormant Gem (>24 mo)",
                "metricKey" to "dormancy_months",
                "operator" to "gt",
                "threshold" to 24.0,
                "isEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "alert_churn_guard" to mapOf(
                "userId" to uid,
                "title" to "Subscription Churn Warning (>5%)",
                "metricKey" to "churn_pct",
                "operator" to "gt",
                "threshold" to 5.0,
                "isEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            "alert_mrr_milestone" to mapOf(
                "userId" to uid,
                "title" to "Portfolio MRR Target ($5,000+)",
                "metricKey" to "mrr_usd",
                "operator" to "gt",
                "threshold" to 5000.0,
                "isEnabled" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        starterAlerts.forEach { (id, data) ->
            batch.set(userDoc(uid).collection("alertRules").document(id), data)
        }

        val starterReport = userDoc(uid).collection("savedReports").document("report_exec_play")
        batch.set(
            starterReport,
            mapOf(
                "userId" to uid,
                "title" to "Google Play High-ROI Opportunities",
                "datasetType" to "opportunities",
                "storeFilter" to "Google Play",
                "minOpportunityScore" to 80L,
                "selectedColumns" to listOf(
                    "Name", "Store", "Category", "Downloads",
                    "MonthsDormant", "OpportunityScore", "EstMonthlyProfitUsd", "RecommendedSubPriceUsd"
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit().await()
        true
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/starter_seed")
    }

    suspend fun trackOpportunity(opportunity: TrackedOpportunity, customId: String? = null): Result<String> = runCatching {
        val uid = requireUserId()
        val col = userDoc(uid).collection("trackedApps")
        val docRef = if (!customId.isNullOrBlank()) col.document(customId) else col.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to opportunity.name.take(80),
            "packageName" to opportunity.packageName.take(120),
            "store" to opportunity.store,
            "category" to opportunity.category.take(40),
            "downloads" to opportunity.downloads.coerceIn(0L, 1_000_000_000L),
            "rating" to opportunity.rating.coerceIn(0.0, 5.0),
            "monthsDormant" to opportunity.monthsDormant.coerceIn(0L, 240L),
            "opportunityScore" to opportunity.opportunityScore.coerceIn(0L, 100L),
            "estMonthlyProfitUsd" to opportunity.estMonthlyProfitUsd.coerceIn(0.0, 10_000_000.0),
            "recommendedSubPriceUsd" to opportunity.recommendedSubPriceUsd.coerceIn(0.0, 999.0),
            "status" to opportunity.status,
            "redesignNotes" to opportunity.redesignNotes.take(500),
            "marketingHook" to opportunity.marketingHook.take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun updateOpportunityStatus(appId: String, newStatus: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(appId.isNotBlank()) { "App ID cannot be blank" }
        val docRef = userDoc(uid).collection("trackedApps").document(appId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun deleteTrackedOpportunity(appId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("trackedApps").document(appId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun createSubscriptionPlan(plan: SubscriptionPlan): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("subscriptionPlans").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to plan.name.take(60),
            "platform" to plan.platform,
            "billingCycle" to plan.billingCycle,
            "priceUsd" to plan.priceUsd.coerceIn(0.49, 999.99),
            "trialDays" to plan.trialDays.coerceIn(0L, 90L),
            "conversionRatePct" to plan.conversionRatePct.coerceIn(0.0, 100.0),
            "monthlyChurnPct" to plan.monthlyChurnPct.coerceIn(0.0, 100.0),
            "activeSubscribers" to plan.activeSubscribers.coerceIn(0L, 10_000_000L),
            "isActive" to plan.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun toggleSubscriptionPlanActive(planId: String, isActive: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("subscriptionPlans").document(planId)
        try {
            docRef.update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun deleteSubscriptionPlan(planId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("subscriptionPlans").document(planId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun upsertPaymentGateway(gateway: PaymentGatewayConfig): Result<String> = runCatching {
        val uid = requireUserId()
        val col = userDoc(uid).collection("paymentGateways")
        val isNew = gateway.id.isBlank()
        val docRef = if (isNew) col.document() else col.document(gateway.id)
        try {
            if (isNew) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "providerName" to gateway.providerName,
                        "skuId" to gateway.skuId.take(100),
                        "mode" to gateway.mode,
                        "webhookConfigured" to gateway.webhookConfigured,
                        "gracePeriodEnabled" to gateway.gracePeriodEnabled,
                        "isConnected" to gateway.isConnected,
                        "payoutReady" to gateway.payoutReady,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "skuId" to gateway.skuId.take(100),
                        "mode" to gateway.mode,
                        "webhookConfigured" to gateway.webhookConfigured,
                        "gracePeriodEnabled" to gateway.gracePeriodEnabled,
                        "isConnected" to gateway.isConnected,
                        "payoutReady" to gateway.payoutReady,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun createAlertRule(rule: AlertRule): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("alertRules").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to rule.title.take(80),
            "metricKey" to rule.metricKey,
            "operator" to rule.operator,
            "threshold" to rule.threshold.coerceIn(0.0, 1_000_000_000.0),
            "isEnabled" to rule.isEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun toggleAlertRule(ruleId: String, isEnabled: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("alertRules").document(ruleId)
        try {
            docRef.update(
                mapOf(
                    "isEnabled" to isEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun deleteAlertRule(ruleId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("alertRules").document(ruleId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun saveReportConfig(config: SavedReportConfig): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("savedReports").document()
        val safeCols = config.selectedColumns.take(15).ifEmpty { listOf("Name", "Store") }
        val payload = mapOf(
            "userId" to uid,
            "title" to config.title.take(80),
            "datasetType" to config.datasetType,
            "storeFilter" to config.storeFilter,
            "minOpportunityScore" to config.minOpportunityScore.coerceIn(0L, 100L),
            "selectedColumns" to safeCols,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun deleteReportConfig(reportId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("savedReports").document(reportId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun saveStrategyBrief(brief: AiStrategyBrief): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("strategyBriefs").document()
        val payload = mapOf(
            "userId" to uid,
            "targetName" to brief.targetName.take(80),
            "platform" to brief.platform.take(40),
            "competitiveMoatScore" to brief.competitiveMoatScore.coerceIn(0L, 100L),
            "monetizationStrategy" to brief.monetizationStrategy.take(600),
            "redesignPriorities" to brief.redesignPriorities.take(600),
            "marketingPlaybook" to brief.marketingPlaybook.take(600),
            "retentionHooks" to brief.retentionHooks.take(600),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        docRef.id
    }

    suspend fun deleteStrategyBrief(briefId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = userDoc(uid).collection("strategyBriefs").document(briefId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
        Unit
    }
}
