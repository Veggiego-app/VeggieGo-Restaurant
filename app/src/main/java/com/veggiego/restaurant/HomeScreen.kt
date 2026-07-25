package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.background

@Composable
fun HomeScreen(

    modifier: Modifier = Modifier,

    onNewOrdersClick: () -> Unit,

    onPreparingClick: () -> Unit,

    onReadyClick: () -> Unit,

    onOnTheWayClick: () -> Unit,

    onDeliveredClick: () -> Unit,

    onSalesClick: () -> Unit
) {

    val db = FirebaseFirestore.getInstance()

    var newOrdersCount by remember {
        mutableIntStateOf(0)
    }

    var preparingCount by remember {
        mutableIntStateOf(0)
    }

    var readyCount by remember {
        mutableIntStateOf(0)
    }

    var onTheWayCount by remember {
        mutableIntStateOf(0)
    }

    var deliveredCount by remember {
        mutableIntStateOf(0)
    }

    var totalSales by remember {
        mutableIntStateOf(0)
    }
    var restaurantName by remember {
        mutableStateOf("Restaurant")
    }

    var restaurantOnline by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        RestaurantRepository()

            .restaurantOrders()
            .addSnapshotListener { value, _ ->

                if (value == null) return@addSnapshotListener

                var newOrders = 0
                var preparing = 0
                var ready = 0
                var onTheWay = 0
                var delivered = 0
                var sales = 0

                value.documents.forEach { doc ->
                    val restaurantId =
                        RestaurantSession.restaurantId

                    if (
                        doc.getString("restaurantId")
                        != restaurantId
                    ) {
                        return@forEach
                    }

                    val status =
                        doc.getString("status") ?: ""

                    val total = when (val amount = doc.get("total")) {

                        is Long -> amount.toInt()

                        is Double -> amount.toInt()

                        else -> 0
                    }

                    when (status) {

                        "APPROVED" ->
                            newOrders++

                        "ACCEPTED",
                        "PREPARING" ->
                            preparing++

                        "READY_FOR_PICKUP" ->
                            ready++

                        "OUT_FOR_DELIVERY" ->
                            onTheWay++

                        "DELIVERED" -> {

                            delivered++
                            sales += total
                        }
                    }
                }

                newOrdersCount = newOrders
                preparingCount = preparing
                readyCount = ready
                onTheWayCount = onTheWay
                deliveredCount = delivered
                totalSales = sales
            }
    }
    RestaurantRepository()

        .restaurantDocument()

        .addSnapshotListener { snapshot, _ ->

            if (snapshot == null) return@addSnapshotListener

            restaurantName =
                snapshot.getString("name")
                    ?: "Restaurant"

            restaurantOnline =
                snapshot.getBoolean("online")
                    ?: true
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFCC80))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = restaurantName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(
            modifier = Modifier.height(8.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),

            colors = CardDefaults.elevatedCardColors(
                containerColor =
                    if (restaurantOnline)
                        Color(0xFFE8F5E9)
                    else
                        Color(0xFFFFEBEE)
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text =
                        if (restaurantOnline)
                            "🟢 Restaurant Online"
                        else
                            "🔴 Restaurant Offline",

                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        if (restaurantOnline)
                            "Currently accepting new orders"
                        else
                            "New orders are currently stopped"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Button(
                    onClick = {

                        RestaurantRepository()
                            .restaurantDocument()
                            .update(
                                "online",
                                !restaurantOnline
                            )
                    },

                    modifier = Modifier.fillMaxWidth(),

                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (restaurantOnline)
                                Color(0xFFE53935)
                            else
                                Color(0xFF22C55E)
                    )
                ) {

                    Text(
                        text =
                            if (restaurantOnline)
                                "GO OFFLINE"
                            else
                                "GO ONLINE",

                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )

        // ROW 1

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            DashboardCard(
                title = "📦 New Orders",
                value = newOrdersCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onNewOrdersClick
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            DashboardCard(
                title = "🍳 Preparing",
                value = preparingCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onPreparingClick
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // ROW 2

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            DashboardCard(
                title = "📦 Ready",
                value = readyCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onReadyClick
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            DashboardCard(
                title = "🚚 On The Way",
                value = onTheWayCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onOnTheWayClick
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // ROW 3

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            DashboardCard(
                title = "✅ Delivered",
                value = deliveredCount.toString(),
                modifier = Modifier.weight(1f),
                onClick = onDeliveredClick
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Card(
                modifier = Modifier.weight(1f),

                onClick = {
                    onSalesClick()
                }
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Today's Sales",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "₹$totalSales",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "$deliveredCount Delivered Orders"
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            onClick = {
                // Combo Offer Screen
            }
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "🏷 Quick Action",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Combo Offers"
                )

                Text(
                    text = "Create restaurant combo deals"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Today's Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                SummaryRow(
                    "New Orders",
                    newOrdersCount.toString()
                )

                SummaryRow(
                    "Preparing",
                    preparingCount.toString()
                )

                SummaryRow(
                    "Ready",
                    readyCount.toString()
                )

                SummaryRow(
                    "On The Way",
                    onTheWayCount.toString()
                )

                SummaryRow(
                    "Delivered",
                    deliveredCount.toString()
                )

                Divider()

                SummaryRow(
                    "Today's Sales",
                    "₹$totalSales"
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor =
                when {

                    title.contains("New") ->
                        Color(0xFFEDE7F6)

                    title.contains("Preparing") ->
                        Color(0xFFFFF3E0)

                    title.contains("Ready") ->
                        Color(0xFFE8F5E9)

                    title.contains("Way") ->
                        Color(0xFFE3F2FD)

                    title.contains("Delivered") ->
                        Color(0xFFF3E5F5)

                    title.contains("Sales") ->
                        Color(0xFFFFF8E1)

                    else ->
                        MaterialTheme.colorScheme.surface
                }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
@Composable
fun SummaryRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = title
        )

        Text(
            text = value,
            fontWeight = FontWeight.Bold
        )
    }
}