package com.example.data

import android.util.Log
import com.example.model.AiScoutedOpportunity
import com.example.model.AiStrategyBrief
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiInsightsRepo"

class AiInsightsRepository {

    private val scoutSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string("Concise title of an abandoned Play Store app niche opportunity"),
            "packageHint" to Schema.string("Realistic reverse-DNS package hint"),
            "store" to Schema.enumeration(listOf("Google Play", "Galaxy Store", "Amazon Appstore")),
            "category" to Schema.string("App store category"),
            "estimatedDownloads" to Schema.integer("Historical install volume estimate"),
            "legacyRating" to Schema.double("Current stale rating between 2.2 and 3.8"),
            "monthsDormant" to Schema.integer("Months since last update, e.g. 20 to 48"),
            "opportunityScore" to Schema.integer("Composite revival score 60 to 98"),
            "estMonthlyProfitUsd" to Schema.double("Projected monthly net revenue potential in USD"),
            "recommendedSubPriceUsd" to Schema.double("Recommended monthly subscription price in USD"),
            "redesignNotes" to Schema.string("Actionable Material 3 UX redesign improvements"),
            "marketingHook" to Schema.string("High-converting ASO positioning hook")
        )
    )

    private val strategySchema = Schema.obj(
        properties = mapOf(
            "competitiveMoatScore" to Schema.integer("Competitive moat score 0 to 100"),
            "monetizationStrategy" to Schema.string("Recurring subscription & pricing anchor playbook"),
            "redesignPriorities" to Schema.string("Top UI/UX modernization & bug-fix priorities"),
            "marketingPlaybook" to Schema.string("ASO keywords, screenshot funnel, and acquisition channels"),
            "retentionHooks" to Schema.string("User behavior loops and D7/D30 retention tactics")
        )
    )

    private val scoutModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scoutSchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            }
        )
    }

    private val scoutFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scoutSchema
            }
        )
    }

    private val strategyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = strategySchema
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            }
        )
    }

    private val strategyFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = strategySchema
            }
        )
    }

    private val streamModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                maxOutputTokens = 512
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            }
        )
    }

    private val forecastFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                maxOutputTokens = 512
            }
        )
    }

    suspend fun scoutAbandonedNiche(
        nichePrompt: String,
        targetStore: String
    ): AiScoutedOpportunity {
        val prompt = """
            You are a senior mobile app store market analyst and monetization strategist.
            Identify a high-potential abandoned or under-maintained app opportunity on $targetStore
            for the niche/keyword: "$nichePrompt".
            Focus on real consumer utility pain points where a modern Material 3 redesign, ASO marketing,
            and recurring subscription model can capture strong monthly profit.
            Provide realistic metrics, a clear redesign blueprint, and a concise marketing hook.
        """.trimIndent()

        return try {
            val response = try {
                withTimeout(40_000L) {
                    scoutModel.generateContent(prompt)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Primary scout model timed out; retrying with gemini-3.5-flash-lite", e)
                withTimeout(30_000L) {
                    scoutFallbackModel.generateContent(prompt)
                }
            }
            val raw = response.text ?: throw IllegalStateException("Empty AI response.")
            val json = JSONObject(raw)
            AiScoutedOpportunity(
                name = json.getString("name").take(80),
                packageHint = json.getString("packageHint").take(120),
                store = json.optString("store", targetStore),
                category = json.optString("category", "Productivity").take(40),
                estimatedDownloads = json.optLong("estimatedDownloads", 450_000L).coerceIn(10_000L, 50_000_000L),
                legacyRating = json.optDouble("legacyRating", 3.2).coerceIn(1.5, 4.5),
                monthsDormant = json.optLong("monthsDormant", 28L).coerceIn(12L, 84L),
                opportunityScore = json.optLong("opportunityScore", 86L).coerceIn(1L, 100L),
                estMonthlyProfitUsd = json.optDouble("estMonthlyProfitUsd", 7_500.0).coerceIn(500.0, 250_000.0),
                recommendedSubPriceUsd = json.optDouble("recommendedSubPriceUsd", 5.99).coerceIn(1.99, 49.99),
                redesignNotes = json.getString("redesignNotes").take(500),
                marketingHook = json.getString("marketingHook").take(300)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Scout generation timed out", e)
            throw IllegalStateException("AI market scout timed out. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Scout generation failed", e)
            throw e
        }
    }

    suspend fun generateCompetitiveStrategy(
        targetName: String,
        platform: String,
        currentRating: Double,
        downloads: Long,
        monthsDormant: Long
    ): AiStrategyBrief {
        val prompt = """
            Analyze the competitive market landscape for reviving/competing against "$targetName" on $platform.
            Metrics: $downloads historical downloads, $currentRating star rating, $monthsDormant months since last update.
            Provide concise, high-impact developer guidance covering:
            1) Competitive moat score (0-100)
            2) Recurring subscription monetization strategy (trial length, annual anchor, paywall trigger)
            3) Redesign priorities (UX friction removal & modern Material 3 polish)
            4) Marketing & ASO playbook
            5) User behavior & retention hooks to lift D30 retention.
            Keep each text field concise (under 500 characters).
        """.trimIndent()

        return try {
            val response = try {
                withTimeout(40_000L) {
                    strategyModel.generateContent(prompt)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Primary strategy model timed out; retrying with gemini-3.5-flash-lite", e)
                withTimeout(30_000L) {
                    strategyFallbackModel.generateContent(prompt)
                }
            }
            val raw = response.text ?: throw IllegalStateException("Empty strategy response.")
            val json = JSONObject(raw)
            AiStrategyBrief(
                targetName = targetName.take(80),
                platform = platform.take(40),
                competitiveMoatScore = json.optLong("competitiveMoatScore", 82L).coerceIn(0L, 100L),
                monetizationStrategy = json.getString("monetizationStrategy").take(600),
                redesignPriorities = json.getString("redesignPriorities").take(600),
                marketingPlaybook = json.getString("marketingPlaybook").take(600),
                retentionHooks = json.getString("retentionHooks").take(600)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Strategy generation timed out", e)
            throw IllegalStateException("AI strategy analysis timed out.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Strategy generation failed", e)
            throw e
        }
    }

    fun streamPredictiveInsights(portfolioSummary: String): Flow<String> = flow {
        val prompt = """
            You are an expert Play Store growth and subscription economist.
            Given this developer portfolio snapshot:
            $portfolioSummary

            Provide a crisp, executive 4-bullet predictive forecast covering:
            • 90-day MRR & ROI trajectory
            • User behavior & churn risk signal
            • Highest-leverage paywall / pricing experiment
            • Immediate same-day revenue & ASO action item
            Keep it scannable and under 180 words.
        """.trimIndent()

        val acc = StringBuilder()
        var streamTimedOut = false

        try {
            withTimeout(30_000L) {
                streamModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        acc.append(piece)
                        emit(acc.toString())
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            streamTimedOut = true
            Log.w(TAG, "Primary forecast stream timed out after 30s; falling back to unary fast model", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Streaming predictive forecast failed", e)
            throw e
        }

        if (acc.isBlank()) {
            try {
                val fallback = withTimeout(30_000L) {
                    forecastFallbackModel.generateContent(prompt)
                }
                val text = fallback.text?.trim().orEmpty()
                if (text.isEmpty()) {
                    throw IllegalStateException("Model returned an empty forecast.")
                }
                emit(text)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Fallback predictive forecast timed out", e)
                throw IllegalStateException(
                    if (streamTimedOut) "Predictive insight request timed out. Please retry." else "Predictive insight fallback timed out.",
                    e
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fallback predictive forecast failed", e)
                throw e
            }
        }
    }
}
