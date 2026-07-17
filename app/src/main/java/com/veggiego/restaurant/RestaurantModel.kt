package com.veggiego.restaurant

data class RestaurantModel(

    val restaurantId: String = "",

    val name: String = "",

    val ownerName: String = "",

    val phone: String = "",

    val online: Boolean = true,

    val openingTime: String = "",

    val closingTime: String = ""

)