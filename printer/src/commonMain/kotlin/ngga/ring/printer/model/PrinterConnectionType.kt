package ngga.ring.printer.model

enum class PrinterConnection(val value: String) {
    NETWORK("NETWORK"),
    USB("USB"),
    SERIAL("SERIAL"),
    BLUETOOTH("BLUETOOTH"),
    BLUETOOTH_LE("BLUETOOTH_LE"),
    BLE("BLUETOOTH_LE"),
    VIRTUAL("VIRTUAL"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun from(type: String): PrinterConnection = when (type.trim().uppercase()) {
            "TCP", "LAN", "WIFI", NETWORK.value -> NETWORK
            "COM", "TTY", "USB_SERIAL", SERIAL.value -> SERIAL
            "BT", "BLUETOOTH_CLASSIC", BLUETOOTH.value -> BLUETOOTH
            "BLE", BLUETOOTH_LE.value -> BLE
            USB.value -> USB
            VIRTUAL.value -> VIRTUAL
            else -> UNKNOWN
        }
    }
}

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

    fun normalize(type: String): String = when (val connection = PrinterConnection.from(type)) {
        PrinterConnection.UNKNOWN -> type.trim().uppercase()
        else -> connection.value
    }

    fun normalize(type: PrinterConnection): String = type.value

    fun parse(type: String): PrinterConnection = PrinterConnection.from(type)

    fun usesSerialPortOnJvm(type: String): Boolean = when (normalize(type)) {
        SERIAL, USB, BLUETOOTH, BLUETOOTH_LE -> true
        else -> false
    }
}
