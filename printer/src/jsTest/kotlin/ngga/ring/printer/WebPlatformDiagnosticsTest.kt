package ngga.ring.printer

import ngga.ring.printer.manager.PrinterPlatformDiagnostics
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterSerialFailureReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Browser platform reporting.
 *
 * The report is what applications show to users when a transport fails, so the
 * capability flags and hints have to match what the browser can actually do.
 */
class WebPlatformDiagnosticsTest {

    private val diagnostics = PrinterPlatformDiagnostics()
    private val config = PrinterConfig(name = "Web", connectionType = "USB")

    @Test
    fun reportIdentifiesTheBrowserBackend() {
        val report = diagnostics.getReport()

        assertTrue(report.platformName.contains("JS", ignoreCase = true))
        assertTrue(report.notes.isNotEmpty(), "notes should explain the browser constraints")
    }

    @Test
    fun usbAndBluetoothAreReportedAsSupported() {
        val report = diagnostics.getReport()

        for (type in listOf(PrinterConnectionType.USB, PrinterConnectionType.BLUETOOTH_LE)) {
            val capability = assertNotNull(report.capabilityFor(type), "missing $type")
            assertTrue(capability.isSupported, "$type must be reported as supported")
        }
    }

    @Test
    fun serialIsReportedAsSupportedAfterWebSerialWasAdded() {
        val capability = assertNotNull(
            diagnostics.getReport().capabilityFor(PrinterConnectionType.SERIAL),
            "missing SERIAL capability",
        )
        assertTrue(capability.isSupported)
        assertFalse(
            capability.supportsDiscovery,
            "the Web Serial API has no enumeration, so discovery must stay false",
        )
    }

    @Test
    fun networkIsReportedAsUnsupported() {
        val capability = assertNotNull(
            diagnostics.getReport().capabilityFor(PrinterConnectionType.NETWORK),
            "missing NETWORK capability",
        )
        assertFalse(capability.isSupported, "browsers cannot open raw TCP sockets")
    }

    @Test
    fun noBrowserTransportClaimsDiscoverySupport() {
        // Every browser transport requires the user to pick the device, so
        // discovery must never be advertised as available.
        for (type in listOf(
            PrinterConnectionType.USB,
            PrinterConnectionType.BLUETOOTH_LE,
            PrinterConnectionType.SERIAL,
        )) {
            val capability = assertNotNull(diagnostics.getReport().capabilityFor(type))
            assertFalse(capability.supportsDiscovery, "$type must not advertise discovery")
        }
    }

    @Test
    fun everySupportedTransportHasATroubleshootingHint() {
        for (type in listOf(
            PrinterConnectionType.USB,
            PrinterConnectionType.BLUETOOTH_LE,
            PrinterConnectionType.SERIAL,
            PrinterConnectionType.NETWORK,
        )) {
            val hint = diagnostics.troubleshootingHint(type)
            assertTrue(hint.isNotBlank(), "empty hint for $type")
            assertFalse(
                hint.contains("No browser-specific"),
                "$type still falls through to the generic hint",
            )
        }
    }

    @Test
    fun serialDiagnosticExplainsThatThePortMustBeChosen() = run {
        val result = diagnostics.diagnoseSerial(config)

        assertFalse(result.portFound, "the browser never reports a port list")
        assertEquals(PrinterSerialFailureReason.PORT_NOT_FOUND, result.failureReason)
        assertTrue(result.suggestedFix.isNotBlank())
    }

    @Test
    fun usbDiagnosticPointsAtTheWebUsbPrompt() {
        val result = diagnostics.diagnoseUsb(config)

        assertFalse(result.canClaimInterface, "claiming happens in the browser prompt")
        assertTrue(result.message.contains("WebUSB"))
        assertTrue(result.suggestedFix.isNotBlank())
    }

    @Test
    fun bleDiagnosticPointsAtTheWebBluetoothPrompt() {
        val result = diagnostics.diagnoseBle(config)

        assertTrue(result.message.contains("Web Bluetooth"))
        assertTrue(result.suggestedFix.isNotBlank())
    }
}