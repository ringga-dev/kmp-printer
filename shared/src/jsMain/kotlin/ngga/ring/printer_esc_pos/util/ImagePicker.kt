package ngga.ring.printer_esc_pos.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberImagePicker(onImagePicked: (Any, ImageBitmap) -> Unit): () -> Unit {
    return {
        // Not implemented for JS
    }
}
