package com.realestate.sami.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.DealType
import com.realestate.sami.data.local.entity.DeedType
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyStatus
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.screens.common.DealTypeChip
import com.realestate.sami.ui.screens.common.MapTypeSwitcher
import com.realestate.sami.ui.screens.common.color
import com.realestate.sami.ui.viewmodel.PropertyFilter
import com.realestate.sami.ui.viewmodel.PropertySortOption
import com.realestate.sami.ui.viewmodel.PropertyViewMode
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.util.parseIntInput
import com.realestate.sami.util.parseNumberInput
import com.realestate.sami.util.parseTomanInput
import com.realestate.sami.util.toTomanShort

/** مرکز پیش‌فرض نقشه: تهران — وقتی هنوز ملکی با موقعیت ثبت نشده. */
private val DEFAULT_MAP_LOCATION = LatLng(35.6892, 51.3890)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyListScreen(
    onAddClick: () -> Unit,
    onPropertyClick: (PropertyEntity) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val properties by viewModel.properties.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                title = { Text(stringResource(R.string.property_list_title), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {
                        viewModel.setViewMode(if (viewMode == PropertyViewMode.LIST) PropertyViewMode.MAP else PropertyViewMode.LIST)
                    }) {
                        Icon(
                            if (viewMode == PropertyViewMode.LIST) Icons.Filled.Map else Icons.Filled.List,
                            contentDescription = stringResource(if (viewMode == PropertyViewMode.LIST) R.string.nav_map else R.string.property_list_title)
                        )
                    }
                    FilterSortButton(
                        filter = filter,
                        sortOption = sortOption,
                        onFilterApply = viewModel::onFilterChanged,
                        onSortSelect = viewModel::onSortOptionChanged
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.nav_add_property)) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // فاز ۵.۴: کل عملکرد اپ به دو نوع معامله محدوده — یک تب‌بندی دوتایی ساده و همیشه یکی انتخاب‌شده،
            // دقیقاً مثل اپ‌های تخصصی املاک.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                DealTypeTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = filter.dealType == tab.dealType,
                        onClick = { viewModel.setDealTypeTab(tab.dealType) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DealTypeTab.entries.size)
                    ) { Text(stringResource(tab.labelRes)) }
                }
            }

            // زیرمجموعه‌ی نوع ملک — همیشه در دسترس، داخل همین تب.
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter.propertyType == null,
                        onClick = { viewModel.onFilterChanged(filter.copy(propertyType = null)) },
                        label = { Text(stringResource(R.string.filter_option_all)) }
                    )
                }
                items(PropertyType.entries.toList()) { type ->
                    FilterChip(
                        selected = filter.propertyType == type,
                        onClick = { viewModel.onFilterChanged(filter.copy(propertyType = type)) },
                        label = { Text(type.toPersianLabel()) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (viewMode == PropertyViewMode.MAP) {
                PropertyMapBody(properties = properties, onPropertyClick = onPropertyClick, modifier = Modifier.weight(1f))
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onSearchChanged,
                    label = { Text(stringResource(R.string.property_list_search_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                if (properties.isEmpty()) {
                    EmptyState(
                        text = if (filter.isActive) stringResource(R.string.property_list_empty_filtered)
                        else stringResource(R.string.property_list_empty)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(properties, key = { it.id }) { property ->
                            PropertyCard(property, onClick = { onPropertyClick(property) })
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }
}

/** دو تب ثابت بالای صفحه‌ی ملک‌ها. */
private enum class DealTypeTab(val dealType: DealType, val labelRes: Int) {
    SALE(DealType.SALE, R.string.deal_type_sale),
    RENT(DealType.RENT, R.string.deal_type_rent)
}

@Composable
private fun PropertyMapBody(
    properties: List<PropertyEntity>,
    onPropertyClick: (PropertyEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val located = properties.filter { it.latitude != null && it.longitude != null }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    val cameraPositionState = rememberCameraPositionState {
        val first = located.firstOrNull()
        position = CameraPosition.fromLatLngZoom(
            if (first != null) LatLng(first.latitude!!, first.longitude!!) else DEFAULT_MAP_LOCATION,
            if (first != null) 12f else 10f
        )
    }

    Box(modifier.fillMaxWidth()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapType)
        ) {
            located.forEach { property ->
                MarkerInfoWindowContent(
                    state = rememberMarkerState(position = LatLng(property.latitude!!, property.longitude!!)),
                    onClick = { onPropertyClick(property); true }
                ) {
                    Surface(shape = MaterialTheme.shapes.small, tonalElevation = 4.dp) {
                        Column(Modifier.padding(10.dp)) {
                            Text(property.address, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(
                                property.totalPrice?.toTomanShort() ?: property.rentPrice?.toTomanShort().orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = property.dealType.color()
                            )
                        }
                    }
                }
            }
        }

        MapTypeSwitcher(
            currentType = mapType,
            onTypeSelected = { mapType = it },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        )

        if (located.isEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp
            ) {
                Text(
                    stringResource(R.string.properties_map_empty),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** گزینه‌های بازه‌ی قدمت بنا در فیلتر — هر گزینه معادل یک بازه‌ی min/max مشخص است. */
private enum class BuildingAgeRange(val minAge: Int?, val maxAge: Int?, val labelRes: Int) {
    NEW(0, 2, R.string.filter_building_age_new),
    RANGE_2_5(2, 5, R.string.filter_building_age_2_5),
    RANGE_5_10(5, 10, R.string.filter_building_age_5_10),
    TEN_PLUS(10, null, R.string.filter_building_age_10_plus);

    companion object {
        fun fromFilter(filter: PropertyFilter): BuildingAgeRange? =
            entries.find { it.minAge == filter.minBuildingAge && it.maxAge == filter.maxBuildingAge }
    }
}

private enum class FilterSortTab(val labelRes: Int) {
    FILTER(R.string.filter_sort_tab_filter),
    SORT(R.string.filter_sort_tab_sort)
}

/** دکمه‌ی ادغام‌شده‌ی فیلتر و ترتیب — به‌جای دو آیکون جدا، یک شیت با دو تب باز می‌کند. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortButton(
    filter: PropertyFilter,
    sortOption: PropertySortOption,
    onFilterApply: (PropertyFilter) -> Unit,
    onSortSelect: (PropertySortOption) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    BadgedBox(badge = { if (filter.isActive) Badge() }) {
        IconButton(onClick = { showSheet = true }) {
            Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.action_filter_sort))
        }
    }
    if (showSheet) {
        FilterSortBottomSheet(
            initialFilter = filter,
            currentSort = sortOption,
            onDismiss = { showSheet = false },
            onApplyFilter = { onFilterApply(it); showSheet = false },
            onSelectSort = onSortSelect
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortBottomSheet(
    initialFilter: PropertyFilter,
    currentSort: PropertySortOption,
    onDismiss: () -> Unit,
    onApplyFilter: (PropertyFilter) -> Unit,
    onSelectSort: (PropertySortOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf(FilterSortTab.FILTER) }

    // ===== حالت محلی فیلدهای فیلتر =====
    var status by remember { mutableStateOf(initialFilter.status) }
    var minPrice by remember { mutableStateOf(initialFilter.minPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(initialFilter.maxPrice?.toString() ?: "") }
    var minArea by remember { mutableStateOf(initialFilter.minArea?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var maxArea by remember { mutableStateOf(initialFilter.maxArea?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    var rooms by remember { mutableStateOf(initialFilter.rooms) }
    var buildingAgeRange by remember { mutableStateOf(BuildingAgeRange.fromFilter(initialFilter)) }
    var requireElevator by remember { mutableStateOf(initialFilter.requireElevator) }
    var requireParking by remember { mutableStateOf(initialFilter.requireParking) }
    var requireStorage by remember { mutableStateOf(initialFilter.requireStorage) }
    var deedType by remember { mutableStateOf(initialFilter.deedType) }
    var exchangeableOnly by remember { mutableStateOf(initialFilter.exchangeableOnly) }

    fun buildFilter(): PropertyFilter = initialFilter.copy(
        status = status,
        minPrice = minPrice.parseTomanInput(),
        maxPrice = maxPrice.parseTomanInput(),
        minArea = minArea.parseNumberInput(),
        maxArea = maxArea.parseNumberInput(),
        rooms = rooms,
        minBuildingAge = buildingAgeRange?.minAge,
        maxBuildingAge = buildingAgeRange?.maxAge,
        requireElevator = requireElevator,
        requireParking = requireParking,
        requireStorage = requireStorage,
        deedType = deedType,
        exchangeableOnly = exchangeableOnly
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            TabRow(selectedTabIndex = tab.ordinal) {
                FilterSortTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = { tab = t },
                        text = { Text(stringResource(t.labelRes)) }
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (tab == FilterSortTab.FILTER) {
                    // --- وضعیت ملک ---
                    Text(stringResource(R.string.filter_status_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = status == null,
                                onClick = { status = null },
                                label = { Text(stringResource(R.string.filter_option_all)) }
                            )
                        }
                        items(PropertyStatus.entries.toList()) { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.toPersianLabel()) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.filter_price_range_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minPrice,
                            onValueChange = { minPrice = it },
                            label = { Text(stringResource(R.string.filter_min_price)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxPrice,
                            onValueChange = { maxPrice = it },
                            label = { Text(stringResource(R.string.filter_max_price)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.filter_area_range_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minArea,
                            onValueChange = { minArea = it },
                            label = { Text(stringResource(R.string.filter_min_area)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxArea,
                            onValueChange = { maxArea = it },
                            label = { Text(stringResource(R.string.filter_max_area)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.filter_rooms_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = rooms == null,
                                onClick = { rooms = null },
                                label = { Text(stringResource(R.string.filter_option_all)) }
                            )
                        }
                        items(listOf(1, 2, 3)) { r ->
                            FilterChip(
                                selected = rooms == r,
                                onClick = { rooms = r },
                                label = { Text(r.toString()) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = rooms == 4,
                                onClick = { rooms = 4 },
                                label = { Text(stringResource(R.string.filter_rooms_4_plus)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.filter_building_age_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = buildingAgeRange == null,
                                onClick = { buildingAgeRange = null },
                                label = { Text(stringResource(R.string.filter_option_all)) }
                            )
                        }
                        items(BuildingAgeRange.entries.toList()) { range ->
                            FilterChip(
                                selected = buildingAgeRange == range,
                                onClick = { buildingAgeRange = range },
                                label = { Text(stringResource(range.labelRes)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.filter_amenities_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { requireElevator = !requireElevator }) {
                        Checkbox(checked = requireElevator, onCheckedChange = { requireElevator = it })
                        Text(stringResource(R.string.amenity_elevator))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { requireParking = !requireParking }) {
                        Checkbox(checked = requireParking, onCheckedChange = { requireParking = it })
                        Text(stringResource(R.string.amenity_parking))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { requireStorage = !requireStorage }) {
                        Checkbox(checked = requireStorage, onCheckedChange = { requireStorage = it })
                        Text(stringResource(R.string.amenity_storage))
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.filter_deed_type_section), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = deedType == null,
                                onClick = { deedType = null },
                                label = { Text(stringResource(R.string.filter_option_all)) }
                            )
                        }
                        items(DeedType.entries.toList()) { d ->
                            FilterChip(
                                selected = deedType == d,
                                onClick = { deedType = d },
                                label = { Text(d.toPersianLabel()) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exchangeableOnly = !exchangeableOnly }) {
                        Checkbox(checked = exchangeableOnly, onCheckedChange = { exchangeableOnly = it })
                        Text(stringResource(R.string.filter_exchangeable_only))
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                status = null
                                minPrice = ""
                                maxPrice = ""
                                minArea = ""
                                maxArea = ""
                                rooms = null
                                buildingAgeRange = null
                                requireElevator = false
                                requireParking = false
                                requireStorage = false
                                deedType = null
                                exchangeableOnly = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.filter_clear)) }
                        Button(
                            onClick = { onApplyFilter(buildFilter()) },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.filter_apply)) }
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    val sortLabels = listOf(
                        PropertySortOption.NEWEST to stringResource(R.string.sort_newest),
                        PropertySortOption.OLDEST to stringResource(R.string.sort_oldest),
                        PropertySortOption.PRICE_LOW_TO_HIGH to stringResource(R.string.property_sort_price_low_to_high),
                        PropertySortOption.PRICE_HIGH_TO_LOW to stringResource(R.string.property_sort_price_high_to_low),
                        PropertySortOption.AREA_LARGE_TO_SMALL to stringResource(R.string.property_sort_area_large_to_small),
                        PropertySortOption.AREA_SMALL_TO_LARGE to stringResource(R.string.property_sort_area_small_to_large),
                        PropertySortOption.ROOMS_MOST_TO_FEWEST to stringResource(R.string.property_sort_rooms_most_to_fewest),
                        PropertySortOption.ROOMS_FEWEST_TO_MOST to stringResource(R.string.property_sort_rooms_fewest_to_most),
                        PropertySortOption.BUILDING_AGE_NEWEST to stringResource(R.string.property_sort_building_age_newest),
                        PropertySortOption.BUILDING_AGE_OLDEST to stringResource(R.string.property_sort_building_age_oldest)
                    )
                    sortLabels.forEach { (option, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = option == currentSort, onClick = { onSelectSort(option) })
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = option == currentSort, onClick = { onSelectSort(option) })
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PropertyCard(property: PropertyEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // نوار رنگی سمت راست به رنگ نوع معامله — سرنخ بصری سریع بدون نیاز به خواندن متن
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(property.dealType.color())
            )
            // بندانگشتی اولین عکس ملک — اگر عکسی ثبت نشده، یک باکس نمادین به‌جاش نشون داده می‌شه؛
            // بدون این، لیست ملک‌ها فقط متن بود و کمتر شبیه یک اپ حرفه‌ای‌ املاک به نظر می‌رسید.
            val thumbnailUri = property.images.firstOrNull()?.localUri
            Box(
                Modifier
                    .padding(vertical = 10.dp, horizontal = 10.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(property.dealType.color().copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailUri != null) {
                    AsyncImage(
                        model = thumbnailUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = null,
                        tint = property.dealType.color().copy(alpha = 0.5f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column(Modifier.padding(top = 14.dp, bottom = 14.dp, start = 4.dp, end = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DealTypeChip(property.dealType, property.dealType.toPersianLabel())
                    Text(property.propertyType.toPersianLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(property.address, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    property.rooms?.let { stringResource(R.string.property_card_area_rooms, property.area.toInt(), it) }
                        ?: stringResource(R.string.property_card_area_only, property.area.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val priceText = property.totalPrice?.toTomanShort()
                    ?: property.rentPrice?.let { stringResource(R.string.property_card_rent_prefix, it.toTomanShort()) }
                    ?: stringResource(R.string.price_not_set)
                Text(priceText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
