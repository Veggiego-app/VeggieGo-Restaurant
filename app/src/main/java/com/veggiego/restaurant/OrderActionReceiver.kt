package com.veggiego.restaurant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class OrderActionReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val orderId =
            intent.getStringExtra(
                "orderId"
            ) ?: return

        val action =
            intent.getStringExtra(
                "action"
            ) ?: return

        val db =
            FirebaseFirestore.getInstance()

        when (action) {

            "ACCEPT" -> {

                db.collection("orders")
                    .document(orderId)
                    .update(
                        "status",
                        "PREPARING"
                    )
            }

            "REJECT" -> {

                db.collection("orders")
                    .document(orderId)
                    .update(
                        "status",
                        "CANCELLED"
                    )
            }
        }
    }
}