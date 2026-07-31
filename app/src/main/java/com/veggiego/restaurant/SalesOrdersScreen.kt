package com.veggiego.restaurant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private data class ReportOrder(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val area: String,
    val city: String,
    val status: String,
    val total: Int,
    val itemTotal: Int,
    val discount: Int,
    val timestamp: Long,
    val items: List<Map<String, Any>>
)

@Composable
fun SalesOrdersScreen(title: String, onBack: () -> Unit) {
    var orders by remember { mutableStateOf(emptyList<ReportOrder>()) }
    var selected by remember { mutableStateOf<ReportOrder?>(null) }

    BackHandler { if (selected != null) selected = null else onBack() }

    DisposableEffect(title) {
        val listener = FirebaseFirestore.getInstance().collection("orders").addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val (start, end) = rangeForTitle(title, today, zone)
            orders = snapshot.documents.mapNotNull { doc ->
                if (doc.getString("restaurantId") != RestaurantSession.restaurantId) return@mapNotNull null
                val status = doc.getString("status").orEmpty()
                if (title != "Today's Orders" && status != "DELIVERED") return@mapNotNull null
                val data = doc.data ?: return@mapNotNull null
                val time = (data["timestamp"] as? Number)?.toLong() ?: 0L
                if (time !in start until end) return@mapNotNull null
                ReportOrder(
                    id = doc.id,
                    customerName = doc.getString("customerName").orEmpty(),
                    customerPhone = doc.getString("customerPhone").orEmpty(),
                    area = doc.getString("area").orEmpty(),
                    city = doc.getString("city").orEmpty(),
                    status = status,
                    total = (data["total"] as? Number)?.toInt() ?: 0,
                    itemTotal = (data["itemTotal"] as? Number)?.toInt() ?: 0,
                    discount = (data["restaurantDiscount"] as? Number)?.toInt() ?: (data["discount"] as? Number)?.toInt() ?: 0,
                    timestamp = time,
                    items = data["items"] as? List<Map<String, Any>> ?: emptyList()
                )
            }.sortedByDescending { it.timestamp }
        }
        onDispose { listener.remove() }
    }

    selected?.let { order ->
        OrderDetailsScreen(
            onBack = { selected = null }, orderId = order.id, customerName = order.customerName,
            customerPhone = order.customerPhone, area = order.area, city = order.city,
            total = order.total, items = order.items, itemTotal = order.itemTotal,
            discount = order.discount, timestamp = order.timestamp
        )
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        if (title != "Today's Orders") {
            val total = orders.sumOf { it.itemTotal - it.discount }
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Delivered Orders"); Text(orders.size.toString(), fontWeight = FontWeight.Bold) }
                    Column { Text("Sales"); Text("₹$total", fontWeight = FontWeight.Bold) }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(orders, key = { it.id }) { order ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { selected = order }) {
                    Column(Modifier.padding(16.dp)) {
                        Text("#${order.id}", fontWeight = FontWeight.Bold)
                        Text(java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a").format(java.util.Date(order.timestamp)), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Text(order.customerName)
                        Text("${order.items.size} Items")
                        Text(order.status.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                        if (order.status == "DELIVERED") Text("Sale ₹${order.itemTotal - order.discount}")
                    }
                }
            }
        }
    }
}

private fun rangeForTitle(title: String, today: LocalDate, zone: ZoneId): Pair<Long, Long> {
    val startDate = when (title) {
        "This Week Orders" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        "This Month Orders" -> today.withDayOfMonth(1)
        else -> today
    }
    val endDate = when (title) {
        "This Week Orders" -> startDate.plusWeeks(1)
        "This Month Orders" -> startDate.plusMonths(1)
        else -> startDate.plusDays(1)
    }
    return startDate.atStartOfDay(zone).toInstant().toEpochMilli() to endDate.atStartOfDay(zone).toInstant().toEpochMilli()
}
