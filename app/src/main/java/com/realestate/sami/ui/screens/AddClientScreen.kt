package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.viewmodel.ClientViewModel
import com.realestate.sami.util.parseTomanInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseIntInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(
    onSaved: () -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
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

    Scaffold(topBar = { TopAppBar(title = { Text("ثبت متقاضی جدید") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("اطلاعات متقاضی", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(fullName, { fullName = it }, label = { Text("نام و نام خانوادگی") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس") }, modifier = Modifier.fillMaxWidth())

            Divider()
            Text("مشخصات ملک مورد نظر", style = MaterialTheme.typography.titleMedium)
            DropdownSelector("نوع ملک", PropertyType.entries.toList(), propertyType, { propertyType = it }) { it.toPersianLabel() }
            DropdownSelector("نوع معامله", DealType.entries.toList(), dealType, { dealType = it }) { it.toPersianLabel() }
            OutlinedTextField(region, { region = it }, label = { Text("منطقه/محله مورد نظر") }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(minArea, { minArea = it }, label = { Text("حداقل متراژ") }, modifier = Modifier.weight(1f))
                OutlinedTextField(maxArea, { maxArea = it }, label = { Text("حداکثر متراژ") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(minRooms, { minRooms = it }, label = { Text("حداقل تعداد اتاق") }, modifier = Modifier.fillMaxWidth())

            CheckboxRow("نیاز به پارکینگ دارد", needsParking) { needsParking = it }
            CheckboxRow("نیاز به آسانسور دارد", needsElevator) { needsElevator = it }

            Divider()
            Text("سقف بودجه", style = MaterialTheme.typography.titleMedium)
            when (dealType) {
                DealType.SALE, DealType.EXCHANGE ->
                    OutlinedTextField(maxTotalPrice, { maxTotalPrice = it }, label = { Text("حداکثر قیمت کل") }, modifier = Modifier.fillMaxWidth())
                DealType.RENT, DealType.MORTGAGE -> {
                    OutlinedTextField(maxDepositPrice, { maxDepositPrice = it }, label = { Text("حداکثر ودیعه/رهن") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(maxRentPrice, { maxRentPrice = it }, label = { Text("حداکثر اجاره ماهانه") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val entity = ClientEntity(
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
                Text("ذخیره متقاضی")
            }
        }
    }
}
