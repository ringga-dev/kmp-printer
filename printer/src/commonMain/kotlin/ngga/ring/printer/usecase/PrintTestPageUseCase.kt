package ngga.ring.printer.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ngga.ring.printer.ReceiptService
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig

class PrintTestPageUseCase(
    private val receiptService: ReceiptService,
    private val printRaw: PrintRawUseCase
) {
    operator fun invoke(config: PrinterConfig): Flow<PrintStatus> = flow {
        emit(PrintStatus.Processing)
        printRaw(config, receiptService.generateTestPrint(config)).collect { status ->
            emit(status)
        }
    }
}
