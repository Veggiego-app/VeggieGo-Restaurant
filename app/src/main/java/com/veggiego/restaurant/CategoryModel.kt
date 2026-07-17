package com.veggiego.restaurant

data class CategoryModel(

    val id: String = "",

    val name: String = "",

    val stockEnabled: Boolean = true,

    val visible: Boolean = true,

    val timeSlots: List<Map<String, String>> = emptyList()

)