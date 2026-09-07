package com.realestate.sami.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberCameraPositionState
import com.realestate.sami.R
import com.realestate.sami.data.local.entity.PropertyEntity
import com.realestate.sami.ui.screens.common.color
import com.realestate.sami.ui.viewmodel.PropertyViewModel
import com.realestate.sami.util.toTomanShort

/** مرکز پیش‌فرض نقشه: تهران — وقتی هنوز ملکی با موقعیت ثبت نشده. */
private val DEFAULT_LOCATION = LatLng(35.6892, 51.3890)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesMapScreen(
    onPropertyClick: (PropertyEntity) -> Unit,
    viewModel: PropertyViewModel = hiltViewModel()
) {
    val properties by viewModel.properties.collectAsState()
    val located = properties.filter { it.latitude != null && it.longitude != null }

    val cameraPositionState = rememberCameraPositionState {
        val first = located.firstOrNull()
        position = CameraPosition.fromLatLngZoom(
            if (first != null) LatLng(first.latitude!!, first.longitude!!) else DEFAULT_LOCATION,
            if (first != null) 12f else 10f
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.properties_map_title)) }) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                located.forEach { property ->
                    MarkerInfoWindowContent(
                        state = com.google.maps.android.compose.rememberMarkerState(
                            position = LatLng(property.latitude!!, property.longitude!!)
                        ),
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
}
