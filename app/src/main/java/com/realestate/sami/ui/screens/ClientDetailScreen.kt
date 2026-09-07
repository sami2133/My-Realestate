package com.realestate.sami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ClientStatus
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.ui.screens.common.*
import com.realestate.sami.ui.viewmodel.ClientDetailViewModel
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toTomanDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    onBack: () -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ClientDetailViewModel = hiltViewModel()
) {
    val client by viewModel.client.collectAsState()
    val matchingProperties by viewModel.matchingProperties.collectAsState()
    val contactLogs by viewModel.contactLogs.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_client_confirm_title)) },
            text = { Text(stringResource(R.string.delete_client_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete(onDeleted)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client?.fullName ?: stringResource(R.string.client_detail_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    client?.let { current ->
                        IconButton(onClick = { onEdit(current.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
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

            SectionCard(title = stringResource(R.string.client_detail_contact_section)) {
                InfoRowClient(stringResource(R.string.label_name), current.fullName)
                InfoRowClient(stringResource(R.string.label_registered_at), current.createdAt.toPersianDateString())
                Spacer(Modifier.height(8.dp))
                PhoneActionRow(current.phone)
            }

            SectionCard(title = stringResource(R.string.client_detail_criteria_section)) {
                InfoRowClient(stringResource(R.string.client_detail_region), current.desiredRegion)
                if (current.minArea != null || current.maxArea != null) {
                    InfoRowClient(
                        stringResource(R.string.label_area),
                        stringResource(R.string.client_detail_area_range, current.minArea?.toInt()?.toString() ?: "-", current.maxArea?.toInt()?.toString() ?: "-")
                    )
                }
                if (current.minRooms != null) InfoRowClient(stringResource(R.string.client_detail_min_rooms), "${current.minRooms}")
                current.maxTotalPrice?.let { InfoRowClient(stringResource(R.string.client_detail_budget_total), it.toTomanDisplay()) }
                current.maxDepositPrice?.let { InfoRowClient(stringResource(R.string.client_detail_budget_deposit), it.toTomanDisplay()) }
                current.maxRentPrice?.let { InfoRowClient(stringResource(R.string.client_detail_budget_rent), it.toTomanDisplay()) }
            }

            SectionCard(title = stringResource(R.string.client_detail_status_section)) {
                StatusSelectorClient(current.status, viewModel::updateStatus)
            }

            SectionCard(title = stringResource(R.string.client_detail_matches_section, matchingProperties.size)) {
                if (matchingProperties.isEmpty()) {
                    Text(stringResource(R.string.client_detail_no_matches), style = MaterialTheme.typography.bodySmall)
                } else {
                    matchingProperties.forEach { prop ->
                        ListItem(
                            headlineContent = { Text(prop.address) },
                            supportingContent = { Text(stringResource(R.string.property_row_area_rooms, prop.area.toInt(), prop.rooms)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSelectorClient(current: ClientStatus, onSelect: (ClientStatus) -> Unit) {
    val labels = mapOf(
        ClientStatus.SEARCHING to stringResource(R.string.client_status_searching),
        ClientStatus.PAUSED to stringResource(R.string.client_status_paused),
        ClientStatus.MATCHED to stringResource(R.string.client_status_matched),
        ClientStatus.CLOSED to stringResource(R.string.client_status_closed)
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
