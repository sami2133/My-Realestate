package com.realestate.sami.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.ClientEntity
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.screens.common.*
import com.realestate.sami.ui.viewmodel.PropertyDetailViewModel
import com.realestate.sami.ui.viewmodel.SyncViewModel
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toTomanDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    onBack: () -> Unit,
    onClientClick: (ClientEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val property by viewModel.property.collectAsState()
    val commissionResult by viewModel.commissionResult.collectAsState()
    val matchingClients by viewModel.matchingClients.collectAsState()
    val contactLogs by viewModel.contactLogs.collectAsState()
    val visits by viewModel.visits.collectAsState()
    val visitEventTitleTemplate = stringResource(R.string.visit_calendar_event_title_property)
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_property_confirm_title)) },
            text = { Text(stringResource(R.string.delete_property_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete(onDeleted)
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                title = { Text(property?.address ?: stringResource(R.string.property_detail_title_fallback), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    property?.let { current ->
                        IconButton(onClick = { onEdit(current.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val current = property ?: return@Scaffold
        var viewerStartIndex by remember { mutableStateOf<Int?>(null) }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // گالری عکس
            val images = current.images
            if (images.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(images) { index, image ->
                        if (image.localUri != null) {
                            AsyncImage(
                                model = image.localUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(220.dp, 150.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewerStartIndex = index }
                            )
                        } else {
                            // این عکس روی یک دستگاه دیگه‌ی تیم ثبت شده و هنوز دانلود نشده؛ چون همه‌ی
                            // اعضا با یک اکانت گوگل مشترک کار می‌کنن (نه اکانت شخصی هرکس)، یک
                            // «همگام‌سازی الان» ساده برای دانلودش کافیه — نیازی به هیچ مجوز جداگونه نیست.
                            Column(
                                Modifier
                                    .size(220.dp, 150.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                    .clickable { syncViewModel.syncNow() },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.CloudDownload,
                                    contentDescription = stringResource(R.string.image_not_downloaded_yet),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    stringResource(R.string.sync_now_button),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                                )
                            }
                        }
                    }
                }
                if (viewerStartIndex != null) {
                    ImageGalleryDialog(
                        images = images,
                        startIndex = viewerStartIndex!!,
                        onDismiss = { viewerStartIndex = null }
                    )
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                }
            }

            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // خلاصه بالای صفحه
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DealTypeChip(current.dealType, current.dealType.toPersianLabel())
                    Text(current.propertyType.toPersianLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (current.isExchangeable) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.property_detail_exchangeable_badge), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(
                    when {
                        current.totalPrice != null -> current.totalPrice.toTomanDisplay()
                        current.rentPrice != null -> stringResource(
                            R.string.property_detail_deposit_rent_format,
                            current.depositPrice?.toTomanDisplay() ?: "-",
                            current.rentPrice.toTomanDisplay()
                        )
                        else -> stringResource(R.string.price_not_set)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // فاز ۵.۲: اگه رهن این ملک قابل‌تعدیل ثبت شده، همینجا هم (نه فقط موقع ثبت) بشه
                // زنده جلوی مشتری بازه رو تنظیم کرد و اجاره‌ی متناظرش رو دید.
                val minDeposit = current.minAdjustableDeposit
                val baseDeposit = current.depositPrice
                if (current.dealType == DealType.RENT && minDeposit != null && baseDeposit != null && minDeposit < baseDeposit) {
                    SectionCard {
                        RentDepositAdjustmentSlider(
                            baseDeposit = baseDeposit,
                            baseRent = current.rentPrice ?: 0L,
                            minDeposit = minDeposit,
                            conversionPercent = viewModel.rentConversionPercent
                        )
                    }
                }

                // فاز ۵.۳: جزئیات معاوضه، وقتی مالک علاوه بر فروش نقدی به معاوضه هم راضیه.
                if (current.isExchangeable) {
                    SectionCard(title = stringResource(R.string.property_detail_exchange_section)) {
                        InfoRow(
                            stringResource(R.string.property_detail_exchange_preferred_type_label),
                            current.exchangePreferredType?.toPersianLabel() ?: stringResource(R.string.add_property_exchange_any_type)
                        )
                        if (!current.exchangeNote.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.property_detail_exchange_note_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(current.exchangeNote, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                SectionCard(title = stringResource(R.string.property_detail_specs_section)) {
                    InfoRow(stringResource(R.string.label_area), stringResource(R.string.property_detail_area_value, current.area.toInt()))
                    current.rooms?.let { InfoRow(stringResource(R.string.label_rooms), it.toString()) }
                    InfoRow(stringResource(R.string.amenity_parking), if (current.hasParking) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.amenity_storage), if (current.hasStorage) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.amenity_elevator), if (current.hasElevator) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.label_address), current.address)
                    InfoRow(stringResource(R.string.label_registered_at), current.createdAt.toPersianDateString())
                    current.lastEditedBy?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(stringResource(R.string.label_last_edited_by), it)
                    }
                    val lat = current.latitude
                    val lng = current.longitude
                    if (lat != null && lng != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(current.address)})")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://maps.google.com/maps?q=$lat,$lng")
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_show_on_map))
                        }
                    }
                }

                commissionResult?.let { result ->
                    Spacer(Modifier.height(12.dp))
                    SectionCard(title = stringResource(R.string.commission_result_section), icon = Icons.Filled.Payments) {
                        InfoRow(stringResource(R.string.commission_result_total), result.total.toTomanDisplay())
                        val (labelA, labelB) = if (current.dealType == DealType.RENT || current.dealType == DealType.MORTGAGE) {
                            stringResource(R.string.commission_result_tenant) to stringResource(R.string.commission_result_landlord)
                        } else {
                            stringResource(R.string.commission_result_buyer) to stringResource(R.string.commission_result_seller)
                        }
                        InfoRow(labelA, result.partyAShare.toTomanDisplay())
                        InfoRow(labelB, result.partyBShare.toTomanDisplay())
                    }
                }

                // فاز ۵.۵/۵.۶ — مشخصات تکمیلیِ بسته به نوع ملک، فقط اگر چیزی برای نشان دادن باشد.
                PropertyDetailedSpecsSection(current)

                if (!current.description.isNullOrBlank()) {
                    SectionCard(title = stringResource(R.string.add_property_description)) {
                        Text(current.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!current.additionalNotes.isNullOrBlank()) {
                    SectionCard(title = stringResource(R.string.label_additional_notes)) {
                        Text(current.additionalNotes, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionCard(title = stringResource(R.string.property_detail_owner_section)) {
                    InfoRow(stringResource(R.string.label_name), current.ownerName)
                    Spacer(Modifier.height(8.dp))
                    PhoneActionRow(current.ownerPhone)
                }

                SectionCard(title = stringResource(R.string.property_detail_status_section)) {
                    StatusSelector(
                        current = current.status,
                        onSelect = viewModel::updateStatus
                    )
                }

                SectionCard(title = stringResource(R.string.property_detail_matches_section, matchingClients.size)) {
                    if (matchingClients.isEmpty()) {
                        Text(stringResource(R.string.property_detail_no_matches), style = MaterialTheme.typography.bodySmall)
                    } else {
                        matchingClients.forEach { client ->
                            ListItem(
                                headlineContent = { Text(client.fullName) },
                                supportingContent = { Text(client.desiredRegion) },
                                trailingContent = { Text(client.phone) },
                                modifier = Modifier.clickable { onClientClick(client) }
                            )
                            Divider()
                        }
                    }
                }

                SectionCard {
                    VisitScheduleSection(
                        visits = visits,
                        candidates = matchingClients.map {
                            VisitCandidate(id = it.id, label = it.fullName, subLabel = it.phone)
                        },
                        otherPartyIdOf = { it.clientId },
                        eventTitleFor = { candidate ->
                            String.format(visitEventTitleTemplate, candidate.label, current.address)
                        },
                        onSchedule = viewModel::scheduleVisit,
                        onResultChange = viewModel::updateVisitResult
                    )
                }

                SectionCard {
                    ContactLogSection(logs = contactLogs, onAddLog = { note, followUpDate -> viewModel.addContactLog(note, followUpDate) })
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/**
 * فاز ۶ — نمایش مشخصات تکمیلیِ بسته به نوع ملک که در فرم ثبت وارد شده، مطابق بازطراحی کامل فرم
 * (آپارتمان/ویلایی/تجاری/زمین). نوع «اداری» دیگر جدا نیست؛ به‌صورت [property.commercialUsage]
 * زیر تجاری نشان داده می‌شود. اگر برای این رکورد هیچ‌کدام پر نشده باشد، کارتی نشان داده نمی‌شود.
 */
@Composable
private fun PropertyDetailedSpecsSection(property: PropertyEntity) {
    val type = property.propertyType
    val isApartmentOrVilla = type == PropertyType.APARTMENT || type == PropertyType.VILLA
    val hasFinishes = isApartmentOrVilla || type == PropertyType.COMMERCIAL
    val rows = mutableListOf<Pair<String, String>>()

    property.deedType?.let { rows += stringResource(R.string.label_deed_type) to it.toPersianLabel() }
    if (type == PropertyType.VILLA) {
        property.totalArea?.let { rows += stringResource(R.string.label_area_total) to it.toPlainSpecString() }
    }
    if (type == PropertyType.COMMERCIAL) {
        property.balconyArea?.let { rows += stringResource(R.string.label_area_balcony) to it.toPlainSpecString() }
    }
    if (type != PropertyType.LAND) {
        property.floor?.let { rows += stringResource(R.string.label_floor) to it.toString() }
        property.totalFloors?.let { rows += stringResource(R.string.label_total_floors) to it.toString() }
        if (type == PropertyType.APARTMENT) property.unitsPerFloor?.let { rows += stringResource(R.string.label_units_per_floor) to it.toString() }
        if (isApartmentOrVilla) property.buildingAge?.let { rows += stringResource(R.string.label_building_age) to it.toString() }
    }
    if (hasFinishes) property.buildingClass?.let { rows += stringResource(R.string.label_building_class) to it.toPersianLabel() }

    if (isApartmentOrVilla) {
        property.cabinetMaterial?.let { rows += stringResource(R.string.label_cabinet_material) to it.toPersianLabel() }
    }
    if (hasFinishes) {
        property.flooring?.let { rows += stringResource(R.string.label_flooring) to it.toPersianLabel() }
        property.wallCovering?.let { rows += stringResource(R.string.label_wall_covering) to it.toPersianLabel() }
        property.ceilingCovering?.let { rows += stringResource(R.string.label_ceiling_covering) to it.toPersianLabel() }
        property.coolingSystem?.let { rows += stringResource(R.string.label_cooling_system) to it.toPersianLabel() }
        property.heatingSystem?.let { rows += stringResource(R.string.label_heating_system) to it.toPersianLabel() }
    }

    if (isApartmentOrVilla) {
        property.terraceArea?.let { rows += stringResource(R.string.label_terrace_area) to it.toPlainSpecString() }
    }
    if (type == PropertyType.VILLA) {
        property.yardArea?.let { rows += stringResource(R.string.label_yard_area) to it.toPlainSpecString() }
        property.masterBedroomCount?.let { rows += stringResource(R.string.label_master_bedroom_count) to it.toString() }
    }

    when (type) {
        PropertyType.LAND -> {
            property.landUse?.let { rows += stringResource(R.string.label_land_use) to it.toPersianLabel() }
            property.frontageWidth?.let { rows += stringResource(R.string.label_frontage_width_land) to it.toPlainSpecString() }
            property.streetWidth?.let { rows += stringResource(R.string.label_street_width) to it.toPlainSpecString() }
            property.buildingPermitArea?.let { rows += stringResource(R.string.label_building_permit_area) to it.toPlainSpecString() }
            property.landPosition?.let { rows += stringResource(R.string.label_land_position) to it.toPersianLabel() }
            property.landSlope?.let { rows += stringResource(R.string.label_land_slope) to it.toPersianLabel() }
        }
        PropertyType.COMMERCIAL -> {
            property.commercialUsage?.let { rows += stringResource(R.string.label_commercial_usage) to it.toPersianLabel() }
            property.commercialFloorPosition?.let { rows += stringResource(R.string.label_commercial_floor_position) to it.toPersianLabel() }
            property.streetPosition?.let { rows += stringResource(R.string.label_street_position) to it.toPersianLabel() }
            property.frontageWidth?.let { rows += stringResource(R.string.label_frontage_width_commercial) to it.toPlainSpecString() }
            property.ceilingHeight?.let { rows += stringResource(R.string.label_ceiling_height) to it.toPlainSpecString() }
        }
        else -> Unit
    }

    // امکانات بولی — فقط مواردی که واقعاً موجودن نشون داده می‌شن (نه یک لیست بلند از «ندارد»)
    val amenities = buildList {
        if (isApartmentOrVilla) {
            if (property.hasKitchenIsland) add(stringResource(R.string.amenity_kitchen_island))
            if (property.hasKitchenette) add(stringResource(R.string.amenity_kitchenette))
            if (property.hasBarbecue) add(stringResource(R.string.amenity_barbecue))
            if (property.hasIranianToilet) add(stringResource(R.string.amenity_iranian_toilet))
            if (property.hasWesternToilet) add(stringResource(R.string.amenity_western_toilet))
            if (property.hasJacuzzi) add(stringResource(R.string.amenity_jacuzzi))
            if (property.hasBuiltInCloset) add(stringResource(R.string.amenity_built_in_closet))
            if (property.hasVideoIntercom) add(stringResource(R.string.amenity_video_intercom))
            if (property.hasAutomaticParkingDoor) add(stringResource(R.string.amenity_automatic_parking_door))
            if (property.hasCaretaker) add(stringResource(R.string.amenity_caretaker))
        }
        if (type == PropertyType.APARTMENT) {
            if (property.hasPrivateParkingPath) add(stringResource(R.string.amenity_private_parking_path))
            if (property.hasSharedParkingPath) add(stringResource(R.string.amenity_shared_parking_path))
            if (property.hasLobby) add(stringResource(R.string.amenity_lobby))
            if (property.hasSecurityGuard) add(stringResource(R.string.amenity_security_guard))
            if (property.hasCourtyard) add(stringResource(R.string.amenity_courtyard))
        }
        if (isApartmentOrVilla) {
            if (property.hasPool) add(stringResource(R.string.amenity_pool))
            if (property.hasGym) add(stringResource(R.string.amenity_gym))
        }
        if (isApartmentOrVilla || type == PropertyType.COMMERCIAL) {
            if (property.hasStorage) add(stringResource(R.string.amenity_storage))
            if (property.hasElevator) add(stringResource(R.string.amenity_elevator))
            if (property.hasParking) add(stringResource(R.string.amenity_parking))
            if (property.hasPrivateWater) add(stringResource(R.string.amenity_private_water))
            if (property.hasSharedWater) add(stringResource(R.string.amenity_shared_water))
            if (property.hasPrivateElectricity) add(stringResource(R.string.amenity_private_electricity))
            if (property.hasSharedElectricity) add(stringResource(R.string.amenity_shared_electricity))
            if (property.hasPrivateGas) add(stringResource(R.string.amenity_private_gas))
            if (property.hasSharedGas) add(stringResource(R.string.amenity_shared_gas))
        }
        if (type == PropertyType.COMMERCIAL) {
            if (property.hasThreePhaseElectricity) add(stringResource(R.string.amenity_three_phase_electricity))
            if (property.hasKitchen) add(stringResource(R.string.amenity_kitchen))
            if (property.hasRestroom) add(stringResource(R.string.amenity_restroom))
        }
        if (type == PropertyType.LAND) {
            if (property.hasWall) add(stringResource(R.string.amenity_wall))
            if (property.waterRightOwned) add(stringResource(R.string.amenity_water_right_owned))
            if (property.waterRightObtainable) add(stringResource(R.string.amenity_water_right_obtainable))
            if (property.electricityRightOwned) add(stringResource(R.string.amenity_electricity_right_owned))
            if (property.electricityRightObtainable) add(stringResource(R.string.amenity_electricity_right_obtainable))
            if (property.gasRightOwned) add(stringResource(R.string.amenity_gas_right_owned))
            if (property.gasRightObtainable) add(stringResource(R.string.amenity_gas_right_obtainable))
        }
    }

    if (rows.isEmpty() && amenities.isEmpty()) return

    SectionCard(title = stringResource(R.string.spec_section_common)) {
        rows.forEach { (label, value) -> InfoRow(label, value) }
        if (amenities.isNotEmpty()) {
            if (rows.isNotEmpty()) Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.spec_section_amenities), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amenities.joinToString("، "), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** فرمت متراژ/طول/ارتفاع بدون ".0" اضافه برای اعداد صحیح (مثلاً «۱۲» به‌جای «۱۲.۰»). */
private fun Double.toPlainSpecString(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSelector(current: PropertyStatus, onSelect: (PropertyStatus) -> Unit) {
    val labels = mapOf(
        PropertyStatus.AVAILABLE to stringResource(R.string.property_status_available),
        PropertyStatus.RESERVED to stringResource(R.string.property_status_reserved),
        PropertyStatus.SOLD_OR_RENTED to stringResource(R.string.property_status_sold),
        PropertyStatus.ARCHIVED to stringResource(R.string.property_status_archived)
    )
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels.entries.toList()) { (status, label) ->
            val selected = status == current
            FilterChip(
                selected = selected,
                onClick = { onSelect(status) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}
