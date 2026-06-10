package ngga.ring.printer_esc_pos

class WasmJsPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmJsPlatform()