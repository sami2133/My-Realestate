package com.realestate.sami.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.data.local.entity.PropertyType
import com.realestate.sami.ui.screens.common.DealTypeChip
import com.realestate.sami.ui.screens.common.MapTypeSwitcher
import com.realestate.sami.ui.screens.common.color
import com.realestate.sami.ui.viewmodel.PropertyFilter
import com.realestate.sami.ui.viewmodel.PropertySortOption
import com.realestate.sami.ui.viewmodel.PropertyViewMode
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.util.PropertyExporter
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
                    PropertyExportButton(properties = properties)
                    PropertyFilterButton(filter = filter, onApply = viewModel::onFilterChanged)
                    PropertySortMenu(current = sortOption, onSelect = viewModel::onSortOptionChanged)
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

@Composable
private fun PropertyExportButton(properties: List<PropertyEntity>) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.reports_export_section))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reports_export_pdf)) },
                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                onClick = {
                    expanded = false
                    PropertyExporter.exportToPdf(context, properties)?.let {
                        PropertyExporter.shareFile(context, it, "application/pdf")
                    }
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reports_export_excel)) },
                leadingIcon = { Icon(Icons.Filled.TableChart, contentDescription = null) },
                onClick = {
                    expanded = false
                    PropertyExporter.exportToExcel(context, properties)?.let {
                        PropertyExporter.shareFile(
                            context, it,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PropertySortMenu(current: PropertySortOption, onSelect: (PropertySortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        PropertySortOption.NEWEST to stringResource(R.string.sort_newest),
        PropertySortOption.OLDEST to stringResource(R.string.sort_oldest),
        PropertySortOption.PRICE_LOW_TO_HIGH to stringResource(R.string.property_sort_price_low_to_high),
        PropertySortOption.PRICE_HIGH_TO_LOW to stringResource(R.string.property_sort_price_high_to_low),
        PropertySortOption.AREA_LARGE_TO_SMALL to stringResource(R.string.property_sort_area_large_to_small)
    )
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.action_sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = { if (option == current) Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PropertyFilterButton(filter: PropertyFilter, onApply: (PropertyFilter) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    BadgedBox(badge = { if (filter.minPrice != null || filter.maxPrice != null) Badge() }) {
        IconButton(onClick = { showDialog = true }) {
            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.action_filter))
        }
    }
    if (showDialog) {
        PropertyFilterDialog(
            initialFilter = filter,
            onDismiss = { showDialog = false },
            onApply = { newFilter ->
                onApply(newFilter)
                showDialog = false
            }
        )
    }
}

/**
 * فاز ۵.۴: چون نوع معامله (تب بالای صفحه) و نوع ملک (نوار چیپ‌ها) الان همیشه در دسترس و مستقیماً
 * قابل‌تنظیمن، این دیالوگ فقط بازه‌ی قیمت رو می‌گیره — بدون تکرار همون دو فیلتر. مقدار فعلی
 * dealType/propertyType از [initialFilter] حفظ و بدون تغییر برگردونده می‌شه.
 */
@Composable
private fun PropertyFilterDialog(
    initialFilter: PropertyFilter,
    onDismiss: () -> Unit,
    onApply: (PropertyFilter) -> Unit
) {
    var minPrice by remember { mutableStateOf(initialFilter.minPrice?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(initialFilter.maxPrice?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.filter_dialog_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            minPrice = ""
                            maxPrice = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.filter_clear)) }
                    Button(
                        onClick = {
                            onApply(
                                initialFilter.copy(
                                    minPrice = minPrice.parseTomanInput(),
                                    maxPrice = maxPrice.parseTomanInput()
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.filter_apply)) }
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
            Column(Modifier.padding(14.dp).weight(1f)) {
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
                    stringResource(R.string.property_card_area_rooms, property.area.toInt(), property.rooms),
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
