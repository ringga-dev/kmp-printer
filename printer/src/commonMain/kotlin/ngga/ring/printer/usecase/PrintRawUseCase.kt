package ngga.ring.printer.usecase

import kotlinx.coroutines.flow.Flow
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.repository.PrinterRepository

class PrintRawUseCase(
    private val repository: PrinterRepository
) {
    operator fun invoke(config: PrinterConfig, data: ByteArray): Flow<PrintStatus> {
        return repository.printRaw(config, data)
    }
}
