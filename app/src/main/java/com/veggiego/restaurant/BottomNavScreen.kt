package com.veggiego.restaurant

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler

@Composable
fun BottomNavScreen(
    onLogout: () -> Unit
) {

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var ordersInitialTab by remember {
        mutableIntStateOf(0)
    }
    var showTimingScreen by remember {
        mutableStateOf(false)
    }
    var showSalesScreen by remember {
        mutableStateOf(false)
    }
    BackHandler(

        enabled = selectedTab != 0 || showTimingScreen

    ) {

        if (showTimingScreen) {

            showTimingScreen = false

        } else {

            selectedTab = 0
        }
    }
    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Orders")
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.RestaurantMenu,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Menu")
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Settings")
                    }
                )
            }
        }

    ) { padding ->
        if (showTimingScreen) {

            TimingScreen(

                onBack = {
                    showTimingScreen = false
                }
            )

            return@Scaffold
        }
        if (showSalesScreen) {

            SalesSummaryScreen(

                onBack = {
                    showSalesScreen = false
                }
            )

            return@Scaffold
        }
        when (selectedTab) {

            0 -> HomeScreen(

                modifier = Modifier.padding(padding),

                onNewOrdersClick = {

                    ordersInitialTab = 0
                    selectedTab = 1
                },

                onPreparingClick = {

                    ordersInitialTab = 1
                    selectedTab = 1
                },

                onReadyClick = {

                    ordersInitialTab = 2
                    selectedTab = 1
                },

                onOnTheWayClick = {

                    ordersInitialTab = 3
                    selectedTab = 1
                },

                onDeliveredClick = {

                    ordersInitialTab = 4
                    selectedTab = 1
                },

                onSalesClick = {

                    showSalesScreen = true
                }
            )

            1 -> OrdersScreen(

                modifier = Modifier.padding(padding),

                initialTab = ordersInitialTab
            )
            2 -> MenuScreen()
            3 -> SettingsScreen(

                modifier = Modifier.padding(padding),

                onTimingClick = {
                    showTimingScreen = true
                },

                onLogout = onLogout
            )
        }
    }
}