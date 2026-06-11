package ngga.ring.printer.manager

import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.*

/**
 * WASM Implementation of PrinterConnectorFactory.
 * Provides a stable foundation for Web hardware printing.
 */
actual class PrinterConnectorFactory actual constructor() : PrinterConnectorProvider {
    actual override fun create(config: PrinterConfig): PrinterConnector {
        return when (PrinterConnectionType.normalize(config.connectionType)) {
            PrinterConnectionType.VIRTUAL -> VirtualPrinterConnector()
            else -> object : BasePrinterConnector() {
                override suspend fun connect(config: PrinterConfig) = false
                override suspend fun sendRawData(data: ByteArray) = false
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
        val devices = mutableListOf<DiscoveredPrinter>()
        if (config.showVirtualDevices) {
            devices.add(DiscoveredPrinter("[VIRTUAL] Wasm $type Printer", PrinterConnectionType.VIRTUAL, "WASM-VIRTUAL-001"))
        }
        onLog("Web hardware discovery requires user gesture and external JS bridge.")
        emit(devices)
    }
}
