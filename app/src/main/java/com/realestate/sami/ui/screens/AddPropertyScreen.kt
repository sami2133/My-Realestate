package com.realestate.sami.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.util.parseTomanInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseIntInput

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
    var imageUris by remember { mutableStateOf(listOf<String>()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> imageUris = imageUris + uris.map { it.toString() } }

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
            Text("عکس‌های ملک", style = MaterialTheme.typography.titleMedium)
            PhotoPickerRow(
                imageUris = imageUris,
                onAddClick = { imagePicker.launch("image/*") },
                onRemove = { uri -> imageUris = imageUris - uri }
            )

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
                Text("ذخیره ملک")
            }
        }
    }
}

@Composable
fun PhotoPickerRow(
    imageUris: List<String>,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = "افزودن عکس", tint = MaterialTheme.colorScheme.primary)
            }
        }
        items(imageUris) { uri ->
            Box(modifier = Modifier.size(90.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable { onRemove(uri) }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "حذف عکس", tint = Color.White, modifier = Modifier.size(16.dp))
                }
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
