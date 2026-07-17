package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TimingScreen(
    onBack: () -> Unit
){
    var temporaryClosed by remember {
        mutableStateOf(false)
    }

    var closeReason by remember {
        mutableStateOf("")
    }

    var slots by remember {

        mutableStateOf(

            listOf(
                TimingSlotModel()
            )
        )
    }
    var customDays by remember {

        mutableStateOf(

            listOf(

                DayTimingModel("Monday"),
                DayTimingModel("Tuesday"),
                DayTimingModel("Wednesday"),
                DayTimingModel("Thursday"),
                DayTimingModel("Friday"),
                DayTimingModel("Saturday"),
                DayTimingModel("Sunday")

            )
        )
    }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {

        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )
            .get()
            .addOnSuccessListener { document ->

                temporaryClosed =
                    document.getBoolean(
                        "temporaryClosed"
                    ) ?: false

                closeReason =
                    document.getString(
                        "closeReason"
                    ) ?: ""

                @Suppress("UNCHECKED_CAST")

                val weeklySlots =

                    document.get("weeklySlots")
                            as? Map<String, List<Map<String, String>>>

                if (!weeklySlots.isNullOrEmpty()) {

                    customDays = listOf(

                        "Monday",
                        "Tuesday",
                        "Wednesday",
                        "Thursday",
                        "Friday",
                        "Saturday",
                        "Sunday"

                    ).map { day ->

                        val daySlots =
                            weeklySlots[day] ?: emptyList()

                        DayTimingModel(

                            day = day,

                            slots =

                                if (daySlots.isNotEmpty()) {

                                    daySlots.map { slot ->

                                        val start =
                                            slot["start"] ?: "09:00"

                                        val end =
                                            slot["end"] ?: "23:00"

                                        fun convert24To12(
                                            time: String
                                        ): Triple<String, String, String> {

                                            val hour =
                                                time.substringBefore(":").toInt()

                                            val minute =
                                                time.substringAfter(":")

                                            val period =
                                                if (hour >= 12) "PM" else "AM"

                                            val displayHour = when {

                                                hour == 0 -> 12

                                                hour > 12 -> hour - 12

                                                else -> hour
                                            }

                                            return Triple(
                                                displayHour.toString().padStart(2, '0'),
                                                minute,
                                                period
                                            )
                                        }

                                        val open =
                                            convert24To12(start)

                                        val close =
                                            convert24To12(end)

                                        TimingSlotModel(

                                            openHour = open.first,
                                            openMinute = open.second,
                                            openPeriod = open.third,

                                            closeHour = close.first,
                                            closeMinute = close.second,
                                            closePeriod = close.third
                                        )
                                    }

                                } else {

                                    listOf(
                                        TimingSlotModel()
                                    )
                                }
                        )
                    }
                }
            }
    }
        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp
            )
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                IconButton(
                    onClick = {
                        onBack()
                    }
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
                    text = "Restaurant Timing",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
            Card {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "🔴 Temporarily Closed"
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(

                        value = closeReason,

                        onValueChange = {
                            closeReason = it
                        },

                        label = {
                            Text("Reason")
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Switch(

                        checked = temporaryClosed,

                        onCheckedChange = {
                            temporaryClosed = it
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

                Card {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Custom Day Timing"
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        customDays.forEachIndexed { dayIndex, day ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        text = day.day,
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )

                                    day.slots.forEachIndexed { slotIndex, slot ->

                                        TimingSlotCard(

                                            title = "Slot ${slotIndex + 1}",

                                            openHour = slot.openHour,
                                            openMinute = slot.openMinute,
                                            openPeriod = slot.openPeriod,

                                            closeHour = slot.closeHour,
                                            closeMinute = slot.closeMinute,
                                            closePeriod = slot.closePeriod,

                                            onOpenHourChange = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                slots[slotIndex] =
                                                    slots[slotIndex].copy(
                                                        openHour = it
                                                    )

                                                updated[dayIndex] =
                                                    updated[dayIndex].copy(
                                                        slots = slots
                                                    )

                                                customDays = updated
                                            },

                                            onOpenMinuteChange = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                slots[slotIndex] =
                                                    slots[slotIndex].copy(
                                                        openMinute = it
                                                    )

                                                updated[dayIndex] =
                                                    updated[dayIndex].copy(
                                                        slots = slots
                                                    )

                                                customDays = updated
                                            },

                                            onOpenPeriodChange = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                slots[slotIndex] =
                                                    slots[slotIndex].copy(
                                                        openPeriod = it
                                                    )

                                                updated[dayIndex] =
                                                    updated[dayIndex].copy(
                                                        slots = slots
                                                    )

                                                customDays = updated
                                            },

                                            onCloseHourChange = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                slots[slotIndex] =
                                                    slots[slotIndex].copy(
                                                        closeHour = it
                                                    )

                                                updated[dayIndex] =
                                                    updated[dayIndex].copy(
                                                        slots = slots
                                                    )

                                                customDays = updated
                                            },

                                            onCloseMinuteChange = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                slots[slotIndex] =
                                                    slots[slotIndex].copy(
                                                        closeMinute = it
                                                    )

                                                updated[dayIndex] =
                                                    updated[dayIndex].copy(
                                                        slots = slots
                                                    )

                                                customDays = updated
                                            },

                                            onClosePeriodChange = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                slots[slotIndex] =
                                                    slots[slotIndex].copy(
                                                        closePeriod = it
                                                    )

                                                updated[dayIndex] =
                                                    updated[dayIndex].copy(
                                                        slots = slots
                                                    )

                                                customDays = updated
                                            },

                                            onDelete = {

                                                val updated =
                                                    customDays.toMutableList()

                                                val slots =
                                                    updated[dayIndex]
                                                        .slots
                                                        .toMutableList()

                                                if (slots.size > 1) {

                                                    slots.removeAt(slotIndex)

                                                    updated[dayIndex] =
                                                        updated[dayIndex].copy(
                                                            slots = slots
                                                        )

                                                    customDays = updated
                                                }
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(8.dp)
                                        )
                                    }

                                    Button(

                                        onClick = {

                                            val updated =
                                                customDays.toMutableList()

                                            val slots =
                                                updated[dayIndex]
                                                    .slots
                                                    .toMutableList()

                                            slots.add(
                                                TimingSlotModel()
                                            )

                                            updated[dayIndex] =
                                                updated[dayIndex].copy(
                                                    slots = slots
                                                )

                                            customDays = updated
                                        },

                                        modifier = Modifier.fillMaxWidth()

                                    ) {

                                        Text("➕ Add Slot")
                                    }
                                }
                            }
                        }
                    }
                }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
            Button(

                onClick = {
                    fun convertTo24Hour(

                        hour: String,
                        minute: String,
                        period: String

                    ): String {

                        var h = hour.toInt()

                        if (
                            period == "PM"
                            &&
                            h != 12
                        ) {
                            h += 12
                        }

                        if (
                            period == "AM"
                            &&
                            h == 12
                        ) {
                            h = 0
                        }

                        return h.toString().padStart(2, '0') + ":" + minute
                    }
                    val weeklySlots =
                        mutableMapOf<String, List<Map<String, String>>>()
                    customDays.forEach { day ->

                        weeklySlots[day.day] =

                            day.slots.map { slot ->

                                mapOf(

                                    "start" to convertTo24Hour(

                                        slot.openHour,
                                        slot.openMinute,
                                        slot.openPeriod

                                    ),

                                    "end" to convertTo24Hour(

                                        slot.closeHour,
                                        slot.closeMinute,
                                        slot.closePeriod

                                    )
                                )
                            }
                    }

                    db.collection("restaurants")
                        .document(
                            RestaurantSession.restaurantId
                        )
                        .update(

                            mapOf(

                                "temporaryClosed" to temporaryClosed,

                                "closeReason" to closeReason,

                                "weeklySlots" to weeklySlots,

                                "liveStatus" to

                                        if (temporaryClosed)
                                            "TEMPORARILY_CLOSED"
                                        else
                                            "OPEN",

                                "openingText" to

                                        if (temporaryClosed)
                                            "Temporarily Closed"
                                        else
                                            ""
                            )
                        )
                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Text("💾 Save Timing")
            }
        }
    }
}