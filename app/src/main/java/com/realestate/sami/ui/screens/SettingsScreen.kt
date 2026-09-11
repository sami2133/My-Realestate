package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.viewmodel.SettingsViewModel
import com.realestate.sami.util.toEnglishDigits
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
