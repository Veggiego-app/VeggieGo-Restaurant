package com.veggiego.restaurant

data class DayTimingModel(

    val day: String = "",

    val slots: List<TimingSlotModel> = listOf(
        TimingSlotModel()
    )
)