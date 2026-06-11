package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrinterConnectionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Collections

/**
 * JVM Implementation for Network (TCP) printers.
 */
class JvmNetworkConnector : BasePrinterConnector() {
    private var socket: Socket? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            configureFlowControl(config)
            socket = Socket()
            socket?.tcpNoDelay = true
            socket?.keepAlive = true
            socket?.connect(InetSocketAddress(config.address ?: "127.0.0.1", config.port), config.connectionTimeoutMs)
            socket?.soTimeout = config.readTimeoutMs
            isConnected()
        } catch (e: Exception) {
            println("PrinterJVM: Network connection failed: ${e.message}")
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
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val input = socket?.inputStream ?: return@withContext null
            
            val start = System.currentTimeMillis()
            while (input.available() <= 0) {
                if (System.currentTimeMillis() - start > timeout) return@withContext null
                kotlinx.coroutines.delay(10)
            }
            
            val buffer = ByteArray(count.coerceAtMost(input.available()))
            val read = input.read(buffer)
            if (read > 0) buffer.copyOf(read) else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {}
    }

    override fun isConnected(): Boolean {
        val current = socket ?: return false
        return current.isConnected && !current.isClosed && !current.isOutputShutdown
    }
}

actual class PrinterConnectorFactory {
    actual constructor()
    private val portService = JvmPrinterPortService()

    actual fun create(config: PrinterConfig): PrinterConnector {
        return when (PrinterConnectionType.normalize(config.connectionType)) {
            PrinterConnectionType.NETWORK -> JvmNetworkConnector()
            PrinterConnectionType.SERIAL -> JvmSerialConnector()
            PrinterConnectionType.USB -> JvmCompositeConnector(
                listOf(
                    JvmRawUsbConnector(),
                    JvmSerialConnector(),
                    JvmPrintServiceConnector()
                )
            )
            PrinterConnectionType.BLUETOOTH -> JvmCompositeConnector(
                listOf(
                    JvmSerialConnector(),
                    JvmPrintServiceConnector()
                )
            )
            PrinterConnectionType.BLUETOOTH_LE -> JvmCompositeConnector(
                listOf(
                    JvmBleConnector(),
                    JvmSerialConnector(),
                    JvmUnsupportedNativeConnector("Native JVM BLE backend is not available for this OS yet.")
                )
            )
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

    actual fun discovery(
        type: String, 
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = callbackFlow {
        val normalizedType = PrinterConnectionType.normalize(type)
        val discoveredDevices = Collections.synchronizedSet(mutableSetOf<DiscoveredPrinter>())

        if (config.showVirtualDevices) {
            discoveredDevices.add(
                DiscoveredPrinter(
                    "[VIRTUAL] $normalizedType JVM Printer",
                    PrinterConnectionType.VIRTUAL,
                    if (normalizedType == PrinterConnectionType.NETWORK) "192.168.1.103" else "COM1-VIRTUAL"
                )
            )
            trySend(discoveredDevices.toList())
        }

        if (normalizedType == PrinterConnectionType.NETWORK) {
            val socket = java.net.DatagramSocket().apply {
                broadcast = true
                soTimeout = config.networkScanTimeoutMs
            }

            launch(Dispatchers.IO) {
                try {
                    val probeData = byteArrayOf(0x1B, 0x40)
                    val packet = java.net.DatagramPacket(
                        probeData, probeData.size,
                        java.net.InetAddress.getByName("255.255.255.255"),
                        9100
                    )
                    socket.send(packet)
                    onLog("JVM: UDP Broadcast sent")

                    val buffer = ByteArray(1024)
                    while (isActive) {
                        val receivePacket = java.net.DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(receivePacket)
                            val address = receivePacket.address.hostAddress
                            discoveredDevices.add(
                                DiscoveredPrinter(
                                    "Printer ($address)",
                                    PrinterConnectionType.NETWORK,
                                    address,
                                    9100
                                )
                            )
                            trySend(discoveredDevices.toList())
                        } catch (e: java.net.SocketTimeoutException) {
                            break
                        }
                    }
                } catch (e: Exception) {
                    onLog("JVM Network discovery error: ${e.message}")
                } finally {
                    socket.close()
                }
            }
        } else if (PrinterConnectionType.usesSerialPortOnJvm(normalizedType)) {
            launch(Dispatchers.IO) {
                onLog("JVM: ${portService.currentOs()} backend scanning serial-backed ports for $normalizedType...")
                onLog("JVM: ${portService.connectionHint(normalizedType)}")
                when (normalizedType) {
                    PrinterConnectionType.USB -> onLog("JVM: Raw USB native is enabled. ${portService.rawUsbHint()}")
                    PrinterConnectionType.BLUETOOTH -> onLog("JVM: Bluetooth Classic uses OS paired serial ports.")
                    PrinterConnectionType.BLUETOOTH_LE -> onLog("JVM: ${portService.bleHint()} Serial-like BLE ports are still scanned as fallback.")
                    else -> Unit
                }

                if (normalizedType == PrinterConnectionType.USB) {
                    val rawUsbDevices = portService.discoverRawUsbPrinters()
                    rawUsbDevices.forEach { printer ->
                        discoveredDevices.add(printer)
                        trySend(discoveredDevices.toList())
                    }
                    onLog("JVM: Found ${rawUsbDevices.size} raw USB printer devices")
                }

                val ports = portService.discoverSerialBackedPrinters(normalizedType)
                ports.forEach { printer ->
                    discoveredDevices.add(printer)
                    trySend(discoveredDevices.toList())
                }

                if (normalizedType == PrinterConnectionType.USB || normalizedType == PrinterConnectionType.BLUETOOTH) {
                    val queues = portService.discoverPrintQueuePrinters(normalizedType)
                    queues.forEach { printer ->
                        discoveredDevices.add(printer)
                        trySend(discoveredDevices.toList())
                    }
                    onLog("JVM: Found ${queues.size} OS printer queues for $normalizedType")
                }

                if (normalizedType == PrinterConnectionType.BLUETOOTH && ports.isEmpty()) {
                    onLog("JVM: No Bluetooth serial port detected. Pair the printer as Bluetooth Classic/SPP, then use the OS assigned COM/rfcomm port.")
                }
                onLog("JVM: Found ${portService.listSerialPorts().size} total serial ports")
            }
        }

        awaitClose {
            // Cleanup if needed
        }
    }.flowOn(Dispatchers.IO)

}
