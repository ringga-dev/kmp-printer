package ngga.ring.printer.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig

class PrintReceiptUseCase(
    private val printRaw: PrintRawUseCase
) {
    operator fun invoke(config: PrinterConfig, data: ByteArray): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        printRaw(config, data).collect { status ->
            emit(status)
        }
    }
}
