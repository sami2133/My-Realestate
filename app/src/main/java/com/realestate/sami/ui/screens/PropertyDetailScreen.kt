package com.realestate.sami.ui.screens

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
import com.realestate.sami.util.toPersianDateString
import com.realestate.sami.util.toTomanDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    onBack: () -> Unit,
    onClientClick: (ClientEntity) -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: PropertyDetailViewModel = hiltViewModel()
) {
    val property by viewModel.property.collectAsState()
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
                            // این عکس روی یک دستگاه دیگه‌ی تیم ثبت شده؛ با «همگام‌سازی الان» دانلود می‌شود
                            Box(
                                Modifier
                                    .size(220.dp, 150.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CloudDownload,
                                    contentDescription = stringResource(R.string.image_not_downloaded_yet),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
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
                    InfoRow(stringResource(R.string.label_rooms), "${current.rooms}")
                    InfoRow(stringResource(R.string.amenity_parking), if (current.hasParking) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.amenity_storage), if (current.hasStorage) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.amenity_elevator), if (current.hasElevator) stringResource(R.string.value_yes) else stringResource(R.string.value_no))
                    InfoRow(stringResource(R.string.label_address), current.address)
                    InfoRow(stringResource(R.string.label_registered_at), current.createdAt.toPersianDateString())
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
 * فاز ۵.۵/۵.۶ — نمایش مشخصات تکمیلیِ بسته به نوع ملک که در فرم ثبت وارد شده (نوع سند، جهت واحد،
 * کلاس ساختمان، امتیازات آب/برق/گاز، مشخصات زمین/تجاری/اداری و…). اگر برای این رکورد هیچ‌کدام
 * پر نشده باشد (مثلاً رکوردهای قدیمی‌تر از قبل از این فاز)، اصلاً کارتی نشان داده نمی‌شود.
 */
@Composable
private fun PropertyDetailedSpecsSection(property: PropertyEntity) {
    val type = property.propertyType
    val rows = mutableListOf<Pair<String, String>>()

    property.deedType?.let { rows += stringResource(R.string.label_deed_type) to it.toPersianLabel() }
    if (type != PropertyType.LAND) {
        property.buildingAge?.let { rows += stringResource(R.string.label_building_age) to it.toString() }
        property.floor?.let { rows += stringResource(R.string.label_floor) to it.toString() }
        property.totalFloors?.let { rows += stringResource(R.string.label_total_floors) to it.toString() }
    }
    if (type == PropertyType.APARTMENT || type == PropertyType.VILLA || type == PropertyType.LAND) {
        property.waterStatus?.let { rows += stringResource(R.string.label_water_status) to it.toPersianLabel() }
        property.electricityStatus?.let { rows += stringResource(R.string.label_electricity_status) to it.toPersianLabel() }
        property.gasStatus?.let { rows += stringResource(R.string.label_gas_status) to it.toPersianLabel() }
    }
    if (type == PropertyType.APARTMENT || type == PropertyType.OFFICE) {
        property.buildingClass?.let { rows += stringResource(R.string.label_building_class) to it.toPersianLabel() }
        property.heatingCoolingSystem?.let { rows += stringResource(R.string.label_heating_cooling) to it.toPersianLabel() }
    }
    if (type == PropertyType.COMMERCIAL || type == PropertyType.OFFICE) {
        property.streetPosition?.let { rows += stringResource(R.string.label_street_position) to it.toPersianLabel() }
    }
    if (type == PropertyType.LAND || type == PropertyType.COMMERCIAL) {
        val frontageLabel = if (type == PropertyType.LAND) stringResource(R.string.label_frontage_width_land)
        else stringResource(R.string.label_frontage_width_commercial)
        property.frontageWidth?.let { rows += frontageLabel to it.toPlainSpecString() }
    }

    when (type) {
        PropertyType.APARTMENT -> {
            property.unitDirection?.let { rows += stringResource(R.string.label_unit_direction) to it.toPersianLabel() }
            property.unitCondition?.let { rows += stringResource(R.string.label_unit_condition) to it.toPersianLabel() }
            property.flooring?.let { rows += stringResource(R.string.label_flooring) to it.toPersianLabel() }
            property.facade?.let { rows += stringResource(R.string.label_facade) to it.toPersianLabel() }
            property.bathroomCount?.let { rows += stringResource(R.string.label_bathroom_count) to it.toString() }
        }
        PropertyType.LAND -> {
            property.landUse?.let { rows += stringResource(R.string.label_land_use) to it.toPersianLabel() }
            property.streetWidth?.let { rows += stringResource(R.string.label_street_width) to it.toPlainSpecString() }
            property.allowedDensity?.let { rows += stringResource(R.string.label_allowed_density) to "$it٪" }
            property.allowedFloors?.let { rows += stringResource(R.string.label_allowed_floors) to it.toString() }
            property.landPosition?.let { rows += stringResource(R.string.label_land_position) to it.toPersianLabel() }
            property.landSlope?.let { rows += stringResource(R.string.label_land_slope) to it.toPersianLabel() }
        }
        PropertyType.COMMERCIAL -> {
            property.keyMoney?.let { rows += stringResource(R.string.label_key_money) to it.toTomanDisplay() }
            property.commercialFloorPosition?.let { rows += stringResource(R.string.label_commercial_floor_position) to it.toPersianLabel() }
            property.ceilingHeight?.let { rows += stringResource(R.string.label_ceiling_height) to it.toPlainSpecString() }
            if (!property.businessLicenseType.isNullOrBlank()) rows += stringResource(R.string.label_business_license_type) to property.businessLicenseType
        }
        PropertyType.OFFICE -> {
            property.partitionCount?.let { rows += stringResource(R.string.label_partition_count) to it.toString() }
        }
        PropertyType.VILLA -> Unit
    }

    // امکانات بولی — فقط مواردی که واقعاً موجودن نشون داده می‌شن (نه یک لیست بلند از «ندارد»)
    val amenities = buildList {
        if (property.hasBalcony) add(stringResource(R.string.amenity_balcony))
        if (property.hasPool) add(stringResource(R.string.amenity_pool))
        if (property.hasSauna) add(stringResource(R.string.amenity_sauna))
        if (property.hasGym) add(stringResource(R.string.amenity_gym))
        if (property.hasVideoIntercom) add(stringResource(R.string.amenity_video_intercom))
        if (property.hasLobby) add(stringResource(R.string.amenity_lobby))
        if (property.hasSecurityGuard) add(stringResource(R.string.amenity_security_guard))
        if (property.hasWall) add(stringResource(R.string.amenity_wall))
        if (property.hasBuildingPermit) add(stringResource(R.string.amenity_building_permit))
        if (property.hasThreePhaseElectricity) add(stringResource(R.string.amenity_three_phase_electricity))
        if (property.hasRestroom) add(stringResource(R.string.amenity_restroom))
        if (property.hasFalseFloor) add(stringResource(R.string.amenity_false_floor))
        if (property.hasFalseCeiling) add(stringResource(R.string.amenity_false_ceiling))
        if (property.hasConferenceRoom) add(stringResource(R.string.amenity_conference_room))
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
