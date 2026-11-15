package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.theme.PlayPulseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KpiSummaryPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardKpiGrid_displaysRevenueGrowthAndChurnAndHandlesClicks() {
        var revenueTapped = false
        val items = listOf(
            KpiMetricItem(
                id = "mrr",
                title = "Monthly Revenue",
                formattedValue = "$8,740",
                deltaLabel = "+12.4%",
                supportingText = "ARR $104.9K",
                trendPositive = true,
                sparklinePoints = listOf(5f, 6f, 8.7f),
                icon = Icons.Filled.AttachMoney,
                accentColor = Color(0xFF00E5A3),
                testTag = "kpi_monthly_revenue",
                onClick = { revenueTapped = true }
            ),
            KpiMetricItem(
                id = "growth",
                title = "User Growth",
                formattedValue = "+14.2%",
                deltaLabel = "1.3K subs",
                supportingText = "4.8M tracked DL",
                trendPositive = true,
                sparklinePoints = listOf(8f, 11f, 14.2f),
                icon = Icons.Filled.GroupAdd,
                accentColor = Color(0xFF38BDF8),
                testTag = "kpi_user_growth"
            ),
            KpiMetricItem(
                id = "churn",
                title = "Sub Churn Rate",
                formattedValue = "2.8%",
                deltaLabel = "Healthy",
                supportingText = "Conv 5.4% avg",
                trendPositive = false,
                isInverseMetric = true,
                sparklinePoints = listOf(4.1f, 3.3f, 2.8f),
                icon = Icons.Filled.Autorenew,
                accentColor = Color(0xFFFFB84D),
                testTag = "kpi_churn_rate"
            )
        )

        composeRule.setContent {
            PlayPulseTheme(darkTheme = true) {
                DashboardKpiGrid(items = items)
            }
        }

        composeRule.onNodeWithText("Monthly Revenue").assertIsDisplayed()
        composeRule.onNodeWithText("$8,740").assertIsDisplayed()
        composeRule.onNodeWithText("User Growth").assertIsDisplayed()
        composeRule.onNodeWithText("+14.2%").assertIsDisplayed()
        composeRule.onNodeWithText("Sub Churn Rate").assertIsDisplayed()
        composeRule.onNodeWithText("2.8%").assertIsDisplayed()

        composeRule.onNodeWithTag("kpi_monthly_revenue").performClick()
        assertTrue(revenueTapped)
    }
}
