package ngga.ring.printer.model

/**
 * Stable connection type names used by the public API.
 *
 * The API keeps [PrinterConfig.connectionType] as a String for binary/source
 * compatibility, but all internal routing should go through this object.
 */
object PrinterConnectionType {
    const val NETWORK = "NETWORK"
    const val USB = "USB"
    const val SERIAL = "SERIAL"
    const val BLUETOOTH = "BLUETOOTH"
    const val BLUETOOTH_LE = "BLUETOOTH_LE"
    const val VIRTUAL = "VIRTUAL"

    fun normalize(type: String): String = when (type.trim().uppercase()) {
        "TCP", "LAN", "WIFI", NETWORK -> NETWORK
        "COM", "TTY", "USB_SERIAL", SERIAL -> SERIAL
        "BT", "BLUETOOTH_CLASSIC", BLUETOOTH -> BLUETOOTH
        "BLE", BLUETOOTH_LE -> BLUETOOTH_LE
        USB -> USB
        VIRTUAL -> VIRTUAL
        else -> type.trim().uppercase()
    }

    fun usesSerialPortOnJvm(type: String): Boolean = when (normalize(type)) {
        SERIAL, USB, BLUETOOTH, BLUETOOTH_LE -> true
        else -> false
    }
}
