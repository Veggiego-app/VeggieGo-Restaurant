package com.veggiego.restaurant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RestaurantMessagingService : FirebaseMessagingService() {
    override fun onNewToken(
        token: String
    ) {

        super.onNewToken(token)

        val restaurantId =
            RestaurantSession.restaurantId

        if (
            restaurantId.isNotBlank()
        ) {

            FirebaseFirestore
                .getInstance()
                .collection("restaurants")
                .document(
                    restaurantId
                )
                .set(
                    mapOf(
                        "fcmToken" to token
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {

                    Log.d(
                        "FCM",
                        "TOKEN SAVED"
                    )
                }
                .addOnFailureListener {

                    Log.e(
                        "FCM",
                        "SAVE FAILED",
                        it
                    )
                }
        }

        Log.d(
            "FCM",
            "Token = $token"
        )
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        Log.d(
            "FCM_TEST",
            "MESSAGE RECEIVED"
        )
        Log.d(
            "FCM_TEST",
            "STARTING POPUP"
        )

        super.onMessageReceived(
            remoteMessage
        )

        val title =
            remoteMessage.data["title"]
                ?: "🍔 New Order"

        val body =
            remoteMessage.data["body"]
                ?: "You received a new order"

        Log.d(
            "FCM_TEST",
            "POPUP START CALLED"
        )

        showNotification(
            title,
            body,
            remoteMessage.data["orderId"] ?: ""
        )
    }

    private fun showNotification(
        title: String,
        body: String,
        orderId: String
    ) {

        val channelId =
            "restaurant_orders_final_v12"

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )
        intent.putExtra(
            "openOrders",
            true
        )

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

                val soundUri =
            android.net.Uri.parse(
                "android.resource://$packageName/raw/new_order"
            )

        val builder =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    body
                )
                .setAutoCancel(true)
                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                    NotificationCompat.CATEGORY_MESSAGE
                )
                .setDefaults(
                    NotificationCompat.DEFAULT_LIGHTS
                )
                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )
                .setSound(soundUri)
                .setOngoing(true)

                .setVibrate(
                    longArrayOf(
                        0,
                        1000,
                        500,
                        1000,
                        500,
                        1000
                    )
                )
                .setContentIntent(
                    pendingIntent
                )

        val notificationManager =
            getSystemService(
                NotificationManager::class.java
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Restaurant Orders",
                    NotificationManager.IMPORTANCE_HIGH
                )
            channel.enableVibration(true)

            channel.vibrationPattern =
                longArrayOf(
                    0,
                    1000,
                    500,
                    1000,
                    500,
                    1000
                )

            channel.enableVibration(true)

            channel.setSound(
                soundUri,
                android.media.AudioAttributes
                    .Builder()
                    .setUsage(
                        android.media.AudioAttributes
                            .USAGE_NOTIFICATION_RINGTONE
                    )
                    .build()
            )

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }
}