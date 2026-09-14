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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.*
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.ui.screens.common.AmenityChip
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.screens.common.FormSubsectionLabel
import com.realestate.sami.ui.screens.common.LabeledField
import com.realestate.sami.ui.screens.common.PriceField
import com.realestate.sami.ui.screens.common.RentDepositAdjustmentSlider
import com.realestate.sami.ui.screens.common.SwitchRow
import com.realestate.sami.ui.screens.common.icon
import com.realestate.sami.util.copyPickedImageToAppStorage
import com.realestate.sami.util.parseIntInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseTomanInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * فرم ثبت/ویرایش ملک — فاز ۶ (بازطراحی کامل مطابق فرم‌های جدید آپارتمان/ویلایی/تجاری/زمین).
 *
 * ترتیب بخش‌ها همیشه ثابت است، مستقل از نوع ملک:
 * ۱) اطلاعات معرف/مالک   ۲) عکس‌های ملک   ۳) مشخصات پایه (نوع ملک/معامله/آدرس/متراژ/اطاق)
 * ۴) مشخصات تکمیلی بسته به نوع ملک   ۵) توضیحات تکمیلی   ۶) قیمت‌گذاری
 *
 * نوع ملک «اداری» دیگر وجود ندارد — به‌عنوان یکی از گزینه‌های «کاربرد» زیر «تجاری» ادغام شده.
 * فیلدهای عددی (∆) با [parseNumberInput]/[parseIntInput]/[parseTomanInput] پردازش می‌شوند که
 * ارقام فارسی و انگلیسی هر دو را می‌پذیرند و همیشه به یک فرمت یکسان (انگلیسی خام) در دیتابیس
 * ذخیره می‌کنند — یعنی جست‌وجو/فیلتر روی این مقادیر مستقل از فرمت ورودی کار می‌کند.
 * فیلدهای قیمت‌گذاری از [PriceField] استفاده می‌کنند که هنگام تایپ جداکننده‌ی سه‌رقمی نشان می‌دهد.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // ===== ۱) معرف/مالک =====
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }

    // ===== ۲) عکس‌ها =====
    var images by remember { mutableStateOf(listOf<PropertyImage>()) }

    // ===== ۳) مشخصات پایه =====
    var propertyType by remember { mutableStateOf(PropertyType.APARTMENT) }
    var dealType by remember { mutableStateOf(DealType.SALE) }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var area by remember { mutableStateOf("") }           // مساحت بنا (یا مساحت‌کل برای زمین)
    var totalArea by remember { mutableStateOf("") }      // مساحت‌کل — فقط ویلایی
    var balconyArea by remember { mutableStateOf("") }    // مساحت بالکن — فقط تجاری
    var rooms by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var totalFloors by remember { mutableStateOf("") }
    var unitsPerFloor by remember { mutableStateOf("") }
    var buildingAge by remember { mutableStateOf("") }

    // ===== ۴) مشخصات تکمیلی =====
    var deedType by remember { mutableStateOf<DeedType?>(null) }
    var buildingClass by remember { mutableStateOf<BuildingClass?>(null) }

    // آشپزخانه — آپارتمان/ویلایی
    var cabinetMaterial by remember { mutableStateOf<CabinetMaterial?>(null) }
    var hasKitchenIsland by remember { mutableStateOf(false) }
    var hasKitchenette by remember { mutableStateOf(false) }
    var hasBarbecue by remember { mutableStateOf(false) }

    // کف‌پوش/دیوارپوش/سقف‌پوش — آپارتمان/ویلایی/تجاری
    var flooring by remember { mutableStateOf<FlooringType?>(null) }
    var wallCovering by remember { mutableStateOf<WallCovering?>(null) }
    var ceilingCovering by remember { mutableStateOf<CeilingCovering?>(null) }

    // سرویس بهداشتی — آپارتمان/ویلایی
    var hasIranianToilet by remember { mutableStateOf(false) }
    var hasWesternToilet by remember { mutableStateOf(false) }
    var hasJacuzzi by remember { mutableStateOf(false) }

    // سرمایش/گرمایش — آپارتمان/ویلایی/تجاری
    var coolingSystem by remember { mutableStateOf<CoolingSystem?>(null) }
    var heatingSystem by remember { mutableStateOf<HeatingSystem?>(null) }

    // امکانات آپارتمان/ویلایی/تجاری
    var hasStorage by remember { mutableStateOf(false) }
    var hasElevator by remember { mutableStateOf(false) }
    var hasParking by remember { mutableStateOf(false) }
    var hasPrivateParkingPath by remember { mutableStateOf(false) }
    var hasSharedParkingPath by remember { mutableStateOf(false) }
    var hasAutomaticParkingDoor by remember { mutableStateOf(false) }
    var hasPrivateWater by remember { mutableStateOf(false) }
    var hasSharedWater by remember { mutableStateOf(false) }
    var hasPrivateElectricity by remember { mutableStateOf(false) }
    var hasSharedElectricity by remember { mutableStateOf(false) }
    var hasPrivateGas by remember { mutableStateOf(false) }
    var hasSharedGas by remember { mutableStateOf(false) }
    var hasBuiltInCloset by remember { mutableStateOf(false) }
    var hasVideoIntercom by remember { mutableStateOf(false) }

    // امکانات ساختمان — عمدتاً آپارتمان
    var hasLobby by remember { mutableStateOf(false) }
    var hasSecurityGuard by remember { mutableStateOf(false) }
    var hasPool by remember { mutableStateOf(false) }
    var hasGym by remember { mutableStateOf(false) }
    var hasCourtyard by remember { mutableStateOf(false) }

    // ویلایی
    var yardArea by remember { mutableStateOf("") }
    var terraceArea by remember { mutableStateOf("") }
    var masterBedroomCount by remember { mutableStateOf("") }
    var hasCaretaker by remember { mutableStateOf(false) }

    // زمین
    var landUse by remember { mutableStateOf<LandUse?>(null) }
    var frontageWidth by remember { mutableStateOf("") }
    var streetWidth by remember { mutableStateOf("") }
    var buildingPermitArea by remember { mutableStateOf("") }
    var landPosition by remember { mutableStateOf<LandPosition?>(null) }
    var landSlope by remember { mutableStateOf<LandSlope?>(null) }
    var hasWall by remember { mutableStateOf(false) }
    var waterRightOwned by remember { mutableStateOf(false) }
    var waterRightObtainable by remember { mutableStateOf(false) }
    var electricityRightOwned by remember { mutableStateOf(false) }
    var electricityRightObtainable by remember { mutableStateOf(false) }
    var gasRightOwned by remember { mutableStateOf(false) }
    var gasRightObtainable by remember { mutableStateOf(false) }

    // تجاری (شامل اداری سابق)
    var commercialUsage by remember { mutableStateOf<CommercialUsage?>(null) }
    var commercialFloorPosition by remember { mutableStateOf<CommercialPosition?>(null) }
    var streetPosition by remember { mutableStateOf<StreetPosition?>(null) }
    var ceilingHeight by remember { mutableStateOf("") }
    var hasThreePhaseElectricity by remember { mutableStateOf(false) }
    var hasKitchen by remember { mutableStateOf(false) }
    var hasRestroom by remember { mutableStateOf(false) }

    // ===== ۵) توضیحات =====
    var description by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    // ===== ۶) قیمت‌گذاری =====
    var totalPrice by remember { mutableStateOf("") }
    var depositPrice by remember { mutableStateOf("") }
    var rentPrice by remember { mutableStateOf("") }
    var isExchangeable by remember { mutableStateOf(false) }
    var exchangePreferredType by remember { mutableStateOf<PropertyType?>(null) }
    var exchangeNote by remember { mutableStateOf("") }
    var isDepositNegotiable by remember { mutableStateOf(false) }
    var minAdjustableDeposit by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // به‌محض بارگذاری رکورد موجود (حالت ویرایش)، فرم را یک‌بار با مقادیرش پر کن
    LaunchedEffect(existingProperty) {
        existingProperty?.let { p ->
            ownerName = p.ownerName
            ownerPhone = p.ownerPhone
            images = p.images

            propertyType = p.propertyType
            dealType = when (p.dealType) {
                DealType.MORTGAGE -> DealType.RENT
                DealType.EXCHANGE -> DealType.SALE
                else -> p.dealType
            }
            address = p.address
            latitude = p.latitude
            longitude = p.longitude
            area = p.area.toPlainInputString()
            totalArea = p.totalArea?.toPlainInputString() ?: ""
            balconyArea = p.balconyArea?.toPlainInputString() ?: ""
            rooms = p.rooms?.toString() ?: ""
            floor = p.floor?.toString() ?: ""
            totalFloors = p.totalFloors?.toString() ?: ""
            unitsPerFloor = p.unitsPerFloor?.toString() ?: ""
            buildingAge = p.buildingAge?.toString() ?: ""

            deedType = p.deedType
            buildingClass = p.buildingClass

            cabinetMaterial = p.cabinetMaterial
            hasKitchenIsland = p.hasKitchenIsland
            hasKitchenette = p.hasKitchenette
            hasBarbecue = p.hasBarbecue

            flooring = p.flooring
            wallCovering = p.wallCovering
            ceilingCovering = p.ceilingCovering

            hasIranianToilet = p.hasIranianToilet
            hasWesternToilet = p.hasWesternToilet
            hasJacuzzi = p.hasJacuzzi

            coolingSystem = p.coolingSystem
            heatingSystem = p.heatingSystem

            hasStorage = p.hasStorage
            hasElevator = p.hasElevator
            hasParking = p.hasParking
            hasPrivateParkingPath = p.hasPrivateParkingPath
            hasSharedParkingPath = p.hasSharedParkingPath
            hasAutomaticParkingDoor = p.hasAutomaticParkingDoor
            hasPrivateWater = p.hasPrivateWater
            hasSharedWater = p.hasSharedWater
            hasPrivateElectricity = p.hasPrivateElectricity
            hasSharedElectricity = p.hasSharedElectricity
            hasPrivateGas = p.hasPrivateGas
            hasSharedGas = p.hasSharedGas
            hasBuiltInCloset = p.hasBuiltInCloset
            hasVideoIntercom = p.hasVideoIntercom

            hasLobby = p.hasLobby
            hasSecurityGuard = p.hasSecurityGuard
            hasPool = p.hasPool
            hasGym = p.hasGym
            hasCourtyard = p.hasCourtyard

            yardArea = p.yardArea?.toPlainInputString() ?: ""
            terraceArea = p.terraceArea?.toPlainInputString() ?: ""
            masterBedroomCount = p.masterBedroomCount?.toString() ?: ""
            hasCaretaker = p.hasCaretaker

            landUse = p.landUse
            frontageWidth = p.frontageWidth?.toPlainInputString() ?: ""
            streetWidth = p.streetWidth?.toPlainInputString() ?: ""
            buildingPermitArea = p.buildingPermitArea?.toPlainInputString() ?: ""
            landPosition = p.landPosition
            landSlope = p.landSlope
            hasWall = p.hasWall
            waterRightOwned = p.waterRightOwned
            waterRightObtainable = p.waterRightObtainable
            electricityRightOwned = p.electricityRightOwned
            electricityRightObtainable = p.electricityRightObtainable
            gasRightOwned = p.gasRightOwned
            gasRightObtainable = p.gasRightObtainable

            commercialUsage = p.commercialUsage
            commercialFloorPosition = p.commercialFloorPosition
            streetPosition = p.streetPosition
            ceilingHeight = p.ceilingHeight?.toPlainInputString() ?: ""
            hasThreePhaseElectricity = p.hasThreePhaseElectricity
            hasKitchen = p.hasKitchen
            hasRestroom = p.hasRestroom

            description = p.description ?: ""
            additionalNotes = p.additionalNotes ?: ""

            totalPrice = p.totalPrice?.toString() ?: ""
            depositPrice = p.depositPrice?.toString() ?: ""
            rentPrice = p.rentPrice?.toString() ?: ""
            isExchangeable = p.isExchangeable || p.dealType == DealType.EXCHANGE
            exchangePreferredType = p.exchangePreferredType
            exchangeNote = p.exchangeNote ?: ""
            isDepositNegotiable = p.minAdjustableDeposit != null
            minAdjustableDeposit = p.minAdjustableDeposit?.toString() ?: ""
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== ۱) اطلاعات معرف/مالک =====
            SectionCard(title = stringResource(R.string.add_property_owner_section), icon = Icons.Filled.Person) {
                LabeledField(ownerName, { ownerName = it }, stringResource(R.string.add_property_owner_name), icon = Icons.Filled.Person)
                LabeledField(ownerPhone, { ownerPhone = it }, stringResource(R.string.label_phone), icon = Icons.Filled.Call, keyboardType = KeyboardType.Phone)
            }

            // ===== ۲) عکس‌های ملک =====
            SectionCard(title = stringResource(R.string.add_property_photos_section), icon = Icons.Filled.PhotoLibrary) {
                PhotoPickerRow(
                    images = images,
                    onAddClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = { image -> images = images - image }
                )
            }

            // ===== ۳) مشخصات پایه =====
            SectionCard(title = stringResource(R.string.add_property_specs_section), icon = propertyType.icon()) {
                DropdownSelector(
                    label = stringResource(R.string.add_property_type_label),
                    options = PropertyType.entries.toList(),
                    selected = propertyType,
                    onSelect = { propertyType = it },
                    icon = propertyType.icon(),
                    display = { it.toPersianLabel() }
                )
                DropdownSelector(
                    label = stringResource(R.string.add_property_deal_type_label),
                    options = DealType.entries.filterNot { it == DealType.MORTGAGE || it == DealType.EXCHANGE },
                    selected = dealType,
                    onSelect = { dealType = it },
                    icon = dealType.icon(),
                    display = { it.toPersianLabel() }
                )

                LabeledField(address, { address = it }, stringResource(R.string.label_address), icon = Icons.Filled.LocationOn)
                OutlinedButton(onClick = onPickLocationOnMap, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (latitude != null) stringResource(R.string.add_property_location_selected)
                        else stringResource(R.string.add_property_pick_location)
                    )
                }

                // مساحت — برچسب و تعداد فیلد بسته به نوع ملک فرق می‌کند
                when (propertyType) {
                    PropertyType.VILLA -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField(totalArea, { totalArea = it }, stringResource(R.string.label_area_total), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        LabeledField(area, { area = it }, stringResource(R.string.label_area_built), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                    PropertyType.COMMERCIAL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField(area, { area = it }, stringResource(R.string.label_area_built), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        LabeledField(balconyArea, { balconyArea = it }, stringResource(R.string.label_area_balcony), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                    PropertyType.LAND -> LabeledField(area, { area = it }, stringResource(R.string.label_area_total), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number)
                    PropertyType.APARTMENT -> LabeledField(area, { area = it }, stringResource(R.string.label_area_built), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number)
                }

                if (propertyType != PropertyType.LAND) {
                    LabeledField(rooms, { rooms = it }, stringResource(R.string.label_rooms), icon = Icons.Filled.MeetingRoom, keyboardType = KeyboardType.Number)
                }

                // طبقات — فقط آپارتمان/ویلایی/تجاری، با فیلدهای متفاوت
                when (propertyType) {
                    PropertyType.APARTMENT -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField(floor, { floor = it }, stringResource(R.string.label_floor), icon = Icons.Filled.Stairs, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        LabeledField(totalFloors, { totalFloors = it }, stringResource(R.string.label_total_floors), icon = Icons.Filled.Layers, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        LabeledField(unitsPerFloor, { unitsPerFloor = it }, stringResource(R.string.label_units_per_floor), icon = Icons.Filled.ViewColumn, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                    PropertyType.COMMERCIAL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField(floor, { floor = it }, stringResource(R.string.label_floor), icon = Icons.Filled.Stairs, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        LabeledField(totalFloors, { totalFloors = it }, stringResource(R.string.label_total_floors), icon = Icons.Filled.Layers, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                    PropertyType.VILLA -> LabeledField(totalFloors, { totalFloors = it }, stringResource(R.string.label_total_floors), icon = Icons.Filled.Layers, keyboardType = KeyboardType.Number)
                    PropertyType.LAND -> Unit
                }

                if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA) {
                    LabeledField(buildingAge, { buildingAge = it }, stringResource(R.string.label_building_age), icon = Icons.Filled.Apartment, keyboardType = KeyboardType.Number)
                }
            }

            // ===== ۴) مشخصات تکمیلی بسته به نوع ملک =====
            SectionCard(title = stringResource(R.string.spec_section_common), icon = Icons.Filled.Tune) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val deedOptions = if (propertyType == PropertyType.LAND) {
                        DeedType.entries.filterNot { it == DeedType.UNDER_CONSTRUCTION }
                    } else DeedType.entries.toList()
                    NullableEnumDropdown(stringResource(R.string.label_deed_type), deedOptions, deedType, { deedType = it }, icon = Icons.Filled.Verified) { it.toPersianLabel() }

                    if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA || propertyType == PropertyType.COMMERCIAL) {
                        NullableEnumDropdown(stringResource(R.string.label_building_class), BuildingClass.entries.toList(), buildingClass, { buildingClass = it }) { it.toPersianLabel() }
                    }

                    when (propertyType) {
                        PropertyType.APARTMENT, PropertyType.VILLA -> {
                            // آشپزخانه
                            FormSubsectionLabel(stringResource(R.string.spec_section_kitchen))
                            NullableEnumDropdown(stringResource(R.string.label_cabinet_material), CabinetMaterial.entries.toList(), cabinetMaterial, { cabinetMaterial = it }, icon = Icons.Filled.Kitchen) { it.toPersianLabel() }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AmenityChip(stringResource(R.string.amenity_kitchen_island), Icons.Filled.Kitchen, hasKitchenIsland) { hasKitchenIsland = it }
                                AmenityChip(stringResource(R.string.amenity_kitchenette), Icons.Filled.Kitchen, hasKitchenette) { hasKitchenette = it }
                                AmenityChip(stringResource(R.string.amenity_barbecue), Icons.Filled.OutdoorGrill, hasBarbecue) { hasBarbecue = it }
                            }

                            // کف‌پوش/دیوارپوش/سقف‌پوش
                            FormSubsectionLabel(stringResource(R.string.spec_section_finishes))
                            NullableEnumDropdown(stringResource(R.string.label_flooring), FlooringType.entries.filterNot { it == FlooringType.CONCRETE }, flooring, { flooring = it }) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_wall_covering), WallCovering.entries.filterNot { it == WallCovering.CERAMIC || it == WallCovering.CONCRETE }, wallCovering, { wallCovering = it }) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_ceiling_covering), CeilingCovering.entries.filterNot { it == CeilingCovering.SUSPENDED }, ceilingCovering, { ceilingCovering = it }) { it.toPersianLabel() }

                            // سرویس بهداشتی
                            FormSubsectionLabel(stringResource(R.string.spec_section_bathroom))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AmenityChip(stringResource(R.string.amenity_iranian_toilet), Icons.Filled.Wc, hasIranianToilet) { hasIranianToilet = it }
                                AmenityChip(stringResource(R.string.amenity_western_toilet), Icons.Filled.Wc, hasWesternToilet) { hasWesternToilet = it }
                                AmenityChip(stringResource(R.string.amenity_jacuzzi), Icons.Filled.Bathtub, hasJacuzzi) { hasJacuzzi = it }
                            }

                            // سرمایش/گرمایش
                            FormSubsectionLabel(stringResource(R.string.spec_section_hvac))
                            NullableEnumDropdown(stringResource(R.string.label_cooling_system), CoolingSystem.entries.toList(), coolingSystem, { coolingSystem = it }, icon = Icons.Filled.AcUnit) { it.toPersianLabel() }
                            val heatingOptions = if (propertyType == PropertyType.VILLA) HeatingSystem.entries.filterNot { it == HeatingSystem.CENTRAL } else HeatingSystem.entries.toList()
                            NullableEnumDropdown(stringResource(R.string.label_heating_system), heatingOptions, heatingSystem, { heatingSystem = it }, icon = Icons.Filled.LocalFireDepartment) { it.toPersianLabel() }

                            if (propertyType == PropertyType.VILLA) {
                                FormSubsectionLabel(stringResource(R.string.spec_section_villa))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LabeledField(yardArea, { yardArea = it }, stringResource(R.string.label_yard_area), icon = Icons.Filled.Yard, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                                    LabeledField(terraceArea, { terraceArea = it }, stringResource(R.string.label_terrace_area), icon = Icons.Filled.Balcony, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                                }
                                LabeledField(masterBedroomCount, { masterBedroomCount = it }, stringResource(R.string.label_master_bedroom_count), icon = Icons.Filled.Bed, keyboardType = KeyboardType.Number)
                            } else {
                                LabeledField(terraceArea, { terraceArea = it }, stringResource(R.string.label_terrace_area), icon = Icons.Filled.Balcony, keyboardType = KeyboardType.Number)
                            }

                            // امکانات
                            FormSubsectionLabel(stringResource(R.string.spec_section_amenities))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AmenityChip(stringResource(R.string.amenity_storage), Icons.Filled.Inventory2, hasStorage) { hasStorage = it }
                                AmenityChip(stringResource(R.string.amenity_elevator), Icons.Filled.Elevator, hasElevator) { hasElevator = it }
                                AmenityChip(stringResource(R.string.amenity_parking), Icons.Filled.LocalParking, hasParking) { hasParking = it }
                                if (propertyType == PropertyType.APARTMENT) {
                                    AmenityChip(stringResource(R.string.amenity_private_parking_path), Icons.Filled.LocalParking, hasPrivateParkingPath) { hasPrivateParkingPath = it }
                                    AmenityChip(stringResource(R.string.amenity_shared_parking_path), Icons.Filled.LocalParking, hasSharedParkingPath) { hasSharedParkingPath = it }
                                }
                                AmenityChip(stringResource(R.string.amenity_automatic_parking_door), Icons.Filled.LocalParking, hasAutomaticParkingDoor) { hasAutomaticParkingDoor = it }
                                AmenityChip(stringResource(R.string.amenity_private_water), Icons.Filled.WaterDrop, hasPrivateWater) { hasPrivateWater = it }
                                AmenityChip(stringResource(R.string.amenity_shared_water), Icons.Filled.WaterDrop, hasSharedWater) { hasSharedWater = it }
                                AmenityChip(stringResource(R.string.amenity_private_electricity), Icons.Filled.Bolt, hasPrivateElectricity) { hasPrivateElectricity = it }
                                AmenityChip(stringResource(R.string.amenity_shared_electricity), Icons.Filled.Bolt, hasSharedElectricity) { hasSharedElectricity = it }
                                AmenityChip(stringResource(R.string.amenity_private_gas), Icons.Filled.LocalFireDepartment, hasPrivateGas) { hasPrivateGas = it }
                                AmenityChip(stringResource(R.string.amenity_shared_gas), Icons.Filled.LocalFireDepartment, hasSharedGas) { hasSharedGas = it }
                                AmenityChip(stringResource(R.string.amenity_built_in_closet), Icons.Filled.Checkroom, hasBuiltInCloset) { hasBuiltInCloset = it }
                                AmenityChip(stringResource(R.string.amenity_video_intercom), Icons.Filled.Videocam, hasVideoIntercom) { hasVideoIntercom = it }
                                if (propertyType == PropertyType.VILLA) {
                                    AmenityChip(stringResource(R.string.amenity_caretaker), Icons.Filled.Security, hasCaretaker) { hasCaretaker = it }
                                }
                            }

                            // امکانات ساختمان
                            FormSubsectionLabel(stringResource(R.string.spec_section_building_amenities))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (propertyType == PropertyType.APARTMENT) {
                                    AmenityChip(stringResource(R.string.amenity_lobby), Icons.Filled.Weekend, hasLobby) { hasLobby = it }
                                    AmenityChip(stringResource(R.string.amenity_security_guard), Icons.Filled.Security, hasSecurityGuard) { hasSecurityGuard = it }
                                }
                                AmenityChip(stringResource(R.string.amenity_pool), Icons.Filled.Pool, hasPool) { hasPool = it }
                                AmenityChip(stringResource(R.string.amenity_gym), Icons.Filled.FitnessCenter, hasGym) { hasGym = it }
                                if (propertyType == PropertyType.APARTMENT) {
                                    AmenityChip(stringResource(R.string.amenity_courtyard), Icons.Filled.Yard, hasCourtyard) { hasCourtyard = it }
                                }
                            }
                        }

                        PropertyType.LAND -> {
                            FormSubsectionLabel(stringResource(R.string.spec_section_land))
                            NullableEnumDropdown(stringResource(R.string.label_land_use), LandUse.entries.toList(), landUse, { landUse = it }) { it.toPersianLabel() }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LabeledField(frontageWidth, { frontageWidth = it }, stringResource(R.string.label_frontage_width_land), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                                LabeledField(streetWidth, { streetWidth = it }, stringResource(R.string.label_street_width), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            }
                            LabeledField(buildingPermitArea, { buildingPermitArea = it }, stringResource(R.string.label_building_permit_area), icon = Icons.Filled.Verified, keyboardType = KeyboardType.Number)
                            NullableEnumDropdown(stringResource(R.string.label_land_position), LandPosition.entries.toList(), landPosition, { landPosition = it }) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_land_slope), LandSlope.entries.toList(), landSlope, { landSlope = it }) { it.toPersianLabel() }

                            FormSubsectionLabel(stringResource(R.string.spec_section_land_rights))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AmenityChip(stringResource(R.string.amenity_water_right_owned), Icons.Filled.WaterDrop, waterRightOwned) { waterRightOwned = it }
                                AmenityChip(stringResource(R.string.amenity_water_right_obtainable), Icons.Filled.WaterDrop, waterRightObtainable) { waterRightObtainable = it }
                                AmenityChip(stringResource(R.string.amenity_electricity_right_owned), Icons.Filled.Bolt, electricityRightOwned) { electricityRightOwned = it }
                                AmenityChip(stringResource(R.string.amenity_electricity_right_obtainable), Icons.Filled.Bolt, electricityRightObtainable) { electricityRightObtainable = it }
                                AmenityChip(stringResource(R.string.amenity_gas_right_owned), Icons.Filled.LocalFireDepartment, gasRightOwned) { gasRightOwned = it }
                                AmenityChip(stringResource(R.string.amenity_gas_right_obtainable), Icons.Filled.LocalFireDepartment, gasRightObtainable) { gasRightObtainable = it }
                                AmenityChip(stringResource(R.string.amenity_wall), Icons.Filled.Fence, hasWall) { hasWall = it }
                            }
                        }

                        PropertyType.COMMERCIAL -> {
                            FormSubsectionLabel(stringResource(R.string.spec_section_commercial))
                            NullableEnumDropdown(stringResource(R.string.label_commercial_usage), CommercialUsage.entries.toList(), commercialUsage, { commercialUsage = it }, icon = Icons.Filled.Business) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_commercial_floor_position), CommercialPosition.entries.toList(), commercialFloorPosition, { commercialFloorPosition = it }) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_street_position), StreetPosition.entries.toList(), streetPosition, { streetPosition = it }) { it.toPersianLabel() }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LabeledField(frontageWidth, { frontageWidth = it }, stringResource(R.string.label_frontage_width_commercial), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                                LabeledField(ceilingHeight, { ceilingHeight = it }, stringResource(R.string.label_ceiling_height), icon = Icons.Filled.Height, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            }

                            FormSubsectionLabel(stringResource(R.string.spec_section_finishes))
                            NullableEnumDropdown(stringResource(R.string.label_flooring), FlooringType.entries.toList(), flooring, { flooring = it }) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_wall_covering), WallCovering.entries.filterNot { it == WallCovering.PAINT }, wallCovering, { wallCovering = it }) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_ceiling_covering), CeilingCovering.entries.filterNot { it == CeilingCovering.PAINT }, ceilingCovering, { ceilingCovering = it }) { it.toPersianLabel() }

                            FormSubsectionLabel(stringResource(R.string.spec_section_hvac))
                            NullableEnumDropdown(stringResource(R.string.label_cooling_system), CoolingSystem.entries.toList(), coolingSystem, { coolingSystem = it }, icon = Icons.Filled.AcUnit) { it.toPersianLabel() }
                            NullableEnumDropdown(stringResource(R.string.label_heating_system), HeatingSystem.entries.toList(), heatingSystem, { heatingSystem = it }, icon = Icons.Filled.LocalFireDepartment) { it.toPersianLabel() }

                            FormSubsectionLabel(stringResource(R.string.spec_section_amenities))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AmenityChip(stringResource(R.string.amenity_parking), Icons.Filled.LocalParking, hasParking) { hasParking = it }
                                AmenityChip(stringResource(R.string.amenity_storage), Icons.Filled.Inventory2, hasStorage) { hasStorage = it }
                                AmenityChip(stringResource(R.string.amenity_elevator), Icons.Filled.Elevator, hasElevator) { hasElevator = it }
                                AmenityChip(stringResource(R.string.amenity_three_phase_electricity), Icons.Filled.Bolt, hasThreePhaseElectricity) { hasThreePhaseElectricity = it }
                                AmenityChip(stringResource(R.string.amenity_private_water), Icons.Filled.WaterDrop, hasPrivateWater) { hasPrivateWater = it }
                                AmenityChip(stringResource(R.string.amenity_shared_water), Icons.Filled.WaterDrop, hasSharedWater) { hasSharedWater = it }
                                AmenityChip(stringResource(R.string.amenity_private_electricity), Icons.Filled.Bolt, hasPrivateElectricity) { hasPrivateElectricity = it }
                                AmenityChip(stringResource(R.string.amenity_shared_electricity), Icons.Filled.Bolt, hasSharedElectricity) { hasSharedElectricity = it }
                                AmenityChip(stringResource(R.string.amenity_private_gas), Icons.Filled.LocalFireDepartment, hasPrivateGas) { hasPrivateGas = it }
                                AmenityChip(stringResource(R.string.amenity_shared_gas), Icons.Filled.LocalFireDepartment, hasSharedGas) { hasSharedGas = it }
                                AmenityChip(stringResource(R.string.amenity_kitchen), Icons.Filled.Kitchen, hasKitchen) { hasKitchen = it }
                                AmenityChip(stringResource(R.string.amenity_restroom), Icons.Filled.Wc, hasRestroom) { hasRestroom = it }
                            }
                        }
                    }
                }
            }

            // ===== ۵) توضیحات تکمیلی =====
            SectionCard(title = stringResource(R.string.add_property_description), icon = Icons.Filled.Description) {
                LabeledField(
                    description, { description = it },
                    stringResource(R.string.add_property_description),
                    icon = Icons.Filled.Description,
                    minLines = 2
                )
                LabeledField(
                    additionalNotes, { additionalNotes = it },
                    stringResource(R.string.label_additional_notes),
                    icon = Icons.Filled.EditNote,
                    placeholder = stringResource(R.string.add_property_additional_notes_hint),
                    minLines = 2
                )
            }

            // ===== ۶) قیمت‌گذاری =====
            SectionCard(title = stringResource(R.string.add_property_pricing_section), icon = Icons.Filled.Payments) {
                when (dealType) {
                    DealType.SALE, DealType.EXCHANGE -> {
                        PriceField(totalPrice, { totalPrice = it }, stringResource(R.string.add_property_total_price), icon = Icons.Filled.Payments)

                        SwitchRow(stringResource(R.string.add_property_exchangeable), isExchangeable) { isExchangeable = it }
                        if (isExchangeable) {
                            DropdownSelector(
                                label = stringResource(R.string.add_property_exchange_preferred_type),
                                options = listOf<PropertyType?>(null) + PropertyType.entries.toList(),
                                selected = exchangePreferredType,
                                onSelect = { exchangePreferredType = it },
                                display = { it?.toPersianLabel() ?: stringResource(R.string.add_property_exchange_any_type) }
                            )
                            LabeledField(exchangeNote, { exchangeNote = it }, stringResource(R.string.add_property_exchange_note), icon = Icons.Filled.EditNote)
                        }
                    }
                    DealType.RENT, DealType.MORTGAGE -> {
                        PriceField(depositPrice, { depositPrice = it }, stringResource(R.string.add_property_deposit_price), icon = Icons.Filled.Payments)
                        PriceField(rentPrice, { rentPrice = it }, stringResource(R.string.add_property_rent_price), icon = Icons.Filled.Payments)

                        SwitchRow(stringResource(R.string.add_property_deposit_negotiable), isDepositNegotiable) { isDepositNegotiable = it }
                        if (isDepositNegotiable) {
                            PriceField(minAdjustableDeposit, { minAdjustableDeposit = it }, stringResource(R.string.add_property_min_deposit), icon = Icons.Filled.Payments)
                            val depositLong = depositPrice.parseTomanInput()
                            val minLong = minAdjustableDeposit.parseTomanInput()
                            if (depositLong != null && minLong != null && minLong < depositLong) {
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
            }

            Button(
                onClick = {
                    val base = existingProperty ?: PropertyEntity(
                        ownerName = "",
                        ownerPhone = "",
                        propertyType = PropertyType.APARTMENT,
                        dealType = DealType.SALE,
                        address = "",
                        area = 0.0
                    )
                    val isApartmentOrVilla = propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA
                    val hasFinishes = propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA || propertyType == PropertyType.COMMERCIAL
                    val entity = base.copy(
                        ownerName = ownerName,
                        ownerPhone = ownerPhone,
                        images = images,
                        propertyType = propertyType,
                        dealType = dealType,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        area = area.parseNumberInput() ?: 0.0,
                        totalArea = if (propertyType == PropertyType.VILLA) totalArea.parseNumberInput() else null,
                        balconyArea = if (propertyType == PropertyType.COMMERCIAL) balconyArea.parseNumberInput() else null,
                        rooms = if (propertyType != PropertyType.LAND) rooms.parseIntInput() else null,
                        floor = if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.COMMERCIAL) floor.parseIntInput() else null,
                        totalFloors = if (propertyType != PropertyType.LAND) totalFloors.parseIntInput() else null,
                        unitsPerFloor = if (propertyType == PropertyType.APARTMENT) unitsPerFloor.parseIntInput() else null,
                        buildingAge = if (isApartmentOrVilla) buildingAge.parseIntInput() else null,

                        deedType = deedType,
                        buildingClass = if (hasFinishes) buildingClass else null,

                        cabinetMaterial = if (isApartmentOrVilla) cabinetMaterial else null,
                        hasKitchenIsland = isApartmentOrVilla && hasKitchenIsland,
                        hasKitchenette = isApartmentOrVilla && hasKitchenette,
                        hasBarbecue = isApartmentOrVilla && hasBarbecue,

                        flooring = if (hasFinishes) flooring else null,
                        wallCovering = if (hasFinishes) wallCovering else null,
                        ceilingCovering = if (hasFinishes) ceilingCovering else null,

                        hasIranianToilet = isApartmentOrVilla && hasIranianToilet,
                        hasWesternToilet = isApartmentOrVilla && hasWesternToilet,
                        hasJacuzzi = isApartmentOrVilla && hasJacuzzi,

                        coolingSystem = if (hasFinishes) coolingSystem else null,
                        heatingSystem = if (hasFinishes) heatingSystem else null,

                        hasStorage = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasStorage,
                        hasElevator = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasElevator,
                        hasParking = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasParking,
                        hasPrivateParkingPath = propertyType == PropertyType.APARTMENT && hasPrivateParkingPath,
                        hasSharedParkingPath = propertyType == PropertyType.APARTMENT && hasSharedParkingPath,
                        hasAutomaticParkingDoor = isApartmentOrVilla && hasAutomaticParkingDoor,
                        hasPrivateWater = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasPrivateWater,
                        hasSharedWater = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasSharedWater,
                        hasPrivateElectricity = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasPrivateElectricity,
                        hasSharedElectricity = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasSharedElectricity,
                        hasPrivateGas = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasPrivateGas,
                        hasSharedGas = (isApartmentOrVilla || propertyType == PropertyType.COMMERCIAL) && hasSharedGas,
                        hasBuiltInCloset = isApartmentOrVilla && hasBuiltInCloset,
                        hasVideoIntercom = isApartmentOrVilla && hasVideoIntercom,

                        hasLobby = propertyType == PropertyType.APARTMENT && hasLobby,
                        hasSecurityGuard = propertyType == PropertyType.APARTMENT && hasSecurityGuard,
                        hasPool = isApartmentOrVilla && hasPool,
                        hasGym = isApartmentOrVilla && hasGym,
                        hasCourtyard = propertyType == PropertyType.APARTMENT && hasCourtyard,

                        yardArea = if (propertyType == PropertyType.VILLA) yardArea.parseNumberInput() else null,
                        terraceArea = if (isApartmentOrVilla) terraceArea.parseNumberInput() else null,
                        masterBedroomCount = if (propertyType == PropertyType.VILLA) masterBedroomCount.parseIntInput() else null,
                        hasCaretaker = propertyType == PropertyType.VILLA && hasCaretaker,

                        landUse = if (propertyType == PropertyType.LAND) landUse else null,
                        frontageWidth = if (propertyType == PropertyType.LAND || propertyType == PropertyType.COMMERCIAL) frontageWidth.parseNumberInput() else null,
                        streetWidth = if (propertyType == PropertyType.LAND) streetWidth.parseNumberInput() else null,
                        buildingPermitArea = if (propertyType == PropertyType.LAND) buildingPermitArea.parseNumberInput() else null,
                        landPosition = if (propertyType == PropertyType.LAND) landPosition else null,
                        landSlope = if (propertyType == PropertyType.LAND) landSlope else null,
                        hasWall = propertyType == PropertyType.LAND && hasWall,
                        waterRightOwned = propertyType == PropertyType.LAND && waterRightOwned,
                        waterRightObtainable = propertyType == PropertyType.LAND && waterRightObtainable,
                        electricityRightOwned = propertyType == PropertyType.LAND && electricityRightOwned,
                        electricityRightObtainable = propertyType == PropertyType.LAND && electricityRightObtainable,
                        gasRightOwned = propertyType == PropertyType.LAND && gasRightOwned,
                        gasRightObtainable = propertyType == PropertyType.LAND && gasRightObtainable,

                        commercialUsage = if (propertyType == PropertyType.COMMERCIAL) commercialUsage else null,
                        commercialFloorPosition = if (propertyType == PropertyType.COMMERCIAL) commercialFloorPosition else null,
                        streetPosition = if (propertyType == PropertyType.COMMERCIAL) streetPosition else null,
                        ceilingHeight = if (propertyType == PropertyType.COMMERCIAL) ceilingHeight.parseNumberInput() else null,
                        hasThreePhaseElectricity = propertyType == PropertyType.COMMERCIAL && hasThreePhaseElectricity,
                        hasKitchen = propertyType == PropertyType.COMMERCIAL && hasKitchen,
                        hasRestroom = propertyType == PropertyType.COMMERCIAL && hasRestroom,

                        description = description.ifBlank { null },
                        additionalNotes = additionalNotes.ifBlank { null },

                        totalPrice = totalPrice.parseTomanInput(),
                        depositPrice = depositPrice.parseTomanInput(),
                        rentPrice = rentPrice.parseTomanInput(),
                        minAdjustableDeposit = if (isDepositNegotiable) minAdjustableDeposit.parseTomanInput() else null,
                        isExchangeable = dealType == DealType.SALE && isExchangeable,
                        exchangePreferredType = if (dealType == DealType.SALE && isExchangeable) exchangePreferredType else null,
                        exchangeNote = if (dealType == DealType.SALE && isExchangeable) exchangeNote.ifBlank { null } else null
                    )
                    viewModel.save(entity) { onSaved() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = ownerName.isNotBlank() && ownerPhone.isNotBlank() && address.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEditMode) stringResource(R.string.edit_property_save)
                    else stringResource(R.string.add_property_save)
                )
            }
        }
    }
}

/** تبدیل عدد به رشته‌ی قابل‌ویرایش در فرم، بدون ".0" اضافه برای اعداد صحیح. */
private fun Double.toPlainInputString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

/** دراپ‌داون یک enum که می‌تواند «نامشخص» (null) هم باشد. */
@Composable
private fun <T : Enum<T>> NullableEnumDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    icon: ImageVector? = null,
    display: @Composable (T) -> String
) {
    DropdownSelector(
        label = label,
        options = listOf<T?>(null) + options,
        selected = selected,
        onSelect = onSelect,
        icon = icon,
        display = { it?.let { value -> display(value) } ?: stringResource(R.string.value_unspecified) }
    )
}

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
                Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.cd_add_photo), tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
 * منوی کشویی عمومی برای انتخاب یک مقدار از بین چند گزینه.
 * نکته‌ی مهم پیاده‌سازی: بدون Modifier.menuAnchor() روی TextField داخلِ ExposedDropdownMenuBox،
 * منو به فیلد «لنگر» نمی‌شود و در برخی دستگاه‌ها/نسخه‌ها اصلاً باز نمی‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    icon: ImageVector? = null,
    display: @Composable (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = display(selected),
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = icon?.let { i -> { Icon(i, contentDescription = null) } },
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
fun PropertyType.toPersianLabel(): String = when (this) {
    PropertyType.APARTMENT -> stringResource(R.string.property_type_apartment)
    PropertyType.VILLA -> stringResource(R.string.property_type_villa)
    PropertyType.LAND -> stringResource(R.string.property_type_land)
    PropertyType.COMMERCIAL -> stringResource(R.string.property_type_commercial)
}

@Composable
fun DealType.toPersianLabel(): String = when (this) {
    DealType.SALE -> stringResource(R.string.deal_type_sale)
    DealType.RENT -> stringResource(R.string.deal_type_rent)
    DealType.MORTGAGE -> stringResource(R.string.deal_type_mortgage)
    DealType.EXCHANGE -> stringResource(R.string.deal_type_exchange)
}

@Composable
fun DeedType.toPersianLabel(): String = when (this) {
    DeedType.SINGLE_PAGE -> stringResource(R.string.deed_type_single_page)
    DeedType.MANGOLEH -> stringResource(R.string.deed_type_mangoleh)
    DeedType.UNDIVIDED -> stringResource(R.string.deed_type_undivided)
    DeedType.AGREEMENT -> stringResource(R.string.deed_type_agreement)
    DeedType.UNDER_CONSTRUCTION -> stringResource(R.string.deed_type_under_construction)
    DeedType.ENDOWMENT -> stringResource(R.string.deed_type_endowment)
}

@Composable
fun BuildingClass.toPersianLabel(): String = when (this) {
    BuildingClass.A_PLUS -> stringResource(R.string.building_class_a_plus)
    BuildingClass.A -> stringResource(R.string.building_class_a)
    BuildingClass.B -> stringResource(R.string.building_class_b)
    BuildingClass.C -> stringResource(R.string.building_class_c)
}

@Composable
fun CabinetMaterial.toPersianLabel(): String = when (this) {
    CabinetMaterial.HIGH_GLOSS -> stringResource(R.string.cabinet_material_high_gloss)
    CabinetMaterial.MDF -> stringResource(R.string.cabinet_material_mdf)
    CabinetMaterial.MEMBRANE -> stringResource(R.string.cabinet_material_membrane)
    CabinetMaterial.WOOD -> stringResource(R.string.cabinet_material_wood)
    CabinetMaterial.METAL -> stringResource(R.string.cabinet_material_metal)
}

@Composable
fun FlooringType.toPersianLabel(): String = when (this) {
    FlooringType.CERAMIC -> stringResource(R.string.flooring_ceramic)
    FlooringType.PARQUET -> stringResource(R.string.flooring_parquet)
    FlooringType.MOSAIC -> stringResource(R.string.flooring_mosaic)
    FlooringType.STONE -> stringResource(R.string.flooring_stone)
    FlooringType.CONCRETE -> stringResource(R.string.flooring_concrete)
    FlooringType.OTHER -> stringResource(R.string.flooring_other)
}

@Composable
fun WallCovering.toPersianLabel(): String = when (this) {
    WallCovering.PLASTER -> stringResource(R.string.wall_covering_plaster)
    WallCovering.WALLPAPER -> stringResource(R.string.wall_covering_wallpaper)
    WallCovering.PAINT -> stringResource(R.string.wall_covering_paint)
    WallCovering.CERAMIC -> stringResource(R.string.wall_covering_ceramic)
    WallCovering.CONCRETE -> stringResource(R.string.wall_covering_concrete)
}

@Composable
fun CeilingCovering.toPersianLabel(): String = when (this) {
    CeilingCovering.PLASTER -> stringResource(R.string.ceiling_covering_plaster)
    CeilingCovering.GYPSUM_BOARD -> stringResource(R.string.ceiling_covering_gypsum_board)
    CeilingCovering.PAINT -> stringResource(R.string.ceiling_covering_paint)
    CeilingCovering.SUSPENDED -> stringResource(R.string.ceiling_covering_suspended)
}

@Composable
fun CoolingSystem.toPersianLabel(): String = when (this) {
    CoolingSystem.SPLIT -> stringResource(R.string.cooling_split)
    CoolingSystem.COOLER -> stringResource(R.string.cooling_cooler)
    CoolingSystem.CENTRAL -> stringResource(R.string.cooling_central)
}

@Composable
fun HeatingSystem.toPersianLabel(): String = when (this) {
    HeatingSystem.PACKAGE -> stringResource(R.string.heating_package)
    HeatingSystem.WATER_HEATER -> stringResource(R.string.heating_water_heater)
    HeatingSystem.RADIATOR -> stringResource(R.string.heating_radiator)
    HeatingSystem.FAN_COIL -> stringResource(R.string.heating_fan_coil)
    HeatingSystem.CENTRAL -> stringResource(R.string.heating_central)
}

@Composable
fun LandUse.toPersianLabel(): String = when (this) {
    LandUse.COMMERCIAL -> stringResource(R.string.land_use_commercial)
    LandUse.RESIDENTIAL -> stringResource(R.string.land_use_residential)
    LandUse.GARDEN -> stringResource(R.string.land_use_garden)
    LandUse.AGRICULTURAL -> stringResource(R.string.land_use_agricultural)
    LandUse.INDUSTRIAL -> stringResource(R.string.land_use_industrial)
    LandUse.UNPLANNED -> stringResource(R.string.land_use_unplanned)
}

@Composable
fun LandPosition.toPersianLabel(): String = when (this) {
    LandPosition.NORTH -> stringResource(R.string.land_position_north)
    LandPosition.SOUTH -> stringResource(R.string.land_position_south)
    LandPosition.TWO_SIDED -> stringResource(R.string.land_position_two_sided)
    LandPosition.CORNER_THREE -> stringResource(R.string.land_position_corner_three)
    LandPosition.CORNER_FOUR -> stringResource(R.string.land_position_corner_four)
}

@Composable
fun LandSlope.toPersianLabel(): String = when (this) {
    LandSlope.FLAT -> stringResource(R.string.land_slope_flat)
    LandSlope.SLOPED -> stringResource(R.string.land_slope_sloped)
}

@Composable
fun CommercialUsage.toPersianLabel(): String = when (this) {
    CommercialUsage.RETAIL -> stringResource(R.string.commercial_usage_retail)
    CommercialUsage.COMMERCIAL_COMPLEX -> stringResource(R.string.commercial_usage_complex)
    CommercialUsage.OFFICE -> stringResource(R.string.commercial_usage_office)
    CommercialUsage.WORKSHOP -> stringResource(R.string.commercial_usage_workshop)
    CommercialUsage.WAREHOUSE -> stringResource(R.string.commercial_usage_warehouse)
}

@Composable
fun CommercialPosition.toPersianLabel(): String = when (this) {
    CommercialPosition.BASEMENT -> stringResource(R.string.commercial_position_basement)
    CommercialPosition.GROUND -> stringResource(R.string.commercial_position_ground)
    CommercialPosition.UPPER_FLOOR -> stringResource(R.string.commercial_position_upper_floor)
}

@Composable
fun StreetPosition.toPersianLabel(): String = when (this) {
    StreetPosition.MAIN_STREET -> stringResource(R.string.street_position_main)
    StreetPosition.SIDE_STREET -> stringResource(R.string.street_position_side)
}
