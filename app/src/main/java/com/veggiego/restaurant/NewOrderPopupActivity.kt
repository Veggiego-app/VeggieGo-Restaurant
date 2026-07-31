package com.veggiego.restaurant

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class NewOrderPopupActivity :
    ComponentActivity() {

    private var mediaPlayer: MediaPlayer? =
        null

    private var vibrator: Vibrator? =
        null

    private var currentOrderId: String =
        ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        currentOrderId =
            intent.getStringExtra(
                "orderId"
            ) ?: ""

        // SHOW ABOVE LOCK SCREEN

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager =
                getSystemService(
                    Context.KEYGUARD_SERVICE
                ) as KeyguardManager

            keyguardManager.requestDismissKeyguard(
                this,
                null
            )
        }

        startOrderSound()
        startOrderVibration()

        setContent {

            MaterialTheme {

                NewOrderPopupScreen(

                    orderId =
                        currentOrderId,

                    onCompleted = {

                        closePopup()
                    }
                )
            }
        }
    }

    private fun startOrderSound() {

        try {

            mediaPlayer =
                MediaPlayer.create(
                    this,
                    R.raw.new_order
                )

            mediaPlayer?.isLooping =
                true

            mediaPlayer?.start()

        } catch (
            error: Exception
        ) {

            error.printStackTrace()
        }
    }

    private fun startOrderVibration() {

        try {

            vibrator =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {

                    val manager =
                        getSystemService(
                            VibratorManager::class.java
                        )

                    manager.defaultVibrator

                } else {

                    @Suppress("DEPRECATION")
                    getSystemService(
                        Context.VIBRATOR_SERVICE
                    ) as Vibrator
                }

            if (vibrator?.hasVibrator() != true) {
                return
            }

            val pattern =
                longArrayOf(
                    0,
                    1000,
                    500
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        pattern,
                        0
                    )
                )

            } else {

                @Suppress("DEPRECATION")
                vibrator?.vibrate(
                    pattern,
                    0
                )
            }

        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    private fun stopOrderVibration() {

        try {
            vibrator?.cancel()
        } catch (ignored: Exception) {
        }

        vibrator = null
    }

    private fun stopOrderSound() {

        try {

            if (
                mediaPlayer?.isPlaying ==
                true
            ) {
                mediaPlayer?.stop()
            }

            mediaPlayer?.release()

        } catch (
            ignored: Exception
        ) {
        }

        mediaPlayer = null
    }

    private fun closePopup() {

        stopOrderSound()
        stopOrderVibration()

        if (
            currentOrderId.isNotBlank()
        ) {

            val notificationManager =
                getSystemService(
                    NotificationManager::class.java
                )

            notificationManager.cancel(
                currentOrderId.hashCode()
            )
        }

        finish()
    }

    override fun onDestroy() {

        stopOrderSound()
        stopOrderVibration()

        super.onDestroy()
    }
}

@Composable
private fun NewOrderPopupScreen(
    orderId: String,
    onCompleted: () -> Unit
) {

    val db =
        remember {
            FirebaseFirestore.getInstance()
        }

    var orderData by remember {
        mutableStateOf<Map<String, Any?>>(
            emptyMap()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var preparationMinutes by
    rememberSaveable {
        mutableIntStateOf(20)
    }

    var remainingSeconds by
    rememberSaveable {
        mutableIntStateOf(300)
    }

    var isUpdating by remember {
        mutableStateOf(false)
    }

    var showRejectDialog by remember {
        mutableStateOf(false)
    }

    var rejectReason by remember {
        mutableStateOf("")
    }

    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    val currentStatus =
        orderData["status"]
            ?.toString()
            ?.trim()
            ?.uppercase()
            ?: ""

    val canAcceptOrReject =
        currentStatus == "NEW" ||
                currentStatus == "APPROVED" ||
                currentStatus == "PENDING" ||
                currentStatus == "RESTAURANT_PENDING"

    LaunchedEffect(
        isLoading,
        currentStatus
    ) {
        if (
            !isLoading &&
            currentStatus.isNotBlank() &&
            !canAcceptOrReject
        ) {
            Toast.makeText(
                context,
                "Order status already updated: $currentStatus",
                Toast.LENGTH_LONG
            ).show()

            delay(900)
            onCompleted()
        }
    }

    // LOAD ORDER FROM FIRESTORE

    DisposableEffect(orderId) {

        if (orderId.isBlank()) {

            isLoading = false

            errorMessage =
                "Order ID missing"

            onDispose { }

        } else {

            val listener =
                db.collection("orders")
                    .document(orderId)
                    .addSnapshotListener {
                            document,
                            error ->

                        if (error != null) {

                            isLoading = false

                            errorMessage =
                                error.message
                                    ?: "Order loading failed"

                            return@addSnapshotListener
                        }

                        if (
                            document == null ||
                            !document.exists()
                        ) {

                            isLoading = false

                            errorMessage =
                                "Order not found"

                            return@addSnapshotListener
                        }

                        orderData =
                            document.data
                                ?: emptyMap()

                        isLoading = false
                    }

            onDispose {

                listener.remove()
            }
        }
    }

    // ACCEPT COUNTDOWN

    LaunchedEffect(Unit) {

        while (
            remainingSeconds > 0
        ) {

            delay(1000)

            remainingSeconds--
        }
    }

    val countdownText =

        String.format(
            "%02d:%02d",
            remainingSeconds / 60,
            remainingSeconds % 60
        )

    val customerName =
        orderData["customerName"]
            ?.toString()
            ?: "Customer"

    val customerPhone =
        orderData["customerPhone"]
            ?.toString()
            ?: ""

    val area =
        orderData["area"]
            ?.toString()
            ?: ""

    val city =
        orderData["city"]
            ?.toString()
            ?: ""

    val paymentMethod =
        orderData["paymentMethod"]
            ?.toString()
            ?: "COD"

    val packagingFee =
        (
                orderData["packagingFee"]
                        as? Number
                )?.toInt()
            ?: 0

    val discount =
        (
                orderData["discount"]
                        as? Number
                )?.toInt()
            ?: 0

    @Suppress("UNCHECKED_CAST")
    val orderItems =
        orderData["items"]
                as? List<Map<String, Any>>
            ?: emptyList()

    val itemsTotal =
        orderItems.sumOf { item ->

            (
                    item["itemTotal"]
                            as? Number
                    )?.toInt()
                ?: 0
        }

    val restaurantAmount =
        itemsTotal +
                packagingFee -
                discount

    Surface(

        modifier =
            Modifier.fillMaxSize(),

        color =
            Color(0xFF121212)

    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
        ) {

            // TOP BAR

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF1B8F3A)
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 18.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Text(
                    text = "🍔 New Order",
                    color = Color.White,
                    style =
                        MaterialTheme.typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = countdownText,
                    color = Color.White,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            when {

                isLoading -> {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }
                }

                errorMessage.isNotBlank() -> {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = errorMessage,
                            color = Color.White
                        )
                    }
                }

                else -> {

                    LazyColumn(

                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),

                        contentPadding =
                            PaddingValues(16.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )

                    ) {

                        item {

                            Text(
                                text =
                                    "Order #$orderId",
                                color =
                                    Color.White,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    customerName,
                                color =
                                    Color(0xFF80CBC4)
                            )

                            if (
                                customerPhone
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        customerPhone,
                                    color =
                                        Color.LightGray
                                )
                            }

                            Text(
                                text =
                                    "$area, $city",
                                color =
                                    Color.LightGray
                            )
                        }

                        item {

                            HorizontalDivider(
                                color =
                                    Color.DarkGray
                            )
                        }

                        items(
                            orderItems
                        ) { item ->

                            val name =
                                item["name"]
                                    ?.toString()
                                    ?: "Item"

                            val quantity =
                                (
                                        item["quantity"]
                                                as? Number
                                        )?.toInt()
                                    ?: 1

                            val variant =
                                item["variant"]
                                    ?.toString()
                                    ?: ""

                            val itemAmount =
                                (
                                        item["itemTotal"]
                                                as? Number
                                        )?.toInt()
                                    ?: 0

                            Card(
                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                Color(
                                                    0xFF1E1E1E
                                                )
                                        )
                            ) {

                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                ) {

                                    Row(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement
                                                .SpaceBetween

                                    ) {

                                        Text(
                                            text =
                                                "$quantity × $name",
                                            color =
                                                Color.White,
                                            modifier =
                                                Modifier
                                                    .weight(1f),
                                            fontWeight =
                                                FontWeight
                                                    .SemiBold
                                        )

                                        Text(
                                            text =
                                                "₹$itemAmount",
                                            color =
                                                Color.White,
                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }

                                    if (
                                        variant.isNotBlank()
                                    ) {

                                        Text(
                                            text =
                                                "Variant: $variant",
                                            color =
                                                Color(
                                                    0xFF81C784
                                                )
                                        )
                                    }

                                    @Suppress(
                                        "UNCHECKED_CAST"
                                    )
                                    val addons =
                                        item["addons"]
                                                as? List<
                                                Map<
                                                        String,
                                                        Any
                                                        >
                                                >
                                            ?: emptyList()

                                    addons.forEach {
                                            addon ->

                                        Text(
                                            text =
                                                "+ ${
                                                    addon["name"]
                                                        ?.toString()
                                                        ?: ""
                                                }",
                                            color =
                                                Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        item {

                            HorizontalDivider(
                                color =
                                    Color.DarkGray
                            )

                            SummaryRow(
                                title =
                                    "Items Total",
                                amount =
                                    itemsTotal
                            )

                            if (
                                packagingFee > 0
                            ) {

                                SummaryRow(
                                    title =
                                        "Packaging",
                                    amount =
                                        packagingFee
                                )
                            }

                            if (
                                discount > 0
                            ) {

                                SummaryRow(
                                    title =
                                        "Discount",
                                    amount =
                                        -discount
                                )
                            }

                            SummaryRow(
                                title =
                                    "Restaurant Total",
                                amount =
                                    restaurantAmount,
                                bold =
                                    true
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    "Payment: $paymentMethod",
                                color =
                                    Color.White,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    // PREPARATION TIMER

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF1A1A1A)
                                )
                                .padding(16.dp)
                    ) {

                        Text(
                            text =
                                "Set food preparation time",
                            color =
                                Color.White
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            OutlinedButton(

                                onClick = {

                                    if (
                                        preparationMinutes > 5
                                    ) {
                                        preparationMinutes -=
                                            5
                                    }
                                }

                            ) {

                                Text("−")
                            }

                            Text(
                                text =
                                    "$preparationMinutes mins",
                                color =
                                    Color.White,
                                modifier =
                                    Modifier.weight(1f),
                                textAlign =
                                    androidx.compose.ui.text.style
                                        .TextAlign.Center,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            OutlinedButton(

                                onClick = {

                                    if (
                                        preparationMinutes < 60
                                    ) {
                                        preparationMinutes +=
                                            5
                                    }
                                }

                            ) {

                                Text("+")
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            OutlinedButton(

                                onClick = {

                                    showRejectDialog =
                                        true
                                },

                                modifier =
                                    Modifier.weight(1f),

                                enabled =
                                    !isUpdating && canAcceptOrReject,

                                colors =
                                    ButtonDefaults
                                        .outlinedButtonColors(
                                            contentColor =
                                                Color(
                                                    0xFFEF5350
                                                )
                                        )

                            ) {

                                Text("REJECT")
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(12.dp)
                            )

                            Button(

                                onClick = {

                                    isUpdating =
                                        true

                                    val currentTime =
                                        System
                                            .currentTimeMillis()

                                    val orderRef =
                                        db.collection("orders")
                                            .document(orderId)

                                    db.runTransaction { transaction ->

                                        val snapshot =
                                            transaction.get(orderRef)

                                        val latestStatus =
                                            snapshot.getString("status")
                                                ?.trim()
                                                ?.uppercase()
                                                ?: ""

                                        val actionAllowed =
                                            latestStatus == "NEW" ||
                                                    latestStatus == "APPROVED" ||
                                                    latestStatus == "PENDING" ||
                                                    latestStatus == "RESTAURANT_PENDING"

                                        if (!actionAllowed) {
                                            throw IllegalStateException(
                                                "ORDER_ALREADY_UPDATED:$latestStatus"
                                            )
                                        }

                                        transaction.update(
                                            orderRef,
                                            mapOf(
                                                "status" to "PREPARING",
                                                "preparationMinutes" to preparationMinutes,
                                                "acceptedAt" to currentTime,
                                                "statusChangedBy" to "RESTAURANT",
                                                "updatedAt" to currentTime
                                            )
                                        )
                                    }
                                        .addOnSuccessListener {

                                            Toast.makeText(
                                                context,
                                                "Order accepted",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            onCompleted()
                                        }
                                        .addOnFailureListener { error ->

                                            isUpdating = false

                                            if (
                                                error.message
                                                    ?.startsWith(
                                                        "ORDER_ALREADY_UPDATED"
                                                    ) == true
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    "Order status has already been updated",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onCompleted()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    error.message
                                                        ?: "Accept failed",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                },

                                modifier =
                                    Modifier.weight(1.6f),

                                enabled =
                                    !isUpdating && canAcceptOrReject,

                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                Color(
                                                    0xFF1B8F3A
                                                )
                                        )

                            ) {

                                if (isUpdating) {

                                    CircularProgressIndicator(
                                        modifier =
                                            Modifier.size(
                                                20.dp
                                            ),
                                        color =
                                            Color.White
                                    )

                                } else {

                                    Text(
                                        text =
                                            "ACCEPT ($countdownText)"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRejectDialog) {

        AlertDialog(

            onDismissRequest = {

                showRejectDialog =
                    false
            },

            title = {

                Text("Reject Order")
            },

            text = {

                Column(
                    modifier =
                        Modifier.verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    listOf(
                        "Restaurant is busy",
                        "Item unavailable",
                        "Restaurant closing",
                        "Other"
                    ).forEach {
                            reason ->

                        TextButton(

                            onClick = {

                                rejectReason =
                                    if (
                                        reason == "Other"
                                    ) {
                                        ""
                                    } else {
                                        reason
                                    }
                            }

                        ) {

                            Text(reason)
                        }
                    }

                    OutlinedTextField(
                        value =
                            rejectReason,
                        onValueChange = {
                            rejectReason = it
                        },
                        label = {
                            Text("Reason")
                        }
                    )
                }
            },

            confirmButton = {

                Button(

                    onClick = {

                        if (
                            rejectReason.trim()
                                .length < 3
                        ) {

                            Toast.makeText(
                                context,
                                "Enter rejection reason",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@Button
                        }

                        isUpdating =
                            true

                        val currentTime =
                            System
                                .currentTimeMillis()

                        val orderRef =
                            db.collection("orders")
                                .document(orderId)

                        db.runTransaction { transaction ->

                            val snapshot =
                                transaction.get(orderRef)

                            val latestStatus =
                                snapshot.getString("status")
                                    ?.trim()
                                    ?.uppercase()
                                    ?: ""

                            val actionAllowed =
                                latestStatus == "NEW" ||
                                        latestStatus == "APPROVED" ||
                                        latestStatus == "PENDING" ||
                                        latestStatus == "RESTAURANT_PENDING"

                            if (!actionAllowed) {
                                throw IllegalStateException(
                                    "ORDER_ALREADY_UPDATED:$latestStatus"
                                )
                            }

                            transaction.update(
                                orderRef,
                                mapOf(
                                    "status" to "CANCELLED",
                                    "cancelReason" to rejectReason.trim(),
                                    "cancelledBy" to "RESTAURANT",
                                    "cancelledAt" to currentTime,
                                    "statusChangedBy" to "RESTAURANT",
                                    "updatedAt" to currentTime
                                )
                            )
                        }
                            .addOnSuccessListener {

                                Toast.makeText(
                                    context,
                                    "Order rejected",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onCompleted()
                            }
                            .addOnFailureListener { error ->

                                isUpdating = false

                                if (
                                    error.message
                                        ?.startsWith(
                                            "ORDER_ALREADY_UPDATED"
                                        ) == true
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Order status has already been updated",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onCompleted()
                                } else {
                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Reject failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    },

                    enabled =
                        !isUpdating && canAcceptOrReject

                ) {

                    Text("REJECT ORDER")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        showRejectDialog =
                            false
                    }

                ) {

                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(
    title: String,
    amount: Int,
    bold: Boolean = false
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween

    ) {

        Text(
            text = title,
            color = Color.White,
            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )

        Text(
            text =
                if (amount < 0) {
                    "-₹${-amount}"
                } else {
                    "₹$amount"
                },
            color = Color.White,
            fontWeight =
                if (bold) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}