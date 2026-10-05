package ngga.ring.printer.util.platform

/**
 * iOS implementation of the platform encoder.
 *
 * Delegates to the shared pure Kotlin tables. The previous implementation used
 * `NSString.dataUsingEncoding`, which returns null for code pages the platform
 * does not handle and then silently fell back to UTF-8.
 */
actual fun encodeString(text: String, charsetName: String): ByteArray =
    CharsetEncoder.encode(text, charsetName)