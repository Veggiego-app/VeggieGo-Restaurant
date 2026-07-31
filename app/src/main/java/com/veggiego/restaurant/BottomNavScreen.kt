package com.veggiego.restaurant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun BottomNavScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var ordersInitialTab by remember { mutableIntStateOf(0) }
    var overlay by remember { mutableStateOf("") }

    BackHandler(enabled = selectedTab != 0 || overlay.isNotBlank()) {
        if (overlay.isNotBlank()) overlay = "" else selectedTab = 0
    }

    Scaffold(bottomBar = {
        if (overlay.isBlank()) {
            NavigationBar {
                NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Orders") })
                NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, { Icon(Icons.Default.RestaurantMenu, null) }, label = { Text("Menu") })
                NavigationBarItem(selectedTab == 3, { selectedTab = 3 }, { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    }) { padding ->
        when (overlay) {
            "TIMING" -> TimingScreen { overlay = "" }
            "TODAY_ORDERS" -> SalesOrdersScreen("Today's Orders") { overlay = "" }
            "TODAY_SALES" -> SalesOrdersScreen("Today's Sales") { overlay = "" }
            "SALES" -> SalesSummaryScreen { overlay = "" }
            "PAYOUT" -> PayoutReportScreen { overlay = "" }
            else -> when (selectedTab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    onTodayOrdersClick = { overlay = "TODAY_ORDERS" },
                    onTodaySalesClick = { overlay = "TODAY_SALES" },
                    onAllOrdersClick = { ordersInitialTab = 0; selectedTab = 1 },
                    onNewOrdersClick = { ordersInitialTab = 1; selectedTab = 1 },
                    onPreparingClick = { ordersInitialTab = 2; selectedTab = 1 },
                    onReadyClick = { ordersInitialTab = 3; selectedTab = 1 },
                    onOnTheWayClick = { ordersInitialTab = 4; selectedTab = 1 },
                    onDeliveredClick = { ordersInitialTab = 5; selectedTab = 1 },
                    onCancelledClick = { ordersInitialTab = 6; selectedTab = 1 },
                    onPayoutReportClick = { overlay = "PAYOUT" }
                )
                1 -> OrdersScreen(Modifier.padding(padding), ordersInitialTab)
                2 -> MenuScreen()
                3 -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    onTimingClick = { overlay = "TIMING" },
                    onLogout = onLogout
                )
            }
        }
    }
}
