package ngga.ring.printer.util.platform

/**
 * JS implementation of the platform encoder.
 *
 * Delegates to the shared pure Kotlin tables. The previous implementation used
 * `TextEncoder`, which only supports UTF-8, and fell back to UTF-8 for every
 * other charset.
 */
actual fun encodeString(text: String, charsetName: String): ByteArray =
    CharsetEncoder.encode(text, charsetName)