package ngga.ring.printer

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import ngga.ring.printer.manager.PrinterConnector
import ngga.ring.printer.manager.PrinterConnectorProvider
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnection
import ngga.ring.printer.repository.DefaultPrinterRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class DefaultPrinterRepositoryTest {
    @Test
    fun retriesConnectionBeforeSending() = runTest {
        val connector = FakeConnector(connectResults = ArrayDeque(listOf(false, true)))
        val repository = DefaultPrinterRepository(FakeConnectorProvider(connector))

        val result = repository.printRaw(
            config(connectAttempts = 2),
            byteArrayOf(1, 2, 3)
        ).toList()

        assertEquals(2, connector.connectCalls)
        assertEquals(1, connector.sendCalls)
        assertEquals(PrintStatus.Success, result.last())
    }

    @Test
    fun reconnectsWhenSendFails() = runTest {
        val connector = FakeConnector(sendResults = ArrayDeque(listOf(false, true)))
        val repository = DefaultPrinterRepository(FakeConnectorProvider(connector))

        val result = repository.printRaw(
            config(sendAttempts = 2, reconnectOnSendFailure = true),
            byteArrayOf(1)
        ).toList()

        assertEquals(2, connector.connectCalls)
        assertEquals(1, connector.disconnectCalls)
        assertEquals(2, connector.sendCalls)
        assertEquals(PrintStatus.Success, result.last())
    }

    @Test
    fun reusesConnectorForSameEndpoint() = runTest {
        val provider = FakeConnectorProvider(FakeConnector())
        val repository = DefaultPrinterRepository(provider)
        val printer = config(address = "192.168.1.50")

        repository.printRaw(printer, byteArrayOf(1)).toList()
        repository.printRaw(printer.copy(name = "Same endpoint, different name"), byteArrayOf(2)).toList()

        assertEquals(1, provider.createCalls)
        assertSame(provider.connector, provider.createdConnectors.single())
        assertEquals(1, provider.connector.connectCalls)
        assertEquals(2, provider.connector.sendCalls)
    }

    @Test
    fun emitsSendErrorAfterRetryExhausted() = runTest {
        val connector = FakeConnector(sendResults = ArrayDeque(listOf(false, false)))
        val repository = DefaultPrinterRepository(FakeConnectorProvider(connector))

        val result = repository.printRaw(
            config(sendAttempts = 2, reconnectOnSendFailure = false),
            byteArrayOf(1)
        ).toList()

        assertEquals(2, connector.sendCalls)
        assertIs<PrintStatus.Error>(result.last())
    }

    @Test
    fun serializesConcurrentPrintsOnSharedRepository() = runTest {
        val connector = FakeConnector(sendDelayMs = 50)
        val repository = DefaultPrinterRepository(FakeConnectorProvider(connector))

        (1..3)
            .map { index ->
                async {
                    repository.printRaw(config(), byteArrayOf(index.toByte())).toList()
                }
            }
            .awaitAll()

        assertEquals(3, connector.sendCalls)
        assertEquals(1, connector.maxConcurrentSends)
    }

    @Test
    fun monitorStatusConnectsRequestedConfig() = runTest {
        val provider = FakeConnectorProvider(FakeConnector())
        val repository = DefaultPrinterRepository(provider)

        repository.printRaw(config(address = "192.168.1.50"), byteArrayOf(1)).toList()
        repository.monitorStatus(config(address = "192.168.1.51"), intervalMs = 1).take(1).toList()

        assertEquals(2, provider.createCalls)
    }

    private fun config(
        address: String = "192.168.1.10",
        connectAttempts: Int = 1,
        sendAttempts: Int = 1,
        reconnectOnSendFailure: Boolean = true
    ) = PrinterConfig(
        name = "Test Printer",
        connection = PrinterConnection.NETWORK,
        address = address,
        connectAttempts = connectAttempts,
        sendAttempts = sendAttempts,
        reconnectOnSendFailure = reconnectOnSendFailure,
        retryDelayMs = 0
    )
}

private class FakeConnectorProvider(
    val connector: FakeConnector
) : PrinterConnectorProvider {
    var createCalls = 0
    val createdConnectors = mutableListOf<PrinterConnector>()

    override fun create(config: PrinterConfig): PrinterConnector {
        createCalls++
        createdConnectors.add(connector)
        return connector
    }

    override fun discovery(
        type: String,
        config: DiscoveryConfig,
        onLog: (String) -> Unit
    ): Flow<List<DiscoveredPrinter>> = flowOf(emptyList())
}

private class FakeConnector(
    private val connectResults: ArrayDeque<Boolean> = ArrayDeque(listOf(true)),
    private val sendResults: ArrayDeque<Boolean> = ArrayDeque(listOf(true)),
    private val sendDelayMs: Long = 0
) : PrinterConnector {
    var connectCalls = 0
    var sendCalls = 0
    var disconnectCalls = 0
    var maxConcurrentSends = 0
    private var activeSends = 0
    private var connected = false

    override suspend fun connect(config: PrinterConfig): Boolean {
        connectCalls++
        val result = connectResults.removeFirstOrNull() ?: connectResults.lastOrNull() ?: true
        connected = result
        return result
    }

    override suspend fun sendData(data: ByteArray): Boolean {
        sendCalls++
        activeSends++
        maxConcurrentSends = maxOf(maxConcurrentSends, activeSends)
        if (sendDelayMs > 0) delay(sendDelayMs)
        activeSends--
        val result = sendResults.removeFirstOrNull() ?: sendResults.lastOrNull() ?: true
        if (!result) connected = false
        return result
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = null

    override suspend fun disconnect() {
        disconnectCalls++
        connected = false
    }

    override fun isConnected(): Boolean = connected
}
