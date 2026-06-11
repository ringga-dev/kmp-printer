package ngga.ring.printer

import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmPrinterHardwareIntegrationTest {
    @Test
    fun validatesConfiguredHardwarePrinter() = runBlocking {
        val config = integrationConfig() ?: return@runBlocking
        val printer = KmpPrinter()

        val connection = printer.testConnection(config)
        assertTrue(connection is PrintStatus.Success, "Connection test failed: $connection")

        val payload = printer.newCommandBuilder(config)
            .initialize()
            .line("KMP PRINTER INTEGRATION TEST")
            .line("Transport: ${config.connectionType}")
            .feed(2)
            .build()

        val printStatus = printer.printRaw(config, payload).last()
        assertTrue(printStatus is PrintStatus.Success, "Print test failed: $printStatus")

        val status = printer.queryStatus()
        assertTrue(status.isOnline || !status.isStatusSupported, "Printer reported offline: ${status.message}")
        assertFalse(status.isError && status.isStatusSupported, "Printer status error: ${status.message}")

        printer.disconnect()
    }

    private fun integrationConfig(): PrinterConfig? {
        val type = env("PRINTER_IT_TYPE") ?: return null
        val address = env("PRINTER_IT_ADDRESS") ?: return null
        return PrinterConfig(
            name = env("PRINTER_IT_NAME") ?: "Integration Printer",
            connectionType = type,
            address = address,
            port = env("PRINTER_IT_PORT")?.toIntOrNull() ?: 9100,
            baudRate = env("PRINTER_IT_BAUD")?.toIntOrNull() ?: 9600,
            bluetoothClassicAutoBind = env("PRINTER_IT_BT_AUTOBIND")?.toBooleanStrictOrNull() ?: true,
            bluetoothClassicRfcommDevice = env("PRINTER_IT_RFCOMM") ?: "/dev/rfcomm0",
            bleServiceUuid = env("PRINTER_IT_BLE_SERVICE") ?: "0000ff00-0000-1000-8000-00805f9b34fb",
            bleWriteCharacteristicUuid = env("PRINTER_IT_BLE_CHARACTERISTIC") ?: "0000ff01-0000-1000-8000-00805f9b34fb",
            bleBridgeCommand = env("PRINTER_IT_BLE_BRIDGE")
        )
    }

    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
}
