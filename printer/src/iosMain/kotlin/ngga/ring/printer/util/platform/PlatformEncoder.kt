package ngga.ring.printer.util.platform

import platform.Foundation.*
import platform.posix.memcpy
import kotlinx.cinterop.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun encodeString(text: String, charsetName: String): ByteArray {
    val nsString = NSString.create(string = text)
    val encoding = when (charsetName.uppercase()) {
        "UTF-8" -> NSUTF8StringEncoding
        "WINDOWS-1252" -> NSWindowsCP1252StringEncoding
        "ISO-8859-1" -> NSISOLatin1StringEncoding
        "GBK", "GB2312" -> 0x80000632u // GB 18030
        "BIG5" -> 0x80000635u
        else -> NSUTF8StringEncoding
    }
    
    val data = nsString.dataUsingEncoding(encoding.toLong().toULong()) ?: return text.encodeToByteArray()
    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
}
