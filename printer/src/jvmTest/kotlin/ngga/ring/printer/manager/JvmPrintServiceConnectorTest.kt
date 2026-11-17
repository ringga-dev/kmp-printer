package ngga.ring.printer.manager

import kotlinx.coroutines.runBlocking
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import java.lang.reflect.Proxy
import javax.print.Doc
import javax.print.DocFlavor
import javax.print.DocPrintJob
import javax.print.PrintService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmPrintServiceConnectorTest {

    private class FakeDocPrintJob {
        val printedDocs = mutableListOf<Doc>()

        val printJob: DocPrintJob = Proxy.newProxyInstance(
            DocPrintJob::class.java.classLoader,
            arrayOf(DocPrintJob::class.java)
        ) { _, method, args ->
            when (method.name) {
                "print" -> {
                    val doc = args[0] as Doc
                    printedDocs.add(doc)
                    null
                }
                "getPrintService" -> null
                "equals" -> false
                "hashCode" -> 0
                "toString" -> "FakeDocPrintJob"
                else -> null
            }
        } as DocPrintJob
    }

    private class FakePrintService(
        val name: String = "TestReceiptPrinter",
        private val supportedFlavor: DocFlavor = DocFlavor.BYTE_ARRAY.AUTOSENSE
    ) {
        val printJobsCreated = mutableListOf<FakeDocPrintJob>()

        val printService: PrintService = Proxy.newProxyInstance(
            PrintService::class.java.classLoader,
            arrayOf(PrintService::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getName" -> name
                "isDocFlavorSupported" -> {
                    val flavor = args[0] as? DocFlavor
                    flavor == supportedFlavor
                }
                "createPrintJob" -> {
                    val job = FakeDocPrintJob()
                    printJobsCreated.add(job)
                    job.printJob
                }
                "equals" -> false
                "hashCode" -> 0
                "toString" -> "FakePrintService($name)"
                else -> null
            }
        } as PrintService
    }

    private class FakePrintQueueService(
        private val fakeService: PrintService
    ) : JvmPrintQueueService() {
        override fun findQueue(name: String?): PrintService {
            return fakeService
        }
    }

    @Test
    fun sendDataDispatchesWholePayloadAsSinglePrintJobWithoutChunking() = runBlocking {
        val fakePrintService = FakePrintService()
        val queueService = FakePrintQueueService(fakePrintService.printService)
        val connector = JvmPrintServiceConnector(queueService)

        val config = PrinterConfig(
            name = "TestPrinter",
            connectionType = PrinterConnectionType.USB,
            address = "TestReceiptPrinter",
            sendChunkSize = 512,
            sendChunkDelayMs = 20
        )

        val connected = connector.connect(config)
        assertTrue(connected, "Connector should connect successfully")

        // 2048 bytes would be split into 4 chunks by BasePrinterConnector default chunking
        val payload = ByteArray(2048) { it.toByte() }
        val sent = connector.sendData(payload)
        assertTrue(sent, "sendData should succeed")

        // Must be sent as 1 single print job to prevent OS spooler fragmentation
        assertEquals(
            1,
            fakePrintService.printJobsCreated.size,
            "Expected exactly 1 print job to be created, but got ${fakePrintService.printJobsCreated.size}"
        )

        val createdJob = fakePrintService.printJobsCreated.single()
        assertEquals(
            1,
            createdJob.printedDocs.size,
            "Expected print job to print exactly 1 document"
        )

        val printedDoc = createdJob.printedDocs.single()
        val printData = printedDoc.printData as ByteArray
        assertEquals(2048, printData.size, "Payload size must match exactly 2048 bytes")
        for (i in 0 until 2048) {
            assertEquals(i.toByte(), printData[i])
        }
    }
}
