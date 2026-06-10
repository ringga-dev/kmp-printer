package ngga.ring.printer_esc_pos.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import javax.imageio.ImageIO

@Composable
actual fun rememberImagePicker(onImagePicked: (Any, ImageBitmap) -> Unit): () -> Unit {
    return {
        val fileChooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Images", "jpg", "png", "jpeg")
        }
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                val bufferedImage = ImageIO.read(file)
                if (bufferedImage != null) {
                    onImagePicked(bufferedImage, bufferedImage.toComposeImageBitmap())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
