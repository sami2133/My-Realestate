package com.realestate.sami.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.realestate.sami.ui.screens.*

sealed class Screen(val route: String, val label: String) {
    data object PropertyList : Screen("property_list", "ملک‌ها")
    data object AddProperty : Screen("add_property", "ثبت ملک")
    data object ClientList : Screen("client_list", "متقاضیان")
    data object AddClient : Screen("add_client", "ثبت متقاضی")
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
                    val icon = if (screen == Screen.PropertyList) Icons.Filled.Home else Icons.Filled.People
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
                    onPropertyClick = { /* TODO فاز بعد: صفحه جزئیات ملک + تطبیق */ }
                )
            }
            composable(Screen.AddProperty.route) {
                AddPropertyScreen(
                    onSaved = { navController.popBackStack() },
                    onPickLocationOnMap = { /* TODO فاز بعد: یکپارچه‌سازی Neshan/Google Maps */ }
                )
            }
            composable(Screen.ClientList.route) {
                ClientListScreen(
                    onAddClick = { navController.navigate(Screen.AddClient.route) },
                    onClientClick = { /* TODO فاز بعد: صفحه جزئیات متقاضی + تطبیق */ }
                )
            }
            composable(Screen.AddClient.route) {
                AddClientScreen(onSaved = { navController.popBackStack() })
            }
        }
    }
}
