package com.veggiego.restaurant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

data class SalesStats(

    val sales: Int = 0,

    val orders: Int = 0

)
@Composable
fun SalesSummaryScreen(

    onBack: () -> Unit

) {

    var selectedScreen by remember {
        mutableStateOf("")
    }
    var todayStats by remember {

        mutableStateOf(
            SalesStats()
        )
    }

    var weekStats by remember {

        mutableStateOf(
            SalesStats()
        )
    }

    var monthStats by remember {

        mutableStateOf(
            SalesStats()
        )
    }

    if (selectedScreen.isNotEmpty()) {

        SalesOrdersScreen(

            title = selectedScreen,

            onBack = {
                selectedScreen = ""
            }
        )

        return@SalesSummaryScreen
    }

    BackHandler {
        onBack()
    }
    LaunchedEffect(Unit) {

        FirebaseFirestore
            .getInstance()
            .collection("orders")

            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null)
                    return@addSnapshotListener

                val now =
                    System.currentTimeMillis()

                var todaySales = 0
                var todayOrders = 0

                var weekSales = 0
                var weekOrders = 0

                var monthSales = 0
                var monthOrders = 0

                snapshot.documents.forEach { doc ->

                    val status =
                        doc.getString("status")
                            ?: ""

                    if (status != "DELIVERED")
                        return@forEach

                    val itemTotal =
                        doc.getLong("itemTotal")
                            ?.toInt() ?: 0

                    val discount =
                        doc.getLong("discount")
                            ?.toInt() ?: 0

                    val timestamp =
                        doc.getLong("timestamp")
                            ?: 0L

                    val sale =
                        itemTotal - discount

                    val diff =
                        now - timestamp

                    if (
                        diff <=
                        24L * 60L * 60L * 1000L
                    ) {

                        todaySales += sale
                        todayOrders++
                    }

                    if (
                        diff <=
                        7L * 24L * 60L * 60L * 1000L
                    ) {

                        weekSales += sale
                        weekOrders++
                    }

                    if (
                        diff <=
                        30L * 24L * 60L * 60L * 1000L
                    ) {

                        monthSales += sale
                        monthOrders++
                    }
                }

                todayStats =
                    SalesStats(
                        todaySales,
                        todayOrders
                    )

                weekStats =
                    SalesStats(
                        weekSales,
                        weekOrders
                    )

                monthStats =
                    SalesStats(
                        monthSales,
                        monthOrders
                    )
            }
    }

    Column(

        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)

    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            IconButton(
                onClick = {
                    onBack()
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription = null
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "💰 Sales Summary",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        SalesCard(

            title = "Today",

            sales = "₹${todayStats.sales}",

            orders = "${todayStats.orders} Orders",
            onClick = {

                selectedScreen = "Today's Orders"
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SalesCard(
            title = "This Week",
            sales = "₹${weekStats.sales}",

            orders = "${weekStats.orders} Orders",
            onClick = {

                selectedScreen = "This Week Orders"
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SalesCard(
            title = "This Month",
            sales = "₹${monthStats.sales}",

            orders = "${monthStats.orders} Orders",
            onClick = {

                selectedScreen = "This Month Orders"
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SalesCard(

            title = "Custom Range",

            sales = "Coming Soon",

            orders = "",

            onClick = {}

        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Last Updated",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "01 Jun 2026 05:15 PM"
                )
            }
        }
    }
}

@Composable
fun SalesCard(

    title: String,

    sales: String,

    orders: String,

    onClick: () -> Unit = {}

) {

    Card(

        modifier = Modifier.fillMaxWidth(),

        onClick = onClick

    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = sales,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = orders
            )
        }
    }
}