package com.veggiego.restaurant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

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
        mutableStateOf(SalesStats())
    }

    var weekStats by remember {
        mutableStateOf(SalesStats())
    }

    var monthStats by remember {
        mutableStateOf(SalesStats())
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

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                val zoneId =
                    ZoneId.systemDefault()

                val today =
                    LocalDate.now(zoneId)

                // आज सुबह 12:00 बजे
                val todayStart =
                    today
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // कल सुबह 12:00 बजे
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

                // सोमवार सुबह 12:00 बजे
                val weekStart =
                    weekStartDate
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // अगले सोमवार सुबह 12:00 बजे
                val nextWeekStart =
                    weekStartDate
                        .plusWeeks(1)
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // इस महीने की पहली तारीख
                val monthStartDate =
                    today.withDayOfMonth(1)

                // महीने की पहली तारीख सुबह 12:00 बजे
                val monthStart =
                    monthStartDate
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                // अगले महीने की पहली तारीख सुबह 12:00 बजे
                val nextMonthStart =
                    monthStartDate
                        .plusMonths(1)
                        .atStartOfDay(zoneId)
                        .toInstant()
                        .toEpochMilli()

                var todaySales = 0
                var todayOrders = 0

                var weekSales = 0
                var weekOrders = 0

                var monthSales = 0
                var monthOrders = 0

                snapshot.documents.forEach { doc ->

                    val orderRestaurantId =
                        doc.getString("restaurantId")
                            ?: ""

                    // केवल login किए हुए restaurant के orders
                    if (
                        orderRestaurantId !=
                        RestaurantSession.restaurantId
                    ) {
                        return@forEach
                    }

                    val status =
                        doc.getString("status")
                            ?: ""

                    // केवल delivered orders
                    if (status != "DELIVERED") {
                        return@forEach
                    }

                    val itemTotal =
                        doc.getLong("itemTotal")
                            ?.toInt()
                            ?: 0

                    val discount =
                        doc.getLong("discount")
                            ?.toInt()
                            ?: 0

                    val timestamp =
                        doc.getLong("timestamp")
                            ?: 0L

                    val sale =
                        itemTotal - discount

                    // Today: आज 12 AM से कल 12 AM तक
                    if (
                        timestamp >= todayStart &&
                        timestamp < tomorrowStart
                    ) {
                        todaySales += sale
                        todayOrders++
                    }

                    // This Week: सोमवार से रविवार तक
                    if (
                        timestamp >= weekStart &&
                        timestamp < nextWeekStart
                    ) {
                        weekSales += sale
                        weekOrders++
                    }

                    // This Month: पहली तारीख से महीने के अंत तक
                    if (
                        timestamp >= monthStart &&
                        timestamp < nextMonthStart
                    ) {
                        monthSales += sale
                        monthOrders++
                    }
                }

                todayStats =
                    SalesStats(
                        sales = todaySales,
                        orders = todayOrders
                    )

                weekStats =
                    SalesStats(
                        sales = weekSales,
                        orders = weekOrders
                    )

                monthStats =
                    SalesStats(
                        sales = monthSales,
                        orders = monthOrders
                    )
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
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