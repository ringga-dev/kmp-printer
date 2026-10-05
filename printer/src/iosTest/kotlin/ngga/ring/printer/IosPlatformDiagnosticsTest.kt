package ngga.ring.printer

import ngga.ring.printer.manager.PrinterConnectorFactory
import ngga.ring.printer.manager.PrinterPlatformDiagnostics
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterSerialFailureReason
import ngga.ring.printer.model.PrinterUsbFailureReason
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS-specific reporting and transport routing.
 *
 * Requires a macOS host: `./gradlew :printer:iosSimulatorArm64Test`. These
 * assertions are written against the platform contract, so a change that breaks
 * iOS support fails in CI rather than on a user's device.
 */
class IosPlatformDiagnosticsTest {

    private val diagnostics = PrinterPlatformDiagnostics()
    private val config = PrinterConfig(name = "iOS Printer", connectionType = "USB")

    @Test
    fun reportIdentifiesApplePlatforms() {
        val report = diagnostics.getReport()

        assertTrue(
            report.platformName.contains("iOS", ignoreCase = true) ||
                report.platformName.contains("Apple", ignoreCase = true),
            "unexpected platform name: ${report.platformName}",
        )
    }

    @Test
    fun networkAndBleAreSupported() {
        val report = diagnostics.getReport()

        for (type in listOf(PrinterConnectionType.NETWORK, PrinterConnectionType.BLUETOOTH_LE)) {
            val capability = assertNotNull(report.capabilityFor(type), "missing $type")
            assertTrue(capability.isSupported, "$type must be supported on iOS")
        }
    }

    @Test
    fun usbAndSerialAreReportedAsUnsupported() {
        val report = diagnostics.getReport()

        for (type in listOf(PrinterConnectionType.USB, PrinterConnectionType.SERIAL)) {
            val capability = assertNotNull(report.capabilityFor(type), "missing $type")
            assertFalse(capability.isSupported, "$type is not available on iOS")
        }
    }

    @Test
    fun usbDiagnosticExplainsThePlatformLimit() {
        val result = diagnostics.diagnoseUsb(config)

        assertFalse(result.deviceFound)
        assertTrue(
            result.failureReason == PrinterUsbFailureReason.UNSUPPORTED_PLATFORM ||
                result.failureReason == PrinterUsbFailureReason.NONE,
            "unexpected reason: ${result.failureReason}",
        )
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun serialDiagnosticReportsNoPorts() {
        val result = diagnostics.diagnoseSerial(config)

        assertFalse(result.portFound)
        assertTrue(
            result.failureReason == PrinterSerialFailureReason.UNSUPPORTED_PLATFORM ||
                result.failureReason == PrinterSerialFailureReason.PORT_NOT_FOUND,
            "unexpected reason: ${result.failureReason}",
        )
    }

    @Test
    fun everyTransportHasATroubleshootingHint() {
        for (type in listOf(
            PrinterConnectionType.USB,
            PrinterConnectionType.NETWORK,
            PrinterConnectionType.BLUETOOTH_LE,
            PrinterConnectionType.SERIAL,
        )) {
            assertTrue(
                diagnostics.troubleshootingHint(type).isNotBlank(),
                "empty hint for $type",
            )
        }
    }

    @Test
    fun factoryResolvesEveryTransportWithoutThrowing() {
        // Resolution only: connecting needs real hardware and a running app.
        val factory = PrinterConnectorFactory()

        for (type in listOf(
            PrinterConnectionType.NETWORK,
            PrinterConnectionType.BLUETOOTH_LE,
            PrinterConnectionType.USB,
            PrinterConnectionType.SERIAL,
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.VIRTUAL,
        )) {
            val connector = factory.create(
                PrinterConfig(name = "Printer", connectionType = type, port = 9100),
            )
            assertFalse(connector.isConnected(), "$type must start disconnected")
        }
    }
}