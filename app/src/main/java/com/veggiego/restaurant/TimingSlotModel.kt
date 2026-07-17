package com.veggiego.restaurant

data class TimingSlotModel(

    val openHour: String = "09",
    val openMinute: String = "00",
    val openPeriod: String = "AM",

    val closeHour: String = "11",
    val closeMinute: String = "00",
    val closePeriod: String = "PM"
)