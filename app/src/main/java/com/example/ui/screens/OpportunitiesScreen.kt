package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MarketCatalogData
import com.example.model.AiScoutedOpportunity
import com.example.model.MarketCatalogApp
import com.example.model.TopStoreBenchmark
import com.example.model.TrackedOpportunity
import com.example.ui.AsyncStatus
import com.example.ui.PlayPulseUiState
import com.example.ui.components.MetricPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatCurrencyCompact
import java.util.Locale

@Composable
fun OpportunitiesScreen(
    uiState: PlayPulseUiState,
    scoutedApp: AiScoutedOpportunity?,
    scoutStatus: AsyncStatus,
    onScoutNiche: (String, String) -> Unit,
    onClaimCatalogApp: (MarketCatalogApp) -> Unit,
    onClaimScoutedApp: (AiScoutedOpportunity) -> Unit,
    onAdvanceAppStatus: (String, String) -> Unit,
    onDeleteTrackedApp: (String) -> Unit,
    onGenerateStrategyForApp: (String, String, Double, Long, Long) -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Abandoned Gems, 1 = Top Store Benchmarks
    var selectedStore by remember { mutableStateOf("All") }
    var benchmarkSegment by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var aiNicheInput by remember { mutableStateOf("") }
    var selectedCatalogDetail by remember { mutableStateOf<MarketCatalogApp?>(null) }

    val stores = listOf("All", "Google Play", "Galaxy Store", "Amazon Appstore")

    val trackedPackages = remember(uiState.trackedApps) {
        uiState.trackedApps.map { it.packageName }.toSet()
    }

    val filteredCatalog = remember(selectedStore, searchQuery) {
        MarketCatalogData.abandonedAppsCatalog.filter { app ->
            (selectedStore == "All" || app.store == selectedStore) &&
                (searchQuery.isBlank() ||
                    app.name.contains(searchQuery, ignoreCase = true) ||
                    app.category.contains(searchQuery, ignoreCase = true))
        }.sortedByDescending { it.opportunityScore }
    }

    val filteredBenchmarks = remember(selectedStore, benchmarkSegment, searchQuery) {
        MarketCatalogData.topStoreBenchmarks.filter { bench ->
            (selectedStore == "All" || bench.store == selectedStore) &&
                (benchmarkSegment == "All" || bench.segment == benchmarkSegment) &&
                (searchQuery.isBlank() ||
                    bench.name.contains(searchQuery, ignoreCase = true) ||
                    bench.category.contains(searchQuery, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Play Store Market Radar",
                subtitle = "Abandoned profit gems & top-grossing store blueprints"
            )
        }

        // Mode Switcher
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_abandoned_gems")
                ) {
                    Text("Abandoned Gems")
                }
                SegmentedButton(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_top_benchmarks")
                ) {
                    Text("Top Store Leaders")
                }
            }
        }

        // Search & Store Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter by app name or category...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radar_search_input")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stores) { store ->
                        FilterChip(
                            selected = selectedStore == store,
                            onClick = { selectedStore = store },
                            label = { Text(store) }
                        )
                    }
                }
            }
        }

        if (selectedMode == 0) {
            // AI Niche Scout Hero Banner
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_opportunity_banner),
                                contentDescription = stringResource(R.string.cd_opportunity_banner),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Gemini AI Niche Scout",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = aiNicheInput,
                                    onValueChange = { aiNicheInput = it },
                                    placeholder = { Text("e.g., offline invoice, tinnitus relief") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_niche_input")
                                )
                                Button(
                                    onClick = {
                                        val store = if (selectedStore == "All") "Google Play" else selectedStore
                                        onScoutNiche(aiNicheInput, store)
                                    },
                                    enabled = scoutStatus !is AsyncStatus.Loading,
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("scout_niche_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (scoutStatus is AsyncStatus.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Scout")
                                    }
                                }
                            }

                            if (scoutStatus is AsyncStatus.Error) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = scoutStatus.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (scoutedApp != null) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = scoutedApp.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            MetricPill(
                                                label = "Score ${scoutedApp.opportunityScore}",
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${scoutedApp.store} • ${scoutedApp.category} • ${formatCompactCount(scoutedApp.estimatedDownloads)} DL • ${scoutedApp.monthsDormant}mo dormant",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Redesign: ${scoutedApp.redesignNotes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "Upside: $%,.0f/mo at $%.2f/mo sub",
                                                scoutedApp.estMonthlyProfitUsd,
                                                scoutedApp.recommendedSubPriceUsd
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Button(
                                            onClick = { onClaimScoutedApp(scoutedApp) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("claim_scouted_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Claim & Save to Portfolio")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tracked Portfolio Pipeline
            if (uiState.trackedApps.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Your Claimed Portfolio (${uiState.trackedApps.size})",
                        subtitle = "Advance apps from Scouted -> Redesigning -> Launched"
                    )
                }
                items(
                    items = uiState.trackedApps.filter {
                        selectedStore == "All" || it.store == selectedStore
                    },
                    key = { "tracked_${it.id}" }
                ) { app ->
                    TrackedOpportunityCard(
                        app = app,
                        onAdvanceStatus = { next -> onAdvanceAppStatus(app.id, next) },
                        onDelete = { onDeleteTrackedApp(app.id) },
                        onAnalyzeStrategy = {
                            onGenerateStrategyForApp(
                                app.name,
                                app.store,
                                app.rating,
                                app.downloads,
                                app.monthsDormant
                            )
                        }
                    )
                }
            }

            // Curated Play Store Abandoned Catalog
            item {
                SectionHeader(
                    title = "High-Potential Abandoned Apps",
                    subtitle = "Ranked by Redesign ROI, Dormancy & Subscription Potential"
                )
            }

            items(filteredCatalog, key = { "catalog_${it.id}" }) { cat ->
                val isClaimed = cat.packageName in trackedPackages
                CatalogOpportunityCard(
                    app = cat,
                    isClaimed = isClaimed,
                    onOpenDetail = { selectedCatalogDetail = cat },
                    onClaim = { onClaimCatalogApp(cat) }
                )
            }
        } else {
            // Top Download & Paid Benchmarks
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Top Downloads", "Top Paid / Grossing").forEach { seg ->
                        item {
                            FilterChip(
                                selected = benchmarkSegment == seg,
                                onClick = { benchmarkSegment = seg },
                                label = { Text(seg) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Top Store Revenue & UX Leaders",
                    subtitle = "Study highest-volume & top-paid apps for monetization & design patterns"
                )
            }

            items(filteredBenchmarks, key = { "bench_${it.id}" }) { bench ->
                BenchmarkLeaderCard(
                    bench = bench,
                    onAnalyzePattern = {
                        onGenerateStrategyForApp(
                            bench.name,
                            bench.store,
                            4.7,
                            bench.downloads,
                            0L
                        )
                    }
                )
            }
        }
    }

    selectedCatalogDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { selectedCatalogDetail = null },
            title = { Text(detail.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${detail.store} • ${detail.category} • ${formatCompactCount(detail.downloads)} downloads",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Abandonment Signal:\n${detail.abandonmentSignal}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Redesign Blueprint:\n${detail.redesignNotes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Marketing Hook:\n\"${detail.marketingHook}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClaimCatalogApp(detail)
                        selectedCatalogDetail = null
                    }
                ) {
                    Text("Claim & Track")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCatalogDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TrackedOpportunityCard(
    app: TrackedOpportunity,
    onAdvanceStatus: (String) -> Unit,
    onDelete: () -> Unit,
    onAnalyzeStrategy: () -> Unit
) {
    val nextStatus = when (app.status) {
        "scouted" -> "redesigning"
        "redesigning" -> "launched"
        "launched" -> "archived"
        else -> null
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${app.store} • ${app.category} • ${app.monthsDormant}mo idle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MetricPill(
                    label = app.status.uppercase(Locale.US),
                    color = when (app.status) {
                        "launched" -> MaterialTheme.colorScheme.primary
                        "redesigning" -> MaterialTheme.colorScheme.secondary
                        "archived" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(label = "Score ${app.opportunityScore}", color = MaterialTheme.colorScheme.primary)
                MetricPill(label = formatCompactCount(app.downloads) + " DL", color = MaterialTheme.colorScheme.tertiary)
                MetricPill(
                    label = formatCurrencyCompact(app.estMonthlyProfitUsd) + "/mo",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = app.redesignNotes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (nextStatus != null) {
                        OutlinedButton(
                            onClick = { onAdvanceStatus(nextStatus) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Move to ${nextStatus.replaceFirstChar { it.uppercase() }}")
                        }
                    }
                    OutlinedButton(
                        onClick = onAnalyzeStrategy,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Playbook")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Remove tracked app",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogOpportunityCard(
    app: MarketCatalogApp,
    isClaimed: Boolean,
    onOpenDetail: () -> Unit,
    onClaim: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${app.store} • ${app.category} • ★ ${app.rating}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${app.opportunityScore}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(label = "${formatCompactCount(app.downloads)} DL", color = MaterialTheme.colorScheme.tertiary)
                MetricPill(label = "${app.monthsDormant}mo dormant", color = MaterialTheme.colorScheme.error)
                MetricPill(
                    label = formatCurrencyCompact(app.estMonthlyProfitUsd) + "/mo",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = app.abandonmentSignal,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "Target Sub: $%.2f/mo", app.recommendedSubPriceUsd),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isClaimed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Claimed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "In Portfolio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Button(
                        onClick = onClaim,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("claim_${app.id}")
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Claim & Track")
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchmarkLeaderCard(
    bench: TopStoreBenchmark,
    onAnalyzePattern: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "#${bench.rank}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bench.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${bench.store} • ${bench.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MetricPill(
                    label = bench.segment,
                    color = if (bench.segment.contains("Paid")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(label = "${formatCompactCount(bench.downloads)} Downloads", color = MaterialTheme.colorScheme.tertiary)
                MetricPill(label = "${formatCurrencyCompact(bench.estMonthlyRevenueUsd)}/mo Gross", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Monetization: ${bench.monetizationModel}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Design Pattern: ${bench.designPattern}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = bench.keyTakeaway,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onAnalyzePattern,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Extract AI Pattern Playbook")
            }
        }
    }
}
