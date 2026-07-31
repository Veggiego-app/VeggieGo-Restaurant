package com.veggiego.restaurant

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class PayoutRange(
    val label: String
) {
    TODAY("Today"),
    CURRENT_WEEK("Current Week"),
    LAST_WEEK("Last Week"),
    CURRENT_MONTH("Current Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom")
}

@Composable
fun PayoutReportScreen(
    onBack: () -> Unit
) {

    var selectedOrderId by remember {
        mutableStateOf<String?>(null)
    }

    BackHandler {

        if (selectedOrderId != null) {
            selectedOrderId = null
        } else {
            onBack()
        }
    }

    val context =
        LocalContext.current

    var selectedRange by remember {
        mutableStateOf(PayoutRange.TODAY)
    }

    var customStartMillis by remember {
        mutableLongStateOf(startOfToday())
    }

    var customEndMillis by remember {
        mutableLongStateOf(startOfToday())
    }

    var records by remember {
        mutableStateOf(
            emptyList<RestaurantPayoutRecord>()
        )
    }

    var settlementFilter by remember {
        mutableStateOf("ALL")
    }

    var errorText by remember {
        mutableStateOf("")
    }

    val dateRange =
        remember(
            selectedRange,
            customStartMillis,
            customEndMillis
        ) {
            payoutRange(
                range = selectedRange,
                customStartMillis = customStartMillis,
                customEndMillis = customEndMillis
            )
        }

    val startMillis =
        dateRange.first

    val endExclusiveMillis =
        dateRange.second

    DisposableEffect(
        startMillis,
        endExclusiveMillis
    ) {

        val listener =
            FirebaseFirestore
                .getInstance()
                .collection("orders")
                .addSnapshotListener {
                        snapshot,
                        _ ->

                    if (snapshot == null) {
                        return@addSnapshotListener
                    }

                    records =
                        snapshot.documents
                            .mapNotNull { doc ->

                                if (
                                    doc.getString(
                                        "restaurantId"
                                    ) !=
                                    RestaurantSession.restaurantId
                                ) {
                                    return@mapNotNull null
                                }

                                val data =
                                    doc.data
                                        ?: return@mapNotNull null

                                val time =
                                    readNumber(
                                        data,
                                        "deliveredAt",
                                        "cancelledAt",
                                        "timestamp",
                                        "createdAt"
                                    ).toLong()

                                if (
                                    time < startMillis ||
                                    time >= endExclusiveMillis
                                ) {
                                    return@mapNotNull null
                                }

                                val status =
                                    doc.getString("status")
                                        .orEmpty()

                                if (
                                    status !in setOf(
                                        "DELIVERED",
                                        "CANCELLED",
                                        "REJECTED"
                                    )
                                ) {
                                    return@mapNotNull null
                                }

                                val itemTotal =
                                    readNumber(
                                        data,
                                        "itemTotal"
                                    ).toInt()

                                val packaging =
                                    readNumber(
                                        data,
                                        "packagingFee"
                                    ).toInt()

                                val discount =
                                    readNumber(
                                        data,
                                        "restaurantDiscount",
                                        "discount"
                                    ).toInt()

                                val savedBase =
                                    readNumber(
                                        data,
                                        "commissionBase",
                                        "restaurantCommissionBase"
                                    )

                                val commissionBase =
                                    if (savedBase > 0) {

                                        savedBase

                                    } else {

                                        (
                                                itemTotal +
                                                        packaging -
                                                        discount
                                                )
                                            .coerceAtLeast(0)
                                            .toDouble()
                                    }

                                val commissionPercent =
                                    readNumber(
                                        data,
                                        "commissionPercent",
                                        "restaurantCommissionPercent",
                                        "commission"
                                    )

                                val savedCommission =
                                    readNumber(
                                        data,
                                        "commissionAmount",
                                        "restaurantCommissionAmount"
                                    )

                                val commissionAmount =
                                    if (savedCommission > 0) {

                                        savedCommission

                                    } else {

                                        commissionBase *
                                                commissionPercent /
                                                100.0
                                    }

                                val savedPayout =
                                    readNumber(
                                        data,
                                        "restaurantPayout",
                                        "restaurantAmount",
                                        "payout"
                                    )

                                RestaurantPayoutRecord(

                                    orderId =
                                        doc.id,

                                    status =
                                        status,

                                    timestamp =
                                        time,

                                    itemTotal =
                                        itemTotal,

                                    packagingFee =
                                        packaging,

                                    restaurantDiscount =
                                        discount,

                                    commissionBase =
                                        commissionBase,

                                    commissionPercent =
                                        commissionPercent,

                                    commissionAmount =
                                        commissionAmount,

                                    restaurantPayout =
                                        if (savedPayout > 0) {
                                            savedPayout
                                        } else {
                                            commissionBase -
                                                    commissionAmount
                                        },

                                    compensation =
                                        readNumber(
                                            data,
                                            "restaurantCompensation",
                                            "compensation"
                                        ),

                                    penalty =
                                        readNumber(
                                            data,
                                            "restaurantPenalty",
                                            "penalty"
                                        ),

                                    manualCredit =
                                        readNumber(
                                            data,
                                            "restaurantManualCredit",
                                            "manualCredit"
                                        ),

                                    manualDebit =
                                        readNumber(
                                            data,
                                            "restaurantManualDebit",
                                            "manualDebit"
                                        ),

                                    adminRemark =
                                        readText(
                                            data,
                                            "restaurantAdminRemark",
                                            "adminRemark",
                                            "settlementRemark"
                                        ),

                                    settlementStatus =
                                        readText(
                                            data,
                                            "restaurantSettlementStatus",
                                            "settlementStatus"
                                        ).ifBlank {
                                            "PENDING"
                                        }
                                )
                            }
                            .sortedByDescending {
                                it.timestamp
                            }
                }

        onDispose {
            listener.remove()
        }
    }

    val filtered =
        records.filter {

            settlementFilter == "ALL" ||
                    it.settlementStatus.equals(
                        settlementFilter,
                        ignoreCase = true
                    )
        }

    val delivered =
        filtered.filter {
            it.status == "DELIVERED"
        }

    val cancelled =
        filtered.filter {
            it.status in setOf(
                "CANCELLED",
                "REJECTED"
            )
        }

    val gross =
        delivered.sumOf {
            it.commissionBase
        }

    val commission =
        delivered.sumOf {
            it.commissionAmount
        }

    val payout =
        delivered.sumOf {
            it.restaurantPayout
        }

    val compensation =
        filtered.sumOf {
            it.compensation
        }

    val penalty =
        filtered.sumOf {
            it.penalty
        }

    val credits =
        filtered.sumOf {
            it.manualCredit
        }

    val debits =
        filtered.sumOf {
            it.manualDebit
        }

    val finalPayable =
        payout +
                compensation -
                penalty +
                credits -
                debits

    if (selectedOrderId != null) {

        OrderDetailsScreen(
            orderId =
                selectedOrderId.orEmpty(),

            onBack = {
                selectedOrderId = null
            }
        )

        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Color(0xFFFFF8F0))
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFF8A00))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "Back",
                    tint = Color.White
                )
            }

            Column {

                Text(
                    text =
                        "Payout Report",

                    style =
                        MaterialTheme.typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text =
                        "${formatDate(startMillis)} to " +
                                formatDate(
                                    endExclusiveMillis -
                                            ONE_DAY_MILLIS
                                ),

                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        LazyColumn(

            modifier =
                Modifier.fillMaxSize(),

            contentPadding =
                PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )

        ) {

            item {

                Text(
                    text =
                        "📅 Date Range",

                    fontWeight =
                        FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                PayoutRange
                    .values()
                    .toList()
                    .chunked(3)
                    .forEach {
                            rowRanges ->

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )

                        ) {

                            rowRanges.forEach {
                                    range ->

                                FilterChip(

                                    selected =
                                        selectedRange ==
                                                range,

                                    onClick = {

                                        selectedRange =
                                            range

                                        errorText =
                                            ""
                                    },

                                    label = {

                                        Text(
                                            range.label
                                        )
                                    },

                                    colors =
                                        FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFFFFF3E0),
                                            labelColor = Color(0xFF6D4C41),
                                            selectedContainerColor = Color(0xFFFF9800),
                                            selectedLabelColor = Color.White
                                        )
                                )
                            }
                        }
                    }

                if (
                    selectedRange ==
                    PayoutRange.CUSTOM
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        OutlinedButton(

                            onClick = {

                                showPicker(
                                    context =
                                        context,

                                    initialMillis =
                                        customStartMillis
                                ) { selectedMillis ->

                                    customStartMillis =
                                        startOfDay(
                                            selectedMillis
                                        )

                                    if (
                                        customEndMillis <
                                        customStartMillis
                                    ) {
                                        customEndMillis =
                                            customStartMillis
                                    }

                                    if (
                                        daysBetween(
                                            customStartMillis,
                                            customEndMillis
                                        ) > 59
                                    ) {
                                        customEndMillis =
                                            addDays(
                                                customStartMillis,
                                                59
                                            )
                                    }

                                    errorText =
                                        ""
                                }
                            },

                            modifier =
                                Modifier.weight(1f)

                        ) {

                            Text(
                                text =
                                    "From: ${
                                        formatDate(
                                            customStartMillis
                                        )
                                    }"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        OutlinedButton(

                            onClick = {

                                showPicker(
                                    context =
                                        context,

                                    initialMillis =
                                        customEndMillis
                                ) { selectedMillis ->

                                    val selectedEnd =
                                        startOfDay(
                                            selectedMillis
                                        )

                                    val days =
                                        daysBetween(
                                            customStartMillis,
                                            selectedEnd
                                        )

                                    when {

                                        days < 0 -> {

                                            errorText =
                                                "To date, From date se pehle nahi ho sakti"
                                        }

                                        days > 59 -> {

                                            errorText =
                                                "Maximum 60 days select kar sakte hain"
                                        }

                                        else -> {

                                            customEndMillis =
                                                selectedEnd

                                            errorText =
                                                ""
                                        }
                                    }
                                }
                            },

                            modifier =
                                Modifier.weight(1f)

                        ) {

                            Text(
                                text =
                                    "To: ${
                                        formatDate(
                                            customEndMillis
                                        )
                                    }"
                            )
                        }
                    }

                    if (
                        errorText.isNotBlank()
                    ) {

                        Text(
                            text =
                                errorText,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Row {

                    listOf(
                        "ALL",
                        "PENDING",
                        "SETTLED"
                    ).forEach {
                            option ->

                        FilterChip(

                            selected =
                                settlementFilter ==
                                        option,

                            onClick = {
                                settlementFilter =
                                    option
                            },

                            label = {
                                Text(option)
                            },

                            colors =
                                FilterChipDefaults.filterChipColors(
                                    containerColor =
                                        when (option) {
                                            "PENDING" -> Color(0xFFFFF8E1)
                                            "SETTLED" -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFE3F2FD)
                                        },
                                    labelColor = Color(0xFF424242),
                                    selectedContainerColor =
                                        when (option) {
                                            "PENDING" -> Color(0xFFFFB300)
                                            "SETTLED" -> Color(0xFF2E7D32)
                                            else -> Color(0xFF1976D2)
                                        },
                                    selectedLabelColor = Color.White
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )
            }

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {

                        Text(
                            text =
                                "💰 Payout Summary",

                            fontWeight =
                                FontWeight.Bold,

                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            color = Color(0xFF1B5E20)
                        )

                        ReportLine(
                            label =
                                "Delivered Orders",

                            value =
                                delivered.size
                                    .toDouble(),

                            moneyValue =
                                false
                        )

                        ReportLine(
                            label =
                                "Gross Commission Base",

                            value =
                                gross
                        )

                        ReportLine(
                            label =
                                "Commission",

                            value =
                                -commission
                        )

                        ReportLine(
                            label =
                                "Delivered Payout",

                            value =
                                payout
                        )

                        if (
                            compensation != 0.0
                        ) {

                            ReportLine(
                                label =
                                    "Compensation",

                                value =
                                    compensation
                            )
                        }

                        if (
                            penalty != 0.0
                        ) {

                            ReportLine(
                                label =
                                    "Penalty",

                                value =
                                    -penalty
                            )
                        }

                        if (
                            credits != 0.0
                        ) {

                            ReportLine(
                                label =
                                    "Manual Credit",

                                value =
                                    credits
                            )
                        }

                        if (
                            debits != 0.0
                        ) {

                            ReportLine(
                                label =
                                    "Manual Debit",

                                value =
                                    -debits
                            )
                        }

                        HorizontalDivider(
                            modifier =
                                Modifier.padding(
                                    vertical = 8.dp
                                )
                        )

                        ReportLine(
                            label =
                                "Final Payable",

                            value =
                                finalPayable,

                            bold =
                                true
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    text =
                        "✅ Delivered Orders",

                    fontWeight =
                        FontWeight.Bold,

                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    color = Color(0xFF2E7D32)
                )
            }

            items(
                items =
                    delivered,

                key = {
                    "D-${it.orderId}"
                }

            ) { row ->

                PayoutOrderCard(
                    row =
                        row,

                    onClick = {
                        selectedOrderId =
                            row.orderId
                    }
                )
            }

            if (
                cancelled.isNotEmpty()
            ) {

                item {

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    Text(
                        text =
                            "❌ Cancelled Adjustments",

                        fontWeight =
                            FontWeight.Bold,

                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        color = Color(0xFFC62828)
                    )
                }

                items(
                    items =
                        cancelled,

                    key = {
                        "C-${it.orderId}"
                    }

                ) { row ->

                    PayoutOrderCard(
                        row =
                            row,

                        onClick = {
                            selectedOrderId =
                                row.orderId
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(30.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportLine(
    label: String,
    value: Double,
    moneyValue: Boolean = true,
    bold: Boolean = false
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween

    ) {

        Text(
            text =
                label,

            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },

            color =
                if (bold) {
                    Color(0xFF1B5E20)
                } else {
                    Color(0xFF4E342E)
                }
        )

        val shown =
            if (!moneyValue) {

                value.toInt()
                    .toString()

            } else if (value < 0) {

                "-₹${money(-value)}"

            } else {

                "₹${money(value)}"
            }

        Text(
            text =
                shown,

            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },

            color =
                when {
                    bold -> Color(0xFF1B5E20)
                    value < 0 -> Color(0xFFC62828)
                    else -> Color(0xFF1565C0)
                }
        )
    }
}

@Composable
private fun PayoutOrderCard(
    row: RestaurantPayoutRecord,
    onClick: () -> Unit
) {

    Card(

        onClick =
            onClick,

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 5.dp
                ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (row.status == "DELIVERED") {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFEBEE)
                    }
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 5.dp
            )

    ) {

        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween

            ) {

                Text(
                    text =
                        "#${row.orderId}",

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.weight(1f),

                    maxLines =
                        1,
                    color =
                        if (row.status == "DELIVERED") {
                            Color(0xFF1B5E20)
                        } else {
                            Color(0xFFB71C1C)
                        }
                )

                Surface(
                    color =
                        if (
                            row.settlementStatus.equals(
                                "SETTLED",
                                ignoreCase = true
                            )
                        ) {
                            Color(0xFF2E7D32)
                        } else {
                            Color(0xFFFFB300)
                        },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = row.settlementStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                    )
                }
            }

            Text(
                text =
                    SimpleDateFormat(
                        "dd/MM/yyyy hh:mm a",
                        Locale.getDefault()
                    ).format(
                        Date(row.timestamp)
                    ),

                style =
                    MaterialTheme.typography
                        .bodySmall
            )

            Text(
                text =
                    "Tap to view full order details",

                style =
                    MaterialTheme.typography
                        .labelSmall,

                color =
                    Color(0xFFFF6F00)
            )

            if (
                row.status ==
                "DELIVERED"
            ) {

                ReportLine(
                    label =
                        "Commission Base",

                    value =
                        row.commissionBase
                )

                ReportLine(
                    label =
                        "Commission",

                    value =
                        -row.commissionAmount
                )

                ReportLine(
                    label =
                        "Restaurant Payout",

                    value =
                        row.restaurantPayout,

                    bold =
                        true
                )

            } else {

                if (
                    row.compensation != 0.0
                ) {

                    ReportLine(
                        label =
                            "Compensation",

                        value =
                            row.compensation
                    )
                }

                if (
                    row.penalty != 0.0
                ) {

                    ReportLine(
                        label =
                            "Penalty",

                        value =
                            -row.penalty
                    )
                }

                if (
                    row.adminRemark
                        .isNotBlank()
                ) {

                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📝 Admin Remark: ${row.adminRemark}",
                            color = Color(0xFF6D4C41),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun payoutRange(
    range: PayoutRange,
    customStartMillis: Long,
    customEndMillis: Long
): Pair<Long, Long> {

    val todayStart =
        startOfToday()

    return when (range) {

        PayoutRange.TODAY -> {

            todayStart to
                    addDays(
                        todayStart,
                        1
                    )
        }

        PayoutRange.CURRENT_WEEK -> {

            val weekStart =
                mondayStart(
                    todayStart
                )

            weekStart to
                    addDays(
                        weekStart,
                        7
                    )
        }

        PayoutRange.LAST_WEEK -> {

            val currentWeekStart =
                mondayStart(
                    todayStart
                )

            val lastWeekStart =
                addDays(
                    currentWeekStart,
                    -7
                )

            lastWeekStart to
                    currentWeekStart
        }

        PayoutRange.CURRENT_MONTH -> {

            val calendar =
                Calendar.getInstance()

            calendar.timeInMillis =
                todayStart

            calendar.set(
                Calendar.DAY_OF_MONTH,
                1
            )

            clearTime(calendar)

            val monthStart =
                calendar.timeInMillis

            calendar.add(
                Calendar.MONTH,
                1
            )

            monthStart to
                    calendar.timeInMillis
        }

        PayoutRange.LAST_MONTH -> {

            val calendar =
                Calendar.getInstance()

            calendar.timeInMillis =
                todayStart

            calendar.set(
                Calendar.DAY_OF_MONTH,
                1
            )

            clearTime(calendar)

            val currentMonthStart =
                calendar.timeInMillis

            calendar.add(
                Calendar.MONTH,
                -1
            )

            calendar.timeInMillis to
                    currentMonthStart
        }

        PayoutRange.CUSTOM -> {

            startOfDay(
                customStartMillis
            ) to
                    addDays(
                        startOfDay(
                            customEndMillis
                        ),
                        1
                    )
        }
    }
}

private fun mondayStart(
    millis: Long
): Long {

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        millis

    clearTime(calendar)

    val dayOfWeek =
        calendar.get(
            Calendar.DAY_OF_WEEK
        )

    val daysFromMonday =
        when (dayOfWeek) {

            Calendar.SUNDAY ->
                6

            else ->
                dayOfWeek -
                        Calendar.MONDAY
        }

    calendar.add(
        Calendar.DAY_OF_MONTH,
        -daysFromMonday
    )

    return calendar.timeInMillis
}

private fun startOfToday(): Long {

    val calendar =
        Calendar.getInstance()

    clearTime(calendar)

    return calendar.timeInMillis
}

private fun startOfDay(
    millis: Long
): Long {

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        millis

    clearTime(calendar)

    return calendar.timeInMillis
}

private fun clearTime(
    calendar: Calendar
) {

    calendar.set(
        Calendar.HOUR_OF_DAY,
        0
    )

    calendar.set(
        Calendar.MINUTE,
        0
    )

    calendar.set(
        Calendar.SECOND,
        0
    )

    calendar.set(
        Calendar.MILLISECOND,
        0
    )
}

private fun addDays(
    millis: Long,
    days: Int
): Long {

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        millis

    calendar.add(
        Calendar.DAY_OF_MONTH,
        days
    )

    return calendar.timeInMillis
}

private fun daysBetween(
    startMillis: Long,
    endMillis: Long
): Long {

    val start =
        startOfDay(
            startMillis
        )

    val end =
        startOfDay(
            endMillis
        )

    return (
            end -
                    start
            ) / ONE_DAY_MILLIS
}

private fun showPicker(
    context: Context,
    initialMillis: Long,
    onDate: (Long) -> Unit
) {

    val initial =
        Calendar.getInstance()

    initial.timeInMillis =
        initialMillis

    DatePickerDialog(

        context,

        {
                _,
                year,
                month,
                day ->

            val selected =
                Calendar.getInstance()

            selected.set(
                year,
                month,
                day,
                0,
                0,
                0
            )

            selected.set(
                Calendar.MILLISECOND,
                0
            )

            onDate(
                selected.timeInMillis
            )
        },

        initial.get(
            Calendar.YEAR
        ),

        initial.get(
            Calendar.MONTH
        ),

        initial.get(
            Calendar.DAY_OF_MONTH
        )

    ).show()
}

private fun formatDate(
    millis: Long
): String {

    return SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    ).format(
        Date(millis)
    )
}

private fun readNumber(
    data: Map<String, Any>,
    vararg keys: String
): Double {

    keys.forEach { key ->

        (
                data[key] as? Number
                )?.toDouble()
            ?.let {
                return it
            }
    }

    return 0.0
}

private fun readText(
    data: Map<String, Any>,
    vararg keys: String
): String {

    keys.forEach { key ->

        data[key]
            ?.toString()
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                return it
            }
    }

    return ""
}

private fun money(
    value: Double
): String {

    return if (
        value % 1.0 ==
        0.0
    ) {

        value.toInt()
            .toString()

    } else {

        String.format(
            Locale.getDefault(),
            "%.2f",
            value
        )
    }
}

private const val ONE_DAY_MILLIS =
    24L * 60L * 60L * 1000L
