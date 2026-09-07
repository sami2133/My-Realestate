package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.viewmodel.ClientViewModel
import com.realestate.sami.util.parseIntInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseTomanInput

/**
 * فرم ثبت/ویرایش متقاضی. وقتی [clientId] مقدار داشته باشد، صفحه در حالت ویرایش باز می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(
    clientId: Long? = null,
    onSaved: () -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    val isEditMode = clientId != null
    val existingClient by viewModel.editingClient.collectAsState()

    LaunchedEffect(clientId) {
        if (clientId != null) viewModel.loadForEdit(clientId) else viewModel.clearEditing()
    }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var minArea by remember { mutableStateOf("") }
    var maxArea by remember { mutableStateOf("") }
    var minRooms by remember { mutableStateOf("") }
    var maxTotalPrice by remember { mutableStateOf("") }
    var maxDepositPrice by remember { mutableStateOf("") }
    var maxRentPrice by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf(PropertyType.APARTMENT) }
    var dealType by remember { mutableStateOf(DealType.SALE) }
    var needsParking by remember { mutableStateOf(false) }
    var needsElevator by remember { mutableStateOf(false) }

    LaunchedEffect(existingClient) {
        existingClient?.let { c ->
            fullName = c.fullName
            phone = c.phone
            region = c.desiredRegion
            minArea = c.minArea?.toPlainInputString() ?: ""
            maxArea = c.maxArea?.toPlainInputString() ?: ""
            minRooms = c.minRooms?.toString() ?: ""
            maxTotalPrice = c.maxTotalPrice?.toString() ?: ""
            maxDepositPrice = c.maxDepositPrice?.toString() ?: ""
            maxRentPrice = c.maxRentPrice?.toString() ?: ""
            propertyType = c.desiredPropertyType
            dealType = c.desiredDealType
            needsParking = c.needsParking
            needsElevator = c.needsElevator
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) stringResource(R.string.edit_client_title)
                        else stringResource(R.string.add_client_title)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.add_client_info_section), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(fullName, { fullName = it }, label = { Text(stringResource(R.string.add_client_full_name)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text(stringResource(R.string.label_phone)) }, modifier = Modifier.fillMaxWidth())

            Divider()
            Text(stringResource(R.string.add_client_desired_section), style = MaterialTheme.typography.titleMedium)
            DropdownSelector(stringResource(R.string.add_property_type_label), PropertyType.entries.toList(), propertyType, { propertyType = it }) { it.toPersianLabel() }
            DropdownSelector(stringResource(R.string.add_property_deal_type_label), DealType.entries.toList(), dealType, { dealType = it }) { it.toPersianLabel() }
            OutlinedTextField(region, { region = it }, label = { Text(stringResource(R.string.add_client_region)) }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(minArea, { minArea = it }, label = { Text(stringResource(R.string.add_client_min_area)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(maxArea, { maxArea = it }, label = { Text(stringResource(R.string.add_client_max_area)) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(minRooms, { minRooms = it }, label = { Text(stringResource(R.string.add_client_min_rooms)) }, modifier = Modifier.fillMaxWidth())

            CheckboxRow(stringResource(R.string.add_client_needs_parking), needsParking) { needsParking = it }
            CheckboxRow(stringResource(R.string.add_client_needs_elevator), needsElevator) { needsElevator = it }

            Divider()
            Text(stringResource(R.string.add_client_budget_section), style = MaterialTheme.typography.titleMedium)
            when (dealType) {
                DealType.SALE, DealType.EXCHANGE ->
                    OutlinedTextField(maxTotalPrice, { maxTotalPrice = it }, label = { Text(stringResource(R.string.add_client_max_total_price)) }, modifier = Modifier.fillMaxWidth())
                DealType.RENT, DealType.MORTGAGE -> {
                    OutlinedTextField(maxDepositPrice, { maxDepositPrice = it }, label = { Text(stringResource(R.string.add_client_max_deposit)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(maxRentPrice, { maxRentPrice = it }, label = { Text(stringResource(R.string.add_client_max_rent)) }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val base = existingClient ?: ClientEntity(
                        fullName = "",
                        phone = "",
                        desiredPropertyType = PropertyType.APARTMENT,
                        desiredDealType = DealType.SALE,
                        desiredRegion = ""
                    )
                    val entity = base.copy(
                        fullName = fullName,
                        phone = phone,
                        desiredPropertyType = propertyType,
                        desiredDealType = dealType,
                        desiredRegion = region,
                        minArea = minArea.parseNumberInput(),
                        maxArea = maxArea.parseNumberInput(),
                        minRooms = minRooms.parseIntInput(),
                        maxTotalPrice = maxTotalPrice.parseTomanInput(),
                        maxDepositPrice = maxDepositPrice.parseTomanInput(),
                        maxRentPrice = maxRentPrice.parseTomanInput(),
                        needsParking = needsParking,
                        needsElevator = needsElevator
                    )
                    viewModel.save(entity) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = fullName.isNotBlank() && phone.isNotBlank()
            ) {
                Text(
                    if (isEditMode) stringResource(R.string.edit_client_save)
                    else stringResource(R.string.add_client_save)
                )
            }
        }
    }
}

private fun Double.toPlainInputString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
