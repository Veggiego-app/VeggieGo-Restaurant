package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CategoryTimingDialog(

    categoryId: String,

    onDismiss: () -> Unit

) {

    var slots by remember {

        mutableStateOf(

            listOf(
                TimingSlotModel()
            )
        )
    }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {

        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )
            .collection("categories")
            .document(categoryId)
            .get()
            .addOnSuccessListener { doc ->

                val timeSlots =
                    doc.get("timeSlots")
                            as? List<Map<String, Any>>

                if (!timeSlots.isNullOrEmpty()) {

                    slots = timeSlots.map {

                        fun to12Hour(
                            time: String
                        ): Triple<String,String,String> {

                            val parts =
                                time.split(":")

                            var hour =
                                parts[0].toInt()

                            val minute =
                                parts[1]

                            val period =
                                if (hour >= 12)
                                    "PM"
                                else
                                    "AM"

                            if (hour == 0)
                                hour = 12
                            else if (hour > 12)
                                hour -= 12

                            return Triple(
                                hour.toString().padStart(2,'0'),
                                minute,
                                period
                            )
                        }

                        val start =
                            to12Hour(
                                it["start"].toString()
                            )

                        val end =
                            to12Hour(
                                it["end"].toString()
                            )

                        TimingSlotModel(

                            openHour = start.first,
                            openMinute = start.second,
                            openPeriod = start.third,

                            closeHour = end.first,
                            closeMinute = end.second,
                            closePeriod = end.third
                        )
                    }
                }
            }
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("⏰ Category Timing")
        },

        text = {

            LazyColumn {

                items(slots.size) { index ->

                    val slot =
                        slots[index]

                    TimingSlotCard(

                        title =
                            "Slot ${index + 1}",

                        openHour =
                            slot.openHour,

                        openMinute =
                            slot.openMinute,

                        openPeriod =
                            slot.openPeriod,

                        closeHour =
                            slot.closeHour,

                        closeMinute =
                            slot.closeMinute,

                        closePeriod =
                            slot.closePeriod,

                        onOpenHourChange = {

                            val updated =
                                slots.toMutableList()

                            updated[index] =
                                updated[index].copy(
                                    openHour = it
                                )

                            slots = updated
                        },

                        onOpenMinuteChange = {

                            val updated =
                                slots.toMutableList()

                            updated[index] =
                                updated[index].copy(
                                    openMinute = it
                                )

                            slots = updated
                        },

                        onOpenPeriodChange = {

                            val updated =
                                slots.toMutableList()

                            updated[index] =
                                updated[index].copy(
                                    openPeriod = it
                                )

                            slots = updated
                        },

                        onCloseHourChange = {

                            val updated =
                                slots.toMutableList()

                            updated[index] =
                                updated[index].copy(
                                    closeHour = it
                                )

                            slots = updated
                        },

                        onCloseMinuteChange = {

                            val updated =
                                slots.toMutableList()

                            updated[index] =
                                updated[index].copy(
                                    closeMinute = it
                                )

                            slots = updated
                        },

                        onClosePeriodChange = {

                            val updated =
                                slots.toMutableList()

                            updated[index] =
                                updated[index].copy(
                                    closePeriod = it
                                )

                            slots = updated
                        },

                        onDelete = {

                            slots =
                                slots.filterIndexed {
                                        i, _ ->
                                    i != index
                                }

                            if (slots.isEmpty()) {

                                slots =
                                    emptyList()
                            }
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )
                }

                item {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = {
                                slots = slots + TimingSlotModel()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➕ Add Slot")
                        }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                slots = emptyList()
                            }
                        ) {
                            Text("🗑 CLEAR ALL TIMING")
                        }
                    }
                }
            }
        },

        confirmButton = {

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
                        ) h += 12

                        if (
                            period == "AM"
                            &&
                            h == 12
                        ) h = 0

                        return h.toString()
                            .padStart(2,'0') +
                                ":" +
                                minute
                    }

                    val timeSlots =

                        slots.map {

                            mapOf(

                                "start" to
                                        convertTo24Hour(
                                            it.openHour,
                                            it.openMinute,
                                            it.openPeriod
                                        ),

                                "end" to
                                        convertTo24Hour(
                                            it.closeHour,
                                            it.closeMinute,
                                            it.closePeriod
                                        )
                            )
                        }

                    db.collection("restaurants")
                        .document(
                            RestaurantSession.restaurantId
                        )
                        .collection("categories")
                        .document(categoryId)
                        .update(

                            mapOf(

                                "startTime" to "",

                                "endTime" to "",

                                "timeSlots" to timeSlots
                            )
                        )

                    onDismiss()
                }

            ) {

                Text("SAVE")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("CANCEL")
            }
        }
    )
}