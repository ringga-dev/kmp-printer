package ngga.ring.printer.manager

import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.PrinterLogger

/**
 * Virtual Printer Emulator for Unit Testing and Debugging.
 * Redirects ESC/POS commands to console log.
 */
class VirtualPrinterConnector : BasePrinterConnector() {
    private var connected = false
    private val printHistory = mutableListOf<String>()
    
    // State
    private var isBold = false
    private var isUnderline = false
    private var alignment = "LEFT"

    override suspend fun connect(config: PrinterConfig): Boolean {
        connected = true
        PrinterLogger.info("VirtualPrinter", "Connected to virtual emulator")
        return true
    }

    override suspend fun sendRawData(data: ByteArray): Boolean {
        if (!connected) return false

        var i = 0
        var currentLine = StringBuilder()
        
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            
            when (b) {
                0x0A -> { // LF
                    val stylePrefix = if (isBold) "[B] " else ""
                    val alignPrefix = when (alignment) {
                        "CENTER" -> "   [C]   "
                        "RIGHT" -> "         [R] "
                        else -> ""
                    }
                    val text = "$alignPrefix$stylePrefix${currentLine}"
                    printHistory.add(text)
                    PrinterLogger.debug("VirtualPrinter", "$text")
                    currentLine = StringBuilder()
                }
                0x1B -> { // ESC
                    if (i + 1 < data.size) {
                        val next = data[++i].toInt() and 0xFF
                        when (next) {
                            0x45 -> isBold = (data[++i].toInt() == 1)
                            0x61 -> alignment = when (data[++i].toInt()) {
                                1 -> "CENTER"
                                2 -> "RIGHT"
                                else -> "LEFT"
                            }
                            0x40 -> { isBold = false; alignment = "LEFT" }
                        }
                    }
                }
                0x1D -> { // GS
                     if (i + 1 < data.size) {
                        val next = data[++i].toInt() and 0xFF
                        if (next == 0x76 && i + 1 < data.size && data[i+1] == 0x30.toByte()) { // GS v 0 (Image)
                             // Skip image data for now but log dimensions
                             i += 6 // Skip xL xH yL yH
                             currentLine.append("[IMAGE]")
                        }
                     }
                }
                in 0x20..0x7E -> {
                    currentLine.append(b.toChar())
                }
            }
            i++
        }
        return true
    }

    /**
     * Specialized function for the visual simulator to get the raw "paper" content.
     */
    fun getVirtualPaper(): List<String> = printHistory.toList()

    override suspend fun readData(count: Int, timeout: Long): ByteArray? {
        // Return a mock byte (e.g. 0x12 -> Online, Paper OK) for simulation
        return byteArrayOf(0x12)
    }

    override suspend fun disconnect() {
        connected = false
        PrinterLogger.info("VirtualPrinter", "Disconnected")
    }

    override fun isConnected(): Boolean = connected
}
