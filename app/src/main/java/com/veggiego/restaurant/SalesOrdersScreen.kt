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

                if (snapshot == null) return@addSnapshotListener

                val now = System.currentTimeMillis()

                val filtered = snapshot.documents.mapNotNull { doc ->

                    val status =
                        doc.getString("status")
                            ?: return@mapNotNull null

                    if (status != "DELIVERED") {
                        return@mapNotNull null
                    }

                    SalesOrder(

                        id = doc.id,

                        customerName =
                            doc.getString(
                                "customerName"
                            ) ?: "",

                        customerPhone =
                            doc.getString(
                                "customerPhone"
                            ) ?: "",

                        area =
                            doc.getString(
                                "area"
                            ) ?: "",
                        house =
                            doc.getString(
                                "house"
                            ) ?: "",

                        landmark =
                            doc.getString(
                                "landmark"
                            ) ?: "",

                        city =
                            doc.getString(
                                "city"
                            ) ?: "",

                        total =
                            doc.getLong(
                                "total"
                            )?.toInt() ?: 0,

                        itemTotal =
                            doc.getLong(
                                "itemTotal"
                            )?.toInt() ?: 0,

                        discount =
                            doc.getLong(
                                "discount"
                            )?.toInt() ?: 0,

                        status = status,

                        timestamp =
                            doc.getLong(
                                "timestamp"
                            ) ?: 0L,

                        items =
                            doc.get(
                                "items"
                            ) as? List<Map<String, Any>>
                                ?: emptyList(),

                        updatedAt =
                            doc.getLong(
                                "updatedAt"
                            ) ?: 0L

                    )

                }.filter { order ->

                    when (title) {

                        "Today's Orders" -> {

                            now - order.timestamp <=
                                    24L * 60L * 60L * 1000L
                        }

                        "This Week Orders" -> {

                            now - order.timestamp <=
                                    7L * 24L * 60L * 60L * 1000L
                        }

                        "This Month Orders" -> {

                            now - order.timestamp <=
                                    30L * 24L * 60L * 60L * 1000L
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
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier.padding(16.dp)
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null
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

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 6.dp
                            )
                            .clickable {

                                selectedOrder = order
                            }

                )
                {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "#${order.id}",
                            fontWeight =
                                FontWeight.Bold
                        )
                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
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
                                MaterialTheme.typography
                                    .bodySmall
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                order.customerName
                        )

                        Text(
                            text =
                                "Sale ₹$sale"
                        )

                        Text(
                            text =
                                "${order.items.size} Items"
                        )

                        Text(
                            text =
                                "Delivered"
                        )
                    }
                }
            }
        }
    }
}