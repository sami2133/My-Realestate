package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.viewmodel.ReportsViewModel
import com.realestate.sami.util.PropertyExporter
import com.realestate.sami.util.toEnglishDigits
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toPlainPercentString
import com.realestate.sami.util.toTomanDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val commissionPercent by viewModel.commissionPercent.collectAsState()
    val rentConversionPercent by viewModel.rentConversionPercent.collectAsState()
    val upcomingVisits by viewModel.upcomingVisits.collectAsState()
    val properties by viewModel.allProperties.collectAsState()
    var commissionInput by remember(commissionPercent) { mutableStateOf(commissionPercent.toString()) }
    var rentConversionInput by remember(rentConversionPercent) { mutableStateOf(rentConversionPercent.toPlainPercentString()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_reports), style = MaterialTheme.typography.titleLarge) })
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = stringResource(R.string.reports_total_properties),
                    value = stats.totalProperties.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.reports_available_properties),
                    value = stats.availableProperties.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = stringResource(R.string.reports_closed_deals),
                    value = stats.closedDeals.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.reports_active_clients),
                    value = stats.activeClients.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            SectionCard(title = stringResource(R.string.reports_commission_section)) {
                Text(
                    stringResource(R.string.reports_commission_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = { commissionInput = it },
                    label = { Text(stringResource(R.string.reports_commission_percent_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        commissionInput.toEnglishDigits().toFloatOrNull()?.let { viewModel.setCommissionPercent(it) }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.action_submit)) }

                Divider(Modifier.padding(vertical = 12.dp))
                Text(stringResource(R.string.reports_estimated_commission), style = MaterialTheme.typography.labelLarge)
                Text(
                    stats.estimatedCommission.toTomanDisplay(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SectionCard(title = stringResource(R.string.reports_rent_conversion_section)) {
                Text(
                    stringResource(R.string.reports_rent_conversion_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rentConversionInput,
                    onValueChange = { rentConversionInput = it },
                    label = { Text(stringResource(R.string.reports_rent_conversion_percent_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        rentConversionInput.toEnglishDigits().toFloatOrNull()?.let {
                            viewModel.setRentConversionPercent(it)
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.action_submit)) }
            }

            SectionCard(title = stringResource(R.string.reports_upcoming_visits_section, stats.upcomingVisits)) {
                if (upcomingVisits.isEmpty()) {
                    Text(stringResource(R.string.reports_no_upcoming_visits), style = MaterialTheme.typography.bodySmall)
                } else {
                    upcomingVisits.take(5).forEach { visit ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.reports_visit_row, visit.propertyId, visit.clientId), style = MaterialTheme.typography.bodyMedium)
                            Text(visit.visitDate.toPersianDateString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider()
                    }
                }
            }

            SectionCard(title = stringResource(R.string.reports_export_section)) {
                Text(
                    stringResource(R.string.reports_export_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            PropertyExporter.exportToPdf(context, properties)?.let {
                                PropertyExporter.shareFile(context, it, "application/pdf")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.reports_export_pdf))
                    }
                    OutlinedButton(
                        onClick = {
                            PropertyExporter.exportToExcel(context, properties)?.let {
                                PropertyExporter.shareFile(
                                    context, it,
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.reports_export_excel))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
