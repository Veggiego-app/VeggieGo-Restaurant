package com.veggiego.restaurant

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "restaurant_session",
            MODE_PRIVATE
        )

        RestaurantSession.restaurantId =
            prefs.getString("restaurantId", "") ?: ""

        RestaurantSession.restaurantName =
            prefs.getString("restaurantName", "") ?: ""

        if (RestaurantSession.restaurantId.isNotBlank()) {
            saveRestaurantFcmToken(RestaurantSession.restaurantId)
        }

        val openOrders = intent.getBooleanExtra(
            "openOrders",
            false
        )

        setContent {
            var showLogin by remember {
                mutableStateOf(RestaurantSession.restaurantId.isEmpty())
            }

            var permissionsCompleted by remember {
                mutableStateOf(false)
            }

            when {
                showLogin -> {
                    LoginScreen(
                        onLoginSuccess = { phone ->
                            val cleanPhone = phone
                                .filter { it.isDigit() }
                                .takeLast(10)

                            FirebaseFirestore.getInstance()
                                .collection("restaurants")
                                .whereIn(
                                    "restaurantPhone",
                                    listOf(cleanPhone, "+91$cleanPhone")
                                )
                                .limit(1)
                                .get()
                                .addOnSuccessListener { result ->
                                    if (result.isEmpty) {
                                        FirebaseAuth.getInstance().signOut()
                                        Toast.makeText(
                                            this,
                                            "This mobile number is not linked with any restaurant",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@addOnSuccessListener
                                    }

                                    val doc = result.documents.first()
                                    val restaurantId = doc.id
                                    val restaurantName = doc.getString("name") ?: ""

                                    RestaurantSession.restaurantId = restaurantId
                                    RestaurantSession.restaurantName = restaurantName

                                    prefs.edit()
                                        .putString("restaurantId", restaurantId)
                                        .putString("restaurantName", restaurantName)
                                        .apply()

                                    saveRestaurantFcmToken(restaurantId)

                                    permissionsCompleted = false
                                    showLogin = false
                                }
                                .addOnFailureListener { error ->
                                    FirebaseAuth.getInstance().signOut()
                                    Toast.makeText(
                                        this,
                                        error.message ?: "Restaurant account check failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    )
                }

                !permissionsCompleted -> {
                    RequiredPermissionsScreen(
                        onAllRequiredPermissionsGranted = {
                            permissionsCompleted = true
                        }
                    )
                }

                openOrders -> {
                    OrdersScreen(initialTab = 0)
                }

                else -> {
                    BottomNavScreen(
                        onLogout = {
                            RestaurantSession.restaurantId = ""
                            RestaurantSession.restaurantName = ""

                            prefs.edit().clear().apply()
                            FirebaseAuth.getInstance().signOut()

                            permissionsCompleted = false
                            showLogin = true
                        }
                    )
                }
            }
        }
    }

    private fun saveRestaurantFcmToken(restaurantId: String) {
        if (restaurantId.isBlank()) {
            Log.e("FCM", "Restaurant ID is empty")
            return
        }

        FirebaseMessaging.getInstance()
            .token
            .addOnSuccessListener { token ->
                FirebaseFirestore.getInstance()
                    .collection("restaurants")
                    .document(restaurantId)
                    .set(
                        mapOf("fcmToken" to token),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        Log.d("FCM", "Restaurant FCM token saved: $restaurantId")
                    }
                    .addOnFailureListener { error ->
                        Log.e("FCM", "FCM token save failed", error)
                    }
            }
            .addOnFailureListener { error ->
                Log.e("FCM", "Unable to get FCM token", error)
            }
    }
}
