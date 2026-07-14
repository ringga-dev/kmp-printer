package ngga.ring.printer.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger

/**
 * Android Implementation for USB OTG Printing.
 */
class AndroidUsbConnector : BasePrinterConnector() {
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var usbEndpointOut: UsbEndpoint? = null
    private var usbEndpointIn: UsbEndpoint? = null

    private val ACTION_USB_PERMISSION = "ngga.ring.printer.USB_PERMISSION"

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        configureFlowControl(config)
        val context = PrinterInitializer.getContext()
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        val deviceList = usbManager?.deviceList
        val address = config.address // Expected format "VID:PID"

        usbDevice = if (address != null && address.contains(":")) {
            val parts = address.split(":")
            val vid = parts[0].toIntOrNull()
            val pid = parts[1].toIntOrNull()
            deviceList?.values?.find { it.vendorId == vid && it.productId == pid }
        } else {
            // Find first printer if address not specified or invalid
            deviceList?.values?.find { device ->
                (0 until device.interfaceCount).any { i ->
                    device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER
                }
            }
        }

        val device = usbDevice ?: return@withContext false

        if (usbManager?.hasPermission(device) == true) {
            openDevice(device)
        } else {
            val granted = requestUsbPermission(context, device, config.connectionTimeoutMs.toLong())
            if (granted) openDevice(device) else false
        }
    }

    private suspend fun requestUsbPermission(context: Context, device: UsbDevice, timeoutMs: Long): Boolean {
        val result = CompletableDeferred<Boolean>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != ACTION_USB_PERMISSION) return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                val receivedDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }

                result.complete(granted && receivedDevice?.deviceName == device.deviceName)
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            usbManager?.requestPermission(device, permissionIntent)
            withTimeoutOrNull(timeoutMs.coerceAtLeast(1000)) {
                result.await()
            } == true
        } finally {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                PrinterLogger.warn(TAG, "USB permission receiver unregister failed", e)
            }
        }
    }

    private fun openDevice(device: UsbDevice): Boolean {
        usbInterface = (0 until device.interfaceCount)
            .map { device.getInterface(it) }
            .find { it.interfaceClass == UsbConstants.USB_CLASS_PRINTER }
            ?: device.getInterface(0)

        usbEndpointOut = (0 until (usbInterface?.endpointCount ?: 0))
            .map { usbInterface?.getEndpoint(it) }
            .find { it?.direction == UsbConstants.USB_DIR_OUT }

        usbEndpointIn = (0 until (usbInterface?.endpointCount ?: 0))
            .map { usbInterface?.getEndpoint(it) }
            .find { it?.direction == UsbConstants.USB_DIR_IN }

        usbConnection = usbManager?.openDevice(device)
        return usbConnection?.claimInterface(usbInterface, true) ?: false
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val connection = usbConnection ?: return@withContext false
        val endpoint = usbEndpointOut ?: return@withContext false
        
        val result = connection.bulkTransfer(endpoint, data, data.size, 5000)
        val success = result >= 0
        if (!success) {
            PrinterLogger.warn(TAG, "USB bulk transfer failed with result $result")
        }
        success
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        val connection = usbConnection ?: return@withContext null
        val endpoint = usbEndpointIn ?: return@withContext null
        
        val buffer = ByteArray(count)
        val result = connection.bulkTransfer(endpoint, buffer, count, timeout.toInt())
        if (result > 0) buffer.copyOf(result) else null
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            usbConnection?.releaseInterface(usbInterface)
            usbConnection?.close()
        } catch (e: Exception) {
            PrinterLogger.warn(TAG, "USB disconnect failed", e)
        }
        usbConnection = null
        usbInterface = null
        usbEndpointOut = null
        usbEndpointIn = null
        usbDevice = null
    }

    override fun isConnected(): Boolean = usbConnection != null && usbEndpointOut != null

    private companion object {
        const val TAG = "AndroidUsbConnector"
    }
}
