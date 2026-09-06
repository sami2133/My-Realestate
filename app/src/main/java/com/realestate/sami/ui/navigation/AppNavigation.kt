package com.realestate.sami.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.realestate.sami.ui.screens.*

sealed class Screen(val route: String, val label: String) {
    data object PropertyList : Screen("property_list", "ملک‌ها")
    data object AddProperty : Screen("add_property", "ثبت ملک")
    data object PropertyDetail : Screen("property_detail/{propertyId}", "جزئیات ملک") {
        fun buildRoute(id: Long) = "property_detail/$id"
    }
    data object ClientList : Screen("client_list", "متقاضیان")
    data object AddClient : Screen("add_client", "ثبت متقاضی")
    data object ClientDetail : Screen("client_detail/{clientId}", "جزئیات متقاضی") {
        fun buildRoute(id: Long) = "client_detail/$id"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.PropertyList, Screen.ClientList)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                bottomItems.forEach { screen ->
                    val icon = if (screen == Screen.PropertyList) Icons.Filled.Home else Icons.Filled.Person
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
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
            composable(Screen.AddProperty.route) {
                AddPropertyScreen(
                    onSaved = { navController.popBackStack() },
                    onPickLocationOnMap = { /* TODO فاز ۳: یکپارچه‌سازی نقشه */ }
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
