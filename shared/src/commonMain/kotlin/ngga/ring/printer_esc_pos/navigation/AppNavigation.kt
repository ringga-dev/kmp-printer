package ngga.ring.printer_esc_pos.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ngga.ring.printer_esc_pos.ui.screens.CodePreviewScreen
import ngga.ring.printer_esc_pos.ui.screens.PrintPreviewScreen
import ngga.ring.printer_esc_pos.ui.screens.SetupPrinterScreen
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@Composable
fun AppNavigation(viewModel: PrinterViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.SetupPrinter) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppScreen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding)
        ) { screen ->
            when (screen) {
                AppScreen.SetupPrinter -> SetupPrinterScreen(viewModel)
                AppScreen.CodePreview -> CodePreviewScreen(viewModel)
                AppScreen.PrintPreview -> PrintPreviewScreen(viewModel)
            }
        }
    }
}
