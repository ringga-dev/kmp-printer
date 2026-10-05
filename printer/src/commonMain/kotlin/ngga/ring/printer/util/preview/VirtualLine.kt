package ngga.ring.printer.util.preview

import ngga.ring.printer.util.escpos.TextAlignment

/**
 * Represents a single rendered line in the virtual printer output.
 * Used by ESCPosVirtualRenderer to build a visual representation of a receipt.
 */
data class VirtualLine(
    val content: String,
    val type: LineType = LineType.TEXT,
    val alignment: TextAlignment = TextAlignment.LEFT,
    val isBold: Boolean = false,
    val isUnderline: Boolean = false,
    val isInverted: Boolean = false,
    val widthMultiplier: Int = 1,
    val heightMultiplier: Int = 1
)

/**
 * Type of content in a virtual line.
 */
enum class LineType {
    TEXT,
    DIVIDER,
    BARCODE,
    QR_CODE,
    IMAGE,
    SPACE,
    CUT,
    SYSTEM_COMMAND
}
