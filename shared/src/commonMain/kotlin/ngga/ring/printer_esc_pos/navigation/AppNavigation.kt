package ngga.ring.printer_esc_pos.navigation

import androidx.compose.runtime.Composable
import ngga.ring.printer_esc_pos.ui.screens.SetupPrinterScreen
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@Composable
fun AppNavigation(
    viewModel: PrinterViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    SetupPrinterScreen(
        viewModel = viewModel,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme
    )
}
