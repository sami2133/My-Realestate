package com.realestate.sami.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.realestate.sami.R
import com.realestate.sami.ui.screens.*

sealed class Screen(val route: String, @StringRes val labelRes: Int) {
    data object PropertyList : Screen("property_list", R.string.nav_properties)
    data object AddProperty : Screen("add_property", R.string.nav_add_property)
    data object PropertyDetail : Screen("property_detail/{propertyId}", R.string.nav_property_detail) {
        fun buildRoute(id: Long) = "property_detail/$id"
    }
    data object PropertiesMap : Screen("properties_map", R.string.nav_map)
    data object LocationPicker : Screen("location_picker", R.string.map_picker_title)
    data object ClientList : Screen("client_list", R.string.nav_clients)
    data object AddClient : Screen("add_client", R.string.nav_add_client)
    data object ClientDetail : Screen("client_detail/{clientId}", R.string.nav_client_detail) {
        fun buildRoute(id: Long) = "client_detail/$id"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.PropertyList, Screen.PropertiesMap, Screen.ClientList)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                bottomItems.forEach { screen ->
                    val icon = when (screen) {
                        Screen.PropertyList -> Icons.Filled.Home
                        Screen.PropertiesMap -> Icons.Filled.Map
                        else -> Icons.Filled.People
                    }
                    val label = stringResource(screen.labelRes)
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.PropertyList.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(Screen.PropertyList.route) {
                PropertyListScreen(
                    onAddClick = { navController.navigate(Screen.AddProperty.route) },
                    onPropertyClick = { navController.navigate(Screen.PropertyDetail.buildRoute(it.id)) }
                )
            }
            composable(Screen.AddProperty.route) { entry ->
                val pickedLat by entry.savedStateHandle
                    .getStateFlow<Double?>("picked_lat", null)
                    .collectAsState()
                val pickedLng by entry.savedStateHandle
                    .getStateFlow<Double?>("picked_lng", null)
                    .collectAsState()
                AddPropertyScreen(
                    onSaved = { navController.popBackStack() },
                    onPickLocationOnMap = { navController.navigate(Screen.LocationPicker.route) },
                    pickedLatitude = pickedLat,
                    pickedLongitude = pickedLng
                )
            }
            composable(
                Screen.PropertyDetail.route,
                arguments = listOf(navArgument("propertyId") { type = NavType.LongType })
            ) {
                PropertyDetailScreen(
                    onBack = { navController.popBackStack() },
                    onClientClick = { navController.navigate(Screen.ClientDetail.buildRoute(it.id)) }
                )
            }
            composable(Screen.PropertiesMap.route) {
                PropertiesMapScreen(
                    onPropertyClick = { navController.navigate(Screen.PropertyDetail.buildRoute(it.id)) }
                )
            }
            composable(Screen.LocationPicker.route) {
                LocationPickerScreen(
                    initialLatitude = null,
                    initialLongitude = null,
                    onBack = { navController.popBackStack() },
                    onConfirm = { lat, lng ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_lat", lat)
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_lng", lng)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.ClientList.route) {
                ClientListScreen(
                    onAddClick = { navController.navigate(Screen.AddClient.route) },
                    onClientClick = { navController.navigate(Screen.ClientDetail.buildRoute(it.id)) }
                )
            }
            composable(Screen.AddClient.route) {
                AddClientScreen(onSaved = { navController.popBackStack() })
            }
            composable(
                Screen.ClientDetail.route,
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) {
                ClientDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPropertyClick = { navController.navigate(Screen.PropertyDetail.buildRoute(it.id)) }
                )
            }
        }
    }
}
