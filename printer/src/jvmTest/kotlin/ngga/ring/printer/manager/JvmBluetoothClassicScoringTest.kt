package ngga.ring.printer.manager

import kotlin.test.Test
import kotlin.test.assertTrue

class JvmBluetoothClassicScoringTest {
    @Test
    fun ranksWindowsBluetoothComPortsHighly() {
        val port = JvmSerialPortInfo(
            name = "Standard Serial over Bluetooth link",
            address = "COM7",
            descriptiveName = "Standard Serial over Bluetooth link",
            looksLikePrinter = false,
            looksLikeBluetooth = true
        )

        assertTrue(port.scoreBluetoothClassic(JvmOperatingSystem.WINDOWS) >= 80)
    }

    @Test
    fun ranksLinuxRfcommPortsHighly() {
        val port = JvmSerialPortInfo(
            name = "rfcomm0",
            address = "/dev/rfcomm0",
            descriptiveName = "rfcomm0",
            looksLikePrinter = false,
            looksLikeBluetooth = true
        )

        assertTrue(port.scoreBluetoothClassic(JvmOperatingSystem.LINUX) >= 75)
    }

    @Test
    fun ranksMacosOutgoingBluetoothPortsHighly() {
        val port = JvmSerialPortInfo(
            name = "Bluetooth-Incoming-Port",
            address = "/dev/cu.Bluetooth-Incoming-Port",
            descriptiveName = "Bluetooth-Incoming-Port",
            looksLikePrinter = false,
            looksLikeBluetooth = true
        )

        assertTrue(port.scoreBluetoothClassic(JvmOperatingSystem.MACOS) >= 75)
    }
}
