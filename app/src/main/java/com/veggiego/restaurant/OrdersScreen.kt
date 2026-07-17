package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun OrdersScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0
) {

    val tabs = listOf(
        "NEW",
        "PREPARING",
        "READY",
        "ON_THE_WAY",
        "COMPLETED",
        "CANCELLED"
    )

    var counts by remember {

        mutableStateOf(
            mapOf<String, Int>()
        )
    }

    var selectedTab by remember {
        mutableIntStateOf(initialTab)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF3E0))
            .pointerInput(selectedTab) {

                detectHorizontalDragGestures {

                        _, dragAmount ->

                    when {

                        dragAmount < -50 -> {

                            if (
                                selectedTab <
                                tabs.lastIndex
                            ) {

                                selectedTab++
                            }
                        }

                        dragAmount > 50 -> {

                            if (
                                selectedTab > 0
                            ) {

                                selectedTab--
                            }
                        }
                    }
                }
            }
    ) {
        LaunchedEffect(Unit) {

            RestaurantRepository()
                .restaurantOrders()

                .addSnapshotListener { value, _ ->

                    if (value == null) return@addSnapshotListener

                    val restaurantOrders =

                        value.documents.filter {

                            it.getString(
                                "restaurantId"
                            ) ==
                                    RestaurantSession.restaurantId
                        }

                    counts = mapOf(

                        "NEW" to restaurantOrders.count {
                            it.getString("status") == "APPROVED"
                        },

                        "PREPARING" to restaurantOrders.count {
                            it.getString("status") == "PREPARING"
                        },

                        "READY" to restaurantOrders.count {

                            it.getString("status") == "READY_FOR_PICKUP"

                                    ||

                                    it.getString("status") == "RIDER_ASSIGNED"
                        },

                        "ON_THE_WAY" to restaurantOrders.count {

                            it.getString("status") == "PICKED_UP"

                                    ||

                                    it.getString("status") == "OUT_FOR_DELIVERY"
                        },

                        "COMPLETED" to restaurantOrders.count {
                            it.getString("status") == "DELIVERED"
                        },

                        "CANCELLED" to restaurantOrders.count {
                            it.getString("status") == "CANCELLED"
                        }
                    )
                }
        }
        ScrollableTabRow(
            selectedTabIndex = selectedTab
        ) {

            tabs.forEachIndexed { index, title ->

                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                    },
                    text = {

                        Text(

                            "$title (${counts[title] ?: 0})"

                        )
                    }
                )
            }
        }

        RestaurantOrdersFirebase(
            selectedTab = tabs[selectedTab]
        )
    }
}