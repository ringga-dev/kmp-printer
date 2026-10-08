package ngga.ring.printer_esc_pos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ngga.ring.printer_esc_pos.ui.screens.PrinterUiShowcaseScreen
import ngga.ring.printer_esc_pos.ui.screens.SetupPrinterScreen
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

enum class AppScreen {
    SETUP,
    UI_SHOWCASE
}

@Composable
fun AppNavigation(
    viewModel: PrinterViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(AppScreen.SETUP) }

    when (currentScreen) {
        AppScreen.SETUP -> {
            SetupPrinterScreen(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateToShowcase = { currentScreen = AppScreen.UI_SHOWCASE }
            )
        }
        AppScreen.UI_SHOWCASE -> {
            PrinterUiShowcaseScreen(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigateBack = { currentScreen = AppScreen.SETUP }
            )
        }
    }
}
