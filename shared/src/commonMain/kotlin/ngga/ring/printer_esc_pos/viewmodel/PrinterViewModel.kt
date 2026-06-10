package ngga.ring.printer_esc_pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.platform.ESCPosImageHelper
import ngga.ring.printer.util.escpos.TextAlignment
import androidx.compose.ui.graphics.ImageBitmap
import ngga.ring.printer.model.*

class PrinterViewModel : ViewModel() {
    private val printer = KmpPrinter()

    // --- Config State ---
    private val _config = MutableStateFlow(PrinterConfig(name = "Not Selected", connectionType = "VIRTUAL", address = ""))
    val config: StateFlow<PrinterConfig> = _config.asStateFlow()

    private val _showVirtual = MutableStateFlow(false)
    val showVirtual: StateFlow<Boolean> = _showVirtual.asStateFlow()

    // --- Discovery State ---
    private val _discoveryMode = MutableStateFlow("BLUETOOTH")
    val discoveryMode: StateFlow<String> = _discoveryMode.asStateFlow()

    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private val _discoveryLog = MutableStateFlow("Ready to scan...")
    val discoveryLog: StateFlow<String> = _discoveryLog.asStateFlow()

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
        // Update preview when config changes
        _config.onEach { updatePreview(it) }.launchIn(viewModelScope)
        
        // Start auto-discovery when mode changes
        combine(_discoveryMode, _showVirtual) { mode, virtual -> mode to virtual }
            .onEach { (mode, virtual) ->
                startDiscovery(mode, virtual)
            }.launchIn(viewModelScope)
    }

    fun setDiscoveryMode(mode: String) {
        _discoveryMode.value = mode
    }

    fun toggleVirtual(enabled: Boolean) {
        _showVirtual.value = enabled
    }

    private fun startDiscovery(mode: String, showVirtual: Boolean) {
        viewModelScope.launch {
            printer.checkAndRequestPermissions(mode) { granted ->
                if (granted) {
                    doDiscovery(mode, showVirtual)
                } else {
                    _discoveryLog.value = "Permission denied. Please enable in settings."
                }
            }
        }
    }

    private fun doDiscovery(mode: String, showVirtual: Boolean) {
        viewModelScope.launch {
            val discoveryConfig = DiscoveryConfig(showVirtualDevices = showVirtual)
            printer.discovery(mode, discoveryConfig) { log ->
                _discoveryLog.value = log
            }.collectLatest { devices ->
                _discoveredPrinters.value = devices
            }
        }
    }

    fun selectPrinter(discovered: DiscoveredPrinter) {
        _config.value = _config.value.copy(
            name = discovered.name,
            connectionType = discovered.connectionType,
            address = discovered.address,
            port = discovered.port ?: 9100
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
        val baseBlocks = printer.receiptService.generateTestPreview(config).toMutableList()
        
        // Inject logo if exists
        _logoPreview.value?.let { bitmap ->
            baseBlocks.add(0, PreviewBlock.Image(
                width = _logoWidth.value,
                height = _logoHeight.value,
                alignment = TextAlignment.CENTER,
                previewData = bitmap
            ))
        }
        
        _previewBlocks.value = baseBlocks
    }
}
