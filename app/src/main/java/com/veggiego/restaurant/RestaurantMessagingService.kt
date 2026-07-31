package com.veggiego.restaurant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RestaurantMessagingService :
    FirebaseMessagingService() {

    override fun onNewToken(
        token: String
    ) {
        super.onNewToken(token)

        val prefs =
            getSharedPreferences(
                "restaurant_session",
                MODE_PRIVATE
            )

        val restaurantId =
            RestaurantSession.restaurantId
                .ifBlank {
                    prefs.getString(
                        "restaurantId",
                        ""
                    ) ?: ""
                }

        Log.d(
            "FCM",
            "New FCM token received"
        )

        if (restaurantId.isBlank()) {

            Log.d(
                "FCM",
                "Restaurant ID empty. Token will save after login."
            )

            return
        }

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
                    "Token saved for restaurant: $restaurantId"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "FCM",
                    "Token save failed",
                    error
                )
            }
    }

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {
        super.onMessageReceived(
            remoteMessage
        )

        Log.d(
            "FCM_POPUP",
            "New restaurant message received"
        )

        Log.d(
            "FCM_POPUP",
            "Message data: ${remoteMessage.data}"
        )

        val title =
            remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: "🍔 New Order Received"

        val body =
            remoteMessage.data["body"]
                ?: remoteMessage.notification?.body
                ?: "You received a new order"

        val orderId =
            remoteMessage.data["orderId"]
                ?: ""

        if (orderId.isBlank()) {

            Log.e(
                "FCM_POPUP",
                "Order ID missing in notification"
            )
        }

        showNewOrderNotification(
            title = title,
            body = body,
            orderId = orderId
        )
    }

    private fun showNewOrderNotification(
        title: String,
        body: String,
        orderId: String
    ) {

        /*
         * New channel ID is important because Android
         * caches old notification-channel settings.
         */
        val channelId =
            "restaurant_orders_fullscreen_v14"

        val notificationManager =
            getSystemService(
                NotificationManager::class.java
            )

        val soundUri: Uri =
            Uri.parse(
                "android.resource://$packageName/raw/new_order"
            )

        val vibrationPattern =
            longArrayOf(
                0,
                1000,
                500,
                1000,
                500,
                1000
            )

        /*
         * This Activity will open as the full-screen popup.
         */
        val popupIntent =
            Intent(
                this,
                NewOrderPopupActivity::class.java
            ).apply {

                putExtra(
                    "orderId",
                    orderId
                )

                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

        val requestCode =
            if (orderId.isNotBlank()) {

                orderId.hashCode()

            } else {

                System.currentTimeMillis()
                    .toInt()
            }

        val popupPendingIntent =
            PendingIntent.getActivity(
                this,
                requestCode,
                popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        /*
         * Android 8+ notification channel.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val audioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(
                        AudioAttributes
                            .USAGE_NOTIFICATION_RINGTONE
                    )
                    .setContentType(
                        AudioAttributes
                            .CONTENT_TYPE_SONIFICATION
                    )
                    .build()

            val channel =
                NotificationChannel(
                    channelId,
                    "Restaurant New Orders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Full-screen alerts for restaurant orders"

                    enableVibration(
                        true
                    )

                    this.vibrationPattern =
                        vibrationPattern

                    setSound(
                        soundUri,
                        audioAttributes
                    )

                    lockscreenVisibility =
                        NotificationCompat
                            .VISIBILITY_PUBLIC

                    setBypassDnd(
                        true
                    )
                }

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }

        val notificationId =
            if (orderId.isNotBlank()) {

                orderId.hashCode()

            } else {

                System.currentTimeMillis()
                    .toInt()
            }

        val notification =
            NotificationCompat
                .Builder(
                    this,
                    channelId
                )
                .setSmallIcon(
                    android.R.drawable
                        .ic_dialog_info
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    body
                )
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(body)
                )
                .setPriority(
                    NotificationCompat
                        .PRIORITY_MAX
                )
                .setCategory(
                    NotificationCompat
                        .CATEGORY_CALL
                )
                .setVisibility(
                    NotificationCompat
                        .VISIBILITY_PUBLIC
                )
                .setSound(
                    soundUri
                )
                .setVibrate(
                    vibrationPattern
                )
                .setOngoing(
                    true
                )
                .setAutoCancel(
                    false
                )

                /*
                 * Normal notification tap also opens popup.
                 */
                .setContentIntent(
                    popupPendingIntent
                )

                /*
                 * This asks Android to open the popup
                 * automatically above the lock screen.
                 */
                .setFullScreenIntent(
                    popupPendingIntent,
                    true
                )
                .build()

        notificationManager.notify(
            notificationId,
            notification
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {

            Log.d(
                "FCM_POPUP",
                "Full-screen permission: ${
                    notificationManager
                        .canUseFullScreenIntent()
                }"
            )
        }

        Log.d(
            "FCM_POPUP",
            "Full-screen notification displayed for: $orderId"
        )
    }
}