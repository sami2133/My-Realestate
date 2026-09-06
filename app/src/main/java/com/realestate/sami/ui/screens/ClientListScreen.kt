package com.realestate.sami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.data.local.entity.ClientEntity
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("متقاضیان") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن متقاضی")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchChanged,
                label = { Text("جستجو بر اساس نام یا منطقه") },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            if (clients.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هنوز متقاضی‌ای ثبت نشده است.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(clients, key = { it.id }) { client ->
                        ListItem(
                            headlineContent = { Text(client.fullName) },
                            supportingContent = {
                                Text("${client.desiredRegion} • ${client.desiredPropertyType} • ${client.desiredDealType}")
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
