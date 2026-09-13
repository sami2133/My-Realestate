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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Balcony
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Elevator
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Weekend
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
import com.realestate.sami.data.local.entity.BuildingClass
import com.realestate.sami.data.local.entity.CommercialPosition
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.DeedType
import com.realestate.sami.data.local.entity.FacadeType
import com.realestate.sami.data.local.entity.FlooringType
import com.realestate.sami.data.local.entity.HeatingCoolingSystem
import com.realestate.sami.data.local.entity.LandPosition
import com.realestate.sami.data.local.entity.LandSlope
import com.realestate.sami.data.local.entity.LandUse
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyImage
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.data.local.entity.StreetPosition
import com.realestate.sami.data.local.entity.UnitCondition
import com.realestate.sami.data.local.entity.UnitDirection
import com.realestate.sami.data.local.entity.UtilityStatus
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.ui.screens.common.AmenityChip
import com.realestate.sami.ui.screens.common.SectionCard
import com.realestate.sami.ui.screens.common.FormSubsectionLabel
import com.realestate.sami.ui.screens.common.LabeledField
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
 * فرم ثبت/ویرایش ملک. وقتی [propertyId] مقدار داشته باشد، صفحه در حالت ویرایش باز می‌شود:
 * رکورد موجود از دیتابیس خوانده و فرم با مقادیرش پر می‌شود؛ در غیر این صورت فرم برای ثبت ملک جدید خالی است.
 *
 * ساختار بصری بازطراحی شده: هر بخش فرم داخل یک [SectionCard] مستقل با آیکون هدر قرار
 * می‌گیرد (به‌جای Divider+Text خام قبلی) و امکانات به‌جای ستون چک‌باکس، چیپ‌های
 * [AmenityChip] در یک FlowRow هستند. منطق state/ذخیره‌سازی نسبت به نسخه‌ی قبلی تغییری نکرده.
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

    // ===== فاز ۵.۵ — مشخصات تکمیلی بسته به نوع ملک =====
    // مشترک
    var deedType by remember { mutableStateOf<DeedType?>(null) }
    var buildingAge by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var totalFloors by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var waterStatus by remember { mutableStateOf<UtilityStatus?>(null) }
    var electricityStatus by remember { mutableStateOf<UtilityStatus?>(null) }
    var gasStatus by remember { mutableStateOf<UtilityStatus?>(null) }
    var buildingClass by remember { mutableStateOf<BuildingClass?>(null) }
    var heatingCoolingSystem by remember { mutableStateOf<HeatingCoolingSystem?>(null) }
    var hasLobby by remember { mutableStateOf(false) }
    var hasSecurityGuard by remember { mutableStateOf(false) }
    var streetPosition by remember { mutableStateOf<StreetPosition?>(null) }
    var frontageWidth by remember { mutableStateOf("") }

    // آپارتمان
    var unitDirection by remember { mutableStateOf<UnitDirection?>(null) }
    var unitCondition by remember { mutableStateOf<UnitCondition?>(null) }
    var flooring by remember { mutableStateOf<FlooringType?>(null) }
    var facade by remember { mutableStateOf<FacadeType?>(null) }
    var bathroomCount by remember { mutableStateOf("") }
    var hasBalcony by remember { mutableStateOf(false) }
    var hasPool by remember { mutableStateOf(false) }
    var hasSauna by remember { mutableStateOf(false) }
    var hasGym by remember { mutableStateOf(false) }
    var hasVideoIntercom by remember { mutableStateOf(false) }

    // زمین
    var landUse by remember { mutableStateOf<LandUse?>(null) }
    var streetWidth by remember { mutableStateOf("") }
    var allowedDensity by remember { mutableStateOf("") }
    var allowedFloors by remember { mutableStateOf("") }
    var landPosition by remember { mutableStateOf<LandPosition?>(null) }
    var hasWall by remember { mutableStateOf(false) }
    var hasBuildingPermit by remember { mutableStateOf(false) }
    var landSlope by remember { mutableStateOf<LandSlope?>(null) }

    // تجاری
    var keyMoney by remember { mutableStateOf("") }
    var commercialFloorPosition by remember { mutableStateOf<CommercialPosition?>(null) }
    var ceilingHeight by remember { mutableStateOf("") }
    var businessLicenseType by remember { mutableStateOf("") }
    var hasThreePhaseElectricity by remember { mutableStateOf(false) }
    var hasRestroom by remember { mutableStateOf(false) }

    // اداری
    var partitionCount by remember { mutableStateOf("") }
    var hasFalseFloor by remember { mutableStateOf(false) }
    var hasFalseCeiling by remember { mutableStateOf(false) }
    var hasConferenceRoom by remember { mutableStateOf(false) }

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

            // فاز ۵.۵/۵.۶ — مشخصات تکمیلی
            deedType = p.deedType
            buildingAge = p.buildingAge?.toString() ?: ""
            floor = p.floor?.toString() ?: ""
            totalFloors = p.totalFloors?.toString() ?: ""
            description = p.description ?: ""
            additionalNotes = p.additionalNotes ?: ""
            waterStatus = p.waterStatus
            electricityStatus = p.electricityStatus
            gasStatus = p.gasStatus
            buildingClass = p.buildingClass
            heatingCoolingSystem = p.heatingCoolingSystem
            hasLobby = p.hasLobby
            hasSecurityGuard = p.hasSecurityGuard
            streetPosition = p.streetPosition
            frontageWidth = p.frontageWidth?.toPlainInputString() ?: ""

            unitDirection = p.unitDirection
            unitCondition = p.unitCondition
            flooring = p.flooring
            facade = p.facade
            bathroomCount = p.bathroomCount?.toString() ?: ""
            hasBalcony = p.hasBalcony
            hasPool = p.hasPool
            hasSauna = p.hasSauna
            hasGym = p.hasGym
            hasVideoIntercom = p.hasVideoIntercom

            landUse = p.landUse
            streetWidth = p.streetWidth?.toPlainInputString() ?: ""
            allowedDensity = p.allowedDensity?.toString() ?: ""
            allowedFloors = p.allowedFloors?.toString() ?: ""
            landPosition = p.landPosition
            hasWall = p.hasWall
            hasBuildingPermit = p.hasBuildingPermit
            landSlope = p.landSlope

            keyMoney = p.keyMoney?.toString() ?: ""
            commercialFloorPosition = p.commercialFloorPosition
            ceilingHeight = p.ceilingHeight?.toPlainInputString() ?: ""
            businessLicenseType = p.businessLicenseType ?: ""
            hasThreePhaseElectricity = p.hasThreePhaseElectricity
            hasRestroom = p.hasRestroom

            partitionCount = p.partitionCount?.toString() ?: ""
            hasFalseFloor = p.hasFalseFloor
            hasFalseCeiling = p.hasFalseCeiling
            hasConferenceRoom = p.hasConferenceRoom
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== اطلاعات معرف/مالک =====
            SectionCard(title = stringResource(R.string.add_property_owner_section), icon = Icons.Filled.Person) {
                LabeledField(ownerName, { ownerName = it }, stringResource(R.string.add_property_owner_name), icon = Icons.Filled.Person)
                LabeledField(ownerPhone, { ownerPhone = it }, stringResource(R.string.label_phone), icon = Icons.Filled.Call, keyboardType = KeyboardType.Phone)
            }

            // ===== عکس‌های ملک =====
            SectionCard(title = stringResource(R.string.add_property_photos_section), icon = Icons.Filled.PhotoLibrary) {
                PhotoPickerRow(
                    images = images,
                    onAddClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = { image -> images = images - image }
                )
            }

            // ===== مشخصات پایه‌ی ملک =====
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField(area, { area = it }, stringResource(R.string.add_property_area_hint), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    LabeledField(rooms, { rooms = it }, stringResource(R.string.label_rooms), icon = Icons.Filled.MeetingRoom, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }

                FormSubsectionLabel(stringResource(R.string.add_property_amenities_section))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmenityChip(stringResource(R.string.amenity_parking), Icons.Filled.LocalParking, hasParking) { hasParking = it }
                    AmenityChip(stringResource(R.string.amenity_storage), Icons.Filled.Inventory2, hasStorage) { hasStorage = it }
                    AmenityChip(stringResource(R.string.amenity_elevator), Icons.Filled.Elevator, hasElevator) { hasElevator = it }
                }
            }

            // ===== مشخصات تکمیلی (بسته به نوع ملک) =====
            SectionCard(title = stringResource(R.string.spec_section_common), icon = Icons.Filled.Tune) {
                PropertySpecsSection(
                    propertyType = propertyType,
                    deedType = deedType, onDeedTypeChange = { deedType = it },
                    buildingAge = buildingAge, onBuildingAgeChange = { buildingAge = it },
                    floor = floor, onFloorChange = { floor = it },
                    totalFloors = totalFloors, onTotalFloorsChange = { totalFloors = it },
                    waterStatus = waterStatus, onWaterStatusChange = { waterStatus = it },
                    electricityStatus = electricityStatus, onElectricityStatusChange = { electricityStatus = it },
                    gasStatus = gasStatus, onGasStatusChange = { gasStatus = it },
                    buildingClass = buildingClass, onBuildingClassChange = { buildingClass = it },
                    heatingCoolingSystem = heatingCoolingSystem, onHeatingCoolingSystemChange = { heatingCoolingSystem = it },
                    hasLobby = hasLobby, onHasLobbyChange = { hasLobby = it },
                    hasSecurityGuard = hasSecurityGuard, onHasSecurityGuardChange = { hasSecurityGuard = it },
                    streetPosition = streetPosition, onStreetPositionChange = { streetPosition = it },
                    frontageWidth = frontageWidth, onFrontageWidthChange = { frontageWidth = it },
                    unitDirection = unitDirection, onUnitDirectionChange = { unitDirection = it },
                    unitCondition = unitCondition, onUnitConditionChange = { unitCondition = it },
                    flooring = flooring, onFlooringChange = { flooring = it },
                    facade = facade, onFacadeChange = { facade = it },
                    bathroomCount = bathroomCount, onBathroomCountChange = { bathroomCount = it },
                    hasBalcony = hasBalcony, onHasBalconyChange = { hasBalcony = it },
                    hasPool = hasPool, onHasPoolChange = { hasPool = it },
                    hasSauna = hasSauna, onHasSaunaChange = { hasSauna = it },
                    hasGym = hasGym, onHasGymChange = { hasGym = it },
                    hasVideoIntercom = hasVideoIntercom, onHasVideoIntercomChange = { hasVideoIntercom = it },
                    landUse = landUse, onLandUseChange = { landUse = it },
                    streetWidth = streetWidth, onStreetWidthChange = { streetWidth = it },
                    allowedDensity = allowedDensity, onAllowedDensityChange = { allowedDensity = it },
                    allowedFloors = allowedFloors, onAllowedFloorsChange = { allowedFloors = it },
                    landPosition = landPosition, onLandPositionChange = { landPosition = it },
                    hasWall = hasWall, onHasWallChange = { hasWall = it },
                    hasBuildingPermit = hasBuildingPermit, onHasBuildingPermitChange = { hasBuildingPermit = it },
                    landSlope = landSlope, onLandSlopeChange = { landSlope = it },
                    keyMoney = keyMoney, onKeyMoneyChange = { keyMoney = it },
                    commercialFloorPosition = commercialFloorPosition, onCommercialFloorPositionChange = { commercialFloorPosition = it },
                    ceilingHeight = ceilingHeight, onCeilingHeightChange = { ceilingHeight = it },
                    businessLicenseType = businessLicenseType, onBusinessLicenseTypeChange = { businessLicenseType = it },
                    hasThreePhaseElectricity = hasThreePhaseElectricity, onHasThreePhaseElectricityChange = { hasThreePhaseElectricity = it },
                    hasRestroom = hasRestroom, onHasRestroomChange = { hasRestroom = it },
                    partitionCount = partitionCount, onPartitionCountChange = { partitionCount = it },
                    hasFalseFloor = hasFalseFloor, onHasFalseFloorChange = { hasFalseFloor = it },
                    hasFalseCeiling = hasFalseCeiling, onHasFalseCeilingChange = { hasFalseCeiling = it },
                    hasConferenceRoom = hasConferenceRoom, onHasConferenceRoomChange = { hasConferenceRoom = it }
                )
            }

            // ===== توضیحات =====
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

            // ===== قیمت‌گذاری =====
            SectionCard(title = stringResource(R.string.add_property_pricing_section), icon = Icons.Filled.Payments) {
                when (dealType) {
                    DealType.SALE, DealType.EXCHANGE -> {
                        LabeledField(totalPrice, { totalPrice = it }, stringResource(R.string.add_property_total_price), icon = Icons.Filled.Payments, keyboardType = KeyboardType.Number)

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
                        LabeledField(depositPrice, { depositPrice = it }, stringResource(R.string.add_property_deposit_price), icon = Icons.Filled.Payments, keyboardType = KeyboardType.Number)
                        LabeledField(rentPrice, { rentPrice = it }, stringResource(R.string.add_property_rent_price), icon = Icons.Filled.Payments, keyboardType = KeyboardType.Number)

                        SwitchRow(stringResource(R.string.add_property_deposit_negotiable), isDepositNegotiable) { isDepositNegotiable = it }
                        if (isDepositNegotiable) {
                            LabeledField(minAdjustableDeposit, { minAdjustableDeposit = it }, stringResource(R.string.add_property_min_deposit), icon = Icons.Filled.Payments, keyboardType = KeyboardType.Number)
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
                        images = images,
                        description = description.ifBlank { null },
                        additionalNotes = additionalNotes.ifBlank { null },

                        // فاز ۵.۵ — مشترک بین چند نوع ملک
                        deedType = deedType,
                        buildingAge = if (propertyType != PropertyType.LAND) buildingAge.parseIntInput() else null,
                        floor = if (propertyType != PropertyType.LAND) floor.parseIntInput() else null,
                        totalFloors = if (propertyType != PropertyType.LAND) totalFloors.parseIntInput() else null,
                        waterStatus = if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA || propertyType == PropertyType.LAND) waterStatus else null,
                        electricityStatus = if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA || propertyType == PropertyType.LAND) electricityStatus else null,
                        gasStatus = if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA || propertyType == PropertyType.LAND) gasStatus else null,
                        buildingClass = if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.OFFICE) buildingClass else null,
                        heatingCoolingSystem = if (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.OFFICE) heatingCoolingSystem else null,
                        hasLobby = (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.OFFICE) && hasLobby,
                        hasSecurityGuard = (propertyType == PropertyType.APARTMENT || propertyType == PropertyType.OFFICE) && hasSecurityGuard,
                        streetPosition = if (propertyType == PropertyType.COMMERCIAL || propertyType == PropertyType.OFFICE) streetPosition else null,
                        frontageWidth = if (propertyType == PropertyType.LAND || propertyType == PropertyType.COMMERCIAL) frontageWidth.parseNumberInput() else null,

                        // آپارتمان
                        unitDirection = if (propertyType == PropertyType.APARTMENT) unitDirection else null,
                        unitCondition = if (propertyType == PropertyType.APARTMENT) unitCondition else null,
                        flooring = if (propertyType == PropertyType.APARTMENT) flooring else null,
                        facade = if (propertyType == PropertyType.APARTMENT) facade else null,
                        bathroomCount = if (propertyType == PropertyType.APARTMENT) bathroomCount.parseIntInput() else null,
                        hasBalcony = propertyType == PropertyType.APARTMENT && hasBalcony,
                        hasPool = propertyType == PropertyType.APARTMENT && hasPool,
                        hasSauna = propertyType == PropertyType.APARTMENT && hasSauna,
                        hasGym = propertyType == PropertyType.APARTMENT && hasGym,
                        hasVideoIntercom = propertyType == PropertyType.APARTMENT && hasVideoIntercom,

                        // زمین
                        landUse = if (propertyType == PropertyType.LAND) landUse else null,
                        streetWidth = if (propertyType == PropertyType.LAND) streetWidth.parseNumberInput() else null,
                        allowedDensity = if (propertyType == PropertyType.LAND) allowedDensity.parseIntInput() else null,
                        allowedFloors = if (propertyType == PropertyType.LAND) allowedFloors.parseIntInput() else null,
                        landPosition = if (propertyType == PropertyType.LAND) landPosition else null,
                        hasWall = propertyType == PropertyType.LAND && hasWall,
                        hasBuildingPermit = propertyType == PropertyType.LAND && hasBuildingPermit,
                        landSlope = if (propertyType == PropertyType.LAND) landSlope else null,

                        // تجاری
                        keyMoney = if (propertyType == PropertyType.COMMERCIAL) keyMoney.parseTomanInput() else null,
                        commercialFloorPosition = if (propertyType == PropertyType.COMMERCIAL) commercialFloorPosition else null,
                        ceilingHeight = if (propertyType == PropertyType.COMMERCIAL) ceilingHeight.parseNumberInput() else null,
                        businessLicenseType = if (propertyType == PropertyType.COMMERCIAL) businessLicenseType.ifBlank { null } else null,
                        hasThreePhaseElectricity = propertyType == PropertyType.COMMERCIAL && hasThreePhaseElectricity,
                        hasRestroom = propertyType == PropertyType.COMMERCIAL && hasRestroom,

                        // اداری
                        partitionCount = if (propertyType == PropertyType.OFFICE) partitionCount.parseIntInput() else null,
                        hasFalseFloor = propertyType == PropertyType.OFFICE && hasFalseFloor,
                        hasFalseCeiling = propertyType == PropertyType.OFFICE && hasFalseCeiling,
                        hasConferenceRoom = propertyType == PropertyType.OFFICE && hasConferenceRoom
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

/** تبدیل متراژ به رشته‌ی قابل‌ویرایش در فرم، بدون ".0" اضافه برای اعداد صحیح. */
private fun Double.toPlainInputString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

/**
 * فاز ۵.۵/۵.۶ — مشخصات تکمیلی ملک، بسته به نوع ملک. بخش «مشترک» همیشه نمایش داده می‌شود (با
 * فیلدهایی که فقط برای بعضی انواع معنا دارند، مشروط)، و بعدش یک بخش مخصوص همان نوع ملک.
 * (این کامپوننت داخل [SectionCard] در AddPropertyScreen صدا زده می‌شود، پس خودش کارت جدا نمی‌سازد.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PropertySpecsSection(
    propertyType: PropertyType,
    deedType: DeedType?, onDeedTypeChange: (DeedType?) -> Unit,
    buildingAge: String, onBuildingAgeChange: (String) -> Unit,
    floor: String, onFloorChange: (String) -> Unit,
    totalFloors: String, onTotalFloorsChange: (String) -> Unit,
    waterStatus: UtilityStatus?, onWaterStatusChange: (UtilityStatus?) -> Unit,
    electricityStatus: UtilityStatus?, onElectricityStatusChange: (UtilityStatus?) -> Unit,
    gasStatus: UtilityStatus?, onGasStatusChange: (UtilityStatus?) -> Unit,
    buildingClass: BuildingClass?, onBuildingClassChange: (BuildingClass?) -> Unit,
    heatingCoolingSystem: HeatingCoolingSystem?, onHeatingCoolingSystemChange: (HeatingCoolingSystem?) -> Unit,
    hasLobby: Boolean, onHasLobbyChange: (Boolean) -> Unit,
    hasSecurityGuard: Boolean, onHasSecurityGuardChange: (Boolean) -> Unit,
    streetPosition: StreetPosition?, onStreetPositionChange: (StreetPosition?) -> Unit,
    frontageWidth: String, onFrontageWidthChange: (String) -> Unit,
    unitDirection: UnitDirection?, onUnitDirectionChange: (UnitDirection?) -> Unit,
    unitCondition: UnitCondition?, onUnitConditionChange: (UnitCondition?) -> Unit,
    flooring: FlooringType?, onFlooringChange: (FlooringType?) -> Unit,
    facade: FacadeType?, onFacadeChange: (FacadeType?) -> Unit,
    bathroomCount: String, onBathroomCountChange: (String) -> Unit,
    hasBalcony: Boolean, onHasBalconyChange: (Boolean) -> Unit,
    hasPool: Boolean, onHasPoolChange: (Boolean) -> Unit,
    hasSauna: Boolean, onHasSaunaChange: (Boolean) -> Unit,
    hasGym: Boolean, onHasGymChange: (Boolean) -> Unit,
    hasVideoIntercom: Boolean, onHasVideoIntercomChange: (Boolean) -> Unit,
    landUse: LandUse?, onLandUseChange: (LandUse?) -> Unit,
    streetWidth: String, onStreetWidthChange: (String) -> Unit,
    allowedDensity: String, onAllowedDensityChange: (String) -> Unit,
    allowedFloors: String, onAllowedFloorsChange: (String) -> Unit,
    landPosition: LandPosition?, onLandPositionChange: (LandPosition?) -> Unit,
    hasWall: Boolean, onHasWallChange: (Boolean) -> Unit,
    hasBuildingPermit: Boolean, onHasBuildingPermitChange: (Boolean) -> Unit,
    landSlope: LandSlope?, onLandSlopeChange: (LandSlope?) -> Unit,
    keyMoney: String, onKeyMoneyChange: (String) -> Unit,
    commercialFloorPosition: CommercialPosition?, onCommercialFloorPositionChange: (CommercialPosition?) -> Unit,
    ceilingHeight: String, onCeilingHeightChange: (String) -> Unit,
    businessLicenseType: String, onBusinessLicenseTypeChange: (String) -> Unit,
    hasThreePhaseElectricity: Boolean, onHasThreePhaseElectricityChange: (Boolean) -> Unit,
    hasRestroom: Boolean, onHasRestroomChange: (Boolean) -> Unit,
    partitionCount: String, onPartitionCountChange: (String) -> Unit,
    hasFalseFloor: Boolean, onHasFalseFloorChange: (Boolean) -> Unit,
    hasFalseCeiling: Boolean, onHasFalseCeilingChange: (Boolean) -> Unit,
    hasConferenceRoom: Boolean, onHasConferenceRoomChange: (Boolean) -> Unit
) {
    val showUtilities = propertyType == PropertyType.APARTMENT || propertyType == PropertyType.VILLA || propertyType == PropertyType.LAND
    val showBuildingClassAndHvac = propertyType == PropertyType.APARTMENT || propertyType == PropertyType.OFFICE
    val showStreetPosition = propertyType == PropertyType.COMMERCIAL || propertyType == PropertyType.OFFICE
    val showFrontageWidth = propertyType == PropertyType.LAND || propertyType == PropertyType.COMMERCIAL

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (propertyType != PropertyType.LAND) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(buildingAge, onBuildingAgeChange, stringResource(R.string.label_building_age), icon = Icons.Filled.Apartment, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                LabeledField(floor, onFloorChange, stringResource(R.string.label_floor), icon = Icons.Filled.Stairs, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                LabeledField(totalFloors, onTotalFloorsChange, stringResource(R.string.label_total_floors), icon = Icons.Filled.Layers, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            }
        }

        NullableEnumDropdown(stringResource(R.string.label_deed_type), DeedType.entries.toList(), deedType, onDeedTypeChange, icon = Icons.Filled.Verified) { it.toPersianLabel() }

        if (showUtilities) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    NullableEnumDropdown(stringResource(R.string.label_water_status), UtilityStatus.entries.toList(), waterStatus, onWaterStatusChange) { it.toPersianLabel() }
                }
                Box(Modifier.weight(1f)) {
                    NullableEnumDropdown(stringResource(R.string.label_electricity_status), UtilityStatus.entries.toList(), electricityStatus, onElectricityStatusChange) { it.toPersianLabel() }
                }
                Box(Modifier.weight(1f)) {
                    NullableEnumDropdown(stringResource(R.string.label_gas_status), UtilityStatus.entries.toList(), gasStatus, onGasStatusChange) { it.toPersianLabel() }
                }
            }
        }

        if (showBuildingClassAndHvac) {
            NullableEnumDropdown(stringResource(R.string.label_building_class), BuildingClass.entries.toList(), buildingClass, onBuildingClassChange) { it.toPersianLabel() }
            NullableEnumDropdown(stringResource(R.string.label_heating_cooling), HeatingCoolingSystem.entries.toList(), heatingCoolingSystem, onHeatingCoolingSystemChange) { it.toPersianLabel() }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmenityChip(stringResource(R.string.amenity_lobby), Icons.Filled.Weekend, hasLobby, onHasLobbyChange)
                AmenityChip(stringResource(R.string.amenity_security_guard), Icons.Filled.Security, hasSecurityGuard, onHasSecurityGuardChange)
            }
        }

        if (showStreetPosition) {
            NullableEnumDropdown(stringResource(R.string.label_street_position), StreetPosition.entries.toList(), streetPosition, onStreetPositionChange) { it.toPersianLabel() }
        }

        if (showFrontageWidth) {
            LabeledField(
                frontageWidth,
                onFrontageWidthChange,
                stringResource(
                    if (propertyType == PropertyType.LAND) R.string.label_frontage_width_land
                    else R.string.label_frontage_width_commercial
                ),
                icon = Icons.Filled.Straighten,
                keyboardType = KeyboardType.Number
            )
        }

        when (propertyType) {
            PropertyType.APARTMENT -> {
                Divider()
                FormSubsectionLabel(stringResource(R.string.spec_section_apartment))
                NullableEnumDropdown(stringResource(R.string.label_unit_direction), UnitDirection.entries.toList(), unitDirection, onUnitDirectionChange) { it.toPersianLabel() }
                NullableEnumDropdown(stringResource(R.string.label_unit_condition), UnitCondition.entries.toList(), unitCondition, onUnitConditionChange) { it.toPersianLabel() }
                NullableEnumDropdown(stringResource(R.string.label_flooring), FlooringType.entries.toList(), flooring, onFlooringChange) { it.toPersianLabel() }
                NullableEnumDropdown(stringResource(R.string.label_facade), FacadeType.entries.toList(), facade, onFacadeChange) { it.toPersianLabel() }
                LabeledField(bathroomCount, onBathroomCountChange, stringResource(R.string.label_bathroom_count), icon = Icons.Filled.Bathtub, keyboardType = KeyboardType.Number)
                FormSubsectionLabel(stringResource(R.string.spec_section_amenities))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmenityChip(stringResource(R.string.amenity_balcony), Icons.Filled.Balcony, hasBalcony, onHasBalconyChange)
                    AmenityChip(stringResource(R.string.amenity_pool), Icons.Filled.Pool, hasPool, onHasPoolChange)
                    AmenityChip(stringResource(R.string.amenity_sauna), Icons.Filled.Spa, hasSauna, onHasSaunaChange)
                    AmenityChip(stringResource(R.string.amenity_gym), Icons.Filled.FitnessCenter, hasGym, onHasGymChange)
                    AmenityChip(stringResource(R.string.amenity_video_intercom), Icons.Filled.Videocam, hasVideoIntercom, onHasVideoIntercomChange)
                }
            }
            PropertyType.LAND -> {
                Divider()
                FormSubsectionLabel(stringResource(R.string.spec_section_land))
                NullableEnumDropdown(stringResource(R.string.label_land_use), LandUse.entries.toList(), landUse, onLandUseChange) { it.toPersianLabel() }
                LabeledField(streetWidth, onStreetWidthChange, stringResource(R.string.label_street_width), icon = Icons.Filled.Straighten, keyboardType = KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField(allowedDensity, onAllowedDensityChange, stringResource(R.string.label_allowed_density), icon = Icons.Filled.Apartment, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    LabeledField(allowedFloors, onAllowedFloorsChange, stringResource(R.string.label_allowed_floors), icon = Icons.Filled.Layers, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                NullableEnumDropdown(stringResource(R.string.label_land_position), LandPosition.entries.toList(), landPosition, onLandPositionChange) { it.toPersianLabel() }
                NullableEnumDropdown(stringResource(R.string.label_land_slope), LandSlope.entries.toList(), landSlope, onLandSlopeChange) { it.toPersianLabel() }
                FormSubsectionLabel(stringResource(R.string.spec_section_amenities))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmenityChip(stringResource(R.string.amenity_wall), Icons.Filled.Fence, hasWall, onHasWallChange)
                    AmenityChip(stringResource(R.string.amenity_building_permit), Icons.Filled.Verified, hasBuildingPermit, onHasBuildingPermitChange)
                }
            }
            PropertyType.COMMERCIAL -> {
                Divider()
                FormSubsectionLabel(stringResource(R.string.spec_section_commercial))
                LabeledField(keyMoney, onKeyMoneyChange, stringResource(R.string.label_key_money), icon = Icons.Filled.Payments, keyboardType = KeyboardType.Number)
                NullableEnumDropdown(stringResource(R.string.label_commercial_floor_position), CommercialPosition.entries.toList(), commercialFloorPosition, onCommercialFloorPositionChange) { it.toPersianLabel() }
                LabeledField(ceilingHeight, onCeilingHeightChange, stringResource(R.string.label_ceiling_height), icon = Icons.Filled.Height, keyboardType = KeyboardType.Number)
                LabeledField(businessLicenseType, onBusinessLicenseTypeChange, stringResource(R.string.label_business_license_type), icon = Icons.Filled.Badge)
                FormSubsectionLabel(stringResource(R.string.spec_section_amenities))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmenityChip(stringResource(R.string.amenity_three_phase_electricity), Icons.Filled.Bolt, hasThreePhaseElectricity, onHasThreePhaseElectricityChange)
                    AmenityChip(stringResource(R.string.amenity_restroom), Icons.Filled.Wc, hasRestroom, onHasRestroomChange)
                }
            }
            PropertyType.OFFICE -> {
                Divider()
                FormSubsectionLabel(stringResource(R.string.spec_section_office))
                LabeledField(partitionCount, onPartitionCountChange, stringResource(R.string.label_partition_count), icon = Icons.Filled.ViewColumn, keyboardType = KeyboardType.Number)
                FormSubsectionLabel(stringResource(R.string.spec_section_amenities))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmenityChip(stringResource(R.string.amenity_false_floor), Icons.Filled.Layers, hasFalseFloor, onHasFalseFloorChange)
                    AmenityChip(stringResource(R.string.amenity_false_ceiling), Icons.Filled.Height, hasFalseCeiling, onHasFalseCeilingChange)
                    AmenityChip(stringResource(R.string.amenity_conference_room), Icons.Filled.MeetingRoom, hasConferenceRoom, onHasConferenceRoomChange)
                }
            }
            PropertyType.VILLA -> Unit // فقط بخش مشترک بالا (سن بنا، طبقات، سند، آب/برق/گاز)؛ فیلد اختصاصی ندارد
        }
    }
}

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
 * پارامتر [icon] اختیاریه (پیش‌فرض null) و بعد از onSelect و قبل از display اومده؛ چون
 * default value داره، فراخوانی‌های قدیمی با trailing-lambda برای display (مثل داخل
 * AddClientScreen) بدون تغییر کامپایل می‌شن.
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

// ===== فاز ۵.۵ — برچسب‌های enum های مشخصات تکمیلی =====

@Composable
fun DeedType.toPersianLabel(): String = when (this) {
    DeedType.SINGLE_PAGE -> stringResource(R.string.deed_type_single_page)
    DeedType.MANGOLEH -> stringResource(R.string.deed_type_mangoleh)
    DeedType.AGREEMENT -> stringResource(R.string.deed_type_agreement)
    DeedType.UNDER_CONSTRUCTION -> stringResource(R.string.deed_type_under_construction)
    DeedType.SIX_DANG -> stringResource(R.string.deed_type_six_dang)
    DeedType.ENDOWMENT -> stringResource(R.string.deed_type_endowment)
    DeedType.OTHER -> stringResource(R.string.deed_type_other)
}

@Composable
fun UtilityStatus.toPersianLabel(): String = when (this) {
    UtilityStatus.AVAILABLE -> stringResource(R.string.utility_status_available)
    UtilityStatus.NOT_AVAILABLE -> stringResource(R.string.utility_status_not_available)
    UtilityStatus.OBTAINABLE -> stringResource(R.string.utility_status_obtainable)
}

@Composable
fun BuildingClass.toPersianLabel(): String = when (this) {
    BuildingClass.A -> stringResource(R.string.building_class_a)
    BuildingClass.B -> stringResource(R.string.building_class_b)
    BuildingClass.C -> stringResource(R.string.building_class_c)
}

@Composable
fun HeatingCoolingSystem.toPersianLabel(): String = when (this) {
    HeatingCoolingSystem.PACKAGE -> stringResource(R.string.heating_cooling_package)
    HeatingCoolingSystem.RADIATOR -> stringResource(R.string.heating_cooling_radiator)
    HeatingCoolingSystem.SPLIT -> stringResource(R.string.heating_cooling_split)
    HeatingCoolingSystem.FAN_COIL -> stringResource(R.string.heating_cooling_fan_coil)
    HeatingCoolingSystem.CENTRAL -> stringResource(R.string.heating_cooling_central)
    HeatingCoolingSystem.OTHER -> stringResource(R.string.heating_cooling_other)
}

@Composable
fun UnitDirection.toPersianLabel(): String = when (this) {
    UnitDirection.NORTH -> stringResource(R.string.unit_direction_north)
    UnitDirection.SOUTH -> stringResource(R.string.unit_direction_south)
    UnitDirection.EAST -> stringResource(R.string.unit_direction_east)
    UnitDirection.WEST -> stringResource(R.string.unit_direction_west)
    UnitDirection.TWO_SIDED -> stringResource(R.string.unit_direction_two_sided)
}

@Composable
fun UnitCondition.toPersianLabel(): String = when (this) {
    UnitCondition.NEW -> stringResource(R.string.unit_condition_new)
    UnitCondition.RENOVATED -> stringResource(R.string.unit_condition_renovated)
    UnitCondition.LIVED_IN -> stringResource(R.string.unit_condition_lived_in)
    UnitCondition.UNOCCUPIED -> stringResource(R.string.unit_condition_unoccupied)
}

@Composable
fun FlooringType.toPersianLabel(): String = when (this) {
    FlooringType.CERAMIC -> stringResource(R.string.flooring_ceramic)
    FlooringType.PARQUET -> stringResource(R.string.flooring_parquet)
    FlooringType.MOSAIC -> stringResource(R.string.flooring_mosaic)
    FlooringType.STONE -> stringResource(R.string.flooring_stone)
    FlooringType.OTHER -> stringResource(R.string.flooring_other)
}

@Composable
fun FacadeType.toPersianLabel(): String = when (this) {
    FacadeType.STONE -> stringResource(R.string.facade_stone)
    FacadeType.BRICK -> stringResource(R.string.facade_brick)
    FacadeType.COMPOSITE -> stringResource(R.string.facade_composite)
    FacadeType.OTHER -> stringResource(R.string.facade_other)
}

@Composable
fun LandUse.toPersianLabel(): String = when (this) {
    LandUse.RESIDENTIAL -> stringResource(R.string.land_use_residential)
    LandUse.COMMERCIAL -> stringResource(R.string.land_use_commercial)
    LandUse.OFFICE -> stringResource(R.string.land_use_office)
    LandUse.AGRICULTURAL -> stringResource(R.string.land_use_agricultural)
    LandUse.INDUSTRIAL -> stringResource(R.string.land_use_industrial)
    LandUse.GARDEN -> stringResource(R.string.land_use_garden)
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
