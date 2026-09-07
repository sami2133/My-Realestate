package com.realestate.sami.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.util.parseIntInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseTomanInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    onSaved: () -> Unit,
    onPickLocationOnMap: () -> Unit,
    pickedLatitude: Double? = null,
    pickedLongitude: Double? = null,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var rooms by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var depositPrice by remember { mutableStateOf("") }
    var rentPrice by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf(PropertyType.APARTMENT) }
    var dealType by remember { mutableStateOf(DealType.SALE) }
    var hasParking by remember { mutableStateOf(false) }
    var hasStorage by remember { mutableStateOf(false) }
    var hasElevator by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf(listOf<String>()) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> imageUris = imageUris + uris.map { it.toString() } }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.add_property_title)) }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.add_property_owner_section), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(ownerName, { ownerName = it }, label = { Text(stringResource(R.string.add_property_owner_name)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ownerPhone, { ownerPhone = it }, label = { Text(stringResource(R.string.label_phone)) }, modifier = Modifier.fillMaxWidth())

            Divider()
            Text(stringResource(R.string.add_property_photos_section), style = MaterialTheme.typography.titleMedium)
            PhotoPickerRow(
                imageUris = imageUris,
                onAddClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { uri -> imageUris = imageUris - uri }
            )

            Divider()
            Text(stringResource(R.string.add_property_specs_section), style = MaterialTheme.typography.titleMedium)

            DropdownSelector(
                label = stringResource(R.string.add_property_type_label),
                options = PropertyType.entries.toList(),
                selected = propertyType,
                onSelect = { propertyType = it },
                display = { it.toPersianLabel() }
            )
            DropdownSelector(
                label = stringResource(R.string.add_property_deal_type_label),
                options = DealType.entries.toList(),
                selected = dealType,
                onSelect = { dealType = it },
                display = { it.toPersianLabel() }
            )

            OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.label_address)) }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onPickLocationOnMap, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (pickedLatitude != null) stringResource(R.string.add_property_location_selected)
                    else stringResource(R.string.add_property_pick_location)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(area, { area = it }, label = { Text(stringResource(R.string.add_property_area_hint)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(rooms, { rooms = it }, label = { Text(stringResource(R.string.label_rooms)) }, modifier = Modifier.weight(1f))
            }

            Text(stringResource(R.string.add_property_amenities_section), style = MaterialTheme.typography.titleSmall)
            CheckboxRow(stringResource(R.string.amenity_parking), hasParking) { hasParking = it }
            CheckboxRow(stringResource(R.string.amenity_storage), hasStorage) { hasStorage = it }
            CheckboxRow(stringResource(R.string.amenity_elevator), hasElevator) { hasElevator = it }

            Divider()
            Text(stringResource(R.string.add_property_pricing_section), style = MaterialTheme.typography.titleMedium)
            when (dealType) {
                DealType.SALE -> OutlinedTextField(totalPrice, { totalPrice = it }, label = { Text(stringResource(R.string.add_property_total_price)) }, modifier = Modifier.fillMaxWidth())
                DealType.RENT, DealType.MORTGAGE -> {
                    OutlinedTextField(depositPrice, { depositPrice = it }, label = { Text(stringResource(R.string.add_property_deposit_price)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(rentPrice, { rentPrice = it }, label = { Text(stringResource(R.string.add_property_rent_price)) }, modifier = Modifier.fillMaxWidth())
                }
                DealType.EXCHANGE -> OutlinedTextField(totalPrice, { totalPrice = it }, label = { Text(stringResource(R.string.add_property_exchange_value)) }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val entity = PropertyEntity(
                        ownerName = ownerName,
                        ownerPhone = ownerPhone,
                        propertyType = propertyType,
                        dealType = dealType,
                        address = address,
                        latitude = pickedLatitude,
                        longitude = pickedLongitude,
                        area = area.parseNumberInput() ?: 0.0,
                        rooms = rooms.parseIntInput() ?: 0,
                        hasParking = hasParking,
                        hasStorage = hasStorage,
                        hasElevator = hasElevator,
                        totalPrice = totalPrice.parseTomanInput(),
                        depositPrice = depositPrice.parseTomanInput(),
                        rentPrice = rentPrice.parseTomanInput(),
                        imageUris = imageUris.joinToString(",")
                    )
                    viewModel.save(entity) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ownerName.isNotBlank() && ownerPhone.isNotBlank() && address.isNotBlank()
            ) {
                Text(stringResource(R.string.add_property_save))
            }
        }
    }
}

@Composable
private fun PhotoPickerRow(
    imageUris: List<String>,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(
                Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.cd_add_photo), tint = MaterialTheme.colorScheme.primary)
            }
        }
        items(imageUris) { uri ->
            Box(Modifier.size(90.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { onRemove(uri) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_photo), tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    display: @Composable (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = display(selected),
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(display(option)) }, onClick = {
                    onSelect(option); expanded = false
                })
            }
        }
    }
}

@Composable
fun CheckboxRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
fun PropertyType.toPersianLabel(): String = when (this) {
    PropertyType.APARTMENT -> stringResource(R.string.property_type_apartment)
    PropertyType.VILLA -> stringResource(R.string.property_type_villa)
    PropertyType.LAND -> stringResource(R.string.property_type_land)
    PropertyType.COMMERCIAL -> stringResource(R.string.property_type_commercial)
    PropertyType.OFFICE -> stringResource(R.string.property_type_office)
}

@Composable
fun DealType.toPersianLabel(): String = when (this) {
    DealType.SALE -> stringResource(R.string.deal_type_sale)
    DealType.RENT -> stringResource(R.string.deal_type_rent)
    DealType.MORTGAGE -> stringResource(R.string.deal_type_mortgage)
    DealType.EXCHANGE -> stringResource(R.string.deal_type_exchange)
}
