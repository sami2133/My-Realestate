package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.viewmodel.SettingsViewModel
import com.realestate.sami.util.CommissionCalculationMode
import com.realestate.sami.util.toEnglishDigits
import com.realestate.sami.util.toGroupedDigitsDisplay
import com.realestate.sami.util.toPlainPercentString

/**
 * فاز ۵.۳ — صفحه‌ی سراسری «تنظیمات». همه‌ی تنظیمات قابل‌شخصی‌سازی اپ (نرخ کمیسیون، نرخ تبدیل
 * رهن↔اجاره، و هر چیزی که بعداً لازم بشه) اینجا جمع شدن، به‌جای پخش‌شدن لای صفحه‌های دیگه.
 *
 * برای افزودن یک تنظیم جدید در آینده: یک فیلد جدید به [SettingsViewModel] اضافه کن (به همون
 * الگوی commissionPercent/rentConversionPercent)، بعد یک SectionCard جدید همینجا اضافه کن —
 * نیازی به تغییر جای دیگه‌ای نیست.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val commissionPercent by viewModel.commissionPercent.collectAsState()
    val rentConversionPercent by viewModel.rentConversionPercent.collectAsState()

    var commissionInput by remember(commissionPercent) { mutableStateOf(commissionPercent.toPlainPercentString()) }
    var rentConversionInput by remember(rentConversionPercent) { mutableStateOf(rentConversionPercent.toPlainPercentString()) }
    var commissionSaved by remember { mutableStateOf(false) }
    var rentConversionSaved by remember { mutableStateOf(false) }

    // ===== فاز ۶ — نرخ‌نامه‌ی پلکانی فروش + نرخ حق‌العمل اجاره =====
    val saleThreshold1 by viewModel.saleThreshold1.collectAsState()
    val saleThreshold2 by viewModel.saleThreshold2.collectAsState()
    val saleThreshold3 by viewModel.saleThreshold3.collectAsState()
    val saleRate1 by viewModel.saleRate1.collectAsState()
    val saleRate2 by viewModel.saleRate2.collectAsState()
    val saleRate3 by viewModel.saleRate3.collectAsState()
    val saleRate4 by viewModel.saleRate4.collectAsState()
    val rentCommissionPercent by viewModel.rentCommissionPercent.collectAsState()
    val calculationMode by viewModel.calculationMode.collectAsState()

    var t1Input by remember(saleThreshold1) { mutableStateOf(saleThreshold1.toString().toGroupedDigitsDisplay()) }
    var t2Input by remember(saleThreshold2) { mutableStateOf(saleThreshold2.toString().toGroupedDigitsDisplay()) }
    var t3Input by remember(saleThreshold3) { mutableStateOf(saleThreshold3.toString().toGroupedDigitsDisplay()) }
    var r1Input by remember(saleRate1) { mutableStateOf(saleRate1.toPlainPercentString()) }
    var r2Input by remember(saleRate2) { mutableStateOf(saleRate2.toPlainPercentString()) }
    var r3Input by remember(saleRate3) { mutableStateOf(saleRate3.toPlainPercentString()) }
    var r4Input by remember(saleRate4) { mutableStateOf(saleRate4.toPlainPercentString()) }
    var rentCommissionInput by remember(rentCommissionPercent) { mutableStateOf(rentCommissionPercent.toPlainPercentString()) }
    var tariffSaved by remember { mutableStateOf(false) }
    var rentCommissionSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard(title = stringResource(R.string.reports_rent_conversion_section)) {
                Text(
                    stringResource(R.string.reports_rent_conversion_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rentConversionInput,
                    onValueChange = { rentConversionInput = it; rentConversionSaved = false },
                    label = { Text(stringResource(R.string.reports_rent_conversion_percent_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (rentConversionSaved) {
                        Text(
                            stringResource(R.string.settings_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(Modifier)
                    }
                    Button(onClick = {
                        rentConversionInput.toEnglishDigits().toFloatOrNull()?.let {
                            viewModel.setRentConversionPercent(it)
                            rentConversionSaved = true
                        }
                    }) { Text(stringResource(R.string.action_submit)) }
                }
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
                    onValueChange = { commissionInput = it; commissionSaved = false },
                    label = { Text(stringResource(R.string.reports_commission_percent_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (commissionSaved) {
                        Text(
                            stringResource(R.string.settings_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(Modifier)
                    }
                    Button(onClick = {
                        commissionInput.toEnglishDigits().toFloatOrNull()?.let {
                            viewModel.setCommissionPercent(it)
                            commissionSaved = true
                        }
                    }) { Text(stringResource(R.string.action_submit)) }
                }
            }

            SectionCard(title = stringResource(R.string.commission_tariff_section)) {
                Text(
                    stringResource(R.string.commission_tariff_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.commission_calculation_mode_label), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Column(Modifier.selectableGroup()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = calculationMode == CommissionCalculationMode.FLAT,
                                onClick = { viewModel.setCalculationMode(CommissionCalculationMode.FLAT) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = calculationMode == CommissionCalculationMode.FLAT,
                            onClick = null
                        )
                        Text(stringResource(R.string.commission_calculation_mode_flat), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = calculationMode == CommissionCalculationMode.MARGINAL,
                                onClick = { viewModel.setCalculationMode(CommissionCalculationMode.MARGINAL) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = calculationMode == CommissionCalculationMode.MARGINAL,
                            onClick = null
                        )
                        Text(stringResource(R.string.commission_calculation_mode_marginal), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = t1Input,
                    onValueChange = { t1Input = it.toGroupedDigitsDisplay(); tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_threshold_1_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = r1Input,
                    onValueChange = { r1Input = it; tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_rate_1_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = t2Input,
                    onValueChange = { t2Input = it.toGroupedDigitsDisplay(); tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_threshold_2_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = r2Input,
                    onValueChange = { r2Input = it; tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_rate_2_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = t3Input,
                    onValueChange = { t3Input = it.toGroupedDigitsDisplay(); tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_threshold_3_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = r3Input,
                    onValueChange = { r3Input = it; tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_rate_3_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = r4Input,
                    onValueChange = { r4Input = it; tariffSaved = false },
                    label = { Text(stringResource(R.string.commission_tariff_rate_4_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (tariffSaved) {
                        Text(
                            stringResource(R.string.settings_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(Modifier)
                    }
                    Button(onClick = {
                        val t1 = t1Input.toEnglishDigits().filter { it.isDigit() }.toLongOrNull()
                        val t2 = t2Input.toEnglishDigits().filter { it.isDigit() }.toLongOrNull()
                        val t3 = t3Input.toEnglishDigits().filter { it.isDigit() }.toLongOrNull()
                        val r1 = r1Input.toEnglishDigits().toFloatOrNull()
                        val r2 = r2Input.toEnglishDigits().toFloatOrNull()
                        val r3 = r3Input.toEnglishDigits().toFloatOrNull()
                        val r4 = r4Input.toEnglishDigits().toFloatOrNull()
                        if (t1 != null && t2 != null && t3 != null && r1 != null && r2 != null && r3 != null && r4 != null) {
                            viewModel.setSaleThreshold1(t1)
                            viewModel.setSaleThreshold2(t2)
                            viewModel.setSaleThreshold3(t3)
                            viewModel.setSaleRate1(r1)
                            viewModel.setSaleRate2(r2)
                            viewModel.setSaleRate3(r3)
                            viewModel.setSaleRate4(r4)
                            tariffSaved = true
                        }
                    }) { Text(stringResource(R.string.action_submit)) }
                }
            }

            SectionCard(title = stringResource(R.string.commission_rent_section)) {
                Text(
                    stringResource(R.string.commission_rent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rentCommissionInput,
                    onValueChange = { rentCommissionInput = it; rentCommissionSaved = false },
                    label = { Text(stringResource(R.string.commission_rent_percent_label)) },
                    singleLine = true,
                    trailingIcon = { Text("٪") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (rentCommissionSaved) {
                        Text(
                            stringResource(R.string.settings_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(Modifier)
                    }
                    Button(onClick = {
                        rentCommissionInput.toEnglishDigits().toFloatOrNull()?.let {
                            viewModel.setRentCommissionPercent(it)
                            rentCommissionSaved = true
                        }
                    }) { Text(stringResource(R.string.action_submit)) }
                }
            }

            // فاز ۵.۳: جای رزرو برای تنظیمات آینده — هر بخش جدید همینجا به‌عنوان یک SectionCard دیگه اضافه می‌شه.
            Text(
                stringResource(R.string.settings_more_soon_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
