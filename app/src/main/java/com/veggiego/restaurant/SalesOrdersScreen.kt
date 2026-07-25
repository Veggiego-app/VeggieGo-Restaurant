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

@Composable
fun SalesOrdersScreen(
    title: String,
    onBack: () -> Unit
) {

    var orders by remember {
        mutableStateOf<List<SalesOrder>>(emptyList())
    }

    var selectedOrder by remember {
        mutableStateOf<SalesOrder?>(null)
    }

    BackHandler {

        if (selectedOrder != null) {
            selectedOrder = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(title) {

        FirebaseFirestore
            .getInstance()
            .collection("orders")
            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                val zoneId =
                    ZoneId.systemDefault()

                val today =
                    LocalDate.now(zoneId)

                // आज सुबह 12 बजे
                val todayStart =
                    today
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // कल सुबह 12 बजे
                val tomorrowStart =
                    today
                        .plusDays(1)
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // इस सप्ताह का सोमवार
                val weekStartDate =
                    today.with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                        )
                    )

                // सोमवार सुबह 12 बजे
                val weekStart =
                    weekStartDate
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // अगले सोमवार सुबह 12 बजे
                val nextWeekStart =
                    weekStartDate
                        .plusWeeks(1)
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // इस महीने की पहली तारीख
                val monthStartDate =
                    today.withDayOfMonth(1)

                // महीने की पहली तारीख सुबह 12 बजे
                val monthStart =
                    monthStartDate
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // अगले महीने की पहली तारीख सुबह 12 बजे
                val nextMonthStart =
                    monthStartDate
                        .plusMonths(1)
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                val filtered =
                    snapshot.documents
                        .mapNotNull { doc ->

                            val orderRestaurantId =
                                doc.getString("restaurantId")
                                    ?: return@mapNotNull null

                            // केवल login किए हुए restaurant के orders
                            if (
                                orderRestaurantId !=
                                RestaurantSession.restaurantId
                            ) {
                                return@mapNotNull null
                            }

                            val status =
                                doc.getString("status")
                                    ?: return@mapNotNull null

                            // केवल delivered orders
                            if (status != "DELIVERED") {
                                return@mapNotNull null
                            }

                            SalesOrder(
                                id = doc.id,

                                customerName =
                                    doc.getString("customerName")
                                        ?: "",

                                customerPhone =
                                    doc.getString("customerPhone")
                                        ?: "",

                                area =
                                    doc.getString("area")
                                        ?: "",

                                house =
                                    doc.getString("house")
                                        ?: "",

                                landmark =
                                    doc.getString("landmark")
                                        ?: "",

                                city =
                                    doc.getString("city")
                                        ?: "",

                                total =
                                    doc.getLong("total")
                                        ?.toInt()
                                        ?: 0,

                                itemTotal =
                                    doc.getLong("itemTotal")
                                        ?.toInt()
                                        ?: 0,

                                discount =
                                    doc.getLong("discount")
                                        ?.toInt()
                                        ?: 0,

                                status = status,

                                timestamp =
                                    doc.getLong("timestamp")
                                        ?: 0L,

                                items =
                                    doc.get("items")
                                            as? List<Map<String, Any>>
                                        ?: emptyList(),

                                updatedAt =
                                    doc.getLong("updatedAt")
                                        ?: 0L
                            )
                        }
                        .filter { order ->

                            when (title) {

                                // आज सुबह 12 बजे से कल सुबह 12 बजे तक
                                "Today's Orders" -> {
                                    order.timestamp >= todayStart &&
                                            order.timestamp < tomorrowStart
                                }

                                // सोमवार से रविवार तक
                                "This Week Orders" -> {
                                    order.timestamp >= weekStart &&
                                            order.timestamp < nextWeekStart
                                }

                                // महीने की पहली से आखिरी तारीख तक
                                "This Month Orders" -> {
                                    order.timestamp >= monthStart &&
                                            order.timestamp < nextMonthStart
                                }

                                else -> true
                            }
                        }

                orders =
                    filtered.sortedByDescending {
                        it.timestamp
                    }
            }
    }

    if (selectedOrder != null) {

        OrderDetailsScreen(
            onBack = {
                selectedOrder = null
            },

            orderId =
                selectedOrder!!.id,

            customerName =
                selectedOrder!!.customerName,

            customerPhone =
                selectedOrder!!.customerPhone,

            area =
                selectedOrder!!.area,

            city =
                selectedOrder!!.city,

            total =
                selectedOrder!!.total,

            itemTotal =
                selectedOrder!!.itemTotal,

            discount =
                selectedOrder!!.discount,

            items =
                selectedOrder!!.items,

            timestamp =
                selectedOrder!!.timestamp
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        Row(
            modifier = Modifier.padding(16.dp)
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = title,
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),

            contentPadding =
                PaddingValues(
                    bottom = 100.dp
                )
        ) {

            items(orders) { order ->

                val sale =
                    order.itemTotal -
                            order.discount

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        )
                        .clickable {
                            selectedOrder = order
                        }
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "#${order.id}",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "📅 " +
                                        java.text.SimpleDateFormat(
                                            "dd MMM yyyy hh:mm a"
                                        ).format(
                                            java.util.Date(
                                                order.timestamp
                                            )
                                        ),

                            style =
                                MaterialTheme.typography.bodySmall
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = order.customerName
                        )

                        Text(
                            text = "Sale ₹$sale"
                        )

                        Text(
                            text = "${order.items.size} Items"
                        )

                        Text(
                            text = "Delivered"
                        )
                    }
                }
            }
        }
    }
}