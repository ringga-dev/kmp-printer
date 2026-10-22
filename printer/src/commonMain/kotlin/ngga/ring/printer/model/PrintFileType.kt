package ngga.ring.printer.model

/**
 * Explicit file type for printing documents straight from bytes.
 *
 * [AUTO] sniffs the magic bytes, so callers that already know what they are
 * holding (a PNG from an image picker, a PDF from a download) can pass the exact
 * type and skip detection entirely.
 */
enum class PrintFileType {
    /** Detect from magic bytes: PNG/JPEG/GIF/BMP/WebP become an image, `%PDF` becomes a PDF. */
    AUTO,
    PNG,
    JPEG,
    GIF,
    BMP,
    WEBP,
    PDF;

    companion object {
        private val PNG_SIGNATURE = byteArrayOf(0x89, 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        private val GIF_SIGNATURE = byteArrayOf('G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte())
        private val BMP_SIGNATURE = byteArrayOf('B'.code.toByte(), 'M'.code.toByte())
        private val PDF_SIGNATURE = byteArrayOf('%'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte())

        /**
         * Sniffs the file type from magic bytes.
         *
         * WebP needs 12 bytes (`RIFF` + 4 length bytes + `WEBP`), so short or
         * truncated buffers return `null` rather than a guess.
         *
         * @param data The first bytes of the file. Only the leading 12 bytes are read.
         * @return The detected type, or `null` when the bytes match no known signature.
         */
        fun fromBytes(data: ByteArray): PrintFileType? {
            if (data.isEmpty()) return null
            return when {
                startsWith(data, PNG_SIGNATURE) -> PNG
                startsWith(data, JPEG_SIGNATURE) -> JPEG
                startsWith(data, GIF_SIGNATURE) -> GIF
                startsWith(data, BMP_SIGNATURE) -> BMP
                startsWith(data, PDF_SIGNATURE) -> PDF
                data.size >= 12 &&
                    data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() &&
                    data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte() &&
                    data[8] == 'W'.code.toByte() && data[9] == 'E'.code.toByte() &&
                    data[10] == 'B'.code.toByte() && data[11] == 'P'.code.toByte() -> WEBP
                else -> null
            }
        }

        private fun startsWith(data: ByteArray, signature: ByteArray): Boolean {
            if (data.size < signature.size) return false
            for (i in signature.indices) {
                if (data[i] != signature[i]) return false
            }
            return true
        }
    }
}
