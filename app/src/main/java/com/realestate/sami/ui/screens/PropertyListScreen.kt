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
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.ui.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    onAddClick: () -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val properties by viewModel.properties.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("ملک‌های ثبت‌شده") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "افزودن ملک")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchChanged,
                label = { Text("جستجو بر اساس آدرس یا نام مالک") },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            if (properties.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هنوز ملکی ثبت نشده است.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(properties, key = { it.id }) { property ->
                        PropertyRow(property, onClick = { onPropertyClick(property) })
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(property: PropertyEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(property.address) },
        supportingContent = {
            Text("${property.propertyType} • ${property.dealType} • ${property.area} متر • ${property.rooms} خواب")
        },
        trailingContent = { Text(property.ownerPhone) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}
