package com.realestate.sami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.ui.viewmodel.ClientSortOption
import com.realestate.sami.ui.viewmodel.ClientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    onAddClick: () -> Unit,
    onClientClick: (ClientEntity) -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.client_list_title)) },
                actions = {
                    ClientSortMenu(current = sortOption, onSelect = viewModel::onSortOptionChanged)
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.nav_add_client)) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchChanged,
                label = { Text(stringResource(R.string.client_list_search_hint)) },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            if (clients.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.client_list_empty))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(clients, key = { it.id }) { client ->
                        ListItem(
                            headlineContent = { Text(client.fullName) },
                            supportingContent = {
                                Text("${client.desiredRegion} • ${client.desiredPropertyType.toPersianLabel()} • ${client.desiredDealType.toPersianLabel()}")
                            },
                            trailingContent = { Text(client.phone) },
                            modifier = Modifier.fillMaxWidth().clickable { onClientClick(client) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientSortMenu(current: ClientSortOption, onSelect: (ClientSortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        ClientSortOption.NEWEST to stringResource(R.string.sort_newest),
        ClientSortOption.OLDEST to stringResource(R.string.sort_oldest),
        ClientSortOption.NAME_A_TO_Z to stringResource(R.string.client_sort_name_a_to_z),
        ClientSortOption.BUDGET_HIGH_TO_LOW to stringResource(R.string.client_sort_budget_high_to_low)
    )
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.action_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = { if (option == current) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}
