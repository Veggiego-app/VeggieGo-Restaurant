package com.veggiego.restaurant

data class TimeSlot(

    val open: String = "09:00 AM",

    val close: String = "11:00 PM"
)

data class RestaurantTimingModel(

    val temporaryClosed: Boolean = false,

    val temporaryCloseReason: String = "",

    val sameTimingEveryday: Boolean = true,

    val monday: List<TimeSlot> = listOf(),

    val tuesday: List<TimeSlot> = listOf(),

    val wednesday: List<TimeSlot> = listOf(),

    val thursday: List<TimeSlot> = listOf(),

    val friday: List<TimeSlot> = listOf(),

    val saturday: List<TimeSlot> = listOf(),

    val sunday: List<TimeSlot> = listOf()
)