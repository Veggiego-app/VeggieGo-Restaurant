package com.veggiego.restaurant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

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

        // RESTORE RESTAURANT SESSION

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

        // SAVE TOKEN IF RESTAURANT IS ALREADY LOGGED IN

        if (
            RestaurantSession.restaurantId
                .isNotBlank()
        ) {
            saveRestaurantFcmToken(
                RestaurantSession.restaurantId
            )
        }

        // NOTIFICATION PERMISSION FOR ANDROID 13+

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
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
                    RestaurantSession.restaurantId
                        .isEmpty()
                )
            }

            if (showLogin) {

                LoginScreen(

                    onLoginSuccess = { phone ->

                        val db =
                            FirebaseFirestore
                                .getInstance()

                        val cleanPhone =
                            phone
                                .filter {
                                    it.isDigit()
                                }
                                .takeLast(10)

                        db.collection("restaurants")
                            .whereIn(
                                "restaurantPhone",
                                listOf(
                                    cleanPhone,
                                    "+91$cleanPhone"
                                )
                            )
                            .limit(1)
                            .get()
                            .addOnSuccessListener { result ->

                                if (!result.isEmpty) {

                                    val doc =
                                        result.documents
                                            .first()

                                    val restaurantId =
                                        doc.id

                                    val restaurantName =
                                        doc.getString("name")
                                            ?: ""

                                    // SAVE SESSION

                                    RestaurantSession.restaurantId =
                                        restaurantId

                                    RestaurantSession.restaurantName =
                                        restaurantName

                                    prefs.edit()
                                        .putString(
                                            "restaurantId",
                                            restaurantId
                                        )
                                        .putString(
                                            "restaurantName",
                                            restaurantName
                                        )
                                        .apply()

                                    // SAVE FCM TOKEN AFTER LOGIN

                                    saveRestaurantFcmToken(
                                        restaurantId
                                    )

                                    showLogin = false

                                } else {

                                    FirebaseAuth
                                        .getInstance()
                                        .signOut()

                                    Toast.makeText(
                                        this,
                                        "This mobile number is not linked with any restaurant",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener { error ->

                                FirebaseAuth
                                    .getInstance()
                                    .signOut()

                                Toast.makeText(
                                    this,
                                    error.message
                                        ?: "Restaurant account check failed",
                                    Toast.LENGTH_LONG
                                ).show()
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

                            RestaurantSession.restaurantId =
                                ""

                            RestaurantSession.restaurantName =
                                ""

                            prefs.edit()
                                .clear()
                                .apply()

                            showLogin = true
                        }
                    )
                }
            }
        }
    }

    private fun saveRestaurantFcmToken(
        restaurantId: String
    ) {

        if (restaurantId.isBlank()) {

            Log.e(
                "FCM",
                "Restaurant ID is empty"
            )

            return
        }

        FirebaseMessaging
            .getInstance()
            .token
            .addOnSuccessListener { token ->

                FirebaseFirestore
                    .getInstance()
                    .collection("restaurants")
                    .document(restaurantId)
                    .set(
                        mapOf(
                            "fcmToken" to token
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {

                        Log.d(
                            "FCM",
                            "Restaurant FCM token saved: $restaurantId"
                        )
                    }
                    .addOnFailureListener { error ->

                        Log.e(
                            "FCM",
                            "FCM token save failed",
                            error
                        )
                    }
            }
            .addOnFailureListener { error ->

                Log.e(
                    "FCM",
                    "Unable to get FCM token",
                    error
                )
            }
    }
}