package com.realestate.sami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.data.local.entity.ClientStatus
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.ui.screens.common.*
import com.realestate.sami.ui.viewmodel.ClientDetailViewModel
import com.realestate.sami.util.toTomanDisplay
import com.realestate.sami.util.toPersianDateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val client by viewModel.client.collectAsState()
    val matchingProperties by viewModel.matchingProperties.collectAsState()
    val contactLogs by viewModel.contactLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.fullName ?: "جزئیات متقاضی") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        val current = client ?: return@Scaffold
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DealTypeChip(current.desiredDealType, current.desiredDealType.toPersianLabel())
                Text(current.desiredPropertyType.toPersianLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard(title = "اطلاعات تماس") {
                InfoRowClient("نام", current.fullName)
                InfoRowClient("تاریخ ثبت", current.createdAt.toPersianDateString())
                Spacer(Modifier.height(8.dp))
                PhoneActionRow(current.phone)
            }

            SectionCard(title = "معیارهای جستجو") {
                InfoRowClient("منطقه مورد نظر", current.desiredRegion)
                if (current.minArea != null || current.maxArea != null) {
                    InfoRowClient("متراژ", "${current.minArea?.toInt() ?: "-"} تا ${current.maxArea?.toInt() ?: "-"} متر")
                }
                if (current.minRooms != null) InfoRowClient("حداقل اتاق", "${current.minRooms}")
                current.maxTotalPrice?.let { InfoRowClient("سقف قیمت", it.toTomanDisplay()) }
                current.maxDepositPrice?.let { InfoRowClient("سقف ودیعه", it.toTomanDisplay()) }
                current.maxRentPrice?.let { InfoRowClient("سقف اجاره ماهانه", it.toTomanDisplay()) }
            }

            SectionCard(title = "وضعیت پیگیری") {
                StatusSelectorClient(current.status, viewModel::updateStatus)
            }

            SectionCard(title = "ملک‌های سازگار (${matchingProperties.size})") {
                if (matchingProperties.isEmpty()) {
                    Text("در حال حاضر ملک سازگاری پیدا نشد.", style = MaterialTheme.typography.bodySmall)
                } else {
                    matchingProperties.forEach { prop ->
                        ListItem(
                            headlineContent = { Text(prop.address) },
                            supportingContent = { Text("${prop.area.toInt()} متر • ${prop.rooms} خواب") },
                            trailingContent = {
                                Text(prop.totalPrice?.toTomanDisplay() ?: prop.rentPrice?.toTomanDisplay().orEmpty())
                            },
                            modifier = Modifier.clickable { onPropertyClick(prop) }
                        )
                        Divider()
                    }
                }
            }

            SectionCard {
                ContactLogSection(logs = contactLogs, onAddLog = { viewModel.addContactLog(it, null) })
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InfoRowClient(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusSelectorClient(current: ClientStatus, onSelect: (ClientStatus) -> Unit) {
    val labels = mapOf(
        ClientStatus.SEARCHING to "در حال جستجو",
        ClientStatus.PAUSED to "متوقف‌شده",
        ClientStatus.MATCHED to "تطبیق یافته",
        ClientStatus.CLOSED to "بسته‌شده"
    )
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels.entries.toList()) { (status, label) ->
            FilterChip(
                selected = status == current,
                onClick = { onSelect(status) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}
