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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import com.realestate.sami.ui.screens.common.*
import com.realestate.sami.ui.viewmodel.PropertyDetailViewModel
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toTomanDisplay

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
                title = { Text(property?.address ?: stringResource(R.string.property_detail_title_fallback), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                        current.rentPrice != null -> stringResource(
                            R.string.property_detail_deposit_rent_format,
                            current.depositPrice?.toTomanDisplay() ?: "-",
                            current.rentPrice.toTomanDisplay()
                        )
                        else -> stringResource(R.string.price_not_set)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                SectionCard(title = stringResource(R.string.property_detail_specs_section)) {
                    InfoRow(stringResource(R.string.label_area), stringResource(R.string.property_detail_area_value, current.area.toInt()))
                    InfoRow(stringResource(R.string.label_rooms), "${current.rooms}")
                    InfoRow(stringResource(R.string.amenity_parking), if (current.hasParking) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.amenity_storage), if (current.hasStorage) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.amenity_elevator), if (current.hasElevator) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.label_address), current.address)
                    InfoRow(stringResource(R.string.label_registered_at), current.createdAt.toPersianDateString())
                }

                SectionCard(title = stringResource(R.string.property_detail_owner_section)) {
                    InfoRow(stringResource(R.string.label_name), current.ownerName)
                    Spacer(Modifier.height(8.dp))
                    PhoneActionRow(current.ownerPhone)
                }

                SectionCard(title = stringResource(R.string.property_detail_status_section)) {
                    StatusSelector(
                        current = current.status,
                        onSelect = viewModel::updateStatus
                    )
                }

                SectionCard(title = stringResource(R.string.property_detail_matches_section, matchingClients.size)) {
                    if (matchingClients.isEmpty()) {
                        Text(stringResource(R.string.property_detail_no_matches), style = MaterialTheme.typography.bodySmall)
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
        PropertyStatus.AVAILABLE to stringResource(R.string.property_status_available),
        PropertyStatus.RESERVED to stringResource(R.string.property_status_reserved),
        PropertyStatus.SOLD_OR_RENTED to stringResource(R.string.property_status_sold),
        PropertyStatus.ARCHIVED to stringResource(R.string.property_status_archived)
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
