package ngga.ring.printer_esc_pos.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * A platform-agnostic image picker.
 * Returns a lambda that triggers the picker.
 */
@Composable
expect fun rememberImagePicker(onImagePicked: (Any, ImageBitmap) -> Unit): () -> Unit
