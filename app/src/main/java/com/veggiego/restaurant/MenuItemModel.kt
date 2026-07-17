package com.veggiego.restaurant

data class VariantModel(

    val name: String = "",

    val price: Long = 0

)

data class MenuItemModel(

    var id: String = "",

    val restaurantId: String = "",

    val name: String = "",

    val description: String = "",

    val image: String = "",

    val price: Long = 0,

    val available: Boolean = true,

    val visible: Boolean = true,

// 👇 NEW
    val manualHidden: Boolean = false,

    val manualOutOfStock: Boolean = false,

    val startTime: String = "",

    val endTime: String = "",

    val categoryId: String = "",

    val categoryName: String = "",

    val subCategoryId: String = "",

    val subCategoryName: String = "",

    val variants: List<VariantModel> = emptyList(),

    val timeSlots:
    List<Map<String, String>> = emptyList()

)