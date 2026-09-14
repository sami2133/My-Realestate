package com.realestate.sami.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.realestate.sami.R
import com.realestate.sami.ui.screens.common.MapTypeSwitcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** مرکز پیش‌فرض نقشه: تهران — وقتی هنوز موقعیتی انتخاب نشده. */
private val DEFAULT_LOCATION = LatLng(35.6892, 51.3890)

/**
 * آدرس نهایی را از اجزای ساخت‌یافته‌ی [android.location.Address] می‌سازد (خیابان، محله، شهر)
 * به‌جای [android.location.Address.getAddressLine] خام. بعضی وقت‌ها getAddressLine(0) به‌جای
 * آدرس معمولی، یک Plus Code گوگل (مثل «GXXX+XX تهران») برمی‌گرداند که برای کاربر قابل‌فهم نیست؛
 * ساختن آدرس از thoroughfare/subLocality/locality این مشکل را حل می‌کند و اگر هیچ‌کدام موجود
 * نبود، به همان addressLine (در صورت نبودن الگوی Plus Code) برمی‌گردد.
 */
private fun buildStandardAddress(address: android.location.Address): String? {
    val plusCodePattern = Regex("^[23456789CFGHJMPQRVWX]{4,8}\\+[23456789CFGHJMPQRVWX]{2,3}")
    val parts = listOfNotNull(
        address.thoroughfare,
        address.subLocality,
        address.locality ?: address.subAdminArea,
        address.adminArea
    ).filter { it.isNotBlank() }

    if (parts.isNotEmpty()) return parts.distinct().joinToString("، ")

    val line = address.getAddressLine(0)
    return if (line != null && !plusCodePattern.containsMatchIn(line)) line else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onBack: () -> Unit,
    onConfirm: (lat: Double, lng: Double, address: String?) -> Unit
) {
    val context = LocalContext.current
    val startPoint = if (initialLatitude != null && initialLongitude != null) {
        LatLng(initialLatitude, initialLongitude)
    } else DEFAULT_LOCATION

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPoint, 15f)
    }

    var selectedPosition by remember { mutableStateOf(startPoint) }
    var isResolvingAddress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.map_picker_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            isResolvingAddress = true
                            scope.launch {
                                val address = withContext(Dispatchers.IO) {
                                    try {
                                        @Suppress("DEPRECATION")
                                        val result = Geocoder(context, Locale("fa", "IR"))
                                            .getFromLocation(selectedPosition.latitude, selectedPosition.longitude, 1)
                                            ?.firstOrNull()
                                        result?.let { buildStandardAddress(it) }
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                isResolvingAddress = false
                                onConfirm(selectedPosition.latitude, selectedPosition.longitude, address)
                            }
                        },
                        enabled = !isResolvingAddress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.map_picker_confirm))
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission, mapType = mapType),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = true),
                onMapClick = { latLng -> selectedPosition = latLng }
            ) {
                Marker(state = MarkerState(position = selectedPosition))
            }

            MapTypeSwitcher(
                currentType = mapType,
                onTypeSelected = { mapType = it },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            )

            FloatingActionButton(
                onClick = {
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        try {
                            fusedClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    val target = LatLng(location.latitude, location.longitude)
                                    selectedPosition = target
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 16f)
                                }
                            }
                        } catch (_: SecurityException) {
                            // کاربر دسترسی را رد کرده؛ نادیده گرفته می‌شود
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 96.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_picker_use_my_location))
            }
        }
    }
}
