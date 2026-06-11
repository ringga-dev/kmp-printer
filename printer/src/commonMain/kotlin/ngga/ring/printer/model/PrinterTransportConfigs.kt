package ngga.ring.printer.model

sealed interface PrinterTransportConfig {
    val name: String
    fun toPrinterConfig(): PrinterConfig
}

data class NetworkPrinterConfig(
    override val name: String,
    val host: String,
    val port: Int = 9100,
    val timeoutMs: Int = 5000
) : PrinterTransportConfig {
    override fun toPrinterConfig(): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.NETWORK,
            address = host,
            port = port,
            connectionTimeoutMs = timeoutMs
        )
    }
}

data class SerialPrinterConfig(
    override val name: String,
    val portName: String,
    val baudRate: Int = 9600,
    val readTimeoutMs: Int = 2000
) : PrinterTransportConfig {
    override fun toPrinterConfig(): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.SERIAL,
            address = portName,
            baudRate = baudRate,
            readTimeoutMs = readTimeoutMs
        )
    }
}

data class UsbPrinterConfig(
    override val name: String,
    val address: String
) : PrinterTransportConfig {
    override fun toPrinterConfig(): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.USB,
            address = address
        )
    }
}

data class BluetoothClassicPrinterConfig(
    override val name: String,
    val osPortQueueOrMac: String,
    val baudRate: Int = 9600,
    val linuxAutoBind: Boolean = true,
    val linuxRfcommDevice: String = "/dev/rfcomm0"
) : PrinterTransportConfig {
    override fun toPrinterConfig(): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.BLUETOOTH,
            address = osPortQueueOrMac,
            baudRate = baudRate,
            bluetoothClassicAutoBind = linuxAutoBind,
            bluetoothClassicRfcommDevice = linuxRfcommDevice
        )
    }
}

data class BlePrinterConfig(
    override val name: String,
    val address: String,
    val serviceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
    val writeCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb",
    val autoDiscover: Boolean = true,
    val bridgeCommand: String? = null
) : PrinterTransportConfig {
    override fun toPrinterConfig(): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.BLUETOOTH_LE,
            address = address,
            bleServiceUuid = serviceUuid,
            bleWriteCharacteristicUuid = writeCharacteristicUuid,
            bleAutoDiscover = autoDiscover,
            bleBridgeCommand = bridgeCommand
        )
    }
}

data class VirtualPrinterConfig(
    override val name: String = "Virtual Printer"
) : PrinterTransportConfig {
    override fun toPrinterConfig(): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.VIRTUAL
        )
    }
}
