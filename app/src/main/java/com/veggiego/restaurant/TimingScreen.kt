package com.veggiego.restaurant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val weekDays = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday",
    "Friday", "Saturday", "Sunday"
)

@Composable
fun TimingScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val restaurantRef = remember {
        db.collection("restaurants").document(RestaurantSession.restaurantId)
    }

    var temporaryClosed by remember { mutableStateOf(false) }
    var closeReason by remember { mutableStateOf("") }
    var customDays by remember {
        mutableStateOf(weekDays.map { DayTimingModel(it) })
    }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        restaurantRef.get()
            .addOnSuccessListener { document ->
                temporaryClosed = document.getBoolean("temporaryClosed") ?: false
                closeReason = document.getString("closeReason") ?: ""

                @Suppress("UNCHECKED_CAST")
                val weeklySlots =
                    document.get("weeklySlots") as? Map<String, List<Map<String, String>>>

                if (!weeklySlots.isNullOrEmpty()) {
                    customDays = weekDays.map { day ->
                        val savedSlots = weeklySlots[day].orEmpty()
                        DayTimingModel(
                            day = day,
                            slots = if (savedSlots.isEmpty()) {
                                emptyList()
                            } else {
                                savedSlots.map { slot ->
                                    val open = convert24To12(slot["start"] ?: "09:00")
                                    val close = convert24To12(slot["end"] ?: "23:00")
                                    TimingSlotModel(
                                        openHour = open.first,
                                        openMinute = open.second,
                                        openPeriod = open.third,
                                        closeHour = close.first,
                                        closeMinute = close.second,
                                        closePeriod = close.third
                                    )
                                }
                            }
                        )
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                message = "Timing load nahi ho saki"
            }
    }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            message = null
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            item {
                TimingHeader(
                    isEditing = isEditing,
                    onBack = {
                        if (isEditing) isEditing = false else onBack()
                    },
                    onEdit = { isEditing = true }
                )

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = if (it.contains("save", true))
                            Color(0xFF168743)
                        else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (!isEditing) {
                item {
                    WeeklyTimingView(
                        days = customDays,
                        temporaryClosed = temporaryClosed,
                        closeReason = closeReason
                    )
                }
            } else {
                item {
                    TemporaryCloseEditor(
                        temporaryClosed = temporaryClosed,
                        closeReason = closeReason,
                        onTemporaryClosedChange = { temporaryClosed = it },
                        onReasonChange = { closeReason = it }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                customDays.forEachIndexed { dayIndex, day ->
                    item(key = day.day) {
                        DayTimingEditor(
                            day = day,
                            onSlotChange = { slotIndex, newSlot ->
                                val updatedDays = customDays.toMutableList()
                                val updatedSlots = day.slots.toMutableList()
                                updatedSlots[slotIndex] = newSlot
                                updatedDays[dayIndex] = day.copy(slots = updatedSlots)
                                customDays = updatedDays
                            },
                            onDeleteSlot = { slotIndex ->
                                val updatedDays = customDays.toMutableList()
                                val updatedSlots = day.slots.toMutableList()
                                updatedSlots.removeAt(slotIndex)
                                updatedDays[dayIndex] = day.copy(slots = updatedSlots)
                                customDays = updatedDays
                            },
                            onAddSlot = {
                                val updatedDays = customDays.toMutableList()
                                updatedDays[dayIndex] = day.copy(
                                    slots = day.slots + TimingSlotModel()
                                )
                                customDays = updatedDays
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                item {
                    Button(
                        onClick = {
                            isSaving = true
                            val weeklySlots = customDays.associate { day ->
                                day.day to day.slots.map { slot ->
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

                            restaurantRef.update(
                                mapOf(
                                    "temporaryClosed" to temporaryClosed,
                                    "closeReason" to closeReason.trim(),
                                    "weeklySlots" to weeklySlots,
                                    "liveStatus" to if (temporaryClosed)
                                        "TEMPORARILY_CLOSED" else "OPEN",
                                    "openingText" to if (temporaryClosed)
                                        "Temporarily Closed" else ""
                                )
                            ).addOnSuccessListener {
                                isSaving = false
                                isEditing = false
                                message = "Timing successfully saved"
                            }.addOnFailureListener {
                                isSaving = false
                                message = "Timing save nahi hui, dobara try karein"
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Timing")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingHeader(
    isEditing: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = if (isEditing) "Edit Restaurant Timing" else "Restaurant Timing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (!isEditing) {
            TextButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Edit Time")
            }
        }
    }
}

@Composable
private fun WeeklyTimingView(
    days: List<DayTimingModel>,
    temporaryClosed: Boolean,
    closeReason: String
) {
    val today = remember {
        SimpleDateFormat("EEEE", Locale.ENGLISH).format(Calendar.getInstance().time)
    }
    val todayTiming = days.firstOrNull { it.day == today }
    val liveText = currentStatusText(todayTiming, temporaryClosed)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = liveText,
                style = MaterialTheme.typography.titleMedium,
                color = if (liveText.startsWith("Open"))
                    Color(0xFF168743) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )

            if (temporaryClosed && closeReason.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = closeReason,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            days.forEach { day ->
                val isToday = day.day == today
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = day.day,
                        modifier = Modifier.weight(0.38f),
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(0.62f)) {
                        if (day.slots.isEmpty()) {
                            Text(
                                text = "Closed",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        } else {
                            day.slots.forEach { slot ->
                                Text(
                                    text = "${displayTime(slot, true)} - ${displayTime(slot, false)}",
                                    fontWeight = if (isToday)
                                        FontWeight.Bold else FontWeight.Medium
                                )
                                if (slot != day.slots.last()) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemporaryCloseEditor(
    temporaryClosed: Boolean,
    closeReason: String,
    onTemporaryClosedChange: (Boolean) -> Unit,
    onReasonChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Temporarily Closed", fontWeight = FontWeight.Bold)
                    Text(
                        "Restaurant ko kuchh samay ke liye band rakhein",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = temporaryClosed,
                    onCheckedChange = onTemporaryClosedChange
                )
            }
            if (temporaryClosed) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = closeReason,
                    onValueChange = onReasonChange,
                    label = { Text("Close reason") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun DayTimingEditor(
    day: DayTimingModel,
    onSlotChange: (Int, TimingSlotModel) -> Unit,
    onDeleteSlot: (Int) -> Unit,
    onAddSlot: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.day,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (day.slots.isEmpty()) {
                    Text(
                        "Closed",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            day.slots.forEachIndexed { slotIndex, slot ->
                Spacer(Modifier.height(12.dp))
                TimingSlotCard(
                    title = "Slot ${slotIndex + 1}",
                    openHour = slot.openHour,
                    openMinute = slot.openMinute,
                    openPeriod = slot.openPeriod,
                    closeHour = slot.closeHour,
                    closeMinute = slot.closeMinute,
                    closePeriod = slot.closePeriod,
                    onOpenHourChange = {
                        onSlotChange(slotIndex, slot.copy(openHour = it))
                    },
                    onOpenMinuteChange = {
                        onSlotChange(slotIndex, slot.copy(openMinute = it))
                    },
                    onOpenPeriodChange = {
                        onSlotChange(slotIndex, slot.copy(openPeriod = it))
                    },
                    onCloseHourChange = {
                        onSlotChange(slotIndex, slot.copy(closeHour = it))
                    },
                    onCloseMinuteChange = {
                        onSlotChange(slotIndex, slot.copy(closeMinute = it))
                    },
                    onClosePeriodChange = {
                        onSlotChange(slotIndex, slot.copy(closePeriod = it))
                    },
                    onDelete = { onDeleteSlot(slotIndex) }
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAddSlot,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (day.slots.isEmpty()) "Add Timing" else "Add Slot")
            }
        }
    }
}

private fun displayTime(slot: TimingSlotModel, isOpen: Boolean): String {
    val hour = if (isOpen) slot.openHour else slot.closeHour
    val minute = if (isOpen) slot.openMinute else slot.closeMinute
    val period = if (isOpen) slot.openPeriod else slot.closePeriod
    return "${hour.toIntOrNull() ?: hour}:$minute ${period.lowercase(Locale.ENGLISH)}"
}

private fun currentStatusText(
    today: DayTimingModel?,
    temporaryClosed: Boolean
): String {
    if (temporaryClosed) return "Temporarily Closed"
    if (today == null || today.slots.isEmpty()) return "Closed today"

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    today.slots.forEach { slot ->
        val open = slotMinutes(
            slot.openHour, slot.openMinute, slot.openPeriod
        )
        val close = slotMinutes(
            slot.closeHour, slot.closeMinute, slot.closePeriod
        )
        val isOpen = if (close > open) {
            nowMinutes in open until close
        } else {
            nowMinutes >= open || nowMinutes < close
        }
        if (isOpen) return "Open now • Closes ${displayTime(slot, false)}"
    }

    val nextSlot = today.slots.firstOrNull {
        slotMinutes(it.openHour, it.openMinute, it.openPeriod) > nowMinutes
    }
    return if (nextSlot != null) {
        "Closed now • Opens ${displayTime(nextSlot, true)}"
    } else {
        "Closed now"
    }
}

private fun slotMinutes(hour: String, minute: String, period: String): Int {
    var h = hour.toIntOrNull() ?: 0
    if (period.equals("PM", true) && h != 12) h += 12
    if (period.equals("AM", true) && h == 12) h = 0
    return h * 60 + (minute.toIntOrNull() ?: 0)
}

private fun convert24To12(time: String): Triple<String, String, String> {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1) ?: "00"
    val period = if (hour >= 12) "PM" else "AM"
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

private fun convertTo24Hour(
    hour: String,
    minute: String,
    period: String
): String {
    var h = hour.toIntOrNull() ?: 0
    if (period.equals("PM", true) && h != 12) h += 12
    if (period.equals("AM", true) && h == 12) h = 0
    return "${h.toString().padStart(2, '0')}:$minute"
}