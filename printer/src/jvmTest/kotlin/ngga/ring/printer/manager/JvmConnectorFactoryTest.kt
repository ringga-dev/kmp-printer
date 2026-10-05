package ngga.ring.printer.manager

import kotlinx.coroutines.runBlocking
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies transport routing and lifecycle behaviour of the JVM connector
 * factory. Only NETWORK and VIRTUAL are exercised end-to-end; hardware
 * dependent transports are only checked for correct type resolution.
 */
class JvmConnectorFactoryTest {

    private fun config(type: String, port: Int? = null) = PrinterConfig(
        name = "Test",
        connectionType = type,
        address = "127.0.0.1",
        port = port ?: 9100
    )

    @Test
    fun resolvesEachTransportToItsConnector() {
        val factory = PrinterConnectorFactory()

        assertTrue(factory.create(config(PrinterConnectionType.NETWORK)) is JvmNetworkConnector)
        assertTrue(factory.create(config(PrinterConnectionType.SERIAL)) is JvmSerialConnector)
        assertTrue(factory.create(config(PrinterConnectionType.USB)) is JvmCompositeConnector)
        assertTrue(factory.create(config(PrinterConnectionType.BLUETOOTH)) is JvmBluetoothClassicConnector)
        assertTrue(factory.create(config(PrinterConnectionType.BLUETOOTH_LE)) is JvmCompositeConnector)
        assertTrue(factory.create(config(PrinterConnectionType.VIRTUAL)) is VirtualPrinterConnector)
    }

    @Test
    fun unknownTransportFailsFastWithoutCrashing() = runBlocking {
        val connector = PrinterConnectorFactory().create(config("carrier-pigeon"))

        assertFalse(connector.connect(config("carrier-pigeon")))
        assertFalse(connector.sendData(byteArrayOf(0x1B)))
        assertFalse(connector.isConnected())
    }

    @Test
    fun networkConnectorFromFactoryPrintsToFakePrinter() = runBlocking {
        val server = ServerSocket(0)
        val received = ByteArrayOutputStream()
        val port = server.localPort

        thread(isDaemon = true, name = "factory-fake-printer") {
            try {
                server.accept().getInputStream().use { input ->
                    val buffer = ByteArray(1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        synchronized(received) { received.write(buffer, 0, read) }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { server.close() } catch (_: Exception) { }
            }
        }

        try {
            val connector = PrinterConnectorFactory().create(config(PrinterConnectionType.NETWORK, port))
            assertTrue(connector.connect(config(PrinterConnectionType.NETWORK, port)))

            val payload = byteArrayOf(0x1B, 0x40) + "FACTORY TEST".toByteArray()
            assertTrue(connector.sendData(payload))

            val deadline = System.currentTimeMillis() + 5_000
            var snapshot = ByteArray(0)
            while (System.currentTimeMillis() < deadline) {
                snapshot = synchronized(received) { received.toByteArray() }
                if (snapshot.size >= payload.size) break
                Thread.sleep(20)
            }

            assertContentEquals(payload, snapshot.copyOfRange(0, payload.size))
            connector.disconnect()
            assertFalse(connector.isConnected())
        } finally {
            try { server.close() } catch (_: Exception) { }
        }
    }

    @Test
    fun virtualConnectorAcceptsPrintsWithoutHardware() = runBlocking {
        val connector = PrinterConnectorFactory().create(config(PrinterConnectionType.VIRTUAL))

        assertTrue(connector.connect(config(PrinterConnectionType.VIRTUAL)))
        assertTrue(connector.isConnected())
        assertTrue(connector.sendData(byteArrayOf(0x1B, 0x40, 0x1D, 0x56, 0x00)))

        connector.disconnect()
        assertFalse(connector.isConnected())
    }

    @Test
    fun compositeConnectorFailsCleanlyWhenNoCandidateIsAvailable() = runBlocking {
        val connector = JvmCompositeConnector(
            listOf(JvmUnsupportedNativeConnector("no backend"))
        )
        val target = config(PrinterConnectionType.USB)

        assertFalse(connector.connect(target))
        assertFalse(connector.isConnected())
        assertFalse(connector.sendData(byteArrayOf(0x1B)))
    }
}