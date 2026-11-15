package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MarketCatalogData
import com.example.model.AiStrategyBrief
import com.example.ui.AsyncStatus
import com.example.ui.PlayPulseUiState
import com.example.ui.components.MetricPill
import com.example.ui.components.SectionHeader

@Composable
fun InsightsScreen(
    uiState: PlayPulseUiState,
    strategyPreview: AiStrategyBrief?,
    strategyStatus: AsyncStatus,
    onGenerateStrategy: (String, String, Double, Long, Long) -> Unit,
    onSaveBrief: (AiStrategyBrief) -> Unit,
    onDeleteBrief: (String) -> Unit
) {
    var targetAppName by remember { mutableStateOf("ScanLite PDF Classic") }
    var targetPlatform by remember { mutableStateOf("Google Play") }

    val quickTargets = remember(uiState.trackedApps) {
        val trackedNames = uiState.trackedApps.map { it.name }
        val catalogNames = MarketCatalogData.abandonedAppsCatalog.take(4).map { it.name }
        (trackedNames + catalogNames).distinct().take(6)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "AI Competitive Strategy Lab",
                subtitle = "Generate developer playbooks for redesign, ASO marketing & retention"
            )
        }

        // Strategy Generator Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Select Portfolio / Catalog App",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickTargets) { appTitle ->
                            FilterChip(
                                selected = targetAppName == appTitle,
                                onClick = { targetAppName = appTitle },
                                label = { Text(appTitle) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = targetAppName,
                        onValueChange = { targetAppName = it },
                        label = { Text("Target App or Niche") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("strategy_target_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Google Play", "Galaxy Store", "Amazon Appstore")) { store ->
                            FilterChip(
                                selected = targetPlatform == store,
                                onClick = { targetPlatform = store },
                                label = { Text(store) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val matchedTracked = uiState.trackedApps.firstOrNull { it.name == targetAppName }
                            val matchedCat = MarketCatalogData.abandonedAppsCatalog.firstOrNull { it.name == targetAppName }
                            onGenerateStrategy(
                                targetAppName.ifBlank { "Utility App" },
                                targetPlatform,
                                matchedTracked?.rating ?: matchedCat?.rating ?: 3.2,
                                matchedTracked?.downloads ?: matchedCat?.downloads ?: 500_000L,
                                matchedTracked?.monthsDormant ?: matchedCat?.monthsDormant ?: 30L
                            )
                        },
                        enabled = strategyStatus !is AsyncStatus.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_strategy_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (strategyStatus is AsyncStatus.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Synthesizing Landscape...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Competitive Playbook")
                        }
                    }

                    if (strategyStatus is AsyncStatus.Error) {
                        Text(
                            text = strategyStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Live Generated Preview
        if (strategyPreview != null) {
            item {
                StrategyBriefCard(
                    brief = strategyPreview,
                    isSaved = false,
                    onSave = { onSaveBrief(strategyPreview) },
                    onDelete = {}
                )
            }
        }

        // Saved Playbooks in Firestore
        item {
            SectionHeader(
                title = "Saved Playbooks (${uiState.strategyBriefs.size})",
                subtitle = "Persisted competitive & monetization blueprints"
            )
        }

        if (uiState.strategyBriefs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "No saved AI playbooks yet. Generate one above and tap Save to keep it in your cloud workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(uiState.strategyBriefs, key = { it.id }) { brief ->
                StrategyBriefCard(
                    brief = brief,
                    isSaved = true,
                    onSave = {},
                    onDelete = { onDeleteBrief(brief.id) }
                )
            }
        }
    }
}

@Composable
private fun StrategyBriefCard(
    brief: AiStrategyBrief,
    isSaved: Boolean,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (!isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = brief.targetName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = brief.platform,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MetricPill(
                    label = "Moat ${brief.competitiveMoatScore}/100",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            PlaybookSection("Monetization Strategy", brief.monetizationStrategy)
            PlaybookSection("Redesign Priorities", brief.redesignPriorities)
            PlaybookSection("ASO & Marketing Playbook", brief.marketingPlaybook)
            PlaybookSection("Retention & Engagement Loops", brief.retentionHooks)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isSaved) {
                    Button(
                        onClick = onSave,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_strategy_brief_button")
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save to Playbooks")
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete strategy brief",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybookSection(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
