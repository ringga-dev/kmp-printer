package ngga.ring.printer.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterStatusMonitor
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterErrorCode
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.PrinterLogger
import ngga.ring.printer.util.ReconnectPolicy
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

/**
 * Thread-safe default implementation of [PrinterSession] with exponential backoff retry,
 * auto-reconnect monitoring, and offline print queue.
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

    // Auto-reconnect state
    private val scope = CoroutineScope(SupervisorJob())
    private var reconnectAttempt = 0
    private var healthCheckJob: kotlinx.coroutines.Job? = null
    private var isAutoReconnectEnabled = false

    // Offline print queue — stores pending print jobs while disconnected
    private val pendingQueue = mutableListOf<PendingPrintJob>()
    private val queueMutex = Mutex()

    private data class PendingPrintJob(
        val data: ByteArray,
        val onRetry: ((Boolean) -> Unit)?
    )

    override suspend fun connect(): Boolean = mutex.withLock {
        if (connector.isConnected()) return true
        _connectionState.value = ConnectionState.Connecting
        val ok = connectWithBackoff()
        if (ok) {
            reconnectAttempt = 0
            _connectionState.value = ConnectionState.Connected(config.name, config.address)
            startAutoReconnect()
            flushPendingQueue()
        } else {
            _connectionState.value = ConnectionState.Error("Failed to connect to ${config.name}")
        }
        return ok
    }

    override suspend fun sendData(data: ByteArray): Boolean = mutex.withLock {
        if (!connector.isConnected() && !connectWithBackoff()) {
            return false
        }
        return sendWithBackoff(data)
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
        var shouldQueue = false
        var queueResult: PrintStatus? = null

        mutex.withLock {
            try {
                if (!connector.isConnected()) {
                    // Queue the job if auto-reconnect is enabled
                    if (config.reconnectOnDrop && isAutoReconnectEnabled) {
                        PrinterLogger.info("Session", "Printer offline — queueing print job (${data.size} bytes)")
                        queueMutex.withLock {
                            pendingQueue.add(PendingPrintJob(data, null))
                        }
                        shouldQueue = true
                        queueResult = PrintStatus.Processing
                        return@withLock
                    }

                    emit(PrintStatus.Connecting)
                    _connectionState.value = ConnectionState.Connecting

                    if (!connectWithBackoff()) {
                        val message = "Failed to connect to ${config.connectionType} printer (${config.address ?: config.name})"
                        _connectionState.value = ConnectionState.Error(message)
                        emit(PrintStatus.Error(message, PrinterErrorCode.CONNECTION_FAILED))
                        return@withLock
                    }
                }

                _connectionState.value = ConnectionState.Connected(config.name, config.address)
                emit(PrintStatus.Sending)

                if (sendWithBackoff(data)) {
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

        if (shouldQueue) {
            emit(queueResult!!)
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
        stopAutoReconnect()
        connector.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    // ── Auto-reconnect ──────────────────────────────────────────────────

    /**
     * Enable auto-reconnect: monitor connection health and automatically reconnect
     * when the printer becomes available again.
     */
    fun startAutoReconnect() {
        if (!config.reconnectOnDrop) return
        isAutoReconnectEnabled = true
        reconnectAttempt = 0
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(config.healthCheckIntervalMs.coerceAtLeast(1000))
                checkHealthAndReconnect()
            }
        }
    }

    fun stopAutoReconnect() {
        isAutoReconnectEnabled = false
        healthCheckJob?.cancel()
        healthCheckJob = null
    }

    /**
     * Number of print jobs waiting in the offline queue.
     */
    suspend fun pendingJobCount(): Int = queueMutex.withLock { pendingQueue.size }

    /**
     * Clear all pending print jobs from the offline queue.
     */
    suspend fun clearPendingQueue() {
        queueMutex.withLock { pendingQueue.clear() }
    }

    private suspend fun checkHealthAndReconnect() {
        if (!isAutoReconnectEnabled) return
        if (connector.isConnected()) {
            // Some transports keep a stale isConnected() flag after the remote
            // endpoint disappears. A zero-length write is the safest cross-transport
            // liveness probe; if it throws, the connection is gone.
            try {
                val alive = connector.sendData(ByteArray(0))
                if (alive) {
                    reconnectAttempt = 0 // Reset backoff on healthy connection
                    return
                }
            } catch (_: Exception) {
                // Probe failed — treat as disconnected
            }
        }

        // Connection is down — attempt reconnect with exponential backoff
        PrinterLogger.warn("Session", "Health check failed — attempting reconnect (attempt ${reconnectAttempt + 1})")
        _connectionState.value = ConnectionState.Connecting

        if (connectWithBackoff()) {
            PrinterLogger.info("Session", "Auto-reconnect succeeded")
            reconnectAttempt = 0
            _connectionState.value = ConnectionState.Connected(config.name, config.address)
            flushPendingQueue()
        } else {
            reconnectAttempt++
            val delay = ReconnectPolicy.calculateDelay(
                attempt = reconnectAttempt,
                baseDelayMs = config.retryDelayMs,
                multiplier = config.backoffMultiplier,
                maxDelayMs = config.maxBackoffMs,
                jitterMs = config.backoffJitterMs
            )
            PrinterLogger.warn("Session", "Auto-reconnect failed — next attempt in ${delay}ms")
            _connectionState.value = ConnectionState.Error("Reconnect failed, retrying in ${delay}ms")
            delay(delay)
        }
    }

    private suspend fun flushPendingQueue() {
        val jobs = queueMutex.withLock {
            val copy = pendingQueue.toList()
            pendingQueue.clear()
            copy
        }
        if (jobs.isEmpty()) return

        PrinterLogger.info("Session", "Flushing ${jobs.size} queued print job(s)")
        for ((index, job) in jobs.withIndex()) {
            val success = sendWithBackoff(job.data)
            job.onRetry?.invoke(success)
            if (!success) {
                // Re-queue remaining jobs
                queueMutex.withLock {
                    pendingQueue.addAll(0, jobs.drop(index + 1))
                }
                break
            }
        }
    }

    // ── Retry with exponential backoff ────────────────────────────────────

    private suspend fun connectWithBackoff(): Boolean {
        val attempts = config.connectAttempts.coerceAtLeast(1)
        repeat(attempts) { index ->
            if (connector.connect(config)) return true
            if (index < attempts - 1) {
                val delay = ReconnectPolicy.calculateDelay(
                    attempt = index,
                    baseDelayMs = config.retryDelayMs,
                    multiplier = config.backoffMultiplier,
                    maxDelayMs = config.maxBackoffMs,
                    jitterMs = config.backoffJitterMs
                )
                PrinterLogger.debug("Session", "Connect retry ${index + 1}/$attempts — waiting ${delay}ms")
                delay(delay)
            }
        }
        return false
    }

    private suspend fun sendWithBackoff(data: ByteArray): Boolean {
        val attempts = config.sendAttempts.coerceAtLeast(1)
        repeat(attempts) { index ->
            if (connector.sendData(data)) return true
            if (config.reconnectOnSendFailure) {
                connector.disconnect()
                if (!connectWithBackoff()) return false
            }
            if (index < attempts - 1) {
                val delay = ReconnectPolicy.calculateDelay(
                    attempt = index,
                    baseDelayMs = config.retryDelayMs,
                    multiplier = config.backoffMultiplier,
                    maxDelayMs = config.maxBackoffMs,
                    jitterMs = config.backoffJitterMs
                )
                delay(delay)
            }
        }
        return false
    }

    fun dispose() {
        stopAutoReconnect()
        scope.cancel()
    }
}
