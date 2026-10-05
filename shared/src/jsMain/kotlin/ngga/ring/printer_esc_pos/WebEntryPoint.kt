package ngga.ring.printer_esc_pos

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * JS entry point for the browser sample.
 *
 * Mounts the Compose Multiplatform UI into the HTML element identified by
 * [containerId]. Passing `null` uses `<body>`.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalJsExport::class)
@JsExport
fun mountApp(containerId: String? = null) {
    ComposeViewport(viewportContainerId = containerId) {
        App()
    }
}