package com.veggiego.restaurant

data class SalesOrder(

    val id: String = "",

    val customerName: String = "",

    val customerPhone: String = "",

    val house: String = "",

    val area: String = "",

    val landmark: String = "",

    val city: String = "",

    val paymentMethod: String = "",

    val itemTotal: Int = 0,

    val discount: Int = 0,

    val total: Int = 0,

    val status: String = "",

    val timestamp: Long = 0,

    val items: List<Map<String, Any>> = emptyList(),

    val updatedAt: Long = 0
)