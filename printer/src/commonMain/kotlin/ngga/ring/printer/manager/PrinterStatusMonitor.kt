package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterStatus
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Real-time printer status monitor.
 * Sends DLE EOT status queries and parses the printer's response bytes.
 *
 * Supports 4 status types:
 * - Type 1: Printer status (online/offline)
 * - Type 2: Offline cause (cover open, feeding, etc.)
 * - Type 3: Error status (auto-cutter error, unrecoverable error)
 * - Type 4: Paper roll sensor status (paper near end, paper out)
 */
class PrinterStatusMonitor {

    /**
     * Continuously polls the printer for status updates.
     *
     * @param connector An active printer connector.
     * @param intervalMs Polling interval in milliseconds.
     * @return Flow of PrinterStatus updates.
     */
    fun monitor(
        connector: PrinterConnector,
        intervalMs: Long = 2000
    ): Flow<PrinterStatus> = flow {
        while (connector.isConnected()) {
            val status = queryStatus(connector)
            emit(status)
            delay(intervalMs)
        }
    }

    /**
     * Performs a single status query.
     */
    suspend fun queryStatus(connector: PrinterConnector): PrinterStatus {
        try {
            // Send all 4 DLE EOT queries
            val statusBytes = mutableListOf<Byte>()

            for (type in 1..4) {
                val command = byteArrayOf(0x10, 0x04, type.toByte())
                if (!connector.sendData(command)) {
                    return PrinterStatus(
                        isOnline = connector.isConnected(),
                        isError = true,
                        isStatusSupported = false,
                        message = "Transport could not send ESC/POS status query."
                    )
                }
                delay(100) // Wait for printer response

                val response = connector.readData(1, 500)
                if (response != null && response.isNotEmpty()) {
                    statusBytes.add(response[0])
                }
            }

            if (statusBytes.isEmpty()) {
                return PrinterStatus(
                    isOnline = connector.isConnected(),
                    isStatusSupported = false,
                    message = "No status bytes returned. This transport or printer driver may not support readback."
                )
            }

            return parseStatus(statusBytes)
        } catch (e: Exception) {
            return PrinterStatus(
                isOnline = false,
                isError = true,
                isStatusSupported = false,
                message = e.message ?: "Status query failed."
            )
        }
    }

    /**
     * Parses raw status bytes into a PrinterStatus object.
     *
     * DLE EOT Response Byte Format:
     * - Bit 3 of Type 1: 0=Online, 1=Offline
     * - Bit 2 of Type 2: 0=Cover closed, 1=Cover open
     * - Bit 5 of Type 2: 0=Not feeding, 1=Paper feeding
     * - Bit 3 of Type 3: 0=No cutter error, 1=Cutter error
     * - Bit 5 of Type 3: 0=No unrecoverable, 1=Unrecoverable error
     * - Bit 2-3 of Type 4: Paper near end sensor
     * - Bit 5-6 of Type 4: Paper out sensor
     */
    private fun parseStatus(bytes: List<Byte>): PrinterStatus {
        val type1 = bytes.getOrNull(0)?.toInt()?.and(0xFF) ?: 0x12
        val type2 = bytes.getOrNull(1)?.toInt()?.and(0xFF) ?: 0x12
        val type3 = bytes.getOrNull(2)?.toInt()?.and(0xFF) ?: 0x12
        val type4 = bytes.getOrNull(3)?.toInt()?.and(0xFF) ?: 0x12

        val isOnline = (type1 and 0x08) == 0
        val isCoverOpen = (type2 and 0x04) != 0
        val isCutterError = (type3 and 0x08) != 0
        val isUnrecoverableError = (type3 and 0x20) != 0
        val isPaperNearEnd = (type4 and 0x0C) != 0
        val isPaperOut = (type4 and 0x60) != 0

        return PrinterStatus(
            isOnline = isOnline,
            isCoverOpen = isCoverOpen,
            isPaperOut = isPaperOut,
            isPaperNearEnd = isPaperNearEnd,
            isError = isCutterError || isUnrecoverableError,
            rawBytes = bytes.toByteArray()
        )
    }
}
