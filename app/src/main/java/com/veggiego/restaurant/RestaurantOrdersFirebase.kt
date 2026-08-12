package com.veggiego.restaurant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class RestaurantOrderUi(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val area: String,
    val city: String,
    val paymentMethod: String,
    val total: Int,
    val itemTotal: Int,
    val packagingFee: Int,
    val discount: Int,
    val status: String,
    val items: List<Map<String, Any>>,
    val timestamp: Long,
    val payout: Double,
    val compensation: Double,
    val penalty: Double,
    val adminRemark: String
)

@Composable
fun RestaurantOrdersFirebase(selectedTab: String) {
    val db = FirebaseFirestore.getInstance()
    var orders by remember { mutableStateOf(emptyList<RestaurantOrderUi>()) }
    var selected by remember { mutableStateOf<RestaurantOrderUi?>(null) }
    var rejectOrder by remember { mutableStateOf<RestaurantOrderUi?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    BackHandler(enabled = selected != null) { selected = null }

    DisposableEffect(Unit) {
        val listener = RestaurantRepository().restaurantOrders().addSnapshotListener { value, _ ->
            if (value == null) return@addSnapshotListener
            orders = value.documents.filter {
                it.getString("restaurantId") == RestaurantSession.restaurantId
            }.map { doc ->
                val data = doc.data ?: emptyMap()
                RestaurantOrderUi(
                    id = doc.id,
                    customerName = doc.getString("customerName").orEmpty(),
                    customerPhone = doc.getString("customerPhone").orEmpty(),
                    area = doc.getString("area").orEmpty(),
                    city = doc.getString("city").orEmpty(),
                    paymentMethod = doc.getString("paymentMethod").orEmpty(),
                    total = number(data, "total").toInt(),
                    itemTotal = number(data, "itemTotal").toInt(),
                    packagingFee = number(data, "packagingFee").toInt(),
                    discount = number(data, "restaurantDiscount", "discount").toInt(),
                    status = doc.getString("status").orEmpty(),
                    items = doc.get("items") as? List<Map<String, Any>> ?: emptyList(),
                    timestamp = number(data, "timestamp", "createdAt").toLong(),
                    payout = number(data, "restaurantPayout", "restaurantAmount", "payout"),
                    compensation = number(data, "restaurantCompensation", "compensation"),
                    penalty = number(data, "restaurantPenalty", "penalty"),
                    adminRemark = string(data, "restaurantAdminRemark", "adminRemark", "settlementRemark")
                )
            }
        }
        onDispose { listener.remove() }
    }

    selected?.let { order ->
        OrderDetailsScreen(
            onBack = { selected = null },
            orderId = order.id,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            area = order.area,
            city = order.city,
            total = order.total,
            items = order.items,
            itemTotal = order.itemTotal,
            discount = order.discount,
            timestamp = order.timestamp
        )
        return
    }

    val filtered = orders.filter { order ->
        when (selectedTab) {
            "ALL" -> order.status !in setOf("PENDING", "NEW")
            "NEW" -> order.status in setOf("APPROVED", "RESTAURANT_PENDING")
            "PREPARING" -> order.status in setOf("ACCEPTED", "PREPARING")
            "READY" -> order.status in setOf("READY_FOR_PICKUP", "RIDER_ASSIGNED")
            "ON_THE_WAY" -> order.status in setOf("PICKED_UP", "OUT_FOR_DELIVERY")
            "COMPLETED" -> order.status == "DELIVERED"
            "CANCELLED" -> order.status in setOf("CANCELLED", "REJECTED")
            else -> false
        }
    }.sortedByDescending { it.timestamp }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        items(filtered, key = { it.id }) { order ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { selected = order }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "🧾 #${order.id}",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🕒 ${formatOrderDate(order.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1565C0)
                        )

                        StatusBadge(order.status)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("👤 ${order.customerName}")
                    Text("📍 ${order.area}${if (order.city.isNotBlank()) ", ${order.city}" else ""}")
                    Text("🛒 ${order.items.size} Items")
                    Text("💰 ₹${order.itemTotal + order.packagingFee - order.discount}", fontWeight = FontWeight.SemiBold)

                    if (order.status == "DELIVERED") {
                        val payout = if (order.payout > 0) order.payout else fallbackPayout(order)
                        Spacer(Modifier.height(8.dp))
                        Text("Restaurant Payout: ₹${payout.toInt()}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    if (order.status in setOf("CANCELLED", "REJECTED") && (order.compensation != 0.0 || order.penalty != 0.0 || order.adminRemark.isNotBlank())) {
                        Spacer(Modifier.height(8.dp))
                        if (order.compensation != 0.0) Text("Compensation: ₹${order.compensation.toInt()}", color = Color(0xFF2E7D32))
                        if (order.penalty != 0.0) Text("Penalty: -₹${order.penalty.toInt()}", color = MaterialTheme.colorScheme.error)
                        if (order.adminRemark.isNotBlank()) Text("Admin: ${order.adminRemark}")
                    }
                    Spacer(Modifier.height(12.dp))
                    when {
                        order.status in setOf("APPROVED", "RESTAURANT_PENDING") -> Row {
                            Button(onClick = { db.collection("orders").document(order.id).update("status", "PREPARING") }) { Text("ACCEPT") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { rejectOrder = order }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("REJECT") }
                        }
                        order.status in setOf("ACCEPTED", "PREPARING") -> Button(onClick = { db.collection("orders").document(order.id).update("status", "READY_FOR_PICKUP") }) { Text("READY FOR PICKUP") }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { selected = order }, modifier = Modifier.fillMaxWidth()) { Text("VIEW DETAILS") }
                }
            }
        }
    }

    rejectOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { rejectOrder = null },
            title = { Text("Reject Order") },
            text = { OutlinedTextField(rejectReason, { rejectReason = it }, label = { Text("Reason") }) },
            confirmButton = {
                Button(onClick = {
                    if (rejectReason.trim().length >= 3) {
                        db.collection("orders").document(order.id).update(
                            mapOf(
                                "status" to "CANCELLED",
                                "cancelReason" to rejectReason.trim(),
                                "cancelledBy" to "RESTAURANT",
                                "cancelledAt" to System.currentTimeMillis(),
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                        rejectReason = ""
                        rejectOrder = null
                    }
                }) { Text("SUBMIT") }
            },
            dismissButton = { TextButton(onClick = { rejectOrder = null }) { Text("CLOSE") } }
        )
    }
}

private fun number(data: Map<String, Any>, vararg keys: String): Double {
    keys.forEach { key -> (data[key] as? Number)?.toDouble()?.let { return it } }
    return 0.0
}
private fun string(data: Map<String, Any>, vararg keys: String): String {
    keys.forEach { key -> data[key]?.toString()?.takeIf { it.isNotBlank() }?.let { return it } }
    return ""
}
private fun fallbackPayout(order: RestaurantOrderUi): Double {
    val base = (order.itemTotal + order.packagingFee - order.discount).coerceAtLeast(0)
    return base * 0.75
}

@Composable private fun StatusBadge(status: String) {
    val label = status.replace('_', ' ')
    Surface(color = when {
        status == "DELIVERED" -> Color(0xFFE8F5E9)
        status in setOf("CANCELLED", "REJECTED") -> Color(0xFFFFEBEE)
        status in setOf("READY_FOR_PICKUP", "RIDER_ASSIGNED") -> Color(0xFFE3F2FD)
        else -> Color(0xFFFFF3E0)
    }, shape = MaterialTheme.shapes.small) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
private fun formatOrderDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM, hh:mm a",
        Locale.ENGLISH
    ).format(Date(timestamp))
}
