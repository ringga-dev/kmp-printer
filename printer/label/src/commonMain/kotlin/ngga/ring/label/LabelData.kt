package ngga.ring.label

/**
 * Available label printer driver protocols.
 */
public enum class LabelProtocol {
    /** Zebra ZPL (Zebra Programming Language) — industrial/label printers */
    ZPL,

    /** Brother Raster Command Set — QL/TD series label printers */
    BROTHER_QL,

    /** Brother TD — transport/label printers */
    BROTHER_TD,

    /** Citizen CPCL — portable label printers */
    CPCL
}

/**
 * Pre-defined label sizes in millimetres.
 * Width is the printable area, height is full label height.
 */
public enum class LabelSize(val labelWidthMm: Int, val labelHeightMm: Int?, val description: String) {
    // Brother QL common sizes
    QL_12(12, null, "Brother 12mm tape"),
    QL_29(29, null, "Brother 29mm tape"),
    QL_38(38, null, "Brother 38mm tape"),
    QL_50(50, null, "Brother 50mm tape"),
    QL_54(54, null, "Brother 54mm tape"),
    QL_62(62, null, "Brother 62mm tape"),
    QL_102(102, null, "Brother 102mm tape"),

    // Zebra common sizes
    ZEBRA_4X6(101, 152, "Zebra 4×6 inch (101×152mm)"),
    ZEBRA_4X4(101, 101, "Zebra 4×4 inch (101×101mm)"),
    ZEBRA_4X2(101, 50, "Zebra 4×2 inch (101×50mm)"),
    ZEBRA_3X2(76, 50, "Zebra 3×2 inch (76×50mm)"),
    ZEBRA_2X1(50, 25, "Zebra 2×1 inch (50×25mm)");

    public companion object {
        /** Default Zebra shipping label */
        public val defaultZebra: LabelSize get() = ZEBRA_4X6

        /** Default Brother label tape */
        public val defaultBrother: LabelSize get() = QL_62
    }
}

/**
 * Barcode format for label printing.
 */
public enum class BarcodeFormat(val description: String) {
    CODE_128("Code 128 — alphanumeric, general purpose"),
    CODE_39("Code 39 — alphanumeric, older standard"),
    EAN_13("EAN-13 — retail product barcode"),
    UPC_A("UPC-A — US retail product barcode"),
    QR("QR Code — 2D, URLs, up to 4K chars"),
    DATAMATRIX("Data Matrix — 2D, industrial/medical"),
    PDF417("PDF417 — 2D stacked, transport/logistics"),
    ITF("Interleaved 2 of 5 — warehouse/distribution")
}

/**
 * Horizontal text alignment within a label element.
 */
public enum class TextAlign {
    LEFT, CENTER, RIGHT
}

/**
 * Font size / height in dots (at 203 DPI).
 */
public enum class FontSize(val dots: Int) {
    SIZE_0(8),
    SIZE_1(16),
    SIZE_2(24),
    SIZE_3(32),
    SIZE_4(40),
    SIZE_5(48);

    public companion object {
        /** Default body text size */
        public val body: FontSize get() = SIZE_1
        /** Default header text size */
        public val header: FontSize get() = SIZE_3
    }
}

/**
 * A single element placed on a label.
 */
public sealed class LabelElement {
    /** Fixed text at a given position */
    public data class Text(
        val text: String,
        val x: Int = 20,
        val y: Int = 10,
        val fontSize: FontSize = FontSize.body,
        val bold: Boolean = false,
        val align: TextAlign = TextAlign.LEFT
    ) : LabelElement()

    /** Barcode or QR code */
    public data class Barcode(
        val data: String,
        val format: BarcodeFormat,
        val x: Int = 20,
        val y: Int = 50,
        val heightDots: Int = 100,
    ) : LabelElement()

    /** Horizontal separator line */
    public data class Separator(
        val x: Int = 0,
        val y: Int,
        val width: Int = 800
    ) : LabelElement()

    /** Empty space / vertical gap */
    public data class Spacer(
        val height: Int
    ) : LabelElement()

    /** Price with currency formatting */
    public data class Price(
        val amount: String,
        val currency: String = "Rp",
        val x: Int = 20,
        val y: Int,
        val fontSize: FontSize = FontSize.SIZE_3,
        val bold: Boolean = true
    ) : LabelElement()
}

/**
 * Complete label definition sent to a builder.
 */
public data class LabelData(
    val labelSize: LabelSize,
    val elements: List<LabelElement>,
    val copies: Int = 1,
    val rotate: Boolean = false
)
