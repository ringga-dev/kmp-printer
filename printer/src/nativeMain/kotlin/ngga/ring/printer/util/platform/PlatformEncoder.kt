package ngga.ring.printer.util.platform

/**
 * Native Implementation of Platform Encoder.
 */
actual fun encodeString(text: String, charsetName: String): ByteArray {
    // Basic implementation for Web. Browser TextEncoder usually only supports UTF-8.
    // For thermal printer specific encodings like GBK or CP1252, we use a simple fallback
    // or the default text-to-byte conversion if running outside browser.
    
    return try {
        // Simple bit-safe conversion if it's the standard ISO-8859-1 or ASCII
        if (charsetName.uppercase() == "ISO-8859-1" || charsetName.uppercase() == "US-ASCII") {
            ByteArray(text.length) { text[it].code.toByte() }
        } else {
            text.encodeToByteArray()
        }
    } catch (e: Exception) {
        text.encodeToByteArray()
    }
}
