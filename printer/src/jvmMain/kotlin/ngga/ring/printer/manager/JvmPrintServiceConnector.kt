package ngga.ring.printer.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.PrinterConfig
import java.io.ByteArrayInputStream
import javax.print.DocFlavor
import javax.print.DocPrintJob
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc

data class JvmPrintQueueInfo(
    val name: String,
    val looksLikePrinter: Boolean,
    val looksLikeUsb: Boolean,
    val looksLikeBluetooth: Boolean
)

class JvmPrintQueueService {
    fun listQueues(): List<JvmPrintQueueInfo> {
        return PrintServiceLookup.lookupPrintServices(null, null).map { service ->
            val lowerName = service.name.lowercase()
            JvmPrintQueueInfo(
                name = service.name,
                looksLikePrinter = lowerName.contains("printer") ||
                    lowerName.contains("thermal") ||
                    lowerName.contains("pos") ||
                    lowerName.contains("esc"),
                looksLikeUsb = lowerName.contains("usb"),
                looksLikeBluetooth = lowerName.contains("bluetooth") ||
                    lowerName.contains("bt") ||
                    lowerName.contains("bth")
            )
        }
    }

    fun findQueue(name: String?): PrintService? {
        val services = PrintServiceLookup.lookupPrintServices(null, null)
        if (name.isNullOrBlank()) {
            return PrintServiceLookup.lookupDefaultPrintService() ?: services.firstOrNull()
        }

        return services.firstOrNull { service ->
            service.name.equals(name, ignoreCase = true)
        }
    }
}

/**
 * Sends ESC/POS bytes through an OS printer queue.
 *
 * This is useful for USB/Bluetooth printers installed as normal desktop
 * printers. It is not as direct as raw USB, but it improves JVM support without
 * adding native dependencies.
 */
class JvmPrintServiceConnector(
    private val queueService: JvmPrintQueueService = JvmPrintQueueService()
) : BasePrinterConnector() {
    private var printService: PrintService? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        configureFlowControl(config)
        printService = queueService.findQueue(config.address)
        printService != null
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = printService ?: return@withContext false
            val job: DocPrintJob = service.createPrintJob()
            val flavor = chooseFlavor(service) ?: return@withContext false
            val payload: Any = when (flavor) {
                DocFlavor.BYTE_ARRAY.AUTOSENSE -> data
                else -> ByteArrayInputStream(data)
            }
            val doc = SimpleDoc(payload, flavor, null)
            job.print(doc, null)
            true
        } catch (e: Exception) {
            println("PrinterJVM: Print service send failed: ${e.message}")
            false
        }
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() {
        printService = null
    }

    override fun isConnected(): Boolean = printService != null

    private fun chooseFlavor(service: PrintService): DocFlavor? {
        val candidates = listOf(
            DocFlavor.BYTE_ARRAY.AUTOSENSE,
            DocFlavor.INPUT_STREAM.AUTOSENSE,
            DocFlavor.BYTE_ARRAY("application/octet-stream"),
            DocFlavor.INPUT_STREAM("application/octet-stream")
        )
        return candidates.firstOrNull { service.isDocFlavorSupported(it) }
    }
}
