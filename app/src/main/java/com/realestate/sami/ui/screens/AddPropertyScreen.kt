package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.viewmodel.PropertyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    onSaved: () -> Unit,
    onPickLocationOnMap: () -> Unit,
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

    Scaffold(topBar = { TopAppBar(title = { Text("ثبت ملک جدید") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("اطلاعات معرف / مالک", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(ownerName, { ownerName = it }, label = { Text("نام معرف/مالک") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ownerPhone, { ownerPhone = it }, label = { Text("شماره تماس") }, modifier = Modifier.fillMaxWidth())

            Divider()
            Text("مشخصات ملک", style = MaterialTheme.typography.titleMedium)

            DropdownSelector(
                label = "نوع ملک",
                options = PropertyType.entries.toList(),
                selected = propertyType,
                onSelect = { propertyType = it },
                display = { it.toPersianLabel() }
            )
            DropdownSelector(
                label = "نوع معامله",
                options = DealType.entries.toList(),
                selected = dealType,
                onSelect = { dealType = it },
                display = { it.toPersianLabel() }
            )

            OutlinedTextField(address, { address = it }, label = { Text("آدرس") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onPickLocationOnMap, modifier = Modifier.fillMaxWidth()) {
                Text("انتخاب موقعیت روی نقشه")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(area, { area = it }, label = { Text("متراژ (متر)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(rooms, { rooms = it }, label = { Text("تعداد اتاق") }, modifier = Modifier.weight(1f))
            }

            Text("امکانات", style = MaterialTheme.typography.titleSmall)
            CheckboxRow("پارکینگ", hasParking) { hasParking = it }
            CheckboxRow("انباری", hasStorage) { hasStorage = it }
            CheckboxRow("آسانسور", hasElevator) { hasElevator = it }

            Divider()
            Text("قیمت‌گذاری", style = MaterialTheme.typography.titleMedium)
            when (dealType) {
                DealType.SALE -> OutlinedTextField(totalPrice, { totalPrice = it }, label = { Text("قیمت کل (تومان)") }, modifier = Modifier.fillMaxWidth())
                DealType.RENT, DealType.MORTGAGE -> {
                    OutlinedTextField(depositPrice, { depositPrice = it }, label = { Text("مبلغ ودیعه/رهن") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(rentPrice, { rentPrice = it }, label = { Text("اجاره ماهانه") }, modifier = Modifier.fillMaxWidth())
                }
                DealType.EXCHANGE -> OutlinedTextField(totalPrice, { totalPrice = it }, label = { Text("ارزش تقریبی") }, modifier = Modifier.fillMaxWidth())
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
                        area = area.toDoubleOrNull() ?: 0.0,
                        rooms = rooms.toIntOrNull() ?: 0,
                        hasParking = hasParking,
                        hasStorage = hasStorage,
                        hasElevator = hasElevator,
                        totalPrice = totalPrice.toLongOrNull(),
                        depositPrice = depositPrice.toLongOrNull(),
                        rentPrice = rentPrice.toLongOrNull()
                    )
                    viewModel.save(entity) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ownerName.isNotBlank() && ownerPhone.isNotBlank() && address.isNotBlank()
            ) {
                Text("ذخیره ملک")
            }
        }
    }
}

@Composable
fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    display: (T) -> String
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
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

fun PropertyType.toPersianLabel(): String = when (this) {
    PropertyType.APARTMENT -> "آپارتمان"
    PropertyType.VILLA -> "ویلایی"
    PropertyType.LAND -> "زمین"
    PropertyType.COMMERCIAL -> "تجاری"
    PropertyType.OFFICE -> "اداری"
}

fun DealType.toPersianLabel(): String = when (this) {
    DealType.SALE -> "خرید و فروش"
    DealType.RENT -> "اجاره"
    DealType.MORTGAGE -> "رهن کامل"
    DealType.EXCHANGE -> "مبادله"
}
