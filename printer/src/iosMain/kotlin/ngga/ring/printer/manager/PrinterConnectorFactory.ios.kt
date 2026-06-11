package ngga.ring.printer.manager

import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.coroutines.resume

/**
 * iOS Implementation for Bluetooth Low Energy (BLE).
 */
@OptIn(ExperimentalForeignApi::class)
class IosBluetoothConnector : BasePrinterConnector() {
    private var centralManager: CBCentralManager? = null
    private var connectedPeripheral: CBPeripheral? = null
    private var writeCharacteristic: CBCharacteristic? = null
    private var continuation: CancellableContinuation<Boolean>? = null
    private val readBuffer = mutableListOf<UByte>()
    private var readContinuation: CancellableContinuation<ByteArray?>? = null

    private var targetUUID: NSUUID? = null
    private val bleDelegate = BleDelegate()

    private val SERVICE_UUID = CBUUID.UUIDWithString("FF00")
    private val WRITE_UUID = CBUUID.UUIDWithString("FF01")

    override suspend fun connect(config: PrinterConfig): Boolean =
        suspendCancellableCoroutine { cont ->
            configureFlowControl(config)
            if (config.address == null) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }

            this.continuation = cont
            this.targetUUID = NSUUID(uUIDString = config.address)

            if (centralManager == null) {
                centralManager = CBCentralManager(delegate = bleDelegate, queue = null)
            } else {
                if (centralManager?.state == CBManagerStatePoweredOn) {
                    initiateConnection()
                }
            }
        }

    private fun initiateConnection() {
        val uuid = targetUUID ?: return
        val peripherals =
            centralManager?.retrievePeripheralsWithIdentifiers(listOf(uuid)) as? List<CBPeripheral>
        val peripheral = peripherals?.firstOrNull()

        if (peripheral != null) {
            connectedPeripheral = peripheral
            peripheral.delegate = bleDelegate
            centralManager?.connectPeripheral(peripheral, options = null)
        } else {
            centralManager?.scanForPeripheralsWithServices(null, null)
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.Default) {
        val char = writeCharacteristic ?: return@withContext false
        val peripheral = connectedPeripheral ?: return@withContext false

        val nsData = data.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
        }

        peripheral.writeValue(
            nsData,
            forCharacteristic = char,
            type = CBCharacteristicWriteWithoutResponse
        )
        true
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withTimeoutOrNull(timeout) {
        if (readBuffer.size >= count) {
            val result = ByteArray(readBuffer.size) { i -> readBuffer[i].toByte() }
            repeat(readBuffer.size) { readBuffer.removeAt(0) }
            return@withTimeoutOrNull result
        }

        suspendCancellableCoroutine<ByteArray?> { cont ->
            readContinuation = cont
        }
    }

    override suspend fun disconnect() {
        connectedPeripheral?.let { centralManager?.cancelPeripheralConnection(it) }
        connectedPeripheral = null
        writeCharacteristic = null
        targetUUID = null
    }

    override fun isConnected(): Boolean = connectedPeripheral != null && writeCharacteristic != null

    private inner class BleDelegate : NSObject(), CBCentralManagerDelegateProtocol,
        CBPeripheralDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            if (central.state == CBManagerStatePoweredOn) {
                initiateConnection()
            } else if (central.state == CBManagerStatePoweredOff) {
                continuation?.resume(false)
                continuation = null
            }
        }

        override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
            didConnectPeripheral.discoverServices(listOf(SERVICE_UUID))
        }

        override fun centralManager(
            central: CBCentralManager,
            didFailToConnectPeripheral: CBPeripheral,
            error: NSError?
        ) {
            continuation?.resume(false)
            continuation = null
        }

        override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
            val service = (peripheral.services ?: emptyList<Any?>()).firstOrNull {
                (it as? CBService)?.UUID == SERVICE_UUID
            } as? CBService

            if (service != null) {
                peripheral.discoverCharacteristics(listOf(WRITE_UUID), forService = service)
            } else {
                (peripheral.services ?: emptyList<Any?>()).firstOrNull()?.let {
                    peripheral.discoverCharacteristics(null, forService = it as CBService)
                }
            }
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didDiscoverCharacteristicsForService: CBService,
            error: NSError?
        ) {
            val char = (didDiscoverCharacteristicsForService.characteristics
                ?: emptyList<Any?>()).firstOrNull {
                (it as? CBCharacteristic)?.UUID == WRITE_UUID
            } as? CBCharacteristic

            writeCharacteristic = char ?: (didDiscoverCharacteristicsForService.characteristics
                ?: emptyList<Any?>()).firstOrNull() as? CBCharacteristic

            if (writeCharacteristic != null) {
                continuation?.resume(true)
            } else {
                continuation?.resume(false)
            }
            continuation = null
        }

        override fun peripheral(
            peripheral: CBPeripheral,
            didUpdateValueForCharacteristic: CBCharacteristic,
            error: NSError?
        ) {
            val nsData = didUpdateValueForCharacteristic.value ?: return
            val bytes = ByteArray(nsData.length.toInt())
            nsData.bytes?.let { bytesPtr ->
                bytes.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), bytesPtr, nsData.length)
                }
            }
            
            readBuffer.addAll(bytes.map { it.toUByte() })
            
            readContinuation?.let { cont ->
                val result = ByteArray(readBuffer.size) { i -> readBuffer[i].toByte() }
                readBuffer.clear()
                cont.resume(result)
                readContinuation = null
            }
        }
    }
}

/**
 * iOS Implementation for Network (TCP) using POSIX Sockets.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkConnector : BasePrinterConnector() {
    private var socket: Int = -1
    private var isConnected = false

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.Default) {
        configureFlowControl(config)
        val host = config.address ?: return@withContext false
        val port = config.port
        
        val sock = socket(AF_INET, SOCK_STREAM, 0)
        if (sock == -1) return@withContext false
        
        memScoped {
            val serverAddr = alloc<sockaddr_in>()
            serverAddr.sin_family = AF_INET.toUByte()
            // Manual htons implementation for big-endian port
            serverAddr.sin_port = (((port and 0xFF00) shr 8) or ((port and 0x00FF) shl 8)).toUShort()
            
            if (inet_pton(AF_INET, host, serverAddr.sin_addr.ptr) <= 0) {
                close(sock)
                return@withContext false
            }
            
            if (connect(sock, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) == 0) {
                socket = sock
                isConnected = true
                true
            } else {
                close(sock)
                false
            }
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.Default) {
        if (socket == -1) return@withContext false
        
        data.usePinned { pinned ->
            val result = send(socket, pinned.addressOf(0), data.size.toULong(), 0)
            result.toLong() != -1L
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.Default) {
        if (socket == -1) return@withContext null
        
        val buffer = ByteArray(count)
        buffer.usePinned { pinned ->
            val result = recv(socket, pinned.addressOf(0), count.toULong(), 0)
            if (result.toLong() > 0) buffer.take(result.toInt()).toByteArray() else null
        }
    }

    override suspend fun disconnect() {
        if (socket != -1) {
            close(socket)
            socket = -1
            isConnected = false
        }
    }

    override fun isConnected(): Boolean = isConnected
}

@OptIn(ExperimentalForeignApi::class)
actual class PrinterConnectorFactory : PrinterConnectorProvider {
    actual constructor()

    actual override fun create(config: PrinterConfig): PrinterConnector {
        return when (PrinterConnectionType.normalize(config.connectionType)) {
            PrinterConnectionType.BLUETOOTH, PrinterConnectionType.BLUETOOTH_LE -> IosBluetoothConnector()
            PrinterConnectionType.NETWORK -> IosNetworkConnector()
            PrinterConnectionType.VIRTUAL -> VirtualPrinterConnector()
            else -> object : PrinterConnector {
                override suspend fun connect(config: PrinterConfig) = false
                override suspend fun sendData(data: ByteArray) = false
                override suspend fun readData(count: Int, timeout: Long) = null
                override suspend fun disconnect() {}
                override fun isConnected() = false
            }
        }
    }

    actual override fun discovery(
        type: String,
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = callbackFlow {
        val normalizedType = PrinterConnectionType.normalize(type)
        onLog("Scanning for $normalizedType...")
        val discoveredDevices = mutableListOf<DiscoveredPrinter>()

        if (config.showVirtualDevices) {
            discoveredDevices.add(
                DiscoveredPrinter(
                    "[VIRTUAL] $type iOS Printer",
                    normalizedType,
                    if (normalizedType == PrinterConnectionType.NETWORK) "192.168.1.102" else "UUID-IOS-TEST-1234"
                )
            )
            trySend(discoveredDevices.toList())
        }

        if (normalizedType == PrinterConnectionType.BLUETOOTH || normalizedType == PrinterConnectionType.BLUETOOTH_LE) {
            val delegate = object : NSObject(), CBCentralManagerDelegateProtocol {
                override fun centralManagerDidUpdateState(central: CBCentralManager) {
                    if (central.state == CBManagerStatePoweredOn) {
                        central.scanForPeripheralsWithServices(null, null)
                    } else if (central.state == CBManagerStatePoweredOff) {
                        onLog("Bluetooth is OFF")
                    }
                }

                override fun centralManager(
                    central: CBCentralManager,
                    didDiscoverPeripheral: CBPeripheral,
                    advertisementData: Map<Any?, *>,
                    RSSI: NSNumber
                ) {
                    val name = didDiscoverPeripheral.name ?: "Unknown Device"
                    val address = didDiscoverPeripheral.identifier.UUIDString

                    if (discoveredDevices.none { it.address == address }) {
                        discoveredDevices.add(DiscoveredPrinter(name, PrinterConnectionType.BLUETOOTH_LE, address))
                        trySend(discoveredDevices.toList())
                    }
                }
            }

            val centralManager = CBCentralManager(delegate = delegate, queue = null)
            awaitClose {
                centralManager.stopScan()
            }
        } else if (normalizedType == PrinterConnectionType.NETWORK) {
            onLog("Network discovery started...")
            delay(500)
            if (discoveredDevices.none { it.address == "192.168.1.100" }) {
                discoveredDevices.add(DiscoveredPrinter("Network Printer", PrinterConnectionType.NETWORK, "192.168.1.100", 9100))
                trySend(discoveredDevices.toList())
            }
        }
    }
}
