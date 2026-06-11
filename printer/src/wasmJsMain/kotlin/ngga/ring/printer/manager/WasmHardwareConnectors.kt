@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.manager.WasmJSBridge.awaitPromise

class WasmBluetoothConnector : BasePrinterConnector() {
    private var characteristic: JsAny? = null

    override suspend fun connect(config: PrinterConfig): Boolean {
        return try {
            configureFlowControl(config)
            val options = WasmJSBridge.createBleOptions()
            val device = awaitPromise(WasmJSBridge.requestBluetoothDevice(options)) ?: return false
            // Connection logic here via JS Bridge
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean {
        return try {
            // Send logic via JS Bridge
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ByteArray.toJsArray(): JsAny = error("Not implemented")
    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null
    override suspend fun disconnect() { characteristic = null }
    override fun isConnected(): Boolean = characteristic != null
}

class WasmUsbConnector : BasePrinterConnector() {
    private var device: JsAny? = null

    override suspend fun connect(config: PrinterConfig): Boolean {
        return try {
            configureFlowControl(config)
            val options = WasmJSBridge.createUsbOptions()
            val d = awaitPromise(WasmJSBridge.requestUsbDevice(options)) ?: return false
            device = d
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean {
        return try {
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ByteArray.toJsArray(): JsAny = error("Not implemented")
    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null
    override suspend fun disconnect() { device = null }
    override fun isConnected(): Boolean = device != null
}

class WasmSerialConnector : BasePrinterConnector() {
    private var port: JsAny? = null

    override suspend fun connect(config: PrinterConfig): Boolean {
        return try {
            configureFlowControl(config)
            val p = awaitPromise(WasmJSBridge.requestSerialPort()) ?: return false
            port = p
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendRawData(data: ByteArray): Boolean {
        return try {
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ByteArray.toJsArray(): JsAny = error("Not implemented")
    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null
    override suspend fun disconnect() { port = null }
    override fun isConnected(): Boolean = port != null
}
