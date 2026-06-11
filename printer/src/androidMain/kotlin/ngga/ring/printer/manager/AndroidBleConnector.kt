package ngga.ring.printer.manager

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import ngga.ring.printer.model.PrinterConfig
import java.util.*
import kotlin.coroutines.resume

/**
 * Android Implementation for Bluetooth Low Energy (BLE).
 */
@SuppressLint("MissingPermission")
class AndroidBleConnector(private val context: Context) : BasePrinterConnector() {
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    
    // Standard Thermal Printer BLE Service/Characteristic UUIDs
    private val PRINTER_SERVICE_UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
    private val PRINTER_WRITE_UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return@withContext false
        val device = adapter.getRemoteDevice(config.address ?: return@withContext false)
        
        val success = suspendCancellableCoroutine<Boolean> { continuation ->
            var resumed = false
            
            val callback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (!resumed) {
                            resumed = true
                            continuation.resume(false)
                        }
                        bluetoothGatt = null
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(PRINTER_SERVICE_UUID) 
                            ?: gatt.services.firstOrNull { it.characteristics.any { c -> c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 } }
                        
                        writeCharacteristic = service?.getCharacteristic(PRINTER_WRITE_UUID)
                            ?: service?.characteristics?.find { it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 }
                        
                        if (!resumed) {
                            resumed = true
                            continuation.resume(writeCharacteristic != null)
                        }
                    } else {
                        if (!resumed) {
                            resumed = true
                            continuation.resume(false)
                        }
                    }
                }
            }
            
            bluetoothGatt = device.connectGatt(context, false, callback)
        }
        
        success
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val gatt = bluetoothGatt ?: return@withContext false
        val char = writeCharacteristic ?: return@withContext false
        
        // BLE MTU is usually small (20-512 bytes). 
        // We chunk the data to ensure reliable transmission.
        val mtu = 20 // Safe default
        
        data.toList().chunked(mtu).forEach { chunk ->
            val chunkArray = chunk.toByteArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, chunkArray, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            } else {
                @Suppress("DEPRECATION")
                char.value = chunkArray
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
            delay(10) // Small delay between chunks
        }
        
        true
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
    }

    override fun isConnected(): Boolean = bluetoothGatt != null && writeCharacteristic != null
}
