package com.veggiego.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onTodayOrdersClick: () -> Unit,
    onTodaySalesClick: () -> Unit,
    onAllOrdersClick: () -> Unit,
    onNewOrdersClick: () -> Unit,
    onPreparingClick: () -> Unit,
    onReadyClick: () -> Unit,
    onOnTheWayClick: () -> Unit,
    onDeliveredClick: () -> Unit,
    onCancelledClick: () -> Unit,
    onPayoutReportClick: () -> Unit
) {
    var restaurantName by remember { mutableStateOf("Restaurant") }
    var restaurantOnline by remember { mutableStateOf(true) }
    var todayOrders by remember { mutableIntStateOf(0) }
    var todaySales by remember { mutableDoubleStateOf(0.0) }
    var counts by remember { mutableStateOf(emptyMap<String, Int>()) }

    DisposableEffect(Unit) {
        val ordersListener = RestaurantRepository().restaurantOrders()
            .addSnapshotListener { value, _ ->
                if (value == null) return@addSnapshotListener
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                var todayCount = 0
                var sales = 0.0
                val mutableCounts = mutableMapOf(
                    "ALL" to 0, "NEW" to 0, "PREPARING" to 0, "READY" to 0,
                    "ON_THE_WAY" to 0, "COMPLETED" to 0, "CANCELLED" to 0
                )
                value.documents.forEach { doc ->
                    if (doc.getString("restaurantId") != RestaurantSession.restaurantId) return@forEach
                    val status = doc.getString("status").orEmpty()
                    val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: 0L
                    mutableCounts["ALL"] = (mutableCounts["ALL"] ?: 0) + 1
                    when (status) {
                        "APPROVED", "NEW", "PENDING", "RESTAURANT_PENDING" -> mutableCounts["NEW"] = (mutableCounts["NEW"] ?: 0) + 1
                        "ACCEPTED", "PREPARING" -> mutableCounts["PREPARING"] = (mutableCounts["PREPARING"] ?: 0) + 1
                        "READY_FOR_PICKUP", "RIDER_ASSIGNED" -> mutableCounts["READY"] = (mutableCounts["READY"] ?: 0) + 1
                        "PICKED_UP", "OUT_FOR_DELIVERY" -> mutableCounts["ON_THE_WAY"] = (mutableCounts["ON_THE_WAY"] ?: 0) + 1
                        "DELIVERED" -> mutableCounts["COMPLETED"] = (mutableCounts["COMPLETED"] ?: 0) + 1
                        "CANCELLED", "REJECTED" -> mutableCounts["CANCELLED"] = (mutableCounts["CANCELLED"] ?: 0) + 1
                    }
                    if (timestamp in start until end) {
                        todayCount++
                        if (status == "DELIVERED") {
                            sales += readDouble(doc.data, "restaurantPayout", "restaurantAmount", "payout").takeIf { it > 0 }
                                ?: ((doc.get("itemTotal") as? Number)?.toDouble() ?: 0.0)
                        }
                    }
                }
                todayOrders = todayCount
                todaySales = sales
                counts = mutableCounts
            }
        val restaurantListener = RestaurantRepository().restaurantDocument()
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    restaurantName = snapshot.getString("name") ?: "Restaurant"
                    restaurantOnline = snapshot.getBoolean("online") ?: true
                }
            }
        onDispose {
            ordersListener.remove()
            restaurantListener.remove()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFFFF7ED))
            .verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(
            text = restaurantName,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )
        Spacer(Modifier.height(10.dp))
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (restaurantOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(if (restaurantOnline) "🟢 Restaurant Online" else "🔴 Restaurant Offline", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (restaurantOnline) "Currently accepting new orders" else "New orders are currently stopped")
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { RestaurantRepository().restaurantDocument().update("online", !restaurantOnline) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (restaurantOnline) Color(0xFFE53935) else Color(0xFF22C55E))
                ) { Text(if (restaurantOnline) "GO OFFLINE" else "GO ONLINE", fontWeight = FontWeight.Bold) }
            }
        }

        SectionTitle("Today's Overview")
        Row(Modifier.fillMaxWidth()) {
            DashboardCard("📦 Today Orders", todayOrders.toString(), Modifier.weight(1f), onTodayOrdersClick)
            Spacer(Modifier.width(12.dp))
            DashboardCard("💰 Today Sales", "₹${todaySales.toInt()}", Modifier.weight(1f), onTodaySalesClick)
        }

        SectionTitle("Order Status")
        DashboardCard("🧾 All Orders", (counts["ALL"] ?: 0).toString(), Modifier.fillMaxWidth(), onAllOrdersClick)
        Spacer(Modifier.height(12.dp))
        StatusRow("🆕 New Orders", counts["NEW"] ?: 0, onNewOrdersClick, "🍳 Preparing", counts["PREPARING"] ?: 0, onPreparingClick)
        Spacer(Modifier.height(12.dp))
        StatusRow("📦 Ready", counts["READY"] ?: 0, onReadyClick, "🚚 On The Way", counts["ON_THE_WAY"] ?: 0, onOnTheWayClick)
        Spacer(Modifier.height(12.dp))
        StatusRow("✅ Delivered", counts["COMPLETED"] ?: 0, onDeliveredClick, "❌ Cancelled", counts["CANCELLED"] ?: 0, onCancelledClick)

        SectionTitle("Reports")
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPayoutReportClick,
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFFE8F5E9)
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "💰 Payout Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1B5E20)
                )
                Spacer(Modifier.height(4.dp))
                Text("Today, current week, last week, month or custom 60-day range")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun readDouble(data: Map<String, Any>?, vararg keys: String): Double {
    keys.forEach { key -> (data?.get(key) as? Number)?.toDouble()?.let { return it } }
    return 0.0
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        color = Color(0xFF6D4C41)
    )
    Spacer(Modifier.height(10.dp))
}

@Composable private fun StatusRow(t1: String, v1: Int, c1: () -> Unit, t2: String, v2: Int, c2: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        DashboardCard(t1, v1.toString(), Modifier.weight(1f), c1)
        Spacer(Modifier.width(12.dp))
        DashboardCard(t2, v2.toString(), Modifier.weight(1f), c2)
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    val backgroundColor =
        when {
            title.contains("Today Orders") -> Color(0xFFFFF3E0)
            title.contains("Today Sales") -> Color(0xFFE8F5E9)
            title.contains("All Orders") -> Color(0xFFE3F2FD)
            title.contains("New Orders") -> Color(0xFFFFEBEE)
            title.contains("Preparing") -> Color(0xFFFFF8E1)
            title.contains("Ready") -> Color(0xFFE8F5E9)
            title.contains("On The Way") -> Color(0xFFE3F2FD)
            title.contains("Delivered") -> Color(0xFFF3E5F5)
            title.contains("Cancelled") -> Color(0xFFFFEBEE)
            else -> Color.White
        }

    val valueColor =
        when {
            title.contains("Today Orders") -> Color(0xFFE65100)
            title.contains("Today Sales") -> Color(0xFF1B5E20)
            title.contains("All Orders") -> Color(0xFF1565C0)
            title.contains("New Orders") -> Color(0xFFC62828)
            title.contains("Preparing") -> Color(0xFFF57F17)
            title.contains("Ready") -> Color(0xFF2E7D32)
            title.contains("On The Way") -> Color(0xFF1565C0)
            title.contains("Delivered") -> Color(0xFF6A1B9A)
            title.contains("Cancelled") -> Color(0xFFC62828)
            else -> Color(0xFF424242)
        }

    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 5.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF424242)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}
