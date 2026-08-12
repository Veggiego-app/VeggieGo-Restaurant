package com.veggiego.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun OrdersScreen(modifier: Modifier = Modifier, initialTab: Int = 0) {
    val tabs = listOf("ALL", "NEW", "PREPARING", "READY", "ON_THE_WAY", "COMPLETED", "CANCELLED")
    var counts by remember { mutableStateOf(emptyMap<String, Int>()) }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(tabs.indices)) }

    DisposableEffect(Unit) {
        val listener = RestaurantRepository().restaurantOrders().addSnapshotListener { value, _ ->
            if (value == null) return@addSnapshotListener
            val docs = value.documents.filter { it.getString("restaurantId") == RestaurantSession.restaurantId }
            counts = mapOf(
                "ALL" to docs.count {
                    it.getString("status") !in setOf("PENDING", "NEW")
                },
                "NEW" to docs.count { it.getString("status") in setOf("APPROVED", "RESTAURANT_PENDING") },
                "PREPARING" to docs.count { it.getString("status") in setOf("ACCEPTED", "PREPARING") },
                "READY" to docs.count { it.getString("status") in setOf("READY_FOR_PICKUP", "RIDER_ASSIGNED") },
                "ON_THE_WAY" to docs.count { it.getString("status") in setOf("PICKED_UP", "OUT_FOR_DELIVERY") },
                "COMPLETED" to docs.count { it.getString("status") == "DELIVERED" },
                "CANCELLED" to docs.count { it.getString("status") in setOf("CANCELLED", "REJECTED") }
            )
        }
        onDispose { listener.remove() }
    }

    Column(
        modifier.fillMaxSize().background(Color(0xFFFFF3E0)).pointerInput(selectedTab) {
            detectHorizontalDragGestures { _, amount ->
                if (amount < -50 && selectedTab < tabs.lastIndex) selectedTab++
                if (amount > 50 && selectedTab > 0) selectedTab--
            }
        }
    ) {
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text("$title (${counts[title] ?: 0})") })
            }
        }
        RestaurantOrdersFirebase(tabs[selectedTab])
    }
}
