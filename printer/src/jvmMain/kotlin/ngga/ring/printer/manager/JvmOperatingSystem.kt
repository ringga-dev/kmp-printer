package ngga.ring.printer.manager

enum class JvmOperatingSystem {
    WINDOWS,
    LINUX,
    MACOS,
    OTHER;

    companion object {
        fun current(): JvmOperatingSystem {
            val name = System.getProperty("os.name").lowercase()
            return when {
                name.contains("win") -> WINDOWS
                name.contains("linux") -> LINUX
                name.contains("mac") || name.contains("darwin") -> MACOS
                else -> OTHER
            }
        }
    }
}
