package ngga.ring.printer.util.escpos

/**
 * Interface for rendering documents (PDF, SVG) into ESC/POS bytes.
 */
interface ESCPosRenderer {
    /**
     * Renders a specific page of a PDF file.
     * @param data The PDF file bytes.
     * @param pageIndex Index of the page (0-indexed).
     * @param targetWidth Width in dots (usually 384 for 58mm, 576 for 80mm).
     * @return BooleanArray (bitonal) prepared for ESC/POS printing.
     */
    suspend fun renderPdfPage(data: ByteArray, pageIndex: Int, targetWidth: Int): BooleanArray?
    
    /**
     * Renders an SVG string into bitonal bytes.
     */
    suspend fun renderSvg(svgString: String, targetWidth: Int): BooleanArray?

    /**
     * Returns the number of pages in a PDF document.
     *
     * Useful to loop over every page before printing. Returns `0` when the
     * platform cannot read the document (for example PDF support is not
     * available on the web target) or when the bytes are not a valid PDF.
     *
     * @param data The PDF file bytes.
     * @return Page count, or 0 when the document cannot be read.
     */
    fun pdfPageCount(data: ByteArray): Int
}

/**
 * Expect declaration for factory to get the platform-specific renderer.
 */
expect fun getPlatformRenderer(): ESCPosRenderer
