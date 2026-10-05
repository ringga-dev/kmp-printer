package ngga.ring.printer.util.preview

import ngga.ring.printer.util.escpos.TextAlignment

/**
 * Logical representation of a receipt element for UI preview.
 */
sealed class PreviewBlock {
    data class Text(
        val text: String,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val isBold: Boolean = false,
        val isUnderline: Boolean = false,
        val isInverted: Boolean = false,
        val widthMultiplier: Int = 1,
        val heightMultiplier: Int = 1
    ) : PreviewBlock()

    data class KeyValue(
        val key: String,
        val value: String,
        val isBold: Boolean = false,
        val isInverted: Boolean = false
    ) : PreviewBlock()

    data class Divider(val char: Char = '-') : PreviewBlock()
    
    data class Barcode(
        val content: String,
        val alignment: TextAlignment = TextAlignment.LEFT
    ) : PreviewBlock()
    
    data class Image(
        val width: Int,
        val height: Int,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val previewData: Any? = null
    ) : PreviewBlock()
    
    data class QRCode(
        val content: String,
        val alignment: TextAlignment = TextAlignment.LEFT
    ) : PreviewBlock()
    
    object Space : PreviewBlock()
    
    data class SystemCommand(val label: String, val detail: String = "") : PreviewBlock()
}
