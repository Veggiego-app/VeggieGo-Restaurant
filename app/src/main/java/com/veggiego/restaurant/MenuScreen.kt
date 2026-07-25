package com.veggiego.restaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton


@Composable
fun MenuScreen() {

    val db = FirebaseFirestore.getInstance()

    var expandedItemId by remember {
        mutableStateOf("")
    }

    var items by remember {
        mutableStateOf<List<MenuItemModel>>(emptyList())
    }
    var selectedItem by remember {
        mutableStateOf<MenuItemModel?>(null)
    }

    var showTimingDialog by remember {
        mutableStateOf(false)
    }
    var searchQuery by remember {
        mutableStateOf("")
    }
    var categories by remember {
        mutableStateOf<List<CategoryModel>>(emptyList())
    }

    var subCategories by remember {
        mutableStateOf<List<SubCategoryModel>>(emptyList())
    }

    var selectedCategoryId by remember {
        mutableStateOf("")
    }
    var expandedCategoryId by remember {
        mutableStateOf("")
    }

    var expandedSubCategoryId by remember {
        mutableStateOf("")
    }
    var selectedCategory by remember {
        mutableStateOf<CategoryModel?>(null)
    }

    var showCategoryTimingDialog by remember {
        mutableStateOf(false)
    }
    fun updateCategoryVisibility(

        category: CategoryModel,

        visible: Boolean

    ) {
        RestaurantRepository()

            .restaurantCategories()

            .document(category.id)

            .update(

                "visible",

                visible

            )
        RestaurantRepository()

            .restaurantSubCategories()

            .whereEqualTo(

                "categoryId",

                category.id

            )

            .get()

            .addOnSuccessListener { result ->

                result.documents.forEach { doc ->

                    doc.reference.update(

                        "visible",

                        visible

                    )

                }

            }
        RestaurantRepository()

            .restaurantMenu()

            .whereEqualTo(

                "categoryId",

                category.id

            )

            .get()

            .addOnSuccessListener { result ->

                result.documents.forEach { doc ->

                    val manualHidden =
                        doc.getBoolean("manualHidden") ?: false

                    val itemVisible =

                        if (visible) {

                            !manualHidden

                        } else {

                            false

                        }

                    doc.reference.update(

                        "visible",

                        itemVisible

                    )

                }

            }


    }
    fun updateCategoryStock(

        category: CategoryModel,

        stockEnabled: Boolean

    ) {

        RestaurantRepository()

            .restaurantCategories()
            .document(category.id)
            .update(

                mapOf(

                    "stockEnabled" to stockEnabled,

                    "available" to stockEnabled

                )

            )


        RestaurantRepository()

            .restaurantSubCategories()

            .whereEqualTo(

                "categoryId",

                category.id

            )

            .get()

            .addOnSuccessListener { result ->

                result.documents.forEach { doc ->

                    doc.reference.update(

                        mapOf(

                            "stockEnabled" to stockEnabled,

                            "available" to stockEnabled

                        )

                    )

                }

            }
        RestaurantRepository()

            .restaurantMenu()

            .whereEqualTo(

                "categoryId",

                category.id

            )

            .get()

            .addOnSuccessListener { result ->

                result.documents.forEach { doc ->

                    val manualOutOfStock =
                        doc.getBoolean("manualOutOfStock") ?: false

                    val itemAvailable =

                        if (stockEnabled) {

                            !manualOutOfStock

                        } else {

                            false

                        }

                    doc.reference.update(

                        "available",

                        itemAvailable

                    )

                }

            }

    }
    fun updateSubCategoryStock(

        subCategory: SubCategoryModel,

        stockEnabled: Boolean

    ) {

        RestaurantRepository()

            .restaurantSubCategories()

            .document(subCategory.id)

            .update(

                mapOf(

                    "stockEnabled" to stockEnabled,

                    "available" to stockEnabled

                )

            )
        if (stockEnabled) {

            RestaurantRepository()

                .restaurantCategories()

                .document(subCategory.categoryId)

                .update(

                    mapOf(

                        "stockEnabled" to true,

                        "available" to true

                    )

                )

        }

        RestaurantRepository()

            .restaurantMenu()

            .whereEqualTo(

                "subCategoryId",

                subCategory.id

            )

            .get()

            .addOnSuccessListener { result ->

                result.documents.forEach { doc ->

                    val manualOutOfStock =
                        doc.getBoolean("manualOutOfStock") ?: false

                    val itemAvailable =

                        if (stockEnabled) {

                            !manualOutOfStock

                        } else {

                            false

                        }

                    doc.reference.update(

                        "available",

                        itemAvailable

                    )

                }

            }

    }
    fun updateSubCategoryVisibility(

        subCategory: SubCategoryModel,

        visible: Boolean

    ) {
        RestaurantRepository()

            .restaurantSubCategories()

            .document(subCategory.id)

            .update(

                "visible",

                visible

            )
        if (visible) {

            RestaurantRepository()

                .restaurantCategories()

                .document(subCategory.categoryId)

                .update(

                    "visible",

                    true

                )

        }

        RestaurantRepository()

            .restaurantMenu()

            .whereEqualTo(

                "subCategoryId",

                subCategory.id

            )

            .get()

            .addOnSuccessListener { result ->

                result.documents.forEach { doc ->

                    val manualHidden =
                        doc.getBoolean("manualHidden") ?: false

                    val itemVisible =

                        if (visible) {

                            !manualHidden

                        } else {

                            false

                        }

                    doc.reference.update(

                        "visible",

                        itemVisible

                    )

                }

            }

    }
    LaunchedEffect(Unit) {

        RestaurantRepository()

            .restaurantMenu()

            .addSnapshotListener { value, _ ->

                if (value == null) return@addSnapshotListener

                println("MENU DOCS = ${value.documents.size}")

                value.documents.forEach {
                    println(it.data)
                }

                items = value.documents.mapNotNull {

                    val item =
                        it.toObject(MenuItemModel::class.java)

                    item?.id = it.id

                    item
                }
                RestaurantRepository()

                    .restaurantCategories()

                    .addSnapshotListener { value, _ ->

                        if (value == null)
                            return@addSnapshotListener

                        categories = value.documents.map {

                            CategoryModel(

                                id = it.id,

                                name =
                                    it.getString("name") ?: "",

                                stockEnabled =
                                    it.getBoolean("stockEnabled") ?: true,

                                visible =
                                    it.getBoolean("visible") ?: true,

                                timeSlots =
                                    it.get("timeSlots")
                                            as? List<Map<String, String>>
                                        ?: emptyList()
                            )
                        }
                    }

                RestaurantRepository()

                    .restaurantSubCategories()

                    .addSnapshotListener { value, _ ->

                        if (value == null)
                            return@addSnapshotListener

                        subCategories =
                            value.documents.map {

                                SubCategoryModel(

                                    id = it.id,

                                    name =
                                        it.getString("name")
                                            ?: "",

                                    categoryId =
                                        it.getString("categoryId")
                                            ?: "",

                                    stockEnabled =
                                        it.getBoolean("stockEnabled") ?: true,

                                    visible =
                                        it.getBoolean("visible") ?: true
                                )
                            }
                    }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFFFCC80))
    ) {

        OutlinedTextField(

            value = searchQuery,

            onValueChange = {
                searchQuery = it
            },

            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),

            label = {
                Text("Search Item")
            },

            trailingIcon = {

                if (searchQuery.isNotEmpty()) {

                    IconButton(

                        onClick = {

                            searchQuery = ""

                        }

                    ) {

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Search"
                        )
                    }
                }
            }
        )
        if (searchQuery.isBlank()) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {

                items(categories) { category ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {

                        Column {

                            TextButton(

                                onClick = {

                                    expandedCategoryId =
                                        if (expandedCategoryId == category.id)
                                            ""
                                        else
                                            category.id
                                }

                            ) {

                                Text(

                                    "${
                                        if (expandedCategoryId == category.id)
                                            "▼"
                                        else
                                            "▶"
                                    }  CATEGORY : ${category.name} (${
                                        items.count {
                                            it.categoryId == category.id
                                        }
                                    })"

                                )
                            }
                            if (
                                expandedCategoryId == category.id
                            ) {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 12.dp,
                                            end = 12.dp,
                                            bottom = 8.dp
                                        ),

                                    horizontalArrangement =
                                        Arrangement.spacedBy(3.dp)
                                ) {

                                    Button(
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(34.dp),
                                        colors = ButtonDefaults.buttonColors(

                                            containerColor =

                                                if (category.stockEnabled)

                                                    Color(0xFF22C55E)
                                                else

                                                    Color(0xFFE53935)
                                        ),

                                        onClick = {

                                            updateCategoryStock(

                                                category,

                                                !category.stockEnabled

                                            )

                                        }

                                    ) {

                                        Text(
                                            if (category.stockEnabled)
                                                "IN STOCK"
                                            else
                                                "OUT OF STOCK",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip
                                        )
                                    }

                                    Button(

                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),

                                        colors = ButtonDefaults.buttonColors(

                                            containerColor =

                                                if (category.visible)

                                                    Color(0xFF22C55E)

                                                else

                                                    Color(0xFFE53935)

                                        ),

                                        onClick = {

                                            updateCategoryVisibility(

                                                category,

                                                !category.visible

                                            )

                                        }

                                    ) {

                                        Text(

                                            if (category.visible)

                                                "SHOW"

                                            else

                                                "HIDE",

                                            style = MaterialTheme.typography.labelSmall

                                        )

                                    }
                                }
                            }

                            if (
                                expandedCategoryId == category.id
                            ) {

                                subCategories

                                    .filter {

                                        it.categoryId ==
                                                category.id
                                    }

                                    .forEach { subCategory ->

                                        TextButton(

                                            onClick = {

                                                expandedSubCategoryId =
                                                    if (
                                                        expandedSubCategoryId ==
                                                        subCategory.id
                                                    ) ""
                                                    else
                                                        subCategory.id
                                            }

                                        ) {

                                            Text(

                                                "${
                                                    if (
                                                        expandedSubCategoryId ==
                                                        subCategory.id
                                                    )
                                                        "▼"
                                                    else
                                                        "▶"
                                                }  SUB CATEGORY : ${subCategory.name} (${
                                                    items.count {
                                                        it.subCategoryId ==
                                                                subCategory.id
                                                    }
                                                })"

                                            )
                                        }
                                        if (
                                            expandedSubCategoryId ==
                                            subCategory.id
                                        ) {

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 24.dp,
                                                        end = 12.dp,
                                                        bottom = 8.dp
                                                    ),

                                                horizontalArrangement =
                                                    Arrangement.spacedBy(3.dp)
                                            ) {

                                                Button(

                                                    modifier = Modifier
                                                        .weight(1.4f)
                                                        .height(34.dp),

                                                    colors = ButtonDefaults.buttonColors(

                                                        containerColor =

                                                            if (subCategory.stockEnabled)

                                                                Color(0xFF22C55E)
                                                            else

                                                                Color(0xFFE53935)
                                                    ),

                                                    onClick = {

                                                        updateSubCategoryStock(

                                                            subCategory,

                                                            !subCategory.stockEnabled

                                                        )

                                                    }

                                                ) {

                                                    Text(

                                                        if (subCategory.stockEnabled)
                                                            "IN STOCK"
                                                        else
                                                            "OUT OF STOCK",

                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                    )
                                                }

                                                Button(

                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp),

                                                    colors = ButtonDefaults.buttonColors(

                                                        containerColor =

                                                            if (subCategory.visible)

                                                                Color(0xFF22C55E)

                                                            else

                                                                Color(0xFFE53935)

                                                    ),

                                                    onClick = {

                                                        updateSubCategoryVisibility(

                                                            subCategory,

                                                            !subCategory.visible

                                                        )

                                                    }

                                                ) {

                                                    Text(

                                                        if (subCategory.visible)

                                                            "SHOW"

                                                        else

                                                            "HIDE",

                                                        style = MaterialTheme.typography.labelSmall

                                                    )

                                                }
                                            }
                                        }

                                        if (

                                            expandedSubCategoryId ==
                                            subCategory.id

                                        ) {

                                            items
                                                .filter {
                                                    it.subCategoryId == subCategory.id
                                                }
                                                .forEachIndexed { index, item ->

                                                    TextButton(

                                                        onClick = {

                                                            expandedItemId =
                                                                if (
                                                                    expandedItemId ==
                                                                    item.id
                                                                )
                                                                    ""
                                                                else
                                                                    item.id
                                                        }

                                                    ) {

                                                        Text(
                                                            text = "${index + 1}. ${item.name}"
                                                        )
                                                    }
                                                    if (

                                                        expandedItemId ==
                                                        item.id

                                                    ) {

                                                        Card(

                                                            modifier = Modifier

                                                                .fillMaxWidth()

                                                                .padding(

                                                                    start = 24.dp,

                                                                    end = 8.dp,

                                                                    bottom = 8.dp

                                                                )

                                                        ) {

                                                            Column(

                                                                modifier = Modifier

                                                                    .padding(12.dp)

                                                            ) {

                                                                Text(
                                                                    text = item.name,
                                                                    style = MaterialTheme.typography.titleMedium
                                                                )

                                                                Text(
                                                                    text = "₹${item.price}",
                                                                    color = Color(0xFF22C55E),
                                                                    style = MaterialTheme.typography.titleSmall
                                                                )

                                                                Text(
                                                                    text = "🍔 Category : ${item.categoryName}",
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )

                                                                Text(
                                                                    text = "📂 Sub Category : ${item.subCategoryName}",
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                                if (item.variants.isNotEmpty()) {

                                                                    Spacer(
                                                                        modifier = Modifier.height(8.dp)
                                                                    )

                                                                    Text(
                                                                        text = "Variants",
                                                                        style = MaterialTheme.typography.titleSmall
                                                                    )

                                                                    Spacer(
                                                                        modifier = Modifier.height(4.dp)
                                                                    )

                                                                    item.variants.forEach { variant ->

                                                                        Text(
                                                                            text = "• ${variant.name}  ₹${variant.price}",
                                                                            style = MaterialTheme.typography.bodyMedium
                                                                        )
                                                                    }
                                                                }
                                                                Spacer(
                                                                    modifier = Modifier.height(8.dp)
                                                                )

                                                                Row(

                                                                    modifier = Modifier.fillMaxWidth(),

                                                                    horizontalArrangement =
                                                                        Arrangement.spacedBy(8.dp)

                                                                ) {

                                                                    Button(

                                                                        modifier = Modifier.weight(1f),

                                                                        colors = ButtonDefaults.buttonColors(

                                                                            containerColor =

                                                                                if (item.available)

                                                                                    Color(0xFF22C55E)

                                                                                else

                                                                                    Color(0xFFE53935)

                                                                        ),

                                                                        onClick = {

                                                                            db.collection("restaurants")
                                                                                .document(
                                                                                    RestaurantSession.restaurantId
                                                                                )
                                                                                .collection("menu")
                                                                                .document(item.id)
                                                                                .update(

                                                                                    mapOf(

                                                                                        "available" to !item.available,

                                                                                        "manualOutOfStock" to item.available

                                                                                    )

                                                                                )
                                                                        }

                                                                    ) {

                                                                        Text(

                                                                            if (item.available)
                                                                                "IN STOCK"
                                                                            else
                                                                                "OUT OF STOCK"

                                                                        )
                                                                    }

                                                                    Button(

                                                                        modifier = Modifier.weight(1f),

                                                                        colors = ButtonDefaults.buttonColors(

                                                                            containerColor =

                                                                                if (item.visible)

                                                                                    Color(0xFF22C55E)

                                                                                else

                                                                                    Color(0xFFE53935)

                                                                        ),

                                                                        onClick = {

                                                                            db.collection("restaurants")
                                                                                .document(
                                                                                    RestaurantSession.restaurantId
                                                                                )
                                                                                .collection("menu")
                                                                                .document(item.id)
                                                                                .update(

                                                                                    mapOf(

                                                                                        "visible" to !item.visible,

                                                                                        "manualHidden" to item.visible

                                                                                    )

                                                                                )
                                                                        }

                                                                    ) {

                                                                        Text(

                                                                            if (item.visible)
                                                                                "SHOW ITEM"
                                                                            else
                                                                                "HIDE ITEM"

                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
            if (searchQuery.isNotBlank()) {

                LazyColumn(

                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(
                        bottom = 90.dp
                    )

                ) {

                    items(

                        items.filter {

                            it.name.contains(
                                searchQuery,
                                ignoreCase = true
                            )
                        }

                    ) { item ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),

                            onClick = {

                                expandedItemId =
                                    if (expandedItemId == item.id)
                                        ""
                                    else
                                        item.id
                            }

                        ) {

                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {

                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "₹${item.price}",
                                    color = Color(0xFF22C55E)
                                )

                                Text(
                                    text = "🍔 Category : ${item.categoryName}"
                                )

                                Text(
                                    text = "📂 Sub Category : ${item.subCategoryName}"
                                )
                                if (item.variants.isNotEmpty()) {

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    item.variants.forEach { variant ->

                                        Text(
                                            text = "• ${variant.name}  ₹${variant.price}"
                                        )
                                    }
                                }

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    Button(
                                        modifier = Modifier.weight(1f),

                                        colors = ButtonDefaults.buttonColors(

                                            containerColor =

                                                if (item.available)
                                                    Color(0xFF22C55E)
                                                else
                                                    Color.Red
                                        ),

                                        onClick = {

                                            db.collection("restaurants")
                                                .document(
                                                    RestaurantSession.restaurantId
                                                )
                                                .collection("menu")
                                                .document(item.id)
                                                .update(

                                                    mapOf(

                                                        "available" to !item.available,

                                                        "manualOutOfStock" to item.available

                                                    )

                                                )
                                        }

                                    ) {

                                        Text(
                                            if (item.available)
                                                "IN STOCK"
                                            else
                                                "OUT OF STOCK"
                                        )
                                    }

                                    Button(
                                        modifier = Modifier.weight(1f),

                                        colors = ButtonDefaults.buttonColors(

                                            containerColor =

                                                if (item.visible)
                                                    Color(0xFF22C55E)
                                                else
                                                    Color.Red
                                        ),

                                        onClick = {

                                            db.collection("restaurants")
                                                .document(
                                                    RestaurantSession.restaurantId
                                                )
                                                .collection("menu")
                                                .document(item.id)
                                                .update(

                                                    mapOf(

                                                        "visible" to !item.visible,

                                                        "manualHidden" to item.visible

                                                    )

                                                )
                                        }

                                    ) {

                                        Text(
                                            if (item.visible)
                                                "SHOW ITEM"
                                            else
                                                "HIDE ITEM"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (

                showTimingDialog
                &&
                selectedItem != null

            ) {

                ItemTimingDialog(

                    itemId = selectedItem!!.id,

                    onDismiss = {

                        showTimingDialog = false
                    }
                )
            }
            if (

                showCategoryTimingDialog
                &&
                selectedCategory != null

            ) {

                CategoryTimingDialog(

                    categoryId =
                        selectedCategory!!.id,

                    onDismiss = {

                        showCategoryTimingDialog = false
                    }
                )
            }
        }
    }