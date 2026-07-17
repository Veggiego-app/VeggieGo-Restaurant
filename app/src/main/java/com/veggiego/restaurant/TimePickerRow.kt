package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimePickerRow(

    title: String,

    hour: String,
    minute: String,
    period: String,

    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit,
    onPeriodChange: (String) -> Unit

) {

    var hourExpanded by remember {
        mutableStateOf(false)
    }

    var minuteExpanded by remember {
        mutableStateOf(false)
    }

    var periodExpanded by remember {
        mutableStateOf(false)
    }

    Column {

        Text(
            text = title
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row {

            Box {

                OutlinedButton(
                    onClick = {
                        hourExpanded = true
                    }
                ) {
                    Text(hour)
                }

                DropdownMenu(
                    expanded = hourExpanded,
                    onDismissRequest = {
                        hourExpanded = false
                    }
                ) {

                    (1..12).forEach {

                        DropdownMenuItem(

                            text = {
                                Text(
                                    it.toString()
                                        .padStart(2, '0')
                                )
                            },

                            onClick = {

                                onHourChange(
                                    it.toString()
                                        .padStart(2, '0')
                                )

                                hourExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Box {

                OutlinedButton(
                    onClick = {
                        minuteExpanded = true
                    }
                ) {
                    Text(minute)
                }

                DropdownMenu(
                    expanded = minuteExpanded,
                    onDismissRequest = {
                        minuteExpanded = false
                    }
                ) {

                    (0..59).forEach {

                        DropdownMenuItem(

                            text = {
                                Text(
                                    it.toString()
                                        .padStart(2, '0')
                                )
                            },

                            onClick = {

                                onMinuteChange(
                                    it.toString()
                                        .padStart(2, '0')
                                )

                                minuteExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Box {

                OutlinedButton(
                    onClick = {
                        periodExpanded = true
                    }
                ) {
                    Text(period)
                }

                DropdownMenu(
                    expanded = periodExpanded,
                    onDismissRequest = {
                        periodExpanded = false
                    }
                ) {

                    DropdownMenuItem(

                        text = {
                            Text("AM")
                        },

                        onClick = {

                            onPeriodChange("AM")

                            periodExpanded = false
                        }
                    )

                    DropdownMenuItem(

                        text = {
                            Text("PM")
                        },

                        onClick = {

                            onPeriodChange("PM")

                            periodExpanded = false
                        }
                    )
                }
            }
        }
    }
}