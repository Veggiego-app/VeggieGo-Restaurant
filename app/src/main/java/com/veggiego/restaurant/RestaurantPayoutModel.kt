package com.veggiego.restaurant

data class RestaurantPayoutRecord(
    val orderId: String = "",
    val status: String = "",
    val timestamp: Long = 0L,
    val itemTotal: Int = 0,
    val packagingFee: Int = 0,
    val restaurantDiscount: Int = 0,
    val commissionBase: Double = 0.0,
    val commissionPercent: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val restaurantPayout: Double = 0.0,
    val compensation: Double = 0.0,
    val penalty: Double = 0.0,
    val manualCredit: Double = 0.0,
    val manualDebit: Double = 0.0,
    val adminRemark: String = "",
    val settlementStatus: String = "PENDING"
)
