package com.veggiego.restaurant

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID
import kotlin.concurrent.thread

object KotPrinter {
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun print(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            if (context is Activity) {
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 4201)
                Toast.makeText(context, "Bluetooth permission allow karke PRINT KOT dobara dabaye", Toast.LENGTH_LONG).show()
            }
            return
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(context, "Bluetooth ON karke thermal printer connect kare", Toast.LENGTH_LONG).show()
            return
        }

        val device = findPrinter(adapter.bondedDevices, context)
        if (device == null) {
            Toast.makeText(context, "Paired thermal printer nahi mila. Phone Bluetooth settings me printer pair kare.", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(context, "KOT printer ko bheja ja raha hai…", Toast.LENGTH_SHORT).show()
        thread {
            try {
                adapter.cancelDiscovery()
                device.createRfcommSocketToServiceRecord(sppUuid).use { socket ->
                    socket.connect()
                    socket.outputStream.use { out ->
                        out.write(byteArrayOf(0x1B, 0x40)) // initialize
                        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // center
                        out.write(text.toByteArray(Charsets.UTF_8))
                        out.write("\n\n\n".toByteArray())
                        out.flush()
                    }
                }
                (context as? Activity)?.runOnUiThread { Toast.makeText(context, "KOT printed", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Printer connect nahi hua: ${e.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun findPrinter(devices: Set<BluetoothDevice>, context: Context): BluetoothDevice? {
        val prefs = context.getSharedPreferences("kot_printer", Context.MODE_PRIVATE)
        val saved = prefs.getString("address", "").orEmpty()
        devices.firstOrNull { it.address == saved }?.let { return it }
        val printer = devices.firstOrNull {
            val name = it.name.orEmpty().lowercase()
            name.contains("printer") || name.contains("pos") || name.contains("thermal") || name.contains("58") || name.contains("80")
        } ?: devices.firstOrNull()
        printer?.let { prefs.edit().putString("address", it.address).apply() }
        return printer
    }
}
