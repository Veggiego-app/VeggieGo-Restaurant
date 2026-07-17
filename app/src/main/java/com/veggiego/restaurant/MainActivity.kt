package com.veggiego.restaurant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        val prefs =
            getSharedPreferences(
                "restaurant_session",
                MODE_PRIVATE
            )

        RestaurantSession.restaurantId =
            prefs.getString(
                "restaurantId",
                ""
            ) ?: ""

        RestaurantSession.restaurantName =
            prefs.getString(
                "restaurantName",
                ""
            ) ?: ""
        com.google.firebase.messaging.FirebaseMessaging
            .getInstance()
            .token
            .addOnSuccessListener {

                android.util.Log.d(
                    "FCM",
                    it
                )
            }
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission
                        .POST_NOTIFICATIONS
                )

                != PackageManager.PERMISSION_GRANTED

            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission
                            .POST_NOTIFICATIONS
                    ),
                    1001
                )
            }
        }

        val openOrders =
            intent.getBooleanExtra(
                "openOrders",
                false
            )

        setContent {
            var showLogin by remember {
                mutableStateOf(
                    RestaurantSession.restaurantId.isEmpty()
                )
            }

            if (showLogin) {

                LoginScreen(

                    onLoginSuccess = { phone ->

                        val db =
                            com.google.firebase.firestore
                                .FirebaseFirestore
                                .getInstance()

                        db.collection("restaurants")
                            .whereEqualTo(
                                "phone",
                                phone
                            )
                            .get()
                            .addOnSuccessListener { result ->

                                if (!result.isEmpty) {

                                    val doc =
                                        result.documents.first()

                                    RestaurantSession.restaurantId =
                                        doc.id

                                    RestaurantSession.restaurantName =
                                        doc.getString("name")
                                            ?: ""

                                    prefs.edit()

                                        .putString(
                                            "restaurantId",
                                            doc.id
                                        )

                                        .putString(
                                            "restaurantName",
                                            doc.getString("name")
                                                ?: ""
                                        )

                                        .apply()

                                    showLogin = false
                                }
                            }
                    }
                )

            } else {

                if (openOrders) {

                    OrdersScreen(
                        initialTab = 0
                    )

                } else {

                    BottomNavScreen(

                        onLogout = {

                            showLogin = true
                        }
                    )
                }
            }
        }
    }
}