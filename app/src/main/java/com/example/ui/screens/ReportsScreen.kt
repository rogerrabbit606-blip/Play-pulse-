package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CsvReportGenerator
import com.example.data.MarketCatalogData
import com.example.model.SavedReportConfig
import com.example.ui.PlayPulseUiState
import com.example.ui.components.MetricPill
import com.example.ui.components.SectionHeader
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    uiState: PlayPulseUiState,
    onSaveReportPreset: (String, String, String, Long, List<String>) -> Unit,
    onDeleteReportPreset: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current

    var datasetType by remember { mutableStateOf("opportunities") }
    var storeFilter by remember { mutableStateOf("All") }
    var minScore by remember { mutableFloatStateOf(70f) }
    var selectedColumns by remember(datasetType) {
        mutableStateOf(
            CsvReportGenerator.columnsByDataset[datasetType].orEmpty().take(6)
        )
    }
    var showSaveDialog by remember { mutableStateOf(false) }

    val preview = remember(
        datasetType,
        storeFilter,
        minScore,
        selectedColumns,
        uiState.trackedApps,
        uiState.subscriptionPlans
    ) {
        CsvReportGenerator.generate(
            datasetType = datasetType,
            storeFilter = storeFilter,
            minOpportunityScore = minScore.roundToInt().toLong(),
            selectedColumns = selectedColumns,
            trackedApps = uiState.trackedApps,
            catalogApps = MarketCatalogData.abandonedAppsCatalog,
            benchmarks = MarketCatalogData.topStoreBenchmarks,
            plans = uiState.subscriptionPlans
        )
    }

    // Zero-permission Storage Access Framework CSV file writer
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(preview.csvContent.toByteArray(Charsets.UTF_8))
                }
                onShowMessage("Exported ${preview.rows.size} rows to CSV.")
            } catch (e: Exception) {
                onShowMessage("CSV export failed: ${e.localizedMessage}")
            }
        }
    }

    val datasets = listOf(
        "opportunities" to "Abandoned Apps",
        "benchmarks" to "Top Store Leaders",
        "subscriptions" to "Subscription Tiers",
        "executive_kpi" to "Executive KPIs"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Custom Reporting & CSV Export",
                subtitle = "Configure store filters, toggle columns & export RFC-4180 CSV"
            )
        }

        // Report Builder Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dataset", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(datasets) { (key, label) ->
                            FilterChip(
                                selected = datasetType == key,
                                onClick = { datasetType = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Play Store Scope", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "Google Play", "Galaxy Store", "Amazon Appstore")) { store ->
                            FilterChip(
                                selected = storeFilter == store,
                                onClick = { storeFilter = store },
                                label = { Text(store) }
                            )
                        }
                    }

                    if (datasetType == "opportunities") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Min Opportunity Score", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${minScore.roundToInt()}+",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = minScore,
                            onValueChange = { minScore = it },
                            valueRange = 0f..95f
                        )
                    }

                    Text("Included CSV Columns", style = MaterialTheme.typography.labelLarge)
                    val availableCols = CsvReportGenerator.columnsByDataset[datasetType].orEmpty()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableCols.forEach { col ->
                            val isSelected = col in selectedColumns
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedColumns = if (isSelected && selectedColumns.size > 1) {
                                        selectedColumns - col
                                    } else if (!isSelected) {
                                        selectedColumns + col
                                    } else {
                                        selectedColumns
                                    }
                                },
                                label = { Text(col) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "playpulse_${datasetType}_${System.currentTimeMillis() / 1000}.csv"
                                createCsvLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save .CSV")
                        }
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "PlayPulse Analytics Export ($datasetType)")
                                    putExtra(Intent.EXTRA_TEXT, preview.csvContent)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share CSV Report"))
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("share_csv_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share CSV", modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_report_preset_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = "Save preset", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Live CSV Table Preview
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Live CSV Preview (${preview.rows.size} rows)",
                        subtitle = "Scroll horizontally to inspect formatted columns"
                    )
                    Spacer(Modifier.height(12.dp))

                    if (preview.rows.isEmpty()) {
                        Text(
                            text = "No rows match your current filter. Lower the minimum Opportunity Score or select 'All' stores.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Header Row
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                preview.headers.forEach { header ->
                                    Text(
                                        text = header,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(128.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                            preview.rows.take(10).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.width(128.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Report Presets
        if (uiState.savedReports.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Saved Report Presets",
                    subtitle = "Tap any preset to load its configuration"
                )
            }
            items(uiState.savedReports, key = { it.id }) { preset ->
                SavedReportPresetCard(
                    preset = preset,
                    onLoad = {
                        datasetType = preset.datasetType
                        storeFilter = preset.storeFilter
                        minScore = preset.minOpportunityScore.toFloat()
                        selectedColumns = preset.selectedColumns
                        onShowMessage("Loaded report preset: ${preset.title}")
                    },
                    onDelete = { onDeleteReportPreset(preset.id) }
                )
            }
        }
    }

    if (showSaveDialog) {
        var title by remember { mutableStateOf("PlayPulse $datasetType Report") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Report Preset") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Report Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSaveReportPreset(
                                title,
                                datasetType,
                                storeFilter,
                                minScore.roundToInt().toLong(),
                                selectedColumns
                            )
                            showSaveDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SavedReportPresetCard(
    preset: SavedReportConfig,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricPill(label = preset.datasetType, color = MaterialTheme.colorScheme.primary)
                    MetricPill(label = preset.storeFilter, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            TextButton(onClick = onLoad) { Text("Load") }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete preset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
