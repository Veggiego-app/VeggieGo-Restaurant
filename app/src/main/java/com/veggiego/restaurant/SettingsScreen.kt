package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onTimingClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    val db = FirebaseFirestore.getInstance()

    val context = LocalContext.current

    var isOnline by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )
            .get()
            .addOnSuccessListener {

                isOnline =
                    it.getBoolean("online")
                        ?: true
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF3E0))
            .padding(16.dp)
    ) {

        Text(
            text = "Restaurant Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Restaurant Status"
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            if (isOnline)
                                "🟢 ONLINE"
                            else
                                "🔴 OFFLINE"
                    )
                }

                Switch(

                    checked = isOnline,

                    onCheckedChange = {

                        isOnline = it

                        val restaurantRef =

                            db.collection("restaurants")
                                .document(
                                    RestaurantSession.restaurantId
                                )

                        if (it) {

                            restaurantRef.update(

                                mapOf(

                                    "online" to true,

                                    "temporaryClosed" to false,

                                    "liveStatus" to "OPEN",

                                    "closeReason" to ""

                                )

                            )

                        } else {

                            restaurantRef.update(

                                mapOf(

                                    "online" to false,

                                    "temporaryClosed" to true,

                                    "liveStatus" to "CLOSED",

                                    "closeReason" to "Restaurant Offline"

                                )

                            )

                        }
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Card(

            onClick = {
                onTimingClick()
            },

            modifier = Modifier.fillMaxWidth()

        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "⏰ Restaurant Timing"
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "Open / Close / Weekly Timing"
                )
            }
        }
        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(

            onClick = {

                RestaurantSession.restaurantId = ""

                RestaurantSession.restaurantName = ""

                val prefs =
                    context.getSharedPreferences(
                        "restaurant_session",
                        android.content.Context.MODE_PRIVATE
                    )

                prefs.edit()
                    .clear()
                    .apply()

                onLogout()

            },

            modifier = Modifier.fillMaxWidth(),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )

        ) {

            Text("🚪 Logout")
        }
    }
}