package ngga.ring.printer.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBleConnector(private val context: Context) : BasePrinterConnector() {
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var negotiatedMtu: Int = DEFAULT_MTU
    private var configuredChunkSize: Int = DEFAULT_PAYLOAD_SIZE
    private var pendingWrite: CompletableDeferred<Boolean>? = null
    private var pendingRead: CompletableDeferred<ByteArray?>? = null
    private var pendingNotify: CompletableDeferred<ByteArray?>? = null

    private val printerServiceUuid = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
    private val printerWriteUuid = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        configureFlowControl(config)
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return@withContext false
        val address = config.address ?: return@withContext false
        configuredChunkSize = config.bleChunkSize.coerceIn(1, MAX_PAYLOAD_SIZE)
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            PrinterLogger.warn(TAG, "Invalid BLE device address: $address", e)
            return@withContext false
        }

        val connected = CompletableDeferred<Boolean>()
        val callback = createCallback(connected)
        @Suppress("DEPRECATION")
        bluetoothGatt = device.connectGatt(context, false, callback)

        val ready = withTimeoutOrNull(config.connectionTimeoutMs.toLong().coerceAtLeast(1000)) {
            connected.await()
        } == true

        if (!ready) {
            disconnect()
            return@withContext false
        }

        requestMtu(config)
        val discovered = discoverServicesWithRetry(config)
        if (!discovered) {
            disconnect()
            return@withContext false
        }

        enableNotifyIfPossible()
        writeCharacteristic != null
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext false
        val characteristic = writeCharacteristic ?: return@withContext false
        val mtuPayloadSize = (negotiatedMtu - ATT_HEADER_SIZE).coerceAtLeast(DEFAULT_PAYLOAD_SIZE)
        val chunkSize = configuredChunkSize.coerceAtMost(mtuPayloadSize).coerceIn(1, MAX_PAYLOAD_SIZE)
        val writeType = if (characteristic.supportsWriteWithResponse()) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

        characteristic.writeType = writeType
        data.toList().chunked(chunkSize).forEach { chunk ->
            val chunkArray = chunk.toByteArray()
            val accepted = writeCharacteristic(gatt, characteristic, chunkArray)
            if (!accepted) return@withContext false

            if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
                val result = withTimeoutOrNull(WRITE_TIMEOUT_MS) {
                    pendingWrite?.await()
                } == true
                pendingWrite = null
                if (!result) return@withContext false
            }

            delay(10)
        }
        true
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext null
        val readable = readCharacteristic
        if (readable != null) {
            pendingRead = CompletableDeferred()
            val accepted = gatt.readCharacteristic(readable)
            if (accepted) {
                return@withContext withTimeoutOrNull(timeout.coerceAtLeast(1)) {
                    pendingRead?.await()
                }?.take(count)
            }
            pendingRead = null
        }

        val notified = pendingNotify ?: CompletableDeferred<ByteArray?>().also { pendingNotify = it }
        withTimeoutOrNull(timeout.coerceAtLeast(1)) {
            notified.await()
        }?.take(count)
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        readCharacteristic = null
        notifyCharacteristic = null
        pendingWrite = null
        pendingRead = null
        pendingNotify = null
        negotiatedMtu = DEFAULT_MTU
        configuredChunkSize = DEFAULT_PAYLOAD_SIZE
    }

    override fun isConnected(): Boolean = bluetoothGatt != null && writeCharacteristic != null

    private fun createCallback(connected: CompletableDeferred<Boolean>): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> connected.complete(status == BluetoothGatt.GATT_SUCCESS)
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connected.complete(false)
                        bluetoothGatt = null
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    negotiatedMtu = mtu
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    selectCharacteristics(gatt)
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
            }

            @Deprecated("Deprecated in Android API")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                @Suppress("DEPRECATION")
                pendingRead?.complete(if (status == BluetoothGatt.GATT_SUCCESS) characteristic.value else null)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                pendingRead?.complete(if (status == BluetoothGatt.GATT_SUCCESS) value else null)
            }

            @Deprecated("Deprecated in Android API")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                @Suppress("DEPRECATION")
                pendingNotify?.complete(characteristic.value)
                pendingNotify = null
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                pendingNotify?.complete(value)
                pendingNotify = null
            }
        }
    }

    private suspend fun requestMtu(config: PrinterConfig) {
        val target = config.bleChunkSize
            .takeIf { it > DEFAULT_PAYLOAD_SIZE }
            ?.plus(ATT_HEADER_SIZE)
            ?.coerceIn(DEFAULT_MTU, MAX_MTU)
            ?: MAX_MTU
        bluetoothGatt?.requestMtu(target)
        delay(150)
    }

    private suspend fun discoverServicesWithRetry(config: PrinterConfig): Boolean {
        val gatt = bluetoothGatt ?: return false
        repeat(3) { attempt ->
            if (gatt.discoverServices()) {
                delay(350)
                if (writeCharacteristic != null || selectCharacteristics(gatt)) {
                    return true
                }
            }
            if (attempt < 2) delay(config.retryDelayMs.coerceAtLeast(100))
        }
        return false
    }

    private fun selectCharacteristics(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(printerServiceUuid)
            ?: gatt.services.firstOrNull { service ->
                service.characteristics.any { it.supportsWrite() || it.supportsWriteWithoutResponse() }
            }
            ?: return false

        writeCharacteristic = service.getCharacteristic(printerWriteUuid)
            ?: service.characteristics.firstOrNull { it.supportsWrite() || it.supportsWriteWithoutResponse() }
        readCharacteristic = service.characteristics.firstOrNull { it.supportsRead() }
        notifyCharacteristic = service.characteristics.firstOrNull { it.supportsNotify() || it.supportsIndicate() }

        return writeCharacteristic != null
    }

    private fun enableNotifyIfPossible() {
        val gatt = bluetoothGatt ?: return
        val characteristic = notifyCharacteristic ?: return
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.descriptors.firstOrNull()
        if (descriptor != null) {
            val value = if (characteristic.supportsNotify()) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private fun writeCharacteristic(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (characteristic.writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) {
            pendingWrite = CompletableDeferred()
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, value, characteristic.writeType) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun ByteArray.take(count: Int): ByteArray {
        return if (size <= count) this else copyOf(count)
    }

    private fun BluetoothGattCharacteristic.supportsWrite(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
    }

    private fun BluetoothGattCharacteristic.supportsWriteWithoutResponse(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
    }

    private fun BluetoothGattCharacteristic.supportsWriteWithResponse(): Boolean = supportsWrite()

    private fun BluetoothGattCharacteristic.supportsRead(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
    }

    private fun BluetoothGattCharacteristic.supportsNotify(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    }

    private fun BluetoothGattCharacteristic.supportsIndicate(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
    }

    private companion object {
        const val TAG = "AndroidBleConnector"
        const val DEFAULT_MTU = 23
        const val MAX_MTU = 185
        const val ATT_HEADER_SIZE = 3
        const val DEFAULT_PAYLOAD_SIZE = 20
        const val MAX_PAYLOAD_SIZE = 512
        const val WRITE_TIMEOUT_MS = 2500L
    }
}
