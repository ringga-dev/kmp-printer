package ngga.ring.printer.model

object PrinterConfigs {
    fun network(
        name: String,
        host: String,
        port: Int = 9100,
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.NETWORK,
            address = host,
            port = port
        ).block()
    }

    fun serial(
        name: String,
        portName: String,
        baudRate: Int = 9600,
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.SERIAL,
            address = portName,
            baudRate = baudRate
        ).block()
    }

    fun usb(
        name: String,
        address: String,
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.USB,
            address = address
        ).block()
    }

    fun bluetoothClassic(
        name: String,
        osPortOrQueue: String,
        baudRate: Int = 9600,
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.BLUETOOTH,
            address = osPortOrQueue,
            baudRate = baudRate
        ).block()
    }

    fun bluetoothClassicLinuxRfcomm(
        name: String,
        macAddress: String,
        rfcommDevice: String = "/dev/rfcomm0",
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.BLUETOOTH,
            address = macAddress,
            bluetoothClassicAutoBind = true,
            bluetoothClassicRfcommDevice = rfcommDevice
        ).block()
    }

    fun ble(
        name: String,
        address: String,
        serviceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
        writeCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb",
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.BLUETOOTH_LE,
            address = address,
            bleServiceUuid = serviceUuid,
            bleWriteCharacteristicUuid = writeCharacteristicUuid
        ).block()
    }

    fun virtual(name: String = "Virtual Printer"): PrinterConfig {
        return PrinterConfig(
            name = name,
            connectionType = PrinterConnectionType.VIRTUAL
        )
    }
}
