package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimingSlotCard(

    title: String,

    openHour: String,
    openMinute: String,
    openPeriod: String,

    closeHour: String,
    closeMinute: String,
    closePeriod: String,

    onOpenHourChange: (String) -> Unit,
    onOpenMinuteChange: (String) -> Unit,
    onOpenPeriodChange: (String) -> Unit,

    onCloseHourChange: (String) -> Unit,
    onCloseMinuteChange: (String) -> Unit,
    onClosePeriodChange: (String) -> Unit,

    onDelete: () -> Unit

) {

    Card {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TimePickerRow(

                title = "Open Time",

                hour = openHour,
                minute = openMinute,
                period = openPeriod,

                onHourChange = onOpenHourChange,
                onMinuteChange = onOpenMinuteChange,
                onPeriodChange = onOpenPeriodChange
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            TimePickerRow(

                title = "Close Time",

                hour = closeHour,
                minute = closeMinute,
                period = closePeriod,

                onHourChange = onCloseHourChange,
                onMinuteChange = onCloseMinuteChange,
                onPeriodChange = onClosePeriodChange
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(

                onClick = onDelete,

                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),

                modifier = Modifier.fillMaxWidth()

            ) {

                Text(
                    text = "🗑 Delete Slot",
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}