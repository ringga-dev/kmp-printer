package ngga.ring.printer_esc_pos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen(
    val icon: ImageVector,
    val label: String
) {
    SetupPrinter(
        icon = Icons.Filled.Build,
        label = "Setup Printer"
    ),
    CodePreview(
        icon = Icons.Filled.Code,
        label = "Code Preview"
    ),
    PrintPreview(
        icon = Icons.Filled.Image,
        label = "Print Preview"
    )
}
