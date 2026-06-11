package ngga.ring.printer.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.ConnectionState

interface PrinterRepository {
    val connectionState: StateFlow<ConnectionState>

    fun discover(
        type: String,
        config: DiscoveryConfig = DiscoveryConfig(),
        onLog: (String) -> Unit = {}
    ): Flow<List<DiscoveredPrinter>>

    fun printRaw(config: PrinterConfig, data: ByteArray): Flow<PrintStatus>

    suspend fun testConnection(config: PrinterConfig): PrintStatus

    fun monitorStatus(config: PrinterConfig, intervalMs: Long = 2000): Flow<PrinterStatus>

    suspend fun queryStatus(): PrinterStatus

    suspend fun disconnect()
}
