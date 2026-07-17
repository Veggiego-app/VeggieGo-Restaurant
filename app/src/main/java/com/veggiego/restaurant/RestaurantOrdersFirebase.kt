package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

data class RestaurantOrder(

    val id: String = "",

    val customerName: String = "",

    val customerPhone: String = "",

    val area: String = "",

    val paymentMethod: String = "",

    val itemsCount: Int = 0,

    val total: Int = 0,

    val status: String = "",

    val restaurantName: String = "",

    val discount: Int = 0,

    val packagingFee: Int = 0,

    val items: List<Map<String, Any>> = emptyList(),

    val timestamp: Long = 0L
)

@Composable
fun RestaurantOrdersFirebase(
    selectedTab: String
) {

    val db =
        FirebaseFirestore.getInstance()

    val context = LocalContext.current

    var orders by remember {

        mutableStateOf(
            listOf<RestaurantOrder>()
        )
    }

    // ✅ REALTIME ORDERS

    LaunchedEffect(Unit) {

        RestaurantRepository()

            .restaurantOrders()

            .addSnapshotListener { value, _ ->

                if (value != null) {
                    orders =

                        value.documents

                            .filter {

                                it.getString(
                                    "restaurantId"
                                ) == RestaurantSession.restaurantId

                            }

                            .map {

                            RestaurantOrder(

                                id = it.id,

                                customerName =
                                    it.getString(
                                        "customerName"
                                    ) ?: "",

                                customerPhone =
                                    it.getString(
                                        "customerPhone"
                                    ) ?: "",

                                area =
                                    it.getString(
                                        "area"
                                    ) ?: "",

                                paymentMethod =
                                    it.getString(
                                        "paymentMethod"
                                    ) ?: "",

                                itemsCount =
                                    (it.get("items") as? List<*>)?.size ?: 0,

                                total =
                                    it.getLong(
                                        "total"
                                    )?.toInt()
                                        ?: 0,

                                status =
                                    it.getString(
                                        "status"
                                    ) ?: "PENDING",

                                restaurantName =
                                    it.getString(
                                        "restaurantName"
                                    ) ?: "",

                                discount =
                                    it.getLong(
                                        "discount"
                                    )?.toInt()
                                        ?: 0,

                                packagingFee =
                                    it.getLong(
                                        "packagingFee"
                                    )?.toInt()
                                        ?: 0,

                                items =
                                    it.get(
                                        "items"
                                    ) as? List<Map<String, Any>>
                                        ?: emptyList(),

                                timestamp =
                                    it.getLong(
                                        "timestamp"
                                    ) ?: 0L

                            )
                        }
                }
            }
    }

    LazyColumn(

        modifier =
            Modifier.fillMaxSize()

    ) {
        val filteredOrders = orders.filter { order ->

            when (selectedTab) {

                "NEW" ->
                    order.status == "APPROVED"

                "PREPARING" ->
                    order.status == "PREPARING"

                "READY" ->

                    order.status == "READY_FOR_PICKUP"

                            ||

                            order.status == "RIDER_ASSIGNED"

                "ON_THE_WAY" ->

                    order.status == "PICKED_UP"

                            ||

                            order.status == "OUT_FOR_DELIVERY"

                "COMPLETED" ->
                    order.status == "DELIVERED"

                "CANCELLED" ->
                    order.status == "CANCELLED"

                else -> false
            }
        }
        val sortedOrders =

            filteredOrders
                .sortedByDescending {

                    it.timestamp

                }
        items(sortedOrders) { order ->

            var showDetails by remember {
                mutableStateOf(false)
            }
            var showRejectDialog by remember {
                mutableStateOf(false)
            }

            var rejectReason by remember {
                mutableStateOf("")
            }

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)

            ) {

                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ) {

                    Text(
                        text = "🧾 #${order.id}",
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(

                        text =
                            "📅 " +
                                    java.text.SimpleDateFormat(
                                        "dd MMM hh:mm a"
                                    ).format(
                                        java.util.Date(
                                            order.timestamp
                                        )
                                    )

                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "👤 ${order.customerName}"
                    )
                    Text(
                        text =
                            "📱 ${order.customerPhone}"
                    )

                    Text(
                        text =
                            "📍 ${order.area}"
                    )

                    Text(
                        text =
                            "🛒 ${order.itemsCount} Items"
                    )
                    val restaurantAmount =

                        order.items.sumOf {

                            (it["itemTotal"] as? Long)
                                ?.toInt() ?: 0

                        } + order.packagingFee - order.discount

                    Text(
                        text =
                            "💰 ₹$restaurantAmount"
                    )

                    Text(
                        text =
                            "📌 ${order.status}"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    when (selectedTab) {

                        "NEW" -> {

                            Row {

                                Button(

                                    onClick = {

                                        db.collection("orders")
                                            .document(order.id)
                                            .update(
                                                "status",
                                                "PREPARING"
                                            )
                                    },

                                    colors = ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFF2E7D32)
                                    )

                                ) {

                                    Row {

                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White
                                        )

                                        Spacer(
                                            modifier = Modifier.width(4.dp)
                                        )

                                        Text(
                                            text = "ACCEPT",
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Button(

                                    onClick = {

                                        showRejectDialog = true
                                    },

                                    colors = ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.error
                                    )

                                ) {

                                    Row {

                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null
                                        )

                                        Spacer(
                                            modifier = Modifier.width(4.dp)
                                        )

                                        Text("REJECT")
                                    }
                                }
                            }
                        }

                        "PREPARING" -> {

                            Button(

                                onClick = {

                                    db.collection("orders")
                                        .document(order.id)
                                        .update(
                                            "status",
                                            "READY_FOR_PICKUP"
                                        )
                                }

                            ) {

                                Text("READY FOR PICKUP")
                            }
                        }

                        "READY" -> {

                            Text(
                                text = "⏳ Waiting Rider"
                            )
                        }

                        "ON_THE_WAY" -> {

                            Text(
                                text = "🚚 Out For Delivery"
                            )
                        }

                        "COMPLETED" -> {

                            Text(
                                text = "✅ Delivered"
                            )
                        }

                        "CANCELLED" -> {

                            Text(
                                text = "❌ Cancelled"
                            )
                        }
                    }
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Button(

                        onClick = {
                            showDetails = true
                        }

                    ) {

                        Row {

                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text("VIEW DETAILS")
                        }
                    }
                }
            }
            if (showDetails) {

                AlertDialog(

                    onDismissRequest = {
                        showDetails = false
                    },

                    confirmButton = {

                        TextButton(

                            onClick = {
                                showDetails = false
                            }

                        ) {

                            Text("CLOSE")
                        }
                    },

                    title = { },

                    text = {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp)
                                    .verticalScroll(
                                        rememberScrollState()
                                    )

                        ) {

                            Text(
                                text = "VEGGIEGO",
                                style =
                                    MaterialTheme.typography
                                        .headlineSmall,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    RestaurantSession.restaurantName,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Restaurant Address"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            HorizontalDivider()

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "Order ID"
                            )

                            Text(
                                text =
                                    order.id,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "Date & Time"
                            )

                            Text(
                                text =
                                    java.text.SimpleDateFormat(
                                        "dd MMM yyyy hh:mm a"
                                    ).format(
                                        java.util.Date(
                                            order.timestamp
                                        )
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "Customer Name"
                            )

                            Text(
                                text =
                                    order.customerName,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            HorizontalDivider()

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            order.items.forEach { item ->

                                val name =
                                    item["name"]
                                        ?.toString()
                                        ?: ""

                                val qty =
                                    (item["quantity"]
                                            as? Long)
                                        ?.toInt()
                                        ?: 1

                                val variant =
                                    item["variant"]
                                        ?.toString()
                                        ?: ""

                                val itemPrice =
                                    (item["itemTotal"] as? Long)
                                        ?.toInt()
                                        ?: 0

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.SpaceBetween

                                ) {

                                    Text(
                                        text =
                                            "$qty × $name",

                                        modifier =
                                            Modifier.weight(1f)
                                    )

                                    Text(
                                        text =
                                            "₹$itemPrice",

                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }

                                if (
                                    variant.isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            variant
                                    )
                                }

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )
                            }

                            HorizontalDivider()

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            val itemTotal =

                                order.items.sumOf {

                                    (
                                            it["itemTotal"] as? Long
                                            )?.toInt() ?: 0
                                }

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween

                            ) {

                                Text("Sub Total")

                                Text("₹$itemTotal")
                            }

                            if (order.packagingFee > 0) {

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.SpaceBetween

                                ) {

                                    Text("Packaging Charge")

                                    Text("₹${order.packagingFee}")
                                }
                            }

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween

                            ) {

                                Text("Discount")

                                Text("-₹${order.discount}")
                            }

                            HorizontalDivider()

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement.SpaceBetween

                            ) {

                                Text(
                                    text = "Grand Total",
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "₹${itemTotal + order.packagingFee - order.discount}",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )

                            Text(
                                text =
                                    "Thank You For Your Order!"
                            )

                            Text(
                                text =
                                    "Visit Again!"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )

                            Button(

                                onClick = {

                                    val kotText = buildString {

                                        appendLine("VEGGIEGO")

                                        appendLine()

                                        appendLine(
                                            order.restaurantName
                                        )

                                        appendLine()

                                        appendLine(
                                            "Order ID : ${order.id}"
                                        )

                                        appendLine(
                                            "Date : " +
                                                    java.text.SimpleDateFormat(
                                                        "dd MMM yyyy hh:mm a"
                                                    ).format(
                                                        java.util.Date(
                                                            order.timestamp
                                                        )
                                                    )
                                        )

                                        appendLine(
                                            "Customer : ${order.customerName}"
                                        )

                                        appendLine()

                                        appendLine(
                                            "----------------"
                                        )

                                        order.items.forEach { item ->

                                            val name =
                                                item["name"]?.toString()
                                                    ?: ""

                                            val qty =
                                                (item["quantity"] as? Long)
                                                    ?.toInt()
                                                    ?: 1

                                            appendLine(
                                                "$qty x $name"
                                            )

                                            val variant =
                                                item["variant"]?.toString()
                                                    ?: ""

                                            if (
                                                variant.isNotBlank()
                                            ) {

                                                appendLine(
                                                    variant
                                                )
                                            }
                                        }

                                        appendLine(
                                            "----------------"
                                        )
                                        val itemTotal =

                                            order.items.sumOf {

                                                (
                                                        it["itemTotal"] as? Long
                                                        )?.toInt() ?: 0
                                            }

                                        appendLine(
                                            "Sub Total : ₹$itemTotal"
                                        )

                                        if (order.packagingFee > 0) {

                                            appendLine(
                                                "Packaging : ₹${order.packagingFee}"
                                            )
                                        }

                                        appendLine(
                                            "Discount : ₹${order.discount}"
                                        )

                                        appendLine(
                                            "Grand Total : ₹${itemTotal + order.packagingFee - order.discount}"
                                        )

                                        appendLine()

                                        appendLine(
                                            "Thank You For Your Order!"
                                        )

                                        appendLine(
                                            "Visit Again!"
                                        )
                                    }

                                    KotPrinter.print(

                                        context,

                                        kotText

                                    )
                                }

                            ) {

                                Text(
                                    "🖨 PRINT KOT"
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Button(

                                onClick = {

                                    val pdfText = buildString {

                                        appendLine(
                                            order.restaurantName
                                        )
                                        appendLine()

                                        appendLine("----------------------------")

                                        appendLine("Order ID : ${order.id}")

                                        appendLine(
                                            "Date : " +
                                                    java.text.SimpleDateFormat(
                                                        "dd MMM yyyy hh:mm a"
                                                    ).format(
                                                        java.util.Date(
                                                            order.timestamp
                                                        )
                                                    )
                                        )

                                        appendLine(
                                            "Customer : ${order.customerName}"
                                        )

                                        appendLine("----------------------------")

                                        order.items.forEach { item ->

                                            val name =
                                                item["name"]?.toString()
                                                    ?: ""

                                            val qty =
                                                (item["quantity"] as? Long)
                                                    ?.toInt()
                                                    ?: 1

                                            val rate =
                                                (item["variantPrice"] as? Long)
                                                    ?.toInt()
                                                    ?: 0

                                            val amount =
                                                (item["itemTotal"] as? Long)
                                                    ?.toInt()
                                                    ?: 0

                                            appendLine(name)

                                            val variant =
                                                item["variant"]?.toString()
                                                    ?: ""

                                            if (
                                                variant.isNotBlank()
                                            ) {

                                                appendLine(variant)
                                            }

                                            appendLine(
                                                "Qty $qty x ₹$rate = ₹$amount"
                                            )

                                            appendLine("----------------------------")

                                        }
                                        val itemTotal =

                                            order.items.sumOf {

                                                (
                                                        it["itemTotal"] as? Long
                                                        )?.toInt() ?: 0
                                            }

                                        appendLine()

                                        appendLine(
                                            "Sub Total : ₹$itemTotal"
                                        )

                                        if (order.packagingFee > 0) {

                                            appendLine(
                                                "Packaging : ₹${order.packagingFee}"
                                            )
                                        }

                                        appendLine(
                                            "Discount : ₹${order.discount}"
                                        )

                                        appendLine(
                                            "Grand Total : ₹${itemTotal + order.packagingFee - order.discount}"
                                        )

                                        appendLine()

                                        appendLine(
                                            "Thank You For Your Order!"
                                        )

                                        appendLine(
                                            "Visit Again!"
                                        )
                                    }

                                    PdfGenerator.createPdf(

                                        context = context,

                                        fileName =
                                            "Order_${order.id}.pdf",

                                        content =
                                            pdfText
                                    )
                                },

                                modifier =
                                    Modifier.fillMaxWidth()

                            ) {

                                Text(
                                    "📄 PDF"
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }
                    }
                )
            }
            if (showRejectDialog) {

                AlertDialog(

                    onDismissRequest = {
                        showRejectDialog = false
                    },

                    title = {
                        Text("Reject Order")
                    },

                    text = {

                        Column {

                            OutlinedTextField(

                                value = rejectReason,

                                onValueChange = {
                                    rejectReason = it
                                },

                                label = {
                                    Text("Reason")
                                }
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Minimum 3 characters required"
                            )
                            if (
                                rejectReason.isNotBlank() &&
                                rejectReason.trim().length < 3
                            ) {

                                Text(
                                    text = "Reason must be at least 3 characters"
                                )
                            }
                        }
                    },

                    confirmButton = {

                        Button(

                            onClick = {

                                if (
                                    rejectReason.trim().length >= 3
                                ) {

                                    db.collection("orders")
                                        .document(order.id)

                                        .update(

                                            mapOf(

                                                "status" to "CANCELLED",

                                                "cancelReason"
                                                        to
                                                        rejectReason.trim(),

                                                "cancelledBy"
                                                        to
                                                        "RESTAURANT",

                                                "cancelledAt"
                                                        to
                                                        System.currentTimeMillis(),

                                                "updatedAt"
                                                        to
                                                        System.currentTimeMillis()
                                            )
                                        )

                                    showRejectDialog = false

                                    rejectReason = ""
                                }
                            }

                        ) {

                            Text("SUBMIT")
                        }
                    },

                    dismissButton = {

                        TextButton(

                            onClick = {
                                showRejectDialog = false
                            }

                        ) {

                            Text("CLOSE")
                        }
                    }
                )
            }
        }
    }
}