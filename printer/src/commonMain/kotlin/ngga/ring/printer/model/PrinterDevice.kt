package ngga.ring.printer.model

/**
 * Represents a physical or virtual printer device categorized by its connection type.
 * Provides type-safe access to transport-specific properties and seamless
 * conversion to [PrinterConfig] and [PrinterTransportConfig].
 */
sealed interface PrinterDevice {
    val name: String
    val address: String
    val connectionType: PrinterConnection

    /**
     * Converts this device into a [PrinterConfig] using the specified [profile].
     */
    fun toPrinterConfig(
        profile: PrinterProfile = PrinterProfile.MM58,
        block: PrinterConfig.() -> PrinterConfig = { this }
    ): PrinterConfig

    /**
     * Converts this device into a strongly typed [PrinterTransportConfig].
     */
    fun toTransportConfig(): PrinterTransportConfig

    /**
     * Converts this device to a generic [DiscoveredPrinter] DTO.
     */
    fun toDiscoveredPrinter(): DiscoveredPrinter = DiscoveredPrinter(
        name = name,
        connectionType = connectionType.value,
        address = address,
        port = if (this is Network) port else 9100
    )

    /**
     * Bluetooth Classic printer device (SPP/RFCOMM).
     */
    data class Bluetooth(
        override val name: String,
        val macAddress: String,
        val isBonded: Boolean = false,
        val deviceClass: Int? = null,
        val baudRate: Int = 9600,
        val autoBindLinux: Boolean = true,
        val rfcommDevice: String = "/dev/rfcomm0"
    ) : PrinterDevice {
        override val address: String get() = macAddress
        override val connectionType: PrinterConnection get() = PrinterConnection.BLUETOOTH

        override fun toPrinterConfig(
            profile: PrinterProfile,
            block: PrinterConfig.() -> PrinterConfig
        ): PrinterConfig {
            return PrinterConfig(
                name = name,
                connection = PrinterConnection.BLUETOOTH,
                profile = profile,
                address = macAddress,
                baudRate = baudRate,
                bluetoothClassicAutoBind = autoBindLinux,
                bluetoothClassicRfcommDevice = rfcommDevice
            ).block()
        }

        override fun toTransportConfig(): PrinterTransportConfig {
            return BluetoothClassicPrinterConfig(
                name = name,
                osPortQueueOrMac = macAddress,
                baudRate = baudRate,
                linuxAutoBind = autoBindLinux,
                linuxRfcommDevice = rfcommDevice
            )
        }
    }

    /**
     * Bluetooth Low Energy (BLE / GATT) printer device.
     */
    data class Ble(
        override val name: String,
        val macOrUuid: String,
        val rssi: Int? = null,
        val serviceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
        val writeCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb",
        val autoDiscover: Boolean = true,
        val bridgeCommand: String? = null
    ) : PrinterDevice {
        override val address: String get() = macOrUuid
        override val connectionType: PrinterConnection get() = PrinterConnection.BLE

        override fun toPrinterConfig(
            profile: PrinterProfile,
            block: PrinterConfig.() -> PrinterConfig
        ): PrinterConfig {
            return PrinterConfig(
                name = name,
                connection = PrinterConnection.BLE,
                profile = profile,
                address = macOrUuid,
                bleServiceUuid = serviceUuid,
                bleWriteCharacteristicUuid = writeCharacteristicUuid,
                bleAutoDiscover = autoDiscover,
                bleBridgeCommand = bridgeCommand
            ).block()
        }

        override fun toTransportConfig(): PrinterTransportConfig {
            return BlePrinterConfig(
                name = name,
                address = macOrUuid,
                serviceUuid = serviceUuid,
                writeCharacteristicUuid = writeCharacteristicUuid,
                autoDiscover = autoDiscover,
                bridgeCommand = bridgeCommand
            )
        }
    }

    /**
     * USB connected printer device (USB Raw / Android UsbDevice).
     */
    data class Usb(
        override val name: String,
        val identifier: String,
        val vendorId: Int? = null,
        val productId: Int? = null,
        val deviceName: String? = null
    ) : PrinterDevice {
        override val address: String get() = identifier
        override val connectionType: PrinterConnection get() = PrinterConnection.USB

        override fun toPrinterConfig(
            profile: PrinterProfile,
            block: PrinterConfig.() -> PrinterConfig
        ): PrinterConfig {
            return PrinterConfig(
                name = name,
                connection = PrinterConnection.USB,
                profile = profile,
                address = identifier
            ).block()
        }

        override fun toTransportConfig(): PrinterTransportConfig {
            return UsbPrinterConfig(
                name = name,
                address = identifier
            )
        }
    }

    /**
     * Network connected printer device (TCP/IP / Ethernet / Wi-Fi).
     */
    data class Network(
        override val name: String,
        val host: String,
        val port: Int = 9100,
        val timeoutMs: Int = 5000
    ) : PrinterDevice {
        override val address: String get() = "$host:$port"
        override val connectionType: PrinterConnection get() = PrinterConnection.NETWORK

        override fun toPrinterConfig(
            profile: PrinterProfile,
            block: PrinterConfig.() -> PrinterConfig
        ): PrinterConfig {
            return PrinterConfig(
                name = name,
                connection = PrinterConnection.NETWORK,
                profile = profile,
                address = host,
                port = port,
                connectionTimeoutMs = timeoutMs
            ).block()
        }

        override fun toTransportConfig(): PrinterTransportConfig {
            return NetworkPrinterConfig(
                name = name,
                host = host,
                port = port,
                timeoutMs = timeoutMs
            )
        }
    }

    /**
     * Serial/COM port connected printer device.
     */
    data class Serial(
        override val name: String,
        val portName: String,
        val baudRate: Int = 9600,
        val description: String? = null,
        val readTimeoutMs: Int = 2000
    ) : PrinterDevice {
        override val address: String get() = portName
        override val connectionType: PrinterConnection get() = PrinterConnection.SERIAL

        override fun toPrinterConfig(
            profile: PrinterProfile,
            block: PrinterConfig.() -> PrinterConfig
        ): PrinterConfig {
            return PrinterConfig(
                name = name,
                connection = PrinterConnection.SERIAL,
                profile = profile,
                address = portName,
                baudRate = baudRate,
                readTimeoutMs = readTimeoutMs
            ).block()
        }

        override fun toTransportConfig(): PrinterTransportConfig {
            return SerialPrinterConfig(
                name = name,
                portName = portName,
                baudRate = baudRate,
                readTimeoutMs = readTimeoutMs
            )
        }
    }

    /**
     * Virtual/Emulated printer device for testing and UI previews.
     */
    data class Virtual(
        override val name: String = "Virtual Printer",
        val id: String = "virtual-01"
    ) : PrinterDevice {
        override val address: String get() = id
        override val connectionType: PrinterConnection get() = PrinterConnection.VIRTUAL

        override fun toPrinterConfig(
            profile: PrinterProfile,
            block: PrinterConfig.() -> PrinterConfig
        ): PrinterConfig {
            return PrinterConfig(
                name = name,
                connection = PrinterConnection.VIRTUAL,
                profile = profile,
                address = id
            ).block()
        }

        override fun toTransportConfig(): PrinterTransportConfig {
            return VirtualPrinterConfig(name = name)
        }
    }

    companion object {
        /**
         * Creates a [PrinterDevice] instance from a [DiscoveredPrinter].
         */
        fun fromDiscoveredPrinter(printer: DiscoveredPrinter): PrinterDevice {
            return when (PrinterConnection.from(printer.connectionType)) {
                PrinterConnection.BLUETOOTH -> Bluetooth(
                    name = printer.name,
                    macAddress = printer.address
                )
                PrinterConnection.BLUETOOTH_LE, PrinterConnection.BLE -> Ble(
                    name = printer.name,
                    macOrUuid = printer.address
                )
                PrinterConnection.USB -> Usb(
                    name = printer.name,
                    identifier = printer.address
                )
                PrinterConnection.NETWORK -> Network(
                    name = printer.name,
                    host = printer.address,
                    port = printer.port
                )
                PrinterConnection.SERIAL -> Serial(
                    name = printer.name,
                    portName = printer.address
                )
                PrinterConnection.VIRTUAL -> Virtual(
                    name = printer.name,
                    id = printer.address
                )
                PrinterConnection.UNKNOWN -> Usb(
                    name = printer.name,
                    identifier = printer.address
                )
            }
        }
    }
}

/**
 * Extension to convert [DiscoveredPrinter] to a typed [PrinterDevice].
 */
fun DiscoveredPrinter.toPrinterDevice(): PrinterDevice = PrinterDevice.fromDiscoveredPrinter(this)

/**
 * Factory helper object to easily create [PrinterDevice] instances.
 */
object PrinterDevices {
    fun bluetooth(
        name: String,
        macAddress: String,
        isBonded: Boolean = false,
        baudRate: Int = 9600
    ): PrinterDevice.Bluetooth = PrinterDevice.Bluetooth(
        name = name,
        macAddress = macAddress,
        isBonded = isBonded,
        baudRate = baudRate
    )

    fun ble(
        name: String,
        macOrUuid: String,
        rssi: Int? = null,
        serviceUuid: String = "0000ff00-0000-1000-8000-00805f9b34fb",
        writeCharacteristicUuid: String = "0000ff01-0000-1000-8000-00805f9b34fb"
    ): PrinterDevice.Ble = PrinterDevice.Ble(
        name = name,
        macOrUuid = macOrUuid,
        rssi = rssi,
        serviceUuid = serviceUuid,
        writeCharacteristicUuid = writeCharacteristicUuid
    )

    fun usb(
        name: String,
        identifier: String,
        vendorId: Int? = null,
        productId: Int? = null,
        deviceName: String? = null
    ): PrinterDevice.Usb = PrinterDevice.Usb(
        name = name,
        identifier = identifier,
        vendorId = vendorId,
        productId = productId,
        deviceName = deviceName
    )

    fun network(
        name: String,
        host: String,
        port: Int = 9100,
        timeoutMs: Int = 5000
    ): PrinterDevice.Network = PrinterDevice.Network(
        name = name,
        host = host,
        port = port,
        timeoutMs = timeoutMs
    )

    fun serial(
        name: String,
        portName: String,
        baudRate: Int = 9600,
        description: String? = null
    ): PrinterDevice.Serial = PrinterDevice.Serial(
        name = name,
        portName = portName,
        baudRate = baudRate,
        description = description
    )

    fun virtual(
        name: String = "Virtual Printer",
        id: String = "virtual-01"
    ): PrinterDevice.Virtual = PrinterDevice.Virtual(
        name = name,
        id = id
    )
}
