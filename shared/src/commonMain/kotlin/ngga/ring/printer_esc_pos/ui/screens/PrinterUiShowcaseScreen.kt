package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.*
import ngga.ring.printer.ui.adaptive.AdaptiveBox
import ngga.ring.printer.ui.components.*
import ngga.ring.printer.ui.theme.ReceiptPaperTheme
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

/**
 * Interactive Showcase and Playground for `kmp_printer-ui` Compose Multiplatform components.
 * Features full live print execution, comprehensive hardware settings, adaptive layouts, and authentic paper preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterUiShowcaseScreen(
    viewModel: PrinterViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val previewResult by viewModel.previewResult.collectAsState()
    val printQuality by viewModel.printQuality.collectAsState()
    val charset by viewModel.charset.collectAsState()

    var showScannerSheet by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(ReceiptPaperTheme.ClassicThermal) }
    var showZigzagCut by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(0) } // 0 = Actions & Print, 1 = Settings & Hardware

    val printer = remember { KmpPrinter() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "kmp_printer-ui",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Interactive Showcase & Settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    PrinterStatusBadge(connectionState = connectionState)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        AdaptiveBox(modifier = Modifier.fillMaxSize().padding(innerPadding)) { windowSize ->
            if (windowSize.isExpanded) {
                // Wide Screen / Desktop / Tablet Landscape: 2-Column Split Master-Detail
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Controls & Settings (50%)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShowcaseStatusBanner(
                            printStatus = printStatus,
                            isPrinting = isPrinting,
                            onDismiss = viewModel::resetPrintStatus
                        )

                        ShowcaseDeviceCard(
                            config = config,
                            connectionState = connectionState,
                            onOpenScanner = { showScannerSheet = true },
                            onTestConnection = viewModel::testConnection,
                            onDisconnect = viewModel::disconnect
                        )

                        ShowcaseThemeSelector(
                            selectedTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it },
                            showZigzag = showZigzagCut,
                            onToggleZigzag = { showZigzagCut = it }
                        )

                        ShowcasePrintActionsCard(
                            isPrinting = isPrinting,
                            onPrintReceipt = viewModel::printExpertReceipt,
                            onPrintTestPage = viewModel::printTestPage,
                            onPrintCalibration = viewModel::printCalibrationPage,
                            onBeep = viewModel::beep,
                            onOpenDrawer = viewModel::openCashDrawer
                        )

                        ShowcaseSettingsCard(
                            config = config,
                            quality = printQuality,
                            charset = charset,
                            onApplyProfile = viewModel::applyProfile,
                            onCharsChanged = viewModel::updateCharsPerLine,
                            onQualityChanged = viewModel::setPrintQuality,
                            onCharsetChanged = viewModel::setCharset,
                            onMarginChanged = viewModel::updateLeftMargin
                        )
                    }

                    // Right Column: Live Thermal Receipt Preview (50%)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🧾 Live ESC/POS Receipt Canvas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val activeTheme = selectedTheme.copy(showCutEdgeZigzag = showZigzagCut)
                            ThermalReceiptPreview(
                                previewResult = previewResult,
                                paperTheme = activeTheme
                            )
                        }
                    }
                }
            } else {
                // Compact Screen (Smartphone) / Medium: Tabbed / Scrollable Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Notification Banner
                    ShowcaseStatusBanner(
                        printStatus = printStatus,
                        isPrinting = isPrinting,
                        onDismiss = viewModel::resetPrintStatus
                    )

                    // Navigation Tabs: Actions vs Hardware Settings
                    PrimaryTabRow(
                        selectedTabIndex = currentTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            text = { Text("Actions & Preview") },
                            icon = { Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            text = { Text("Printer Settings") },
                            icon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    if (currentTab == 0) {
                        // Active Printer Device Card
                        ShowcaseDeviceCard(
                            config = config,
                            connectionState = connectionState,
                            onOpenScanner = { showScannerSheet = true },
                            onTestConnection = viewModel::testConnection,
                            onDisconnect = viewModel::disconnect
                        )

                        // Theme & Paper Style Selector
                        ShowcaseThemeSelector(
                            selectedTheme = selectedTheme,
                            onThemeSelected = { selectedTheme = it },
                            showZigzag = showZigzagCut,
                            onToggleZigzag = { showZigzagCut = it }
                        )

                        // Live Thermal Receipt Preview Box
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Live Authentic Thermal Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                val activeTheme = selectedTheme.copy(showCutEdgeZigzag = showZigzagCut)
                                ThermalReceiptPreview(
                                    previewResult = previewResult,
                                    paperTheme = activeTheme
                                )
                            }
                        }

                        // Print Execution Actions
                        ShowcasePrintActionsCard(
                            isPrinting = isPrinting,
                            onPrintReceipt = viewModel::printExpertReceipt,
                            onPrintTestPage = viewModel::printTestPage,
                            onPrintCalibration = viewModel::printCalibrationPage,
                            onBeep = viewModel::beep,
                            onOpenDrawer = viewModel::openCashDrawer
                        )
                    } else {
                        // Printer Settings & Hardware Tuning
                        ShowcaseSettingsCard(
                            config = config,
                            quality = printQuality,
                            charset = charset,
                            onApplyProfile = viewModel::applyProfile,
                            onCharsChanged = viewModel::updateCharsPerLine,
                            onQualityChanged = viewModel::setPrintQuality,
                            onCharsetChanged = viewModel::setCharset,
                            onMarginChanged = viewModel::updateLeftMargin
                        )
                    }
                }
            }
        }
    }

    // Adaptive Discovery Sheet / Dialog
    if (showScannerSheet) {
        PrinterDiscoverySheet(
            printer = printer,
            onDismiss = { showScannerSheet = false },
            onDeviceSelected = { device ->
                viewModel.selectPrinter(device.toDiscoveredPrinter())
                showScannerSheet = false
            }
        )
    }
}

/**
 * Animated Status Banner for Printing / Hardware Events.
 */
@Composable
private fun ShowcaseStatusBanner(
    printStatus: PrintStatus,
    isPrinting: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = isPrinting || printStatus !is PrintStatus.Idle) {
        val (bgColor, contentColor, icon, text) = when {
            isPrinting || printStatus is PrintStatus.Processing -> Quadruple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Icons.Rounded.HourglassTop,
                "Sedang mencetak dokumen ke printer..."
            )
            printStatus is PrintStatus.Success -> Quadruple(
                Color(0xFFDCFCE7),
                Color(0xFF166534),
                Icons.Rounded.CheckCircle,
                "Pencetakan Berhasil Dikirim!"
            )
            printStatus is PrintStatus.Error -> Quadruple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                Icons.Rounded.ErrorOutline,
                "Gagal Mencetak: ${printStatus.message}"
            )
            else -> Quadruple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Rounded.Info,
                "Status: $printStatus"
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            contentColor = contentColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isPrinting || printStatus is PrintStatus.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!isPrinting) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Active Device Summary Card.
 */
@Composable
private fun ShowcaseDeviceCard(
    config: PrinterConfig,
    connectionState: ConnectionState,
    onOpenScanner: () -> Unit,
    onTestConnection: () -> Unit,
    onDisconnect: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (config.connectionType.uppercase()) {
                                    "BLUETOOTH", "BLE" -> Icons.Rounded.Bluetooth
                                    "USB" -> Icons.Rounded.Usb
                                    "NETWORK" -> Icons.Rounded.Wifi
                                    else -> Icons.Rounded.Print
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = config.name.ifBlank { "Printer Belum Dipilih" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${config.connectionType} • ${config.address ?: "Virtual Mode"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                PrinterStatusBadge(connectionState = connectionState)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = onOpenScanner,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Device", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Cable, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Link", fontSize = 12.sp)
                }

                if (connectionState is ConnectionState.Connected) {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Disconnect", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * Theme & Receipt Paper Style Selector.
 */
@Composable
private fun ShowcaseThemeSelector(
    selectedTheme: ReceiptPaperTheme,
    onThemeSelected: (ReceiptPaperTheme) -> Unit,
    showZigzag: Boolean,
    onToggleZigzag: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🎨 Receipt Paper Appearance",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTheme == ReceiptPaperTheme.ClassicThermal,
                    onClick = { onThemeSelected(ReceiptPaperTheme.ClassicThermal) },
                    label = { Text("Classic Ivory", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTheme == ReceiptPaperTheme.FreshWhite,
                    onClick = { onThemeSelected(ReceiptPaperTheme.FreshWhite) },
                    label = { Text("Crisp White", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTheme == ReceiptPaperTheme.DarkTerminal,
                    onClick = { onThemeSelected(ReceiptPaperTheme.DarkTerminal) },
                    label = { Text("Dark Matrix", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Paper Tear Zigzag Effect", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = showZigzag,
                    onCheckedChange = onToggleZigzag
                )
            }
        }
    }
}

/**
 * Print Execution Buttons Card.
 */
@Composable
private fun ShowcasePrintActionsCard(
    isPrinting: Boolean,
    onPrintReceipt: () -> Unit,
    onPrintTestPage: () -> Unit,
    onPrintCalibration: () -> Unit,
    onBeep: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "⚡ Print Execution & Hardware Control",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Primary Print Button
            Button(
                onClick = onPrintReceipt,
                enabled = !isPrinting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cetak Struk POS Sekarang", fontWeight = FontWeight.Bold)
            }

            // Secondary Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = onPrintTestPage,
                    enabled = !isPrinting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Test Page", fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = onPrintCalibration,
                    enabled = !isPrinting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Kalibrasi", fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onBeep,
                    enabled = !isPrinting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buzzer Beep", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onOpenDrawer,
                    enabled = !isPrinting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Buka Laci", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Hardware Settings & Paper Dimension Controls.
 */
@Composable
private fun ShowcaseSettingsCard(
    config: PrinterConfig,
    quality: PrintQuality,
    charset: PrinterCharset,
    onApplyProfile: (PrinterProfile) -> Unit,
    onCharsChanged: (Int) -> Unit,
    onQualityChanged: (PrintQuality) -> Unit,
    onCharsetChanged: (PrinterCharset) -> Unit,
    onMarginChanged: (Int) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "⚙️ Paper Dimension & ESC/POS Tuning",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Paper Width Profile Chips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ukuran Kertas & Profil:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = config.paperWidthDots <= 384,
                        onClick = { onApplyProfile(PrinterProfile.MM58) },
                        label = { Text("58mm (32 Chars / 384 Dots)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = config.paperWidthDots > 384,
                        onClick = { onApplyProfile(PrinterProfile.MM80) },
                        label = { Text("80mm (48 Chars / 576 Dots)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Characters Per Line Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Karakter Per Baris (CPL):", style = MaterialTheme.typography.bodySmall)
                    Text("${config.characterPerLine} kolom", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = config.characterPerLine.toFloat(),
                    onValueChange = { onCharsChanged(it.toInt()) },
                    valueRange = 24f..64f,
                    steps = 39
                )
            }

            // Left Margin Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Left Margin:", style = MaterialTheme.typography.bodySmall)
                    Text("${config.leftMargin} dots", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = config.leftMargin.toFloat(),
                    onValueChange = { onMarginChanged(it.toInt()) },
                    valueRange = 0f..80f,
                    steps = 15
                )
            }

            // Print Quality / Density
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Kualitas Cetak (Density):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = quality == PrintQuality.Light,
                        onClick = { onQualityChanged(PrintQuality.Light) },
                        label = { Text("Light") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = quality == PrintQuality.Default,
                        onClick = { onQualityChanged(PrintQuality.Default) },
                        label = { Text("Default") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = quality == PrintQuality.Dark,
                        onClick = { onQualityChanged(PrintQuality.Dark) },
                        label = { Text("Dark") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
