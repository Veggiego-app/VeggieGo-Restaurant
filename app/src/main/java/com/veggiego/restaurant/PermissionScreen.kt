package com.veggiego.restaurant

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun RequiredPermissionsScreen(
    onAllRequiredPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshKey by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            refreshKey++
        }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationPermissionGranted = remember(refreshKey) {
        hasNotificationPermission(context)
    }

    val notificationsEnabled = remember(refreshKey) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val fullScreenPermissionGranted = remember(refreshKey) {
        canUseFullScreenAlerts(context)
    }

    val batteryUnrestricted = remember(refreshKey) {
        isIgnoringBatteryOptimizations(context)
    }

    val allRequiredGranted =
        notificationPermissionGranted &&
                notificationsEnabled &&
                fullScreenPermissionGranted &&
                batteryUnrestricted

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF6F7F8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))

            Text(
                text = "Complete Required Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "New orders ko time par receive karne aur lock screen par order popup dikhane ke liye ye permissions zaroori hain.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(24.dp))

            PermissionCard(
                title = "Notification Permission",
                description = "New order ki ringtone, vibration aur notification ke liye.",
                granted = notificationPermissionGranted && notificationsEnabled,
                buttonText = if (!notificationPermissionGranted) "ALLOW NOTIFICATIONS" else "OPEN NOTIFICATION SETTINGS",
                onClick = {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notificationPermissionGranted
                    ) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        openAppNotificationSettings(context)
                    }
                }
            )

            Spacer(Modifier.height(14.dp))

            PermissionCard(
                title = "Full-Screen Order Alert",
                description = "Phone lock ho tab screen ON karke Accept/Reject popup dikhane ke liye.",
                granted = fullScreenPermissionGranted,
                buttonText = "ALLOW FULL-SCREEN ALERT",
                onClick = {
                    openFullScreenAlertSettings(context)
                }
            )

            Spacer(Modifier.height(14.dp))

            PermissionCard(
                title = "Battery Unrestricted",
                description = "Phone app ko background me band na kare, isliye Battery Unrestricted compulsory hai.",
                granted = batteryUnrestricted,
                buttonText = "OPEN BATTERY SETTINGS",
                required = true,
                onClick = {
                    openBatterySettings(context)
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    refreshKey++
                    if (allRequiredGranted) {
                        onAllRequiredPermissionsGranted()
                    }
                },
                enabled = allRequiredGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B8F3A)
                )
            ) {
                Text(
                    text = if (allRequiredGranted) "CONTINUE TO HOME" else "COMPLETE REQUIRED PERMISSIONS",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { activity.finish() }
            ) {
                Text("CLOSE APP")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    required: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (granted) "✅" else if (required) "❌" else "⚠️",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (granted) "Enabled" else if (required) "Required" else "Recommended",
                        color = if (granted) Color(0xFF1B8F3A) else Color(0xFFC62828),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )

            if (!granted) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun canUseFullScreenAlerts(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    val manager = context.getSystemService(NotificationManager::class.java)
    return manager.canUseFullScreenIntent()
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

private fun openFullScreenAlertSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return
    }

    try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppDetailsSettings(context)
    }
}

private fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppDetailsSettings(context)
    }
}

private fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
