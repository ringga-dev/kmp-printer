package ngga.ring.printer.usecase

import kotlinx.coroutines.flow.Flow
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.repository.PrinterRepository

class DiscoverPrintersUseCase(
    private val repository: PrinterRepository
) {
    operator fun invoke(
        type: String,
        config: DiscoveryConfig = DiscoveryConfig(),
        onLog: (String) -> Unit = {}
    ): Flow<List<DiscoveredPrinter>> = repository.discover(type, config, onLog)
}
