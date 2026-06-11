package ngga.ring.printer.util

enum class PrinterLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

data class PrinterLogEvent(
    val level: PrinterLogLevel,
    val tag: String,
    val message: String,
    val error: String? = null
)

object PrinterLogger {
    private var sink: ((PrinterLogEvent) -> Unit)? = null

    fun setSink(logger: ((PrinterLogEvent) -> Unit)?) {
        sink = logger
    }

    fun debug(tag: String, message: String) = log(PrinterLogLevel.DEBUG, tag, message)

    fun info(tag: String, message: String) = log(PrinterLogLevel.INFO, tag, message)

    fun warn(tag: String, message: String, error: Throwable? = null) {
        log(PrinterLogLevel.WARN, tag, message, error?.message)
    }

    fun error(tag: String, message: String, error: Throwable? = null) {
        log(PrinterLogLevel.ERROR, tag, message, error?.message)
    }

    private fun log(level: PrinterLogLevel, tag: String, message: String, error: String? = null) {
        val event = PrinterLogEvent(level, tag, message, error)
        sink?.invoke(event)
    }
}
