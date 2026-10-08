package ngga.ring.printer.session

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterStatusMonitor
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterErrorCode
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

/**
 * Thread-safe default implementation of [PrinterSession].
 */
class DefaultPrinterSession(
    override val config: PrinterConfig,
    private val connector: PrinterConnector,
    private val statusMonitor: PrinterStatusMonitor = PrinterStatusMonitor()
) : PrinterSession {

    private val mutex = Mutex()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val isConnected: Boolean
        get() = connector.isConnected()

    override suspend fun connect(): Boolean = mutex.withLock {
        if (connector.isConnected()) return true
        _connectionState.value = ConnectionState.Connecting
        val ok = connectWithRetry()
        if (ok) {
            _connectionState.value = ConnectionState.Connected(config.name, config.address)
        } else {
            _connectionState.value = ConnectionState.Error("Failed to connect to ${config.name}")
        }
        return ok
    }

    override suspend fun sendData(data: ByteArray): Boolean = mutex.withLock {
        if (!connector.isConnected() && !connectWithRetry()) {
            return false
        }
        return sendWithRetry(data)
    }

    override suspend fun print(block: ESCPosCommandBuilder.() -> Unit): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        val builder = ESCPosCommandBuilder.fromPrinterConfig(config).initialize()
        builder.block()
        printRaw(builder.build()).collect { status ->
            emit(status)
        }
    }

    override suspend fun printLabel(
        widthMm: Double,
        heightMm: Double,
        block: ngga.ring.printer.util.tspl.TsplCommandBuilder.() -> Unit
    ): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        val builder = ngga.ring.printer.util.tspl.TsplCommandBuilder(widthMm, heightMm)
        builder.block()
        printRaw(builder.build()).collect { status ->
            emit(status)
        }
    }

    override fun printRaw(data: ByteArray): Flow<PrintStatus> = flow {
        mutex.withLock {
            try {
                if (!connector.isConnected()) {
                    emit(PrintStatus.Connecting)
                    _connectionState.value = ConnectionState.Connecting

                    if (!connectWithRetry()) {
                        val message = "Failed to connect to ${config.connectionType} printer (${config.address ?: config.name})"
                        _connectionState.value = ConnectionState.Error(message)
                        emit(PrintStatus.Error(message, PrinterErrorCode.CONNECTION_FAILED))
                        return@flow
                    }
                }

                _connectionState.value = ConnectionState.Connected(config.name, config.address)
                emit(PrintStatus.Sending)

                if (sendWithRetry(data)) {
                    emit(PrintStatus.Success)
                } else {
                    val message = "Failed to send data after ${config.sendAttempts.coerceAtLeast(1)} attempt(s)"
                    _connectionState.value = ConnectionState.Error(message)
                    emit(PrintStatus.Error(message, PrinterErrorCode.SEND_FAILED))
                }
            } catch (e: Exception) {
                val message = e.message ?: "Unknown print error"
                _connectionState.value = ConnectionState.Error(message)
                emit(PrintStatus.Error(message, PrinterErrorCode.UNKNOWN, e::class.simpleName))
            }
        }
    }

    override suspend fun queryStatus(): PrinterStatus = mutex.withLock {
        if (!connector.isConnected()) return PrinterStatus(isOnline = false)
        return statusMonitor.queryStatus(connector)
    }

    override fun monitorStatus(intervalMs: Long): Flow<PrinterStatus> = flow {
        if (!connector.isConnected()) {
            emit(PrinterStatus(isOnline = false))
            return@flow
        }
        statusMonitor.monitor(connector, intervalMs).collect {
            emit(it)
        }
    }

    override suspend fun disconnect(): Unit = mutex.withLock {
        connector.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    private suspend fun connectWithRetry(): Boolean {
        val attempts = config.connectAttempts.coerceAtLeast(1)
        repeat(attempts) { index ->
            if (connector.connect(config)) return true
            if (index < attempts - 1) delay(config.retryDelayMs.coerceAtLeast(0))
        }
        return false
    }

    private suspend fun sendWithRetry(data: ByteArray): Boolean {
        val attempts = config.sendAttempts.coerceAtLeast(1)
        repeat(attempts) { index ->
            if (connector.sendData(data)) return true
            if (config.reconnectOnSendFailure) {
                connector.disconnect()
                if (!connectWithRetry()) return false
            }
            if (index < attempts - 1) delay(config.retryDelayMs.coerceAtLeast(0))
        }
        return false
    }
}
