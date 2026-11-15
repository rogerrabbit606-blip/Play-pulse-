package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.data.MarketCatalogData
import com.example.model.TrackedOpportunity
import com.example.ui.AsyncStatus
import com.example.ui.PlayPulseUiState
import com.example.ui.theme.PlayPulseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpportunitiesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun opportunitiesScreen_rendersTrackedAndCatalogWithOverlappingIdsWithoutDuplicateKeyCrash() {
        val starterTracked = MarketCatalogData.abandonedAppsCatalog.take(3).mapIndexed { index, cat ->
            TrackedOpportunity(
                id = cat.id,
                userId = "test_user",
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
                status = if (index == 0) "redesigning" else "scouted",
                redesignNotes = cat.redesignNotes,
                marketingHook = cat.marketingHook
            )
        }

        val state = PlayPulseUiState(
            isLoading = false,
            trackedApps = starterTracked
        )

        composeRule.setContent {
            PlayPulseTheme(darkTheme = true) {
                OpportunitiesScreen(
                    uiState = state,
                    scoutedApp = null,
                    scoutStatus = AsyncStatus.Idle,
                    onScoutNiche = { _, _ -> },
                    onClaimCatalogApp = {},
                    onClaimScoutedApp = {},
                    onAdvanceAppStatus = { _, _ -> },
                    onDeleteTrackedApp = {},
                    onGenerateStrategyForApp = { _, _, _, _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Play Store Market Radar").assertIsDisplayed()

        val mainList = composeRule.onAllNodes(hasScrollAction()).onFirst()
        mainList.performScrollToNode(hasText("Your Claimed Portfolio (3)"))
        composeRule.onNodeWithText("Your Claimed Portfolio (3)").assertIsDisplayed()

        // Scroll the outer LazyColumn down to the catalog entry for cat_scanlite
        // to ensure both tracked_cat_scanlite and catalog_cat_scanlite compose cleanly.
        val firstCatalogSignal = MarketCatalogData.abandonedAppsCatalog.first().abandonmentSignal
        mainList.performScrollToNode(hasText("High-Potential Abandoned Apps"))
        composeRule.onNodeWithText("High-Potential Abandoned Apps").assertIsDisplayed()

        mainList.performScrollToNode(hasText(firstCatalogSignal))
        composeRule.onNodeWithText(firstCatalogSignal).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("In Portfolio").fetchSemanticsNodes().isNotEmpty())
    }
}
