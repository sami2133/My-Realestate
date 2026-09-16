package com.realestate.sami.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.viewmodel.ReportsViewModel
import com.realestate.sami.util.PropertyExporter
import com.realestate.sami.util.parseTomanInput
import com.realestate.sami.util.toGroupedDigitsDisplay
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toTomanDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onOpenSettings: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val upcomingVisits by viewModel.upcomingVisits.collectAsState()
    val properties by viewModel.allProperties.collectAsState()

    // نرخ کمیسیون فقط از صفحه‌ی تنظیمات ویرایش می‌شه؛ هر بار کاربر به این تب برمی‌گرده، آخرین
    // مقدار ذخیره‌شده دوباره خونده می‌شه تا اگه تازه از تنظیمات عوضش کرده، همینجا هم به‌روز باشه.
    LaunchedEffect(Unit) { viewModel.refreshCommissionPercent() }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                title = { Text(stringResource(R.string.nav_reports), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
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
                Text(stringResource(R.string.reports_estimated_commission), style = MaterialTheme.typography.labelLarge)
                Text(
                    stats.estimatedCommission.toTomanDisplay(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_open_action))
                }
            }

            SectionCard(title = stringResource(R.string.commission_calculator_section)) {
                Text(
                    stringResource(R.string.commission_calculator_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                var calculatorInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = calculatorInput,
                    onValueChange = { calculatorInput = it.toGroupedDigitsDisplay() },
                    label = { Text(stringResource(R.string.commission_calculator_amount_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val amount = calculatorInput.parseTomanInput()
                if (amount != null && amount > 0L) {
                    val result = viewModel.calculateLiveSaleCommission(amount)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LiveCalcRow(stringResource(R.string.commission_result_total), result.total.toTomanDisplay(), emphasize = true)
                        LiveCalcRow(stringResource(R.string.commission_result_buyer), result.partyAShare.toTomanDisplay())
                        LiveCalcRow(stringResource(R.string.commission_result_seller), result.partyBShare.toTomanDisplay())
                    }
                }
            }

            SectionCard(title = stringResource(R.string.commission_calculator_rent_section)) {
                Text(
                    stringResource(R.string.commission_calculator_rent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                var depositInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = depositInput,
                    onValueChange = { depositInput = it.toGroupedDigitsDisplay() },
                    label = { Text(stringResource(R.string.commission_calculator_deposit_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                var monthlyRentInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = monthlyRentInput,
                    onValueChange = { monthlyRentInput = it.toGroupedDigitsDisplay() },
                    label = { Text(stringResource(R.string.commission_calculator_monthly_rent_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val deposit = depositInput.parseTomanInput() ?: 0L
                val monthlyRent = monthlyRentInput.parseTomanInput() ?: 0L
                if (deposit > 0L || monthlyRent > 0L) {
                    val result = viewModel.calculateLiveRentCommission(deposit, monthlyRent)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LiveCalcRow(stringResource(R.string.commission_result_total), result.total.toTomanDisplay(), emphasize = true)
                        LiveCalcRow(stringResource(R.string.commission_result_tenant), result.partyAShare.toTomanDisplay())
                        LiveCalcRow(stringResource(R.string.commission_result_landlord), result.partyBShare.toTomanDisplay())
                    }
                }
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

                Divider(modifier = Modifier.padding(vertical = 14.dp))

                Text(stringResource(R.string.backup_restore_section), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.backup_restore_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                BackupRestoreButtons(viewModel = viewModel)
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

/**
 * دو دکمه‌ی «تهیه پشتیبان» و «بازیابی از پشتیبان». بازیابی چون داده‌ی فعلی رو کامل جایگزین
 * می‌کنه، قبلش یک دیالوگ تأیید نشون داده می‌شه؛ بعد از بازیابیِ موفق، اپ خودکار دوباره باز می‌شه
 * (چون Room باید با فایل دیتابیس جدید از نو ساخته بشه).
 */
@Composable
private fun BackupRestoreButtons(viewModel: ReportsViewModel) {
    val context = LocalContext.current
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestoreUri = uri }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = {
                val backupFile = viewModel.createBackup(context)
                if (backupFile != null) {
                    viewModel.shareBackup(context, backupFile)
                } else {
                    Toast.makeText(context, context.getString(R.string.backup_create_failed), Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.backup_create_action))
        }
        OutlinedButton(
            onClick = { restoreLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.backup_restore_action))
        }
    }

    val uriToRestore = pendingRestoreUri
    if (uriToRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val success = viewModel.restoreBackup(context, uriToRestore)
                    pendingRestoreUri = null
                    if (success) {
                        viewModel.restartApp(context)
                    } else {
                        Toast.makeText(context, context.getString(R.string.backup_restore_failed), Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text(stringResource(R.string.backup_restore_action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun LiveCalcRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
