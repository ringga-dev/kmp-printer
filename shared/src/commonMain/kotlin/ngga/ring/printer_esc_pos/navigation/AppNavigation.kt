package ngga.ring.printer_esc_pos.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import ngga.ring.printer_esc_pos.ui.screens.SetupPrinterScreen
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@Composable
fun AppNavigation(viewModel: PrinterViewModel) {
    SetupPrinterScreen(viewModel)
}
