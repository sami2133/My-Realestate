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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyImage
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.ui.screens.common.RentDepositAdjustmentSlider
import com.realestate.sami.util.copyPickedImageToAppStorage
import com.realestate.sami.util.parseIntInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseTomanInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * فرم ثبت/ویرایش ملک. وقتی [propertyId] مقدار داشته باشد، صفحه در حالت ویرایش باز می‌شود:
 * رکورد موجود از دیتابیس خوانده و فرم با مقادیرش پر می‌شود؛ در غیر این صورت فرم برای ثبت ملک جدید خالی است.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPropertyScreen(
    propertyId: Long? = null,
    onSaved: () -> Unit,
    onPickLocationOnMap: () -> Unit,
    pickedLatitude: Double? = null,
    pickedLongitude: Double? = null,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val isEditMode = propertyId != null
    val existingProperty by viewModel.editingProperty.collectAsState()

    LaunchedEffect(propertyId) {
        if (propertyId != null) viewModel.loadForEdit(propertyId) else viewModel.clearEditing()
    }

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
    var images by remember { mutableStateOf(listOf<PropertyImage>()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDepositNegotiable by remember { mutableStateOf(false) }
    var minAdjustableDeposit by remember { mutableStateOf("") }
    var isExchangeable by remember { mutableStateOf(false) }
    var exchangePreferredType by remember { mutableStateOf<PropertyType?>(null) }
    var exchangeNote by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    // به‌محض بارگذاری رکورد موجود (حالت ویرایش)، فرم را یک‌بار با مقادیرش پر کن
    LaunchedEffect(existingProperty) {
        existingProperty?.let { p ->
            ownerName = p.ownerName
            ownerPhone = p.ownerPhone
            address = p.address
            area = p.area.toPlainInputString()
            rooms = p.rooms.toString()
            totalPrice = p.totalPrice?.toString() ?: ""
            depositPrice = p.depositPrice?.toString() ?: ""
            rentPrice = p.rentPrice?.toString() ?: ""
            propertyType = p.propertyType
            // رکوردهای محلی قدیمی‌تر ممکنه هنوز dealType=MORTGAGE یا EXCHANGE داشته باشن (قبل از
            // migration)؛ چون دیگه به‌صورت جدا قابل‌انتخاب نیستن، همینجا معادلشون نشون داده می‌شه.
            dealType = when (p.dealType) {
                DealType.MORTGAGE -> DealType.RENT
                DealType.EXCHANGE -> DealType.SALE
                else -> p.dealType
            }
            hasParking = p.hasParking
            hasStorage = p.hasStorage
            hasElevator = p.hasElevator
            images = p.images
            latitude = p.latitude
            longitude = p.longitude
            isDepositNegotiable = p.minAdjustableDeposit != null
            minAdjustableDeposit = p.minAdjustableDeposit?.toString() ?: ""
            isExchangeable = p.isExchangeable || p.dealType == DealType.EXCHANGE
            exchangePreferredType = p.exchangePreferredType
            exchangeNote = p.exchangeNote ?: ""
        }
    }

    // موقعیتی که کاربر به‌تازگی از روی نقشه انتخاب کرده جایگزین موقعیت قبلی می‌شود
    LaunchedEffect(pickedLatitude, pickedLongitude) {
        if (pickedLatitude != null && pickedLongitude != null) {
            latitude = pickedLatitude
            longitude = pickedLongitude
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        // بایت‌های هر عکس بلافاصله به حافظه‌ی دائمی خودِ اپ کپی می‌شن، چون URIای که Photo Picker
        // برمی‌گردونه فقط موقتیه و بعد از مدتی (وقتی اپ از حافظه پاک بشه) دیگه قابل خوندن نیست.
        scope.launch(Dispatchers.IO) {
            val copied = uris.mapNotNull { uri -> context.copyPickedImageToAppStorage(uri) }
                .map { PropertyImage(localUri = it) }
            withContext(Dispatchers.Main) {
                images = images + copied
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) stringResource(R.string.edit_property_title)
                        else stringResource(R.string.add_property_title)
                    )
                }
            )
        }
    ) { padding ->
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
                images = images,
                onAddClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemove = { image -> images = images - image }
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
                options = DealType.entries.filterNot { it == DealType.MORTGAGE || it == DealType.EXCHANGE },
                selected = dealType,
                onSelect = { dealType = it },
                display = { it.toPersianLabel() }
            )

            OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.label_address)) }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onPickLocationOnMap, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (latitude != null) stringResource(R.string.add_property_location_selected)
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
                DealType.SALE, DealType.EXCHANGE -> {
                    OutlinedTextField(totalPrice, { totalPrice = it }, label = { Text(stringResource(R.string.add_property_total_price)) }, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isExchangeable, onCheckedChange = { isExchangeable = it })
                        Text(stringResource(R.string.add_property_exchangeable), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (isExchangeable) {
                        DropdownSelector(
                            label = stringResource(R.string.add_property_exchange_preferred_type),
                            options = listOf<PropertyType?>(null) + PropertyType.entries.toList(),
                            selected = exchangePreferredType,
                            onSelect = { exchangePreferredType = it },
                            display = { it?.toPersianLabel() ?: stringResource(R.string.add_property_exchange_any_type) }
                        )
                        OutlinedTextField(
                            exchangeNote,
                            { exchangeNote = it },
                            label = { Text(stringResource(R.string.add_property_exchange_note)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                DealType.RENT, DealType.MORTGAGE -> {
                    OutlinedTextField(depositPrice, { depositPrice = it }, label = { Text(stringResource(R.string.add_property_deposit_price)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(rentPrice, { rentPrice = it }, label = { Text(stringResource(R.string.add_property_rent_price)) }, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDepositNegotiable, onCheckedChange = { isDepositNegotiable = it })
                        Text(stringResource(R.string.add_property_deposit_negotiable), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (isDepositNegotiable) {
                        OutlinedTextField(
                            minAdjustableDeposit,
                            { minAdjustableDeposit = it },
                            label = { Text(stringResource(R.string.add_property_min_deposit)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val depositLong = depositPrice.parseTomanInput()
                        val minLong = minAdjustableDeposit.parseTomanInput()
                        if (depositLong != null && minLong != null && minLong < depositLong) {
                            Spacer(Modifier.height(10.dp))
                            val conversionPercent by viewModel.rentConversionPercent.collectAsState()
                            RentDepositAdjustmentSlider(
                                baseDeposit = depositLong,
                                baseRent = rentPrice.parseTomanInput() ?: 0L,
                                minDeposit = minLong,
                                conversionPercent = conversionPercent
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    // در حالت ویرایش، رکورد موجود را با مقادیر جدید copy می‌کنیم تا id و فیلدهای
                    // بدون UI (وضعیت، تاریخ ثبت، پرچم‌های sync و ...) دست‌نخورده باقی بمانند.
                    val base = existingProperty ?: PropertyEntity(
                        ownerName = "",
                        ownerPhone = "",
                        propertyType = PropertyType.APARTMENT,
                        dealType = DealType.SALE,
                        address = "",
                        area = 0.0,
                        rooms = 0
                    )
                    val entity = base.copy(
                        ownerName = ownerName,
                        ownerPhone = ownerPhone,
                        propertyType = propertyType,
                        dealType = dealType,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        area = area.parseNumberInput() ?: 0.0,
                        rooms = rooms.parseIntInput() ?: 0,
                        hasParking = hasParking,
                        hasStorage = hasStorage,
                        hasElevator = hasElevator,
                        totalPrice = totalPrice.parseTomanInput(),
                        depositPrice = depositPrice.parseTomanInput(),
                        rentPrice = rentPrice.parseTomanInput(),
                        minAdjustableDeposit = if (isDepositNegotiable) minAdjustableDeposit.parseTomanInput() else null,
                        isExchangeable = dealType == DealType.SALE && isExchangeable,
                        exchangePreferredType = if (dealType == DealType.SALE && isExchangeable) exchangePreferredType else null,
                        exchangeNote = if (dealType == DealType.SALE && isExchangeable) exchangeNote.ifBlank { null } else null,
                        images = images
                    )
                    viewModel.save(entity) { onSaved() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ownerName.isNotBlank() && ownerPhone.isNotBlank() && address.isNotBlank()
            ) {
                Text(
                    if (isEditMode) stringResource(R.string.edit_property_save)
                    else stringResource(R.string.add_property_save)
                )
            }
        }
    }
}

/** تبدیل متراژ به رشته‌ی قابل‌ویرایش در فیلد فرم، بدون ".0" اضافه برای اعداد صحیح. */
private fun Double.toPlainInputString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

@Composable
private fun PhotoPickerRow(
    images: List<PropertyImage>,
    onAddClick: () -> Unit,
    onRemove: (PropertyImage) -> Unit
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
        items(images) { image ->
            Box(Modifier.size(90.dp)) {
                if (image.localUri != null) {
                    AsyncImage(
                        model = image.localUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    )
                } else {
                    // این عکس روی یک دستگاه دیگه‌ی تیم اضافه شده و هنوز روی این دستگاه دانلود نشده
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CloudDownload,
                            contentDescription = stringResource(R.string.image_not_downloaded_yet),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { onRemove(image) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_photo), tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * منوی کشویی عمومی برای انتخاب یک مقدار از بین چند گزینه (مثل نوع ملک/نوع معامله).
 *
 * نکته‌ی مهم پیاده‌سازی: بدون Modifier.menuAnchor() روی TextField داخلِ
 * ExposedDropdownMenuBox، منو به فیلد "لنگر" نمی‌شود و در برخی دستگاه‌ها/نسخه‌ها
 * اصلاً باز نمی‌شود یا در جای اشتباه رندر می‌شود — همان باگ «منو نمایش داده نمی‌شود».
 */
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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
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
