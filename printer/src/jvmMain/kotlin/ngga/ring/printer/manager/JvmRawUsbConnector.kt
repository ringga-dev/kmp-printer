package ngga.ring.printer.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrinterUsbDiagnostic
import ngga.ring.printer.model.PrinterUsbFailureReason
import org.usb4java.ConfigDescriptor
import org.usb4java.Context
import org.usb4java.Device
import org.usb4java.DeviceDescriptor
import org.usb4java.DeviceHandle
import org.usb4java.DeviceList
import org.usb4java.EndpointDescriptor
import org.usb4java.InterfaceDescriptor
import org.usb4java.LibUsb
import java.nio.ByteBuffer
import java.nio.IntBuffer

data class JvmUsbDeviceInfo(
    val vendorId: Int,
    val productId: Int,
    val busNumber: Int,
    val deviceAddress: Int,
    val manufacturer: String? = null,
    val product: String? = null,
    val serialNumber: String? = null,
    val isPrinterClass: Boolean = false
) {
    val address: String = "USB_RAW:${vendorId.hex4()}:${productId.hex4()}:$busNumber:$deviceAddress"
    val displayName: String = listOfNotNull(product, manufacturer)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" - ")
        ?: "USB Printer ${vendorId.hex4()}:${productId.hex4()}"
}

data class JvmUsbEndpoint(
    val interfaceNumber: Int,
    val outEndpointAddress: Byte,
    val inEndpointAddress: Byte? = null
)

interface JvmUsbBackend {
    val os: JvmOperatingSystem
    fun listDevices(): List<JvmUsbDeviceInfo>
    fun open(config: PrinterConfig): JvmUsbSession?
    fun diagnose(config: PrinterConfig): PrinterUsbDiagnostic
    fun troubleshootingHint(): String
}

interface JvmUsbSession {
    fun write(data: ByteArray, timeoutMs: Int): Boolean
    fun read(count: Int, timeoutMs: Int): ByteArray?
    fun close()
}

abstract class BaseLibUsbBackend(
    override val os: JvmOperatingSystem
) : JvmUsbBackend {
    override fun listDevices(): List<JvmUsbDeviceInfo> {
        val context = Context()
        if (LibUsb.init(context) != LibUsb.SUCCESS) return emptyList()

        val list = DeviceList()
        return try {
            val result = LibUsb.getDeviceList(context, list)
            if (result < 0) return emptyList()

            list.mapNotNull { device ->
                val descriptor = DeviceDescriptor()
                if (LibUsb.getDeviceDescriptor(device, descriptor) != LibUsb.SUCCESS) return@mapNotNull null

                val info = JvmUsbDeviceInfo(
                    vendorId = descriptor.idVendor().toInt() and 0xFFFF,
                    productId = descriptor.idProduct().toInt() and 0xFFFF,
                    busNumber = LibUsb.getBusNumber(device) and 0xFF,
                    deviceAddress = LibUsb.getDeviceAddress(device) and 0xFF,
                    isPrinterClass = descriptor.bDeviceClass().toInt() == USB_CLASS_PRINTER ||
                        findBulkEndpoints(device)?.isPrinterInterface == true
                )

                if (info.isPrinterClass) info else null
            }
        } finally {
            LibUsb.freeDeviceList(list, true)
            LibUsb.exit(context)
        }
    }

    override fun open(config: PrinterConfig): JvmUsbSession? {
        val target = JvmUsbAddress.parse(config.address) ?: return null
        val context = Context()
        if (LibUsb.init(context) != LibUsb.SUCCESS) return null

        val list = DeviceList()
        val listResult = LibUsb.getDeviceList(context, list)
        if (listResult < 0) {
            LibUsb.exit(context)
            return null
        }

        var selectedDevice: Device? = null
        var selectedEndpoint: JvmUsbEndpoint? = null
        val selectedHandle = DeviceHandle()

        try {
            for (device in list) {
                val descriptor = DeviceDescriptor()
                if (LibUsb.getDeviceDescriptor(device, descriptor) != LibUsb.SUCCESS) continue

                val vendorId = descriptor.idVendor().toInt() and 0xFFFF
                val productId = descriptor.idProduct().toInt() and 0xFFFF
                val busNumber = LibUsb.getBusNumber(device) and 0xFF
                val deviceAddress = LibUsb.getDeviceAddress(device) and 0xFF

                if (!target.matches(vendorId, productId, busNumber, deviceAddress)) continue

                val endpoint = findBulkEndpoints(device) ?: continue
                if (LibUsb.open(device, selectedHandle) != LibUsb.SUCCESS) continue

                selectedDevice = device
                selectedEndpoint = JvmUsbEndpoint(
                    interfaceNumber = endpoint.interfaceNumber,
                    outEndpointAddress = endpoint.outEndpointAddress,
                    inEndpointAddress = endpoint.inEndpointAddress
                )
                break
            }
        } finally {
            LibUsb.freeDeviceList(list, true)
        }

        val endpoint = selectedEndpoint
        if (selectedDevice == null || endpoint == null) {
            LibUsb.exit(context)
            return null
        }

        detachKernelDriverIfNeeded(selectedHandle, endpoint.interfaceNumber)
        if (LibUsb.claimInterface(selectedHandle, endpoint.interfaceNumber) != LibUsb.SUCCESS) {
            LibUsb.close(selectedHandle)
            LibUsb.exit(context)
            return null
        }

        return LibUsbSession(context, selectedHandle, endpoint)
    }

    override fun diagnose(config: PrinterConfig): PrinterUsbDiagnostic {
        val target = JvmUsbAddress.parse(config.address)
            ?: return diagnostic(
                reason = PrinterUsbFailureReason.INVALID_ADDRESS,
                message = "Invalid raw USB address: ${config.address ?: "<empty>"}",
                target = null
            )

        val context = Context()
        val initResult = LibUsb.init(context)
        if (initResult != LibUsb.SUCCESS) {
            return diagnostic(
                reason = PrinterUsbFailureReason.LIBUSB_INIT_FAILED,
                message = "libusb initialization failed: ${LibUsb.strError(initResult)}",
                target = target
            )
        }

        val list = DeviceList()
        val listResult = LibUsb.getDeviceList(context, list)
        if (listResult < 0) {
            LibUsb.exit(context)
            return diagnostic(
                reason = classifyLibUsbError(listResult),
                message = "Unable to list USB devices: ${LibUsb.strError(listResult)}",
                target = target
            )
        }

        val handle = DeviceHandle()
        var endpoint: JvmUsbEndpoint? = null
        var found = false
        var openResult = LibUsb.ERROR_NO_DEVICE
        var claimResult = LibUsb.ERROR_OTHER

        try {
            for (device in list) {
                val descriptor = DeviceDescriptor()
                if (LibUsb.getDeviceDescriptor(device, descriptor) != LibUsb.SUCCESS) continue

                val vendorId = descriptor.idVendor().toInt() and 0xFFFF
                val productId = descriptor.idProduct().toInt() and 0xFFFF
                val busNumber = LibUsb.getBusNumber(device) and 0xFF
                val deviceAddress = LibUsb.getDeviceAddress(device) and 0xFF

                if (!target.matches(vendorId, productId, busNumber, deviceAddress)) continue
                found = true

                val candidate = findBulkEndpoints(device)
                if (candidate == null) {
                    return diagnostic(
                        deviceFound = true,
                        reason = PrinterUsbFailureReason.NO_BULK_OUT_ENDPOINT,
                        message = "USB device found, but no bulk OUT endpoint was detected.",
                        target = target
                    )
                }

                endpoint = JvmUsbEndpoint(
                    interfaceNumber = candidate.interfaceNumber,
                    outEndpointAddress = candidate.outEndpointAddress,
                    inEndpointAddress = candidate.inEndpointAddress
                )
                openResult = LibUsb.open(device, handle)
                if (openResult != LibUsb.SUCCESS) {
                    return diagnostic(
                        deviceFound = true,
                        reason = classifyLibUsbError(openResult),
                        message = "USB device found, but libusb could not open it: ${LibUsb.strError(openResult)}",
                        target = target
                    )
                }

                detachKernelDriverIfNeeded(handle, endpoint.interfaceNumber)
                claimResult = LibUsb.claimInterface(handle, endpoint.interfaceNumber)
                if (claimResult != LibUsb.SUCCESS) {
                    LibUsb.close(handle)
                    return diagnostic(
                        deviceFound = true,
                        canOpen = true,
                        reason = classifyClaimError(claimResult),
                        message = "USB device opened, but interface ${endpoint.interfaceNumber} could not be claimed: ${LibUsb.strError(claimResult)}",
                        target = target
                    )
                }

                LibUsb.releaseInterface(handle, endpoint.interfaceNumber)
                LibUsb.close(handle)
                return PrinterUsbDiagnostic(
                    deviceFound = true,
                    canOpen = true,
                    canClaimInterface = true,
                    message = "Raw USB access is ready for ${target.displayName()}."
                )
            }
        } finally {
            LibUsb.freeDeviceList(list, true)
            LibUsb.exit(context)
        }

        if (!found) {
            return diagnostic(
                reason = PrinterUsbFailureReason.DEVICE_NOT_FOUND,
                message = "USB device ${target.displayName()} was not found.",
                target = target
            )
        }

        val reason = if (openResult != LibUsb.SUCCESS) classifyLibUsbError(openResult) else classifyClaimError(claimResult)
        return diagnostic(
            deviceFound = true,
            reason = reason,
            message = "Raw USB diagnostic failed: ${LibUsb.strError(if (openResult != LibUsb.SUCCESS) openResult else claimResult)}",
            target = target
        )
    }

    private fun findBulkEndpoints(device: Device): UsbEndpointCandidate? {
        val descriptor = DeviceDescriptor()
        if (LibUsb.getDeviceDescriptor(device, descriptor) != LibUsb.SUCCESS) return null

        for (configIndex in 0 until descriptor.bNumConfigurations()) {
            val configDescriptor = ConfigDescriptor()
            if (LibUsb.getConfigDescriptor(device, configIndex.toByte(), configDescriptor) != LibUsb.SUCCESS) continue

            try {
                val interfaces = configDescriptor.iface() ?: continue
                for (usbInterface in interfaces) {
                    val altSettings = usbInterface.altsetting() ?: continue
                    for (alt in altSettings) {
                        val candidate = findBulkEndpoints(alt)
                        if (candidate != null) return candidate
                    }
                }
            } finally {
                LibUsb.freeConfigDescriptor(configDescriptor)
            }
        }
        return null
    }

    private fun findBulkEndpoints(descriptor: InterfaceDescriptor): UsbEndpointCandidate? {
        val endpoints = descriptor.endpoint() ?: return null
        var outEndpoint: Byte? = null
        var inEndpoint: Byte? = null
        for (endpoint in endpoints) {
            if (endpoint.isBulkOut()) {
                outEndpoint = endpoint.bEndpointAddress()
            } else if (endpoint.isBulkIn()) {
                inEndpoint = endpoint.bEndpointAddress()
            }
        }
        val out = outEndpoint ?: return null
        return UsbEndpointCandidate(
            interfaceNumber = descriptor.bInterfaceNumber().toInt() and 0xFF,
            outEndpointAddress = out,
            inEndpointAddress = inEndpoint,
            isPrinterInterface = descriptor.bInterfaceClass().toInt() == USB_CLASS_PRINTER
        )
    }

    private fun detachKernelDriverIfNeeded(handle: DeviceHandle, interfaceNumber: Int) {
        try {
            if (LibUsb.kernelDriverActive(handle, interfaceNumber) == 1) {
                LibUsb.detachKernelDriver(handle, interfaceNumber)
            }
        } catch (_: Exception) {
            // Not supported on every OS/backend.
        }
    }

    private fun diagnostic(
        deviceFound: Boolean = false,
        canOpen: Boolean = false,
        reason: PrinterUsbFailureReason,
        message: String,
        target: JvmUsbAddress?
    ): PrinterUsbDiagnostic {
        return PrinterUsbDiagnostic(
            deviceFound = deviceFound,
            canOpen = canOpen,
            canClaimInterface = false,
            failureReason = reason,
            message = message,
            suggestedFix = suggestedFix(reason),
            udevRule = if (os == JvmOperatingSystem.LINUX && target != null) target.udevRule() else null
        )
    }

    private fun classifyLibUsbError(error: Int): PrinterUsbFailureReason {
        return when (error) {
            LibUsb.ERROR_ACCESS -> PrinterUsbFailureReason.ACCESS_DENIED
            LibUsb.ERROR_NO_DEVICE -> PrinterUsbFailureReason.DEVICE_NOT_FOUND
            LibUsb.ERROR_BUSY -> PrinterUsbFailureReason.INTERFACE_BUSY
            else -> PrinterUsbFailureReason.UNKNOWN
        }
    }

    private fun classifyClaimError(error: Int): PrinterUsbFailureReason {
        return when (error) {
            LibUsb.ERROR_ACCESS -> PrinterUsbFailureReason.ACCESS_DENIED
            LibUsb.ERROR_BUSY -> PrinterUsbFailureReason.INTERFACE_BUSY
            LibUsb.ERROR_NOT_SUPPORTED -> PrinterUsbFailureReason.DRIVER_NOT_COMPATIBLE
            else -> PrinterUsbFailureReason.CLAIM_FAILED
        }
    }

    private fun suggestedFix(reason: PrinterUsbFailureReason): String {
        return when (os) {
            JvmOperatingSystem.WINDOWS -> when (reason) {
                PrinterUsbFailureReason.ACCESS_DENIED,
                PrinterUsbFailureReason.DRIVER_NOT_COMPATIBLE,
                PrinterUsbFailureReason.CLAIM_FAILED -> "Install a WinUSB/libusb-compatible driver for the printer interface, for example with Zadig, then retry raw USB."
                PrinterUsbFailureReason.INTERFACE_BUSY -> "Close applications or printer spooler jobs using this USB printer, then retry. If it stays busy, use the OS printer queue fallback."
                else -> troubleshootingHint()
            }
            JvmOperatingSystem.LINUX -> when (reason) {
                PrinterUsbFailureReason.ACCESS_DENIED -> "Add the generated udev rule, reload udev rules, reconnect the printer, then retry."
                PrinterUsbFailureReason.INTERFACE_BUSY -> "Another process or kernel driver owns the USB interface. Close it or use serial/print queue fallback."
                else -> troubleshootingHint()
            }
            JvmOperatingSystem.MACOS -> when (reason) {
                PrinterUsbFailureReason.INTERFACE_BUSY,
                PrinterUsbFailureReason.CLAIM_FAILED,
                PrinterUsbFailureReason.DRIVER_NOT_COMPATIBLE -> "macOS may own the USB printer interface. Prefer OS printer queue or USB-serial fallback for this device."
                else -> troubleshootingHint()
            }
            JvmOperatingSystem.OTHER -> troubleshootingHint()
        }
    }

    private fun EndpointDescriptor.isBulkOut(): Boolean {
        val transferType = bmAttributes().toInt() and LibUsb.TRANSFER_TYPE_MASK.toInt()
        val direction = bEndpointAddress().toInt() and LibUsb.ENDPOINT_DIR_MASK.toInt()
        return transferType == LibUsb.TRANSFER_TYPE_BULK.toInt() && direction == LibUsb.ENDPOINT_OUT.toInt()
    }

    private fun EndpointDescriptor.isBulkIn(): Boolean {
        val transferType = bmAttributes().toInt() and LibUsb.TRANSFER_TYPE_MASK.toInt()
        val direction = bEndpointAddress().toInt() and LibUsb.ENDPOINT_DIR_MASK.toInt()
        return transferType == LibUsb.TRANSFER_TYPE_BULK.toInt() && direction == LibUsb.ENDPOINT_IN.toInt()
    }

    private data class UsbEndpointCandidate(
        val interfaceNumber: Int,
        val outEndpointAddress: Byte,
        val inEndpointAddress: Byte?,
        val isPrinterInterface: Boolean
    )
}

class WindowsLibUsbBackend : BaseLibUsbBackend(JvmOperatingSystem.WINDOWS) {
    override fun troubleshootingHint(): String {
        return "Windows raw USB needs a WinUSB/libusb-compatible driver for the printer interface."
    }
}

class LinuxLibUsbBackend : BaseLibUsbBackend(JvmOperatingSystem.LINUX) {
    override fun troubleshootingHint(): String {
        return "Linux raw USB needs permission for /dev/bus/usb; add a udev rule if access is denied."
    }
}

class MacosLibUsbBackend : BaseLibUsbBackend(JvmOperatingSystem.MACOS) {
    override fun troubleshootingHint(): String {
        return "macOS raw USB can fail when another driver owns the printer interface."
    }
}

class GenericLibUsbBackend : BaseLibUsbBackend(JvmOperatingSystem.OTHER) {
    override fun troubleshootingHint(): String {
        return "Raw USB needs libusb access to the printer bulk OUT endpoint."
    }
}

class JvmUsbDeviceService {
    private val backend: JvmUsbBackend = when (JvmOperatingSystem.current()) {
        JvmOperatingSystem.WINDOWS -> WindowsLibUsbBackend()
        JvmOperatingSystem.LINUX -> LinuxLibUsbBackend()
        JvmOperatingSystem.MACOS -> MacosLibUsbBackend()
        JvmOperatingSystem.OTHER -> GenericLibUsbBackend()
    }

    fun currentOs(): JvmOperatingSystem = backend.os

    fun troubleshootingHint(): String = backend.troubleshootingHint()

    fun listRawUsbPrinters(): List<JvmUsbDeviceInfo> = backend.listDevices()

    fun discoverRawUsbPrinters(): List<DiscoveredPrinter> {
        return listRawUsbPrinters().map { device ->
            DiscoveredPrinter(
                name = device.displayName,
                connectionType = PrinterConnectionType.USB,
                address = device.address
            )
        }
    }

    fun open(config: PrinterConfig): JvmUsbSession? = backend.open(config)

    fun diagnose(config: PrinterConfig): PrinterUsbDiagnostic = backend.diagnose(config)
}

class JvmRawUsbConnector(
    private val usbService: JvmUsbDeviceService = JvmUsbDeviceService()
) : BasePrinterConnector() {
    private var session: JvmUsbSession? = null

    override suspend fun connect(config: PrinterConfig): Boolean = withContext(Dispatchers.IO) {
        configureFlowControl(config)
        session = usbService.open(config)
        session != null
    }

    override suspend fun sendRawData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        session?.write(data, 3000) ?: false
    }

    override suspend fun readData(count: Int, timeout: Long): ByteArray? = withContext(Dispatchers.IO) {
        session?.read(count, timeout.toInt().coerceAtLeast(1))
    }

    override suspend fun disconnect() {
        session?.close()
        session = null
    }

    override fun isConnected(): Boolean = session != null
}

private class LibUsbSession(
    private val context: Context,
    private val handle: DeviceHandle,
    private val endpoint: JvmUsbEndpoint
) : JvmUsbSession {
    override fun write(data: ByteArray, timeoutMs: Int): Boolean {
        val buffer = ByteBuffer.allocateDirect(data.size)
        buffer.put(data)
        buffer.rewind()

        val transferred = IntBuffer.allocate(1)
        val result = LibUsb.bulkTransfer(
            handle,
            endpoint.outEndpointAddress,
            buffer,
            transferred,
            timeoutMs.toLong()
        )
        return result == LibUsb.SUCCESS && transferred.get(0) == data.size
    }

    override fun read(count: Int, timeoutMs: Int): ByteArray? {
        val inEndpoint = endpoint.inEndpointAddress ?: return null
        val buffer = ByteBuffer.allocateDirect(count.coerceAtLeast(1))
        val transferred = IntBuffer.allocate(1)
        val result = LibUsb.bulkTransfer(
            handle,
            inEndpoint,
            buffer,
            transferred,
            timeoutMs.toLong()
        )
        val size = transferred.get(0)
        if (result != LibUsb.SUCCESS || size <= 0) return null

        val data = ByteArray(size)
        buffer.rewind()
        buffer.get(data)
        return data
    }

    override fun close() {
        try {
            LibUsb.releaseInterface(handle, endpoint.interfaceNumber)
        } catch (_: Exception) {
        } finally {
            LibUsb.close(handle)
            LibUsb.exit(context)
        }
    }
}

private data class JvmUsbAddress(
    val vendorId: Int,
    val productId: Int,
    val busNumber: Int? = null,
    val deviceAddress: Int? = null
) {
    fun matches(vendorId: Int, productId: Int, busNumber: Int, deviceAddress: Int): Boolean {
        return this.vendorId == vendorId &&
            this.productId == productId &&
            (this.busNumber == null || this.busNumber == busNumber) &&
            (this.deviceAddress == null || this.deviceAddress == deviceAddress)
    }

    companion object {
        fun parse(value: String?): JvmUsbAddress? {
            if (value.isNullOrBlank()) return null
            val normalized = value.removePrefix("USB_RAW:").replace("-", ":")
            val parts = normalized.split(":")
            if (parts.size < 2) return null

            val vendorId = parts[0].toIntOrNull(16) ?: parts[0].toIntOrNull() ?: return null
            val productId = parts[1].toIntOrNull(16) ?: parts[1].toIntOrNull() ?: return null
            val busNumber = parts.getOrNull(2)?.toIntOrNull()
            val deviceAddress = parts.getOrNull(3)?.toIntOrNull()

            return JvmUsbAddress(vendorId, productId, busNumber, deviceAddress)
        }
    }
}

private fun JvmUsbAddress.displayName(): String = "${vendorId.hex4()}:${productId.hex4()}"

private fun JvmUsbAddress.udevRule(): String {
    return "SUBSYSTEM==\"usb\", ATTR{idVendor}==\"${vendorId.hex4().lowercase()}\", ATTR{idProduct}==\"${productId.hex4().lowercase()}\", MODE=\"0666\", GROUP=\"plugdev\""
}

private fun Int.hex4(): String = toString(16).uppercase().padStart(4, '0')

private const val USB_CLASS_PRINTER = 7
