package ngga.ring.printer.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.manager.PrinterConnectorProvider
import ngga.ring.printer.manager.PrinterStatusMonitor
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterErrorCode
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.ConnectionState

class DefaultPrinterRepository(
    private val connectorFactory: PrinterConnectorProvider = PrinterConnectorFactory(),
    private val statusMonitor: PrinterStatusMonitor = PrinterStatusMonitor()
) : PrinterRepository {
    private var activeConnector: PrinterConnector? = null
    private var activeConfig: PrinterConfig? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override fun discover(
        type: String,
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> {
        return connectorFactory.discovery(PrinterConnectionType.normalize(type), config, onLog)
    }

    override fun printRaw(config: PrinterConfig, data: ByteArray): Flow<PrintStatus> = flow {
        val normalizedConfig = config.normalized()

        try {
            val connector = connectorFor(normalizedConfig)
            if (!connector.isConnected()) {
                emit(PrintStatus.Connecting)
                _connectionState.value = ConnectionState.Connecting

                if (!connectWithRetry(connector, normalizedConfig)) {
                    val message = connectionFailureMessage(normalizedConfig)
                    emit(PrintStatus.Error(message, PrinterErrorCode.CONNECTION_FAILED))
                    _connectionState.value = ConnectionState.Error(message)
                    return@flow
                }
            }

            _connectionState.value = ConnectionState.Connected(normalizedConfig.name, normalizedConfig.address)
            emit(PrintStatus.Sending)

            if (sendWithRetry(connector, normalizedConfig, data)) {
                emit(PrintStatus.Success)
            } else {
                val message = "Failed to send data to printer after ${normalizedConfig.sendAttempts.coerceAtLeast(1)} attempt(s)"
                _connectionState.value = ConnectionState.Error(message)
                emit(PrintStatus.Error(message, PrinterErrorCode.SEND_FAILED))
            }
        } catch (e: Exception) {
            val message = e.message ?: "Unknown print error"
            _connectionState.value = ConnectionState.Error(message)
            emit(PrintStatus.Error(message, PrinterErrorCode.UNKNOWN, e::class.simpleName))
        }
    }

    override suspend fun testConnection(config: PrinterConfig): PrintStatus {
        val normalizedConfig = config.normalized()
        val connector = connectorFactory.create(normalizedConfig)
        return try {
            if (connectWithRetry(connector, normalizedConfig)) {
                connector.disconnect()
                PrintStatus.Success
            } else {
                PrintStatus.Error(connectionFailureMessage(normalizedConfig), PrinterErrorCode.CONNECTION_FAILED)
            }
        } catch (e: Exception) {
            PrintStatus.Error(e.message ?: "Unknown connection test error", PrinterErrorCode.UNKNOWN, e::class.simpleName)
        }
    }

    override fun monitorStatus(config: PrinterConfig, intervalMs: Long): Flow<PrinterStatus> = flow {
        val connector = activeConnector
        if (connector == null || !connector.isConnected()) {
            emit(PrinterStatus(isOnline = false))
            return@flow
        }

        statusMonitor.monitor(connector, intervalMs).collect { status ->
            emit(status)
        }
    }

    override suspend fun queryStatus(): PrinterStatus {
        val connector = activeConnector ?: return PrinterStatus(isOnline = false)
        if (!connector.isConnected()) return PrinterStatus(isOnline = false)
        return statusMonitor.queryStatus(connector)
    }

    override suspend fun disconnect() {
        activeConnector?.disconnect()
        activeConnector = null
        activeConfig = null
        _connectionState.value = ConnectionState.Disconnected
    }

    private suspend fun connectorFor(config: PrinterConfig): PrinterConnector {
        val current = activeConnector
        if (current != null && activeConfig?.sameEndpointAs(config) == true) {
            return current
        }

        current?.disconnect()
        return connectorFactory.create(config).also {
            activeConnector = it
            activeConfig = config
        }
    }

    private suspend fun connectWithRetry(connector: PrinterConnector, config: PrinterConfig): Boolean {
        val attempts = config.connectAttempts.coerceAtLeast(1)
        repeat(attempts) { index ->
            if (connector.connect(config)) return true
            if (index < attempts - 1) delay(config.retryDelayMs.coerceAtLeast(0))
        }
        return false
    }

    private suspend fun sendWithRetry(
        connector: PrinterConnector,
        config: PrinterConfig,
        data: ByteArray
    ): Boolean {
        val attempts = config.sendAttempts.coerceAtLeast(1)
        repeat(attempts) { index ->
            if (connector.sendData(data)) return true

            if (config.reconnectOnSendFailure) {
                connector.disconnect()
                if (!connectWithRetry(connector, config)) return false
            }

            if (index < attempts - 1) delay(config.retryDelayMs.coerceAtLeast(0))
        }
        return false
    }

    private fun connectionFailureMessage(config: PrinterConfig): String {
        val target = config.address ?: config.name
        return "Failed to connect to ${config.connectionType} printer ($target) after ${config.connectAttempts.coerceAtLeast(1)} attempt(s)"
    }

    private fun PrinterConfig.normalized(): PrinterConfig {
        val normalizedType = PrinterConnectionType.normalize(connectionType)
        return if (normalizedType == connectionType) this else copy(connectionType = normalizedType)
    }

    private fun PrinterConfig.sameEndpointAs(other: PrinterConfig): Boolean {
        return PrinterConnectionType.normalize(connectionType) == PrinterConnectionType.normalize(other.connectionType) &&
            address == other.address &&
            port == other.port
    }
}
