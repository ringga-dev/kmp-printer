package ngga.ring.printer.manager

import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WebBluetoothConnector : BasePrinterConnector() {
    private var device: dynamic = null
    private var characteristic: dynamic = null

    override suspend fun connect(config: PrinterConfig): Boolean = suspendCancellableCoroutine { cont ->
        try {
            configureFlowControl(config)
            val nav = kotlinx.browser.window.navigator.asDynamic()
            nav.bluetooth.requestDevice(js("({ filters: [{ services: ['0000ff00-0000-1000-8000-00805f9b34fb'] }, { services: [0xff00] }, { namePrefix: 'Printer' }, { namePrefix: 'MTP' }] })"))
                .then { d ->
                    device = d
                    d.gatt.connect()
                }
                .then { server ->
                    server.getPrimaryService(0xff00)
                }
                .then { service ->
                    service.getCharacteristic(0xff01)
                }
                .then { char ->
                    characteristic = char
                    cont.resume(true)
                }
                .catch { err ->
                    cont.resume(false)
                }
        } catch (e: Exception) {
            cont.resume(false)
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = suspendCancellableCoroutine { cont ->
        val char = characteristic ?: run { cont.resume(false); return@suspendCancellableCoroutine }
        try {
            char.writeValue(data.toTypedArray().asDynamic())
                .then { cont.resume(true) }
                .catch { cont.resume(false) }
        } catch (e: Exception) {
            cont.resume(false)
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null
    override suspend fun disconnect() {
        device?.gatt?.disconnect()
        device = null
        characteristic = null
    }
    override fun isConnected(): Boolean = device != null
}

class WebUsbConnector : BasePrinterConnector() {
    private var device: dynamic = null

    override suspend fun connect(config: PrinterConfig): Boolean = suspendCancellableCoroutine { cont ->
        try {
            configureFlowControl(config)
            val nav = kotlinx.browser.window.navigator.asDynamic()
            nav.usb.requestDevice(js("({ filters: [{ classCode: 7 }] })"))
                .then { d ->
                    device = d
                    d.open()
                }
                .then { 
                    device.selectConfiguration(1)
                }
                .then { 
                    device.claimInterface(0)
                }
                .then { 
                    cont.resume(true)
                }
                .catch { 
                    cont.resume(false)
                }
        } catch (e: Exception) {
            cont.resume(false)
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = suspendCancellableCoroutine { cont ->
        val d = device ?: run { cont.resume(false); return@suspendCancellableCoroutine }
        try {
            // Usually endpoint 1 or 2 is OUT for printers
            d.transferOut(1, data.toTypedArray().asDynamic())
                .then { cont.resume(true) }
                .catch { cont.resume(false) }
        } catch (e: Exception) {
            cont.resume(false)
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null
    override suspend fun disconnect() {
        device?.close()
        device = null
    }
    override fun isConnected(): Boolean = device != null
}

actual class PrinterConnectorFactory : PrinterConnectorProvider {
    actual constructor()
    actual override fun create(config: PrinterConfig): PrinterConnector {
        return when (PrinterConnectionType.normalize(config.connectionType)) {
            PrinterConnectionType.BLUETOOTH, PrinterConnectionType.BLUETOOTH_LE -> WebBluetoothConnector()
            PrinterConnectionType.USB -> WebUsbConnector()
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
    ): Flow<List<DiscoveredPrinter>> = flow {
        // Web discovery usually requires user interaction (popup)
        // so it's handled differently than scanning.
        emit(emptyList())
    }
}
