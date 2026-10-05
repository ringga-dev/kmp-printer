package ngga.ring.printer.util.escpos

/**
 * Configuration for ESC/POS command generation.
 */
data class ESCPosConfig(
    /** Maximum characters per line for the printer (e.g., 32 for 58mm, 42 for 80mm). */
    val charsPerLine: Int = 32,
    
    /** Printable area width in dots (e.g., 384 for 58mm, 576 for 80mm). */
    val paperWidthDots: Int = 384,

    /** Left margin in dots. */
    val leftMargin: Int = 0,

    /** Line spacing in dots between printed lines (0 = printer default, e.g. ~30 dots). */
    val lineSpacing: Int = 0,

    /** Charset for encoding text. ESC/POS typically uses CP437, but UTF-8 is often supported. */
    val charset: String = "UTF-8"
)
