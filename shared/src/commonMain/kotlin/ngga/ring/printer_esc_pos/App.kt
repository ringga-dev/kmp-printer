package ngga.ring.printer_esc_pos

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ngga.ring.printer_esc_pos.ui.screens.*
import ngga.ring.printer_esc_pos.ui.theme.PrinterTheme
import ngga.ring.printer_esc_pos.ui.components.ConnectionBadge
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(viewModel: PrinterViewModel = viewModel { PrinterViewModel() }) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    val connectionState by viewModel.connectionState.collectAsState()
    val config by viewModel.config.collectAsState()

    PrinterTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "KmpPrinter Pro",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = (-0.5).sp
                            )
                            ConnectionBadge(
                                isConnected = connectionState.isConnected(),
                                name = config.name
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 0.dp
                ) {
                    NavButton(selectedTab == 0, Icons.Default.Radar, "Discovery") { selectedTab = 0 }
                    NavButton(selectedTab == 1, Icons.Default.ReceiptLong, "Studio") { selectedTab = 1 }
                    NavButton(selectedTab == 2, Icons.Default.Settings, "Config") { selectedTab = 2 }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Background Aesthetic Glow
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 120.dp, y = (-80).dp)
                        .blur(120.dp)
                        .alpha(0.15f)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )

                AnimatedContent(
                    targetState = currentSubScreen ?: selectedTab.toString(),
                    transitionSpec = {
                        fadeIn(tween(400)) + slideInHorizontally { if (targetState != "null") it else -it } togetherWith
                        fadeOut(tween(400)) + slideOutHorizontally { if (targetState != "null") -it else it }
                    }
                ) { target ->
                    when (target) {
                        "0" -> DiscoveryScreen(viewModel)
                        "1" -> StudioScreen(viewModel)
                        "2" -> ConfigScreen(
                            viewModel = viewModel,
                            onLaunchCalibration = { currentSubScreen = "calibration" }
                        )
                        "calibration" -> CalibrationScreen(
                            viewModel = viewModel,
                            onBack = { currentSubScreen = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.NavButton(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    )
}