package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun OrderDetailsScreen(

    onBack: () -> Unit = {},

    orderId: String = "4qwowFHm",

    customerName: String = "Vipul",

    customerPhone: String = "9099116001",

    area: String = "Apnanagar",

    city: String = "Gandhidham",

    total: Int = 4101,

    items: List<Map<String, Any>> = emptyList(),

    itemTotal: Int = 0,

    discount: Int = 0,

    timestamp: Long = 0L

) {

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
                    imageVector =
                        Icons.Default.ArrowBack,
                    contentDescription = null
                )
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "Order Details",
                style =
                    MaterialTheme.typography.headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text = "🧾 Order ID"
                )

                Text(
                    text = orderId,
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
                                    java.util.Date(timestamp)
                                ),

                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text = "👤 Customer"
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(customerName)

            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text = "📍 Delivery Address"
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(area)

                Text(city)
            }
        }
        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text = "🍔 Ordered Items",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                items.forEach { item ->

                    val name =
                        item["name"]?.toString()
                            ?: "Item"

                    val qty =
                        item["quantity"]
                            ?.toString()
                            ?: "1"

                    val variant =
                        item["variant"]
                            ?.toString()
                            ?: ""

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text = name,
                                maxLines = 2,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                text = "Qty : $qty"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )

                        Text(
                            text = "₹${
                                (
                                        item["itemTotal"]
                                                as? Number
                                        )?.toInt() ?: 0
                            }",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    if (variant.isNotBlank()) {

                        Text(
                            text =
                                "Variant : $variant",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }

                    val addons =
                        item["addons"]
                                as? List<Map<String, Any>>
                            ?: emptyList()

                    addons.forEach { addon ->

                        Card {

                            Text(
                                text =
                                    addon["name"]
                                        ?.toString()
                                        ?: "",
                                modifier =
                                    Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )
                    }

                    HorizontalDivider()

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )
                }
            }
        }
        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    )
            ) {

                Text(
                    text = "🛒 Order Summary"
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "${items.size} Items"
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Item Total : ₹$itemTotal"
                )

                Text(
                    text =
                        "Discount : ₹$discount"
                )

                Text(
                    text =
                        "Final Sale : ₹${itemTotal - discount}",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}