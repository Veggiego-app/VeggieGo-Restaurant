package com.veggiego.restaurant

import com.google.firebase.firestore.FirebaseFirestore

class RestaurantRepository {

    private val db =
        FirebaseFirestore.getInstance()

    fun restaurantOrders() =
        db.collection("orders")
            .whereEqualTo(
                "restaurantId",
                RestaurantSession.restaurantId
            )

    fun restaurantDocument() =
        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )

    fun restaurantMenu() =
        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )
            .collection("menu")

    fun restaurantCategories() =
        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )
            .collection("categories")

    fun restaurantSubCategories() =
        db.collection("restaurants")
            .document(
                RestaurantSession.restaurantId
            )
            .collection("subcategories")
}