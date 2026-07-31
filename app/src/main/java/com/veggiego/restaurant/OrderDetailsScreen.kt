package com.veggiego.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import java.util.Date

@Composable
fun OrderDetailsScreen(
    onBack: () -> Unit = {},
    orderId: String = "",
    customerName: String = "",
    customerPhone: String = "",
    area: String = "",
    city: String = "",
    total: Int = 0,
    items: List<Map<String, Any>> = emptyList(),
    itemTotal: Int = 0,
    discount: Int = 0,
    timestamp: Long = 0L
) {
    val context = LocalContext.current
    var liveData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    DisposableEffect(orderId) {
        if (orderId.isBlank()) return@DisposableEffect onDispose { }
        val listener = FirebaseFirestore.getInstance().collection("orders").document(orderId)
            .addSnapshotListener { doc, _ -> if (doc != null && doc.exists()) liveData = doc.data ?: emptyMap() }
        onDispose { listener.remove() }
    }

    val status = liveData["status"]?.toString().orEmpty()
    val finalItems = (liveData["items"] as? List<Map<String, Any>>) ?: items
    val finalItemTotal = value(liveData, "itemTotal").toInt().takeIf { it > 0 } ?: itemTotal
    val packaging = value(liveData, "packagingFee").toInt()
    val restaurantDiscount = value(liveData, "restaurantDiscount", "discount").toInt().takeIf { it > 0 } ?: discount
    val commissionBaseSaved = value(liveData, "commissionBase", "restaurantCommissionBase")
    val commissionPercent = value(liveData, "commissionPercent", "restaurantCommissionPercent", "commission")
    val commissionBase = if (commissionBaseSaved > 0) commissionBaseSaved else (finalItemTotal + packaging - restaurantDiscount).coerceAtLeast(0).toDouble()
    val commissionSaved = value(liveData, "commissionAmount", "restaurantCommissionAmount")
    val commissionAmount = if (commissionSaved > 0) commissionSaved else commissionBase * commissionPercent / 100.0
    val payoutSaved = value(liveData, "restaurantPayout", "restaurantAmount", "payout")
    val payout = if (payoutSaved > 0) payoutSaved else commissionBase - commissionAmount
    val compensation = value(liveData, "restaurantCompensation", "compensation")
    val penalty = value(liveData, "restaurantPenalty", "penalty")
    val manualCredit = value(liveData, "restaurantManualCredit", "manualCredit")
    val manualDebit = value(liveData, "restaurantManualDebit", "manualDebit")
    val adminRemark = textValue(liveData, "restaurantAdminRemark", "adminRemark", "settlementRemark")
    val cancelReason = textValue(liveData, "cancelReason", "rejectionReason")
    val cancelledBy = textValue(liveData, "cancelledBy")
    val settlementStatus = textValue(
        liveData,
        "restaurantSettlementStatus",
        "settlementStatus"
    ).ifBlank { "PENDING" }

    val settlementPaymentMode = textValue(
        liveData,
        "restaurantSettlementPaymentMode",
        "settlementPaymentMode",
        "paymentMode"
    )

    val settlementPaymentDate = textValue(
        liveData,
        "restaurantSettlementPaymentDate",
        "settlementPaymentDate",
        "paymentDate"
    )

    val settlementPaymentReference = textValue(
        liveData,
        "restaurantSettlementPaymentReference",
        "settlementPaymentReference"
    )

    val finalName = textValue(liveData, "customerName").ifBlank { customerName }
    val finalPhone = textValue(liveData, "customerPhone").ifBlank { customerPhone }
    val finalArea = textValue(liveData, "area").ifBlank { area }
    val finalCity = textValue(liveData, "city").ifBlank { city }
    val finalTimestamp = value(liveData, "timestamp", "createdAt").toLong().takeIf { it > 0 } ?: timestamp

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFFFF8F0))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Order Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                if (status.isNotBlank()) {
                    Surface(
                        color = statusColor(status),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = status.replace('_', ' '),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                        )
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "🧾 Order",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                MainDetailRow(
                    label = "Order ID",
                    value = orderId
                )

                MainDetailRow(
                    label = "Date",
                    value = formatDate(finalTimestamp)
                )

                MainDetailRow(
                    label = "Payment",
                    value = textValue(
                        liveData,
                        "paymentMethod"
                    ).ifBlank {
                        "COD"
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 12.dp
                    )
                )

                Text(
                    text = "👤 Customer",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                MainDetailRow(
                    label = "Name",
                    value = finalName
                )

                MainDetailRow(
                    label = "Phone",
                    value = finalPhone
                )

                MainDetailRow(
                    label = "Address",
                    value = listOf(
                        finalArea,
                        finalCity
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .joinToString(", ")
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 12.dp
                    )
                )

                Text(
                    text = "🍔 Ordered Items",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                finalItems.forEach { item ->

                    val name =
                        item["name"]
                            ?.toString()
                            ?: "Item"

                    val qty =
                        (
                                item["quantity"]
                                        as? Number
                                )?.toInt()
                            ?: 1

                    val amount =
                        (
                                item["itemTotal"]
                                        as? Number
                                )?.toInt()
                            ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "$qty × $name",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242)
                        )

                        Text(
                            text = "₹$amount",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    item["variant"]
                        ?.toString()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {

                            Text(
                                text = "Variant: $it",
                                color = Color(0xFF1565C0)
                            )
                        }

                    (
                            item["addons"]
                                    as? List<Map<String, Any>>
                            )
                        .orEmpty()
                        .forEach { addon ->

                            Text(
                                text = "+ ${
                                    addon["name"]
                                        ?: ""
                                }",
                                color = Color(0xFF6A1B9A)
                            )
                        }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    HorizontalDivider()

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 12.dp
                    )
                )

                Text(
                    text = "🛒 Order Summary",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                MoneyLine(
                    label = "Item Total",
                    amount = finalItemTotal.toDouble()
                )

                MoneyLine(
                    label = "Packaging",
                    amount = packaging.toDouble()
                )

                if (
                    restaurantDiscount > 0
                ) {

                    MoneyLine(
                        label = "Restaurant Discount",
                        amount = -restaurantDiscount.toDouble()
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 8.dp
                    )
                )

                MoneyLine(
                    label = "Order Amount",
                    amount = (
                            finalItemTotal +
                                    packaging -
                                    restaurantDiscount
                            ).toDouble(),
                    bold = true
                )
            }
        }

        if (status == "DELIVERED") {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "💰 Restaurant Settlement",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1B5E20)
                    )
                    MoneyLine("Item Total", finalItemTotal.toDouble())
                    MoneyLine("Packaging", packaging.toDouble())
                    if (restaurantDiscount > 0) MoneyLine("Restaurant Discount", -restaurantDiscount.toDouble())
                    MoneyLine("Commission Base", commissionBase)
                    Text("Commission Rate: ${trimMoney(commissionPercent)}%")
                    MoneyLine("Commission", -commissionAmount)
                    if (compensation != 0.0) MoneyLine("Compensation", compensation)
                    if (penalty != 0.0) MoneyLine("Penalty", -penalty)
                    if (manualCredit != 0.0) MoneyLine("Manual Credit", manualCredit)
                    if (manualDebit != 0.0) MoneyLine("Manual Debit", -manualDebit)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    MoneyLine("Restaurant Payout", payout + compensation - penalty + manualCredit - manualDebit, true)
                    Surface(
                        color =
                            when {
                                settlementStatus.equals(
                                    "PAID",
                                    ignoreCase = true
                                ) ||
                                        settlementStatus.equals(
                                            "SETTLED",
                                            ignoreCase = true
                                        ) -> Color(0xFF2E7D32)

                                settlementStatus.equals(
                                    "FINALIZED",
                                    ignoreCase = true
                                ) -> Color(0xFF1565C0)

                                else -> Color(0xFFFFB300)
                            },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Settlement: $settlementStatus",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                        )
                    }

                    if (
                        settlementStatus.equals(
                            "PAID",
                            ignoreCase = true
                        ) ||
                        settlementStatus.equals(
                            "SETTLED",
                            ignoreCase = true
                        )
                    ) {
                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        if (settlementPaymentMode.isNotBlank()) {
                            MainDetailRow(
                                label = "Payment Mode",
                                value = settlementPaymentMode
                                    .replace(
                                        "_",
                                        " "
                                    )
                            )
                        }

                        if (settlementPaymentDate.isNotBlank()) {
                            MainDetailRow(
                                label = "Payment Date",
                                value = formatSettlementDate(
                                    settlementPaymentDate
                                )
                            )
                        }

                        if (
                            settlementPaymentReference.isNotBlank() &&
                            !settlementPaymentReference.equals(
                                "CASH",
                                ignoreCase = true
                            )
                        ) {
                            MainDetailRow(
                                label = "Reference / UTR",
                                value = settlementPaymentReference
                            )
                        }
                    }

                    if (adminRemark.isNotBlank()) {
                        Text("Admin Remark: $adminRemark")
                    }
                }
            }
        }

        if (status in setOf("CANCELLED", "REJECTED")) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "❌ Cancellation Details",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    if (cancelledBy.isNotBlank()) Text("Cancelled By: $cancelledBy")
                    if (cancelReason.isNotBlank()) Text("Reason: $cancelReason")
                    if (compensation != 0.0) MoneyLine("Compensation", compensation)
                    if (penalty != 0.0) MoneyLine("Penalty", -penalty)
                    if (adminRemark.isNotBlank()) Text("Admin Remark: $adminRemark")
                    if (compensation == 0.0 && penalty == 0.0 && adminRemark.isBlank()) Text("No compensation or admin adjustment added.")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                KotPrinter.print(context, buildKot(orderId, finalTimestamp, finalName, finalItems))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF8A00)
            )
        ) {
            Text(
                text = "🖨 PRINT KOT",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                PdfGenerator.createPdf(context, "Order_$orderId.pdf", buildInvoice(orderId, finalTimestamp, finalName, finalItems, finalItemTotal, packaging, restaurantDiscount))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF1565C0)
            )
        ) {
            Text(
                text = "📄 INVOICE PDF",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun MainDetailRow(
    label: String,
    value: String
) {

    if (
        value.isBlank()
    ) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 3.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color = Color(0xFF6D4C41)
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF424242)
        )
    }
}

@Composable
private fun MoneyLine(
    label: String,
    amount: Double,
    bold: Boolean = false
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
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
                    Color(0xFF424242)
                }
        )

        Text(
            text =
                if (amount < 0) {
                    "-₹${trimMoney(-amount)}"
                } else {
                    "₹${trimMoney(amount)}"
                },
            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
            color =
                when {
                    bold -> Color(0xFF1B5E20)
                    amount < 0 -> Color(0xFFC62828)
                    else -> Color(0xFF1565C0)
                }
        )
    }
}
private fun statusColor(
    status: String
): Color {

    return when (status) {

        "APPROVED",
        "NEW",
        "PENDING",
        "RESTAURANT_PENDING" ->
            Color(0xFFE53935)

        "ACCEPTED",
        "PREPARING" ->
            Color(0xFFF9A825)

        "READY_FOR_PICKUP",
        "RIDER_ASSIGNED" ->
            Color(0xFF2E7D32)

        "PICKED_UP",
        "OUT_FOR_DELIVERY" ->
            Color(0xFF1565C0)

        "DELIVERED" ->
            Color(0xFF6A1B9A)

        "CANCELLED",
        "REJECTED" ->
            Color(0xFFC62828)

        else ->
            Color(0xFF616161)
    }
}

private fun value(data: Map<String, Any>, vararg keys: String): Double { keys.forEach { (data[it] as? Number)?.toDouble()?.let { n -> return n } }; return 0.0 }
private fun textValue(data: Map<String, Any>, vararg keys: String): String { keys.forEach { data[it]?.toString()?.takeIf(String::isNotBlank)?.let { s -> return s } }; return "" }
private fun trimMoney(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)

private fun formatDate(time: Long): String =
    if (time > 0) {
        SimpleDateFormat(
            "dd/MM/yyyy hh:mm a"
        ).format(
            Date(time)
        )
    } else {
        ""
    }

private fun formatSettlementDate(
    value: String
): String {

    return try {

        val source =
            SimpleDateFormat(
                "yyyy-MM-dd"
            )

        val target =
            SimpleDateFormat(
                "dd/MM/yyyy"
            )

        val parsed =
            source.parse(value)

        if (parsed != null) {
            target.format(parsed)
        } else {
            value
        }

    } catch (_: Exception) {

        value
    }
}

private fun buildKot(id: String, time: Long, customer: String, items: List<Map<String, Any>>): String = buildString {
    appendLine("${RestaurantSession.restaurantName}")
    appendLine("KOT - NOT A BILL")
    appendLine("--------------------------------")
    appendLine("Order: $id")
    appendLine("Date: ${formatDate(time)}")
    if (customer.isNotBlank()) appendLine("Customer: $customer")
    appendLine("--------------------------------")
    items.forEach { item ->
        val q = (item["quantity"] as? Number)?.toInt() ?: 1
        appendLine("$q x ${item["name"] ?: "Item"}")
        item["variant"]?.toString()?.takeIf { it.isNotBlank() }?.let { appendLine("  Variant: $it") }
        (item["addons"] as? List<Map<String, Any>>).orEmpty().forEach { appendLine("  + ${it["name"] ?: ""}") }
    }
    appendLine("--------------------------------")
    appendLine("KITCHEN COPY")
    appendLine(); appendLine(); appendLine()
}
private fun buildInvoice(id: String, time: Long, customer: String, items: List<Map<String, Any>>, itemTotal: Int, packaging: Int, discount: Int): String = buildString {
    appendLine(RestaurantSession.restaurantName); appendLine("Order ID: $id"); appendLine("Date: ${formatDate(time)}"); appendLine("Customer: $customer"); appendLine("----------------------------")
    items.forEach { appendLine("${(it["quantity"] as? Number)?.toInt() ?: 1} x ${it["name"] ?: "Item"}  ₹${(it["itemTotal"] as? Number)?.toInt() ?: 0}") }
    appendLine("----------------------------"); appendLine("Item Total: ₹$itemTotal"); appendLine("Packaging: ₹$packaging"); appendLine("Discount: -₹$discount"); appendLine("Total: ₹${itemTotal + packaging - discount}")
}
