package ngga.ring.printer.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
            requestUsbPermission(context, device)
        }
    }

    private suspend fun requestUsbPermission(context: Context, device: UsbDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            val intent = Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

            val isUnregistered = AtomicBoolean(false)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_USB_PERMISSION) {
                        val targetDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        if (targetDevice == null || targetDevice.deviceId == device.deviceId) {
                            if (isUnregistered.compareAndSet(false, true)) {
                                try {
                                    context.unregisterReceiver(this)
                                } catch (e: Exception) {
                                    PrinterLogger.warn(TAG, "Failed to unregister USB permission receiver", e)
                                }
                            }
                            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            if (cont.isActive) {
                                if (granted) {
                                    val opened = openDevice(device)
                                    cont.resume(opened)
                                } else {
                                    cont.resume(false)
                                }
                            }
                        }
                    }
                }
            }

            cont.invokeOnCancellation {
                if (isUnregistered.compareAndSet(false, true)) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (e: Exception) {
                        PrinterLogger.warn(TAG, "Failed to unregister USB permission receiver on cancel", e)
                    }
                }
            }

            val filter = IntentFilter(ACTION_USB_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }

            val manager = usbManager
            if (manager == null) {
                if (isUnregistered.compareAndSet(false, true)) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                if (cont.isActive) {
                    cont.resume(false)
                }
                return@suspendCancellableCoroutine
            }

            try {
                manager.requestPermission(device, permissionIntent)
            } catch (e: Exception) {
                PrinterLogger.warn(TAG, "Failed to request USB permission", e)
                if (isUnregistered.compareAndSet(false, true)) {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (unregEx: Exception) {
                        // ignore
                    }
                }
                if (cont.isActive) {
                    cont.resume(false)
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
