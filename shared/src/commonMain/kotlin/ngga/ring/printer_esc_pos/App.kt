package ngga.ring.printer_esc_pos

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ngga.ring.printer_esc_pos.navigation.AppNavigation
import ngga.ring.printer_esc_pos.ui.theme.AppTheme
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@Composable
fun App() {
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDark) }
    val viewModel = remember { PrinterViewModel() }

    AppTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            AppNavigation(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme }
            )
        }
    }
}
