package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.AlertRule
import com.example.model.PaymentGatewayConfig
import com.example.model.SavedReportConfig
import com.example.model.SubscriptionPlan
import com.example.model.TrackedOpportunity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PlayPulseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun trackAndObserveOpportunity_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = PlayPulseRepository(firestore)
        val docId = "opp_${UUID.randomUUID().toString().replace("-", "").take(12)}"

        val opp = TrackedOpportunity(
            name = "ScanLite PDF",
            packageName = "com.legacy.scanlite",
            store = "Google Play",
            category = "Productivity",
            downloads = 1_200_000L,
            rating = 3.1,
            monthsDormant = 34L,
            opportunityScore = 92L,
            estMonthlyProfitUsd = 11_200.0,
            recommendedSubPriceUsd = 6.99,
            status = "scouted",
            redesignNotes = "Modernize with Material 3",
            marketingHook = "Instant zero-ad PDF scan"
        )

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.trackOpportunity(opp, customId = docId).getOrThrow()
        }
        assertEquals(docId, createdId)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTrackedApps().first { list -> list.any { it.id == docId } }
        }
        assertTrue(observed.any { it.id == docId && it.name == "ScanLite PDF" })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOpportunityStatus(docId, "redesigning").getOrThrow()
        }
    }

    @Test
    fun subscriptionPlanAndAlertAndGatewayAndReport_crud_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = PlayPulseRepository(firestore)

        val planId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createSubscriptionPlan(
                SubscriptionPlan(
                    name = "Pro Monthly",
                    platform = "Google Play",
                    billingCycle = "monthly",
                    priceUsd = 6.99,
                    trialDays = 7L,
                    conversionRatePct = 5.2,
                    monthlyChurnPct = 3.4,
                    activeSubscribers = 310L,
                    isActive = true
                )
            ).getOrThrow()
        }
        assertTrue(planId.isNotBlank())

        val ruleId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createAlertRule(
                AlertRule(
                    title = "High Score Gem",
                    metricKey = "opportunity_score",
                    operator = "gt",
                    threshold = 85.0,
                    isEnabled = true
                )
            ).getOrThrow()
        }
        assertTrue(ruleId.isNotBlank())

        val gwId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertPaymentGateway(
                PaymentGatewayConfig(
                    providerName = "Google Play Billing v7",
                    skuId = "pro_monthly_sub",
                    mode = "live",
                    webhookConfigured = true,
                    gracePeriodEnabled = true,
                    isConnected = true,
                    payoutReady = true
                )
            ).getOrThrow()
        }
        assertTrue(gwId.isNotBlank())

        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveReportConfig(
                SavedReportConfig(
                    title = "Play Export",
                    datasetType = "opportunities",
                    storeFilter = "Google Play",
                    minOpportunityScore = 70L,
                    selectedColumns = listOf("Name", "Store", "OpportunityScore")
                )
            ).getOrThrow()
        }
        assertTrue(reportId.isNotBlank())
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PlayPulseRepository(firestore)
        val docId = "opp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.trackOpportunity(
                TrackedOpportunity(
                    name = "Private App",
                    packageName = "com.alice.privateapp",
                    store = "Google Play",
                    category = "Tools",
                    downloads = 50_000L,
                    rating = 3.0,
                    monthsDormant = 24L,
                    opportunityScore = 80L,
                    estMonthlyProfitUsd = 3000.0,
                    recommendedSubPriceUsd = 4.99,
                    status = "scouted",
                    redesignNotes = "Notes",
                    marketingHook = "Hook"
                ),
                customId = docId
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("trackedApps").document(docId).get().await()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun observeTrackedApps_unauthenticatedUser_failsGracefully() = runBlocking {
        auth.signOut()
        val repository = PlayPulseRepository(firestore)
        var threwIllegalState = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTrackedApps().first()
            }
        } catch (e: IllegalStateException) {
            threwIllegalState = true
        }
        assertTrue(threwIllegalState)
    }

    @Test
    fun directFirestoreQuery_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document("any_user_id")
                    .collection("trackedApps").get().await()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_playpulse@test.com"
        const val BOB_EMAIL = "bob_playpulse@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
