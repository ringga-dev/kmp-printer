package ngga.ring.printer_esc_pos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer_esc_pos.navigation.AppNavigation
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val viewModel = remember { PrinterViewModel() }
            AppNavigation(viewModel)
        }
    }
}
