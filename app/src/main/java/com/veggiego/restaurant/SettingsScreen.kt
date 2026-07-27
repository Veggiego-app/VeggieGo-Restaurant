package com.veggiego.restaurant

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SettingsScreen(

    modifier: Modifier = Modifier,

    onTimingClick: () -> Unit = {},

    onLogout: () -> Unit = {}

) {

    val context =
        LocalContext.current

    val restaurantId =
        RestaurantSession.restaurantId

    val restaurantRef =
        remember(restaurantId) {

            FirebaseFirestore
                .getInstance()
                .collection("restaurants")
                .document(restaurantId)
        }

    val closeReasons =
        remember {

            listOf(
                "Kitchen maintenance",
                "Staff unavailable",
                "Required items unavailable",
                "Power or technical issue",
                "Personal emergency",
                "Restaurant cleaning",
                "Other operational reason"
            )
        }

    /*
     * RESTAURANT PROFILE
     */
    var restaurantName by remember {
        mutableStateOf(
            RestaurantSession.restaurantName
        )
    }

    var restaurantPhone by remember {
        mutableStateOf("")
    }

    /*
     * RESTAURANT STATUS
     */
    var isOnline by remember {
        mutableStateOf(true)
    }

    var temporaryClosed by remember {
        mutableStateOf(false)
    }

    var closeReason by remember {
        mutableStateOf("")
    }

    /*
     * REASON DIALOG
     */
    var showCloseReasonDialog by remember {
        mutableStateOf(false)
    }

    var selectedCloseReason by remember {
        mutableStateOf("")
    }

    /*
     * LOADING STATES
     */
    var isSavingOnlineStatus by remember {
        mutableStateOf(false)
    }

    var isSavingTemporaryClose by remember {
        mutableStateOf(false)
    }

    /*
     * RESTAURANT DATA REALTIME LOAD
     */
    DisposableEffect(restaurantId) {

        val listener =
            restaurantRef
                .addSnapshotListener {
                        document,
                        error ->

                    if (
                        error != null ||
                        document == null ||
                        !document.exists()
                    ) {
                        return@addSnapshotListener
                    }

                    restaurantName =
                        document.getString(
                            "restaurantName"
                        )
                            ?: document.getString(
                                "name"
                            )
                                    ?: RestaurantSession.restaurantName

                    restaurantPhone =
                        document.getString(
                            "restaurantPhone"
                        )
                            ?: document.getString(
                                "phone"
                            )
                                    ?: document.getString(
                                "ownerPhone"
                            )
                                    ?: ""

                    isOnline =
                        document.getBoolean(
                            "online"
                        ) ?: true

                    temporaryClosed =
                        document.getBoolean(
                            "temporaryClosed"
                        ) ?: false

                    closeReason =
                        document.getString(
                            "closeReason"
                        ) ?: ""
                }

        onDispose {

            listener.remove()
        }
    }

    /*
     * TEMPORARY CLOSE REASON DIALOG
     */
    if (showCloseReasonDialog) {

        AlertDialog(

            onDismissRequest = {

                if (!isSavingTemporaryClose) {

                    showCloseReasonDialog =
                        false

                    selectedCloseReason =
                        ""
                }
            },

            title = {

                Text(
                    text =
                        "Temporarily close restaurant"
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "Restaurant बंद करने का reason select करें"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    closeReasons.forEach {
                            reason ->

                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled =
                                            !isSavingTemporaryClose
                                    ) {

                                        selectedCloseReason =
                                            reason
                                    }
                                    .padding(
                                        vertical = 8.dp
                                    ),

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            RadioButton(

                                selected =
                                    selectedCloseReason ==
                                            reason,

                                enabled =
                                    !isSavingTemporaryClose,

                                onClick = {

                                    selectedCloseReason =
                                        reason
                                }
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    reason
                            )
                        }
                    }

                    if (isSavingTemporaryClose) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        LinearProgressIndicator(
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(

                    enabled =
                        !isSavingTemporaryClose,

                    onClick =
                        confirmClose@{

                            if (
                                selectedCloseReason.isBlank()
                            ) {

                                Toast.makeText(
                                    context,
                                    "Close reason select करें",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@confirmClose
                            }

                            isSavingTemporaryClose =
                                true

                            restaurantRef
                                .update(
                                    mapOf(

                                        /*
                                         * Normal online status
                                         * change नहीं होगा।
                                         */
                                        "temporaryClosed"
                                                to
                                                true,

                                        "closeReason"
                                                to
                                                selectedCloseReason,

                                        "openingText"
                                                to
                                                "Temporarily Closed",

                                        "liveStatus"
                                                to
                                                "TEMPORARILY_CLOSED",

                                        /*
                                         * पुराने date field को
                                         * remove कर देंगे।
                                         */
                                        "temporaryClosedUntil"
                                                to
                                                FieldValue.delete()
                                    )
                                )
                                .addOnSuccessListener {

                                    isSavingTemporaryClose =
                                        false

                                    temporaryClosed =
                                        true

                                    closeReason =
                                        selectedCloseReason

                                    selectedCloseReason =
                                        ""

                                    showCloseReasonDialog =
                                        false

                                    Toast.makeText(
                                        context,
                                        "Restaurant temporarily closed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                        error ->

                                    isSavingTemporaryClose =
                                        false

                                    Toast.makeText(
                                        context,
                                        error.message
                                            ?: "Temporary close failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }

                ) {

                    Text(
                        "CONFIRM CLOSE"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    enabled =
                        !isSavingTemporaryClose,

                    onClick = {

                        showCloseReasonDialog =
                            false

                        selectedCloseReason =
                            ""
                    }

                ) {

                    Text(
                        "CANCEL"
                    )
                }
            }
        )
    }

    Column(

        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Color(0xFFFFF3E0)
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)

    ) {

        /*
         * RESTAURANT PROFILE
         */
        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text =
                        if (
                            restaurantName.isNotBlank()
                        ) {
                            restaurantName
                        } else {
                            "Restaurant"
                        },
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        if (
                            restaurantPhone.isNotBlank()
                        ) {
                            "📱 $restaurantPhone"
                        } else {
                            "📱 Mobile number unavailable"
                        },
                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                "Restaurant Settings",
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        /*
         * NORMAL ONLINE / OFFLINE
         */
        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Restaurant Status",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            if (isOnline) {
                                "🟢 ONLINE"
                            } else {
                                "🔴 OFFLINE"
                            }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            if (isOnline) {
                                "Restaurant orders ले सकता है"
                            } else {
                                "Restaurant manually offline है"
                            },
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }

                Switch(

                    checked =
                        isOnline,

                    enabled =
                        !isSavingOnlineStatus,

                    onCheckedChange = {
                            newStatus ->

                        val previousStatus =
                            isOnline

                        isOnline =
                            newStatus

                        isSavingOnlineStatus =
                            true

                        /*
                         * यह केवल normal online status बदलेगा।
                         */
                        restaurantRef
                            .update(
                                mapOf(
                                    "online"
                                            to
                                            newStatus
                                )
                            )
                            .addOnSuccessListener {

                                isSavingOnlineStatus =
                                    false

                                Toast.makeText(
                                    context,
                                    if (newStatus) {
                                        "Restaurant online"
                                    } else {
                                        "Restaurant offline"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                    error ->

                                isSavingOnlineStatus =
                                    false

                                isOnline =
                                    previousStatus

                                Toast.makeText(
                                    context,
                                    error.message
                                        ?: "Status update failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        /*
         * TEMPORARY CLOSE
         *
         * Switch ON  = Restaurant open
         * Switch OFF = Restaurant temporarily closed
         */
        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Temporary Restaurant Status",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                if (temporaryClosed) {
                                    "🔴 TEMPORARILY CLOSED"
                                } else {
                                    "🟢 OPEN"
                                },
                            color =
                                if (temporaryClosed) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color(0xFF168743)
                                },
                            fontWeight =
                                FontWeight.Medium
                        )
                    }

                    Switch(

                        /*
                         * temporaryClosed true होने पर
                         * switch OFF दिखाई देगा।
                         */
                        checked =
                            !temporaryClosed,

                        enabled =
                            !isSavingTemporaryClose,

                        onCheckedChange = {
                                shouldBeOpen ->

                            if (!shouldBeOpen) {

                                /*
                                 * Switch OFF:
                                 * Reason dialog खोलेंगे।
                                 */
                                selectedCloseReason =
                                    ""

                                showCloseReasonDialog =
                                    true

                            } else {

                                /*
                                 * Switch ON:
                                 * Restaurant reopen करेंगे।
                                 */
                                isSavingTemporaryClose =
                                    true

                                restaurantRef
                                    .update(
                                        mapOf(

                                            /*
                                             * Normal online field
                                             * change नहीं होगा।
                                             */
                                            "temporaryClosed"
                                                    to
                                                    false,

                                            "closeReason"
                                                    to
                                                    "",

                                            "openingText"
                                                    to
                                                    if (isOnline) {
                                                        ""
                                                    } else {
                                                        "Restaurant Offline"
                                                    },

                                            "liveStatus"
                                                    to
                                                    if (isOnline) {
                                                        "OPEN"
                                                    } else {
                                                        "CLOSED"
                                                    },

                                            "temporaryClosedUntil"
                                                    to
                                                    FieldValue.delete()
                                        )
                                    )
                                    .addOnSuccessListener {

                                        isSavingTemporaryClose =
                                            false

                                        temporaryClosed =
                                            false

                                        closeReason =
                                            ""

                                        Toast.makeText(
                                            context,
                                            if (isOnline) {
                                                "Restaurant reopened"
                                            } else {
                                                "Temporary close removed"
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                            error ->

                                        isSavingTemporaryClose =
                                            false

                                        temporaryClosed =
                                            true

                                        Toast.makeText(
                                            context,
                                            error.message
                                                ?: "Restaurant reopen failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        }
                    )
                }

                /*
                 * CLOSED MESSAGE AND REASON
                 */
                if (temporaryClosed) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    HorizontalDivider()

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            "Your restaurant is temporarily closed",
                        color =
                            MaterialTheme.colorScheme.error,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            if (closeReason.isNotBlank()) {
                                "Reason: $closeReason"
                            } else {
                                "Reason unavailable"
                            },
                        style =
                            MaterialTheme.typography
                                .bodyMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Restaurant reopen करने के लिए switch ON करें",
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )
                }

                if (isSavingTemporaryClose) {

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        /*
         * WEEKLY RESTAURANT TIMING
         */
        Card(

            onClick = {

                onTimingClick()
            },

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text =
                        "⏰ Restaurant Timing",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Weekly Opening / Closing Timing"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        /*
         * LOGOUT
         */
        Button(

            onClick = {

                RestaurantSession.restaurantId =
                    ""

                RestaurantSession.restaurantName =
                    ""

                val prefs =
                    context.getSharedPreferences(
                        "restaurant_session",
                        android.content.Context
                            .MODE_PRIVATE
                    )

                prefs
                    .edit()
                    .clear()
                    .apply()

                onLogout()
            },

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color.Red
                )

        ) {

            Text(
                "🚪 Logout"
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )
    }
}