package ngga.ring.printer_esc_pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.platform.ESCPosImageHelper
import ngga.ring.printer.util.escpos.TextAlignment
import androidx.compose.ui.graphics.ImageBitmap
import ngga.ring.printer.model.*
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder

class PrinterViewModel : ViewModel() {
    private val printer = KmpPrinter()

    // --- Config State ---
    private val _config = MutableStateFlow(PrinterConfig(name = "Not Selected", connectionType = "VIRTUAL", address = ""))
    val config: StateFlow<PrinterConfig> = _config.asStateFlow()

    private val _showVirtual = MutableStateFlow(false)
    val showVirtual: StateFlow<Boolean> = _showVirtual.asStateFlow()

    // --- Discovery State ---
    private val _discoveryMode = MutableStateFlow("NETWORK")
    val discoveryMode: StateFlow<String> = _discoveryMode.asStateFlow()

    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private val _discoveryLog = MutableStateFlow("Ready")
    val discoveryLog: StateFlow<String> = _discoveryLog.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var _discoveryJob: Job? = null

    private val _availableModes = MutableStateFlow(listOf("BLUETOOTH", "USB", "NETWORK", "SERIAL"))
    val availableModes: StateFlow<List<String>> = _availableModes.asStateFlow()

    // --- Code Preview State ---
    private val _rawCommandHex = MutableStateFlow("")
    val rawCommandHex: StateFlow<String> = _rawCommandHex.asStateFlow()

    private val _rawCommandBytes = MutableStateFlow(0)
    val rawCommandBytes: StateFlow<Int> = _rawCommandBytes.asStateFlow()

    // --- Connection & Print State ---
    val connectionState: StateFlow<ConnectionState> = printer.connectionState

    private val _printStatus = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    val printStatus: StateFlow<PrintStatus> = _printStatus.asStateFlow()

    private val _previewBlocks = MutableStateFlow<List<PreviewBlock>>(emptyList())
    val previewBlocks: StateFlow<List<PreviewBlock>> = _previewBlocks.asStateFlow()

    // --- Logo State ---
    private val _originalLogoSource = MutableStateFlow<Any?>(null)
    private val _selectedLogoBytes = MutableStateFlow<ByteArray?>(null)
    private val _logoWidth = MutableStateFlow(0)
    private val _logoHeight = MutableStateFlow(0)
    
    private val _logoPreview = MutableStateFlow<ImageBitmap?>(null)
    val logoPreview: StateFlow<ImageBitmap?> = _logoPreview.asStateFlow()

    // --- Enterprise Imaging State ---
    private val _imagingDithering = MutableStateFlow("THRESHOLD")
    val imagingDithering: StateFlow<String> = _imagingDithering.asStateFlow()
    
    private val _imagingContrast = MutableStateFlow(0)
    val imagingContrast: StateFlow<Int> = _imagingContrast.asStateFlow()
    
    private val _imagingBrightness = MutableStateFlow(0)
    val imagingBrightness: StateFlow<Int> = _imagingBrightness.asStateFlow()

    fun setLogo(image: Any, preview: ImageBitmap) {
        _originalLogoSource.value = image
        _logoPreview.value = preview
        reprocessLogo()
    }

    fun updateImaging(dithering: String? = null, contrast: Int? = null, brightness: Int? = null) {
        dithering?.let { _imagingDithering.value = it }
        contrast?.let { _imagingContrast.value = it }
        brightness?.let { _imagingBrightness.value = it }
        reprocessLogo()
    }

    private fun reprocessLogo() {
        val source = _originalLogoSource.value ?: return
        viewModelScope.launch {
            try {
                val maxWidth = _config.value.paperWidthDots.let { if (it > 0) it else 384 }
                // Use standard raster for now, dithering is showcased in printExpertReceipt if using grayscale
                val (bytes, w, h) = ESCPosImageHelper.processToRaster(source, maxWidth)
                
                _selectedLogoBytes.value = bytes
                _logoWidth.value = w
                _logoHeight.value = h
                
                updatePreview(_config.value)
            } catch (e: Exception) {
                _discoveryLog.value = "Error processing: ${e.message}"
            }
        }
    }

    fun runStressTest() {
        viewModelScope.launch {
            _printStatus.value = PrintStatus.Processing
            _discoveryLog.value = "Starting Stress Test (Mutex Demonstration)..."
            
            // Launch 10 concurrent print jobs
            (1..10).forEach { i ->
                launch {
                    val buildConfig = _config.value
                    val data = printer.newCommandBuilder(buildConfig)
                        .initialize()
                        .line("STRESS TEST TICKET #$i")
                        .line("Mutex protection check...")
                        .feed(2)
                        .cut()
                        .build()
                    
                    printer.printRaw(buildConfig, data).collect { status ->
                        if (status is PrintStatus.Success) {
                            _discoveryLog.value = "Ticket #$i printed successfully"
                        }
                    }
                }
            }
        }
    }

    fun clearLogo() {
        _selectedLogoBytes.value = null
        _logoPreview.value = null
        _config.value = _config.value.copy()
        updatePreview(_config.value)
    }

    fun resetPrintStatus() {
        _printStatus.value = PrintStatus.Idle
    }

    init {
        _config.onEach { updatePreview(it) }.launchIn(viewModelScope)
        _showVirtual.onEach { virtual ->
            val base = listOf("BLUETOOTH", "USB", "NETWORK", "SERIAL")
            _availableModes.value = if (virtual) base + "VIRTUAL" else base
        }.launchIn(viewModelScope)
    }

    fun cancelDiscovery() {
        _discoveryJob?.cancel()
        _discoveryJob = null
        _isScanning.value = false
    }

    fun setConnectionType(type: String) {
        cancelDiscovery()
        _discoveryMode.value = type
        _config.value = _config.value.copy(connectionType = type)
        _discoveredPrinters.value = emptyList()
        _discoveryLog.value = "Connection: $type"
    }

    fun setDiscoveryMode(mode: String) {
        _discoveryMode.value = mode
    }

    fun toggleVirtual(enabled: Boolean) {
        _showVirtual.value = enabled
    }

    fun startDiscovery() {
        cancelDiscovery()
        val mode = _discoveryMode.value
        val showVirtual = _showVirtual.value
        _discoveryJob = viewModelScope.launch {
            _isScanning.value = true
            _discoveredPrinters.value = emptyList()
            _discoveryLog.value = "Scanning $mode..."
            printer.checkAndRequestPermissions(mode) { granted ->
                if (granted) {
                    viewModelScope.launch {
                        doDiscovery(mode, showVirtual)
                    }
                } else {
                    _discoveryLog.value = "Permission denied. Please enable in settings."
                    _isScanning.value = false
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _discoveryLog.value = "Testing connection..."
            val status = printer.testConnection(_config.value)
            _discoveryLog.value = when (status) {
                PrintStatus.Success -> "Connection test success."
                is PrintStatus.Error -> "Connection test failed: ${status.message}"
                else -> "Connection test: $status"
            }
            _printStatus.value = status
        }
    }

    fun runDiagnostics() {
        val cfg = _config.value
        val report = printer.platformReport()
        val diag = when (cfg.connectionType) {
            PrinterConnectionType.USB -> {
                val usb = printer.diagnoseUsb(cfg)
                buildString {
                    append("USB: ${usb.failureReason}. ${usb.message}")
                    if (usb.suggestedFix.isNotBlank()) append(" Fix: ${usb.suggestedFix}")
                    if (usb.udevRule != null) append(" Udev: ${usb.udevRule}")
                }
            }
            PrinterConnectionType.BLUETOOTH_LE -> {
                val ble = printer.diagnoseBle(cfg)
                "BLE: ${ble.failureReason}. ${ble.message} Fix: ${ble.suggestedFix}"
            }
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.SERIAL -> {
                val serial = printer.diagnoseSerial(cfg)
                buildString {
                    append("${cfg.connectionType}: ${serial.failureReason}. ${serial.message}")
                    if (serial.suggestedFix.isNotBlank()) append(" Fix: ${serial.suggestedFix}")
                    if (serial.ports.isNotEmpty()) {
                        append(" Ports: ")
                        append(serial.ports.joinToString { "${it.address}(${it.confidence}%)" })
                    }
                }
            }
            else -> {
                val capability = report.capabilityFor(cfg.connectionType)
                "Platform ${report.platformName}/${report.osName}. ${cfg.connectionType}: supported=${capability?.isSupported}, native=${capability?.isNative}."
            }
        }
        _discoveryLog.value = diag
    }

    // --- Individual field updaters (to avoid Map.copy() ambiguity) ---
    fun updateName(value: String) { _config.update { it.copy(name = value) } }
    fun updateAddress(value: String) { _config.update { it.copy(address = value.ifBlank { null }) } }
    fun updatePort(value: Int) { _config.update { it.copy(port = value) } }
    fun updateBaudRate(value: Int) { _config.update { it.copy(baudRate = value) } }
    fun updateCharsPerLine(value: Int) { _config.update { it.copy(characterPerLine = value) } }
    fun updatePaperDots(value: Int) { _config.update { it.copy(paperWidthDots = value) } }
    fun updateLeftMargin(value: Int) { _config.update { it.copy(leftMargin = value) } }
    fun updateBleServiceUuid(value: String) { _config.update { it.copy(bleServiceUuid = value) } }
    fun updateBleCharacteristicUuid(value: String) { _config.update { it.copy(bleWriteCharacteristicUuid = value) } }
    fun updateBleAutoDiscover(value: Boolean) { _config.update { it.copy(bleAutoDiscover = value) } }
    fun updateBleHandshake(value: Boolean) { _config.update { it.copy(bleHandshakeEnabled = value) } }
    fun updateBluetoothAutoBind(value: Boolean) { _config.update { it.copy(bluetoothClassicAutoBind = value) } }
    fun updateBluetoothRfcomm(value: String) { _config.update { it.copy(bluetoothClassicRfcommDevice = value) } }

    fun buildRawHex() {
        viewModelScope.launch {
            try {
                val cfg = _config.value
                val data = printer.newCommandBuilder(cfg)
                    .initialize()
                    .alignCenter()
                    .bold(true)
                    .line("KMP PRINTER")
                    .bold(false)
                    .line("Test Receipt")
                    .divider()
                    .alignLeft()
                    .line("Type: ${cfg.connectionType}")
                    .line("Name: ${cfg.name}")
                    .line("Address: ${cfg.address ?: "-"}")
                    .feed(3)
                    .cut()
                    .build()

                val hexLines = data.toHexDump()
                _rawCommandHex.value = hexLines
                _rawCommandBytes.value = data.size
            } catch (e: Exception) {
                _rawCommandHex.value = "Error: ${e.message}"
                _rawCommandBytes.value = 0
            }
        }
    }

    private fun ByteArray.toHexDump(): String {
        val sb = StringBuilder()
        var offset = 0
        while (offset < size) {
            val lineEnd = minOf(offset + 16, size)
            val hexPart = (offset until lineEnd).joinToString(" ") { this[it].toUByte().toString(16).padStart(2, '0') }
            val asciiPart = (offset until lineEnd).joinToString("") {
                val c = this[it].toInt().toChar()
                if (c in ' '..'~') c.toString() else "."
            }
            val addrHex = offset.toString(16).padStart(8, '0')
            sb.appendLine("$addrHex  $hexPart  |$asciiPart|")
            offset = lineEnd
        }
        return sb.toString().trimEnd()
    }

    private fun startDiscovery(mode: String, showVirtual: Boolean) {
        viewModelScope.launch {
            printer.checkAndRequestPermissions(mode) { granted ->
                if (granted) {
                    viewModelScope.launch {
                        doDiscovery(mode, showVirtual)
                    }
                } else {
                    _discoveryLog.value = "Permission denied. Please enable in settings."
                }
            }
        }
    }

    private suspend fun doDiscovery(mode: String, showVirtual: Boolean) {
        val discoveryConfig = DiscoveryConfig(showVirtualDevices = showVirtual)
        printer.discovery(mode, discoveryConfig) { log ->
            _discoveryLog.value = log
        }.collectLatest { devices ->
            _discoveredPrinters.value = devices
            _isScanning.value = false
            _discoveryLog.value = if (devices.isEmpty()) "No devices found."
            else "Found ${devices.size} device(s)."
        }
    }

    fun selectPrinter(discovered: DiscoveredPrinter) {
        _config.value = _config.value.copy(
            name = discovered.name,
            connectionType = discovered.connectionType,
            address = discovered.address,
            port = discovered.port
        )
    }

    fun updateConfig(newConfig: PrinterConfig) {
        _config.value = newConfig
    }

    fun printTestPage() {
        viewModelScope.launch {
            printer.printTestPage(_config.value).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun printCalibrationPage() {
        viewModelScope.launch {
            val bytes = printer.receiptService.generateCalibrationReceipt(_config.value)
            printer.printRaw(_config.value, bytes).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun applyCalibration(leftMostDot: Int, rightMostDot: Int) {
        val current = _config.value
        // If paper starts at leftMostDot (e.g. 40) and ends at rightMostDot (e.g. 580)
        // Then printable width is 580 - 40 = 540 dots.
        // We set leftMargin to 40 so dot 0 in code starts at 40 on paper.
        val newWidth = (rightMostDot - leftMostDot).coerceAtLeast(384)
        
        // Suggest chars per line based on standard font (12 dots)
        val suggestedChars = (newWidth / 12).coerceAtMost(64)

        _config.value = current.copy(
            leftMargin = leftMostDot,
            paperWidthDots = newWidth,
            characterPerLine = suggestedChars,
            autoCenter = true // Enable by default for calibrated devices
        )
    }

    fun printExpertTest() {
        viewModelScope.launch {
            val buildConfig = _config.value
            val data = printer.newCommandBuilder(buildConfig)
                .initialize()
                .selectCodePage(buildConfig.escPosCodePage)
                .line("EXPERT NATIVE TEST")
                .divider()
                .line("Native Barcode (128):")
                .barcode("KMP-PRINTER-V2")
                .feed(1)
                .line("Native QR Code:")
                .qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
                .feed(1)
                .line("Charset: ${buildConfig.charsetName}")
                .line("Special Char: " + if(buildConfig.charsetName == "UTF-8") "€ £ ¥ ©" else "Testing Charset")
                .feed(3)
                .cut()
                .build()
            
            printer.printRaw(buildConfig, data).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun printPageModeDemo() {
        viewModelScope.launch {
            val buildConfig = _config.value
            val data = printer.newCommandBuilder(buildConfig)
                .initialize()
                .line("--- PAGE MODE DEMO ---")
                .enterPageMode()
                .setPagePrintArea(0, 0, 384, 200)
                // Diagonal Teks
                .setHorizontalPosition(10)
                .setPageVerticalPosition(10)
                .text("X:10, Y:10")
                .setHorizontalPosition(100)
                .setPageVerticalPosition(50)
                .text("X:100, Y:50")
                .setHorizontalPosition(200)
                .setPageVerticalPosition(90)
                .text("X:200, Y:90")
                .printPageAndReturn()
                .feed(3)
                .cut()
                .build()
            
            printer.printRaw(buildConfig, data).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun printBarcodeSuite() {
        viewModelScope.launch {
            val buildConfig = _config.value
            val data = printer.newCommandBuilder(buildConfig)
                .initialize()
                .alignCenter()
                .line("--- BARCODE SUITE ---")
                .feed(1)
                .line("PDF417 (High Density)")
                .pdf417("KMP-PRINTER-PDF417-TEST")
                .feed(1)
                .line("DataMatrix")
                .dataMatrix("KMP-PRINTER-DATAMATRIX")
                .feed(1)
                .line("Native QR Code")
                .qrCodeNative("https://github.com/ringga-dev", size = 10)
                .feed(3)
                .cut()
                .build()
            
            printer.printRaw(buildConfig, data).collect { status ->
                _printStatus.value = status
            }
        }
    }

    fun printExpertReceipt() {
        viewModelScope.launch {
            val buildConfig = _config.value
            val logoBytes = _selectedLogoBytes.value
            val logoW = _logoWidth.value
            val logoH = _logoHeight.value

            val data = printer.newCommandBuilder(buildConfig)
                .initialize()
                .alignCenter()
                
            if (logoBytes != null) {
                data.image(logoBytes, logoW, logoH)
                data.feed(1)
            }
            
            data.bold(true)
                .line("ENTERPRISE STORE POS")
                .bold(false)
                .line("Sudirman St. 123, Jakarta")
                .line("Tel: +62 21 555-0199")
                .divider()
                .alignLeft()
                .tableRow(listOf("Cappuccino", "1x", "45.000"), listOf(2, 1, 1))
                .tableRow(listOf("Croissant Cheese", "2x", "60.000"), listOf(2, 1, 1))
                .tableRow(listOf("Iced Matcha", "1x", "38.000"), listOf(2, 1, 1))
                .divider()
                .alignRight()
                .bold(true)
                .line("TOTAL: 143.000")
                .bold(false)
                .divider()
                .alignCenter()
                .line("Order #88901 - 2024-10-21")
                .feed(1)
                .qrCodeNative("TRX-88901-VERIFIED", size = 6)
                .feed(1)
                .line("Thank you for your visit!")
                .feed(4)
                .cut()
            
            printer.printRaw(buildConfig, data.build()).collect { status ->
                _printStatus.value = status
            }
        }
    }


    private fun updatePreview(config: PrinterConfig) {
        try {
            val baseBlocks = printer.receiptService.generateTestPreview(config).toMutableList()
            _logoPreview.value?.let { bitmap ->
                baseBlocks.add(0, PreviewBlock.Image(
                    width = _logoWidth.value,
                    height = _logoHeight.value,
                    alignment = TextAlignment.CENTER,
                    previewData = bitmap
                ))
            }
            _previewBlocks.value = baseBlocks
        } catch (e: Exception) {
            _discoveryLog.value = "Preview error: ${e.message}"
            _previewBlocks.value = emptyList()
        }
    }
}
