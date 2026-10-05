package ngga.ring.printer.manager

import kotlinx.coroutines.runBlocking
import ngga.ring.printer.model.PrinterConfig
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end tests for [JvmNetworkConnector] against a local fake printer
 * listening on an ephemeral TCP port. No physical printer required.
 */
class JvmNetworkConnectorTest {

    @Test
    fun connectsSendsAndReadsFromFakePrinter() = runBlocking {
        val server = FakePrinterServer()
        server.start()
        try {
            val connector = JvmNetworkConnector()
            val config = PrinterConfig(
                name = "Fake",
                connectionType = "NETWORK",
                address = "127.0.0.1",
                port = server.port,
                connectionTimeoutMs = 2_000,
                readTimeoutMs = 2_000
            )

            assertTrue(connector.connect(config), "connect should succeed")
            assertTrue(connector.isConnected())

            val payload = byteArrayOf(0x1B, 0x40, 0x1D, 0x56, 0x00) + "KMP TEST".toByteArray()
            assertTrue(connector.sendData(payload), "sendData should succeed")
            server.awaitPayload(payload.size, timeoutMs = 5_000)

            // Fake printer answers with a single status byte.
            assertContentEquals(byteArrayOf(0x20), connector.readData(1, timeout = 5_000))

            connector.disconnect()
            assertFalse(connector.isConnected(), "isConnected must be false after disconnect")
        } finally {
            server.stop()
        }
    }

    @Test
    fun chunkedSendingReassemblesPayloadOnServer() = runBlocking {
        val server = FakePrinterServer()
        server.start()
        try {
            val connector = JvmNetworkConnector()
            val config = PrinterConfig(
                name = "Fake",
                connectionType = "NETWORK",
                address = "127.0.0.1",
                port = server.port,
                connectionTimeoutMs = 2_000,
                readTimeoutMs = 2_000,
                sendChunkSize = 64,
                sendChunkDelayMs = 0
            )

            assertTrue(connector.connect(config))

            val payload = ByteArray(512) { (it % 251).toByte() }
            assertTrue(connector.sendData(payload), "chunked send should succeed")
            val received = server.awaitPayload(payload.size, timeoutMs = 10_000)

            assertContentEquals(payload, received, "chunked bytes must arrive unchanged")
            connector.disconnect()
        } finally {
            server.stop()
        }
    }

    @Test
    fun connectFailsWhenNothingIsListening() = runBlocking {
        // Reserve then release a port so nothing answers on it.
        val closedPort = ServerSocket(0).use { it.localPort }

        val connector = JvmNetworkConnector()
        val config = PrinterConfig(
            name = "Missing",
            connectionType = "NETWORK",
            address = "127.0.0.1",
            port = closedPort,
            connectionTimeoutMs = 500
        )

        assertFalse(connector.connect(config), "connect must fail on closed port")
        assertFalse(connector.isConnected())
        assertFalse(connector.sendData(byteArrayOf(0x1B)), "send must fail when not connected")
        assertNull(connector.readData(4, timeout = 100), "read must return null when not connected")
    }

    @Test
    fun readReturnsNullWhenPrinterIsSilent() = runBlocking {
        val server = FakePrinterServer(answerPayload = null)
        server.start()
        try {
            val connector = JvmNetworkConnector()
            val config = PrinterConfig(
                name = "Silent",
                connectionType = "NETWORK",
                address = "127.0.0.1",
                port = server.port,
                connectionTimeoutMs = 2_000,
                readTimeoutMs = 300
            )

            assertTrue(connector.connect(config))
            assertNull(connector.readData(4, timeout = 300), "read must time out to null")
            connector.disconnect()
        } finally {
            server.stop()
        }
    }

    @Test
    fun disconnectIsIdempotent() = runBlocking {
        val server = FakePrinterServer()
        server.start()
        try {
            val connector = JvmNetworkConnector()
            val config = PrinterConfig(
                name = "Fake",
                connectionType = "NETWORK",
                address = "127.0.0.1",
                port = server.port
            )
            assertTrue(connector.connect(config))
            connector.disconnect()
            connector.disconnect()
            assertFalse(connector.isConnected())
        } finally {
            server.stop()
        }
    }
}

/** Minimal TCP server that behaves like an ESC/POS printer on port 9100. */
private class FakePrinterServer(private val answerPayload: ByteArray? = byteArrayOf(0x20)) {

    private val serverSocket = ServerSocket(0)
    private val received = ByteArrayOutputStream()
    private val accepted = CopyOnWriteArrayList<Thread>()
    @Volatile private var running = true

    val port: Int get() = serverSocket.localPort

    fun start() {
        thread(isDaemon = true, name = "fake-printer-accept") {
            while (running) {
                try {
                    val client = serverSocket.accept()
                    accepted += thread(isDaemon = true, name = "fake-printer-client") {
                        client.getInputStream().use { input ->
                            val buffer = ByteArray(4096)
                            while (running) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                synchronized(received) { received.write(buffer, 0, read) }
                                answerPayload?.let { client.getOutputStream().apply { write(it); flush() } }
                            }
                        }
                    }
                } catch (_: Exception) {
                    if (!running) return@thread
                }
            }
        }
    }

    /** Waits until [expected] bytes have been received and returns them. */
    fun awaitPayload(expected: Int, timeoutMs: Long): ByteArray {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val snapshot = synchronized(received) { received.toByteArray() }
            if (snapshot.size >= expected) return snapshot.copyOf(expected)
            Thread.sleep(20)
        }
        val snapshot = synchronized(received) { received.toByteArray() }
        assertTrue(
            snapshot.size >= expected,
            "expected $expected bytes but server received ${snapshot.size}"
        )
        return assertNotNull(snapshot.copyOfRange(0, expected).takeIf { snapshot.size >= expected })
    }

    fun stop() {
        running = false
        try { serverSocket.close() } catch (_: Exception) { }
        accepted.forEach { it.interrupt() }
    }
}