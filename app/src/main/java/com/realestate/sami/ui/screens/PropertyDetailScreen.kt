package com.realestate.sami.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import com.realestate.sami.ui.screens.common.*
import com.realestate.sami.ui.viewmodel.PropertyDetailViewModel
import com.realestate.sami.util.toTomanDisplay
import com.realestate.sami.util.toPersianDateString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    onBack: () -> Unit,
    onClientClick: (ClientEntity) -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel()
) {
    val property by viewModel.property.collectAsState()
    val matchingClients by viewModel.matchingClients.collectAsState()
    val contactLogs by viewModel.contactLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(property?.address ?: "جزئیات ملک", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        val current = property ?: return@Scaffold
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // گالری عکس
            val images = current.imageUris.split(",").filter { it.isNotBlank() }
            if (images.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(220.dp, 150.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        )
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
            }

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // خلاصه بالای صفحه
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DealTypeChip(current.dealType, current.dealType.toPersianLabel())
                    Text(current.propertyType.toPersianLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    when {
                        current.totalPrice != null -> current.totalPrice.toTomanDisplay()
                        current.rentPrice != null -> "رهن ${current.depositPrice?.toTomanDisplay() ?: "-"} / اجاره ${current.rentPrice.toTomanDisplay()}"
                        else -> "قیمت ثبت نشده"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                SectionCard(title = "مشخصات") {
                    InfoRow("متراژ", "${current.area.toInt()} متر مربع")
                    InfoRow("تعداد اتاق", "${current.rooms}")
                    InfoRow("پارکینگ", if (current.hasParking) "دارد" else "ندارد")
                    InfoRow("انباری", if (current.hasStorage) "دارد" else "ندارد")
                    InfoRow("آسانسور", if (current.hasElevator) "دارد" else "ندارد")
                    InfoRow("آدرس", current.address)
                    InfoRow("تاریخ ثبت", current.createdAt.toPersianDateString())
                }

                SectionCard(title = "معرف / مالک") {
                    InfoRow("نام", current.ownerName)
                    Spacer(Modifier.height(8.dp))
                    PhoneActionRow(current.ownerPhone)
                }

                SectionCard(title = "وضعیت ملک") {
                    StatusSelector(
                        current = current.status,
                        onSelect = viewModel::updateStatus
                    )
                }

                SectionCard(title = "متقاضیان سازگار (${matchingClients.size})") {
                    if (matchingClients.isEmpty()) {
                        Text("در حال حاضر متقاضی سازگاری پیدا نشد.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        matchingClients.forEach { client ->
                            ListItem(
                                headlineContent = { Text(client.fullName) },
                                supportingContent = { Text(client.desiredRegion) },
                                trailingContent = { Text(client.phone) },
                                modifier = Modifier.clickable { onClientClick(client) }
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
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusSelector(current: PropertyStatus, onSelect: (PropertyStatus) -> Unit) {
    val labels = mapOf(
        PropertyStatus.AVAILABLE to "قابل معرفی",
        PropertyStatus.RESERVED to "رزرو شده",
        PropertyStatus.SOLD_OR_RENTED to "فروخته/اجاره شده",
        PropertyStatus.ARCHIVED to "بایگانی"
    )
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels.entries.toList()) { (status, label) ->
            val selected = status == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(status) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}
