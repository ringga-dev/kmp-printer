package ngga.ring.printer.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger
import java.util.*

/**
 * Android Implementation for Bluetooth Classic (SPP).
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothConnector : BasePrinterConnector() {
    private var socket: BluetoothSocket? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        val address = config.address ?: return@withContext false
        configureFlowControl(config)
        val context = PrinterInitializer.getContext()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter == null) {
            PrinterLogger.warn(TAG, "Bluetooth adapter not available")
            return@withContext false
        }
        
        if (!adapter.isEnabled) {
            PrinterLogger.warn(TAG, "Bluetooth is disabled")
            return@withContext false
        }
        
        // Ensure standard disconnect first to clear previous state
        disconnect()
        
        try {
            val device = adapter.getRemoteDevice(address)
            adapter.cancelDiscovery()
            
            PrinterLogger.info(TAG, "Attempting to connect to ${device.name} ($address)")
            
            // Try standard way
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                if (socket?.isConnected == true) {
                    PrinterLogger.info(TAG, "Connected successfully using standard RFCOMM")
                    return@withContext true
                }
            } catch (e: Exception) {
                PrinterLogger.warn(TAG, "Standard RFCOMM failed", e)
                socket?.close()
                socket = null
            }

            // Fallback for some devices (reflection)
            PrinterLogger.info(TAG, "Attempting reflection fallback (channel 1)")
            try {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = m.invoke(device, 1) as BluetoothSocket
                socket?.connect()
                val success = socket?.isConnected == true
                if (success) PrinterLogger.info(TAG, "Connected successfully using reflection")
                success
            } catch (e2: Exception) {
                PrinterLogger.warn(TAG, "Reflection fallback failed", e2)
                socket?.close()
                socket = null
                false
            }
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "General connection error", e)
            socket?.close()
            socket = null
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val current = socket ?: return@withContext false
            if (!isConnected()) return@withContext false
            current.outputStream.write(data)
            current.outputStream.flush()
            true
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Bluetooth send failed", e)
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val input = socket?.inputStream ?: return@withContext null
            
            // Wait for data with timeout
            val start = System.currentTimeMillis()
            while (input.available() <= 0) {
                if (System.currentTimeMillis() - start > timeout) return@withContext null
                kotlinx.coroutines.delay(10)
            }
            
            val buffer = ByteArray(count.coerceAtMost(input.available()))
            val read = input.read(buffer)
            if (read > 0) buffer.copyOf(read) else null
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Bluetooth status read failed", e)
            null
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.let { s ->
                if (s.isConnected) {
                    s.outputStream?.flush()
                }
                s.close()
            }
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "Bluetooth disconnect failed", e)
        } finally {
            socket = null
        }
        Unit
    }

    override fun isConnected(): Boolean = socket?.isConnected ?: false

    private companion object {
        const val TAG = "AndroidBluetoothConnector"
    }
}
