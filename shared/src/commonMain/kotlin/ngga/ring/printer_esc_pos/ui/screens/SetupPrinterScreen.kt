package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ngga.ring.printer.model.PrintQuality
import ngga.ring.printer.model.PrinterCharset
import ngga.ring.printer.model.PrinterProfile
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer_esc_pos.ui.components.ConnectionStatusBadge
import ngga.ring.printer_esc_pos.ui.components.DeviceItemCard
import ngga.ring.printer_esc_pos.ui.components.HexDumpViewer
import ngga.ring.printer_esc_pos.ui.components.PrintStatusBanner
import ngga.ring.printer_esc_pos.ui.components.SectionCard
import ngga.ring.printer_esc_pos.ui.components.ThermalReceiptView
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPrinterScreen(
    viewModel: PrinterViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val quality by viewModel.printQuality.collectAsState()
    val charset by viewModel.charset.collectAsState()
    val availableModes by viewModel.availableModes.collectAsState()
    val showVirtual by viewModel.showVirtual.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredPrinters by viewModel.discoveredPrinters.collectAsState()
    val log by viewModel.discoveryLog.collectAsState()
    val previewResult by viewModel.previewResult.collectAsState()
    val rawHex by viewModel.rawCommandHex.collectAsState()
    val rawBytes by viewModel.rawCommandBytes.collectAsState()
    val dithering by viewModel.imagingDithering.collectAsState()
    val contrast by viewModel.imagingContrast.collectAsState()
    val brightness by viewModel.imagingBrightness.collectAsState()

    var showReceiptPreview by remember { mutableStateOf(true) }
    var showHexInspector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Print,
                                contentDescription = null,
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "KmpPrinter Sample",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ESC/POS Multiplatform Library",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    ConnectionStatusBadge(connectionState = connectionState)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrintStatusBanner(
                        status = printStatus,
                        onDismiss = viewModel::resetPrintStatus,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = viewModel::testConnection,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Ping")
                        }

                        Button(
                            onClick = viewModel::printTestPage,
                            enabled = !isPrinting,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(2f)
                        ) {
                            if (isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mencetak…")
                            } else {
                                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cetak Test Page")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. KONEKSI & TRANSPORT ─────────────────────────────────────────
            SectionCard(
                title = "Koneksi & Transport",
                subtitle = "Pilih konektor dan konfigurasikan alamat perangkat",
                icon = Icons.Rounded.Cable
            ) {
                // Transport Mode Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableModes.forEach { mode ->
                        val isSelected = config.connectionType.equals(mode, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setConnectionType(mode) },
                            label = { Text(mode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Virtual Printer Simulator", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showVirtual, onCheckedChange = viewModel::toggleVirtual)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = config.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Nama Printer") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    OutlinedTextField(
                        value = config.address ?: "",
                        onValueChange = viewModel::updateAddress,
                        label = { Text("Address (IP/MAC/VID:PID)") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                }

                if (config.connectionType.equals("NETWORK", ignoreCase = true)) {
                    OutlinedTextField(
                        value = config.port.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::updatePort) },
                        label = { Text("Port (Default: 9100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                }

                if (config.connectionType.equals("SERIAL", ignoreCase = true)) {
                    OutlinedTextField(
                        value = config.baudRate.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::updateBaudRate) },
                        label = { Text("Baud Rate (9600, 115200)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                }

                // Discovery / Scanner Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cari Perangkat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (isScanning) {
                        OutlinedButton(
                            onClick = viewModel::cancelDiscovery,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Batal")
                        }
                    } else {
                        FilledTonalButton(onClick = viewModel::startDiscovery, shape = MaterialTheme.shapes.small) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan")
                        }
                    }
                }

                if (log.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = MaterialTheme.shapes.small,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                discoveredPrinters.forEach { device ->
                    DeviceItemCard(
                        device = device,
                        isSelected = config.address == device.address && config.connectionType.equals(device.connectionType, ignoreCase = true),
                        onSelect = { viewModel.selectPrinter(device) }
                    )
                }
            }

            // ── 2. FORMAT KERTAS & MARGIN ──────────────────────────────────────
            SectionCard(
                title = "Kertas & Margin",
                subtitle = "Preset lebar kertas, padding tepi, dan line spacing",
                icon = Icons.Rounded.SquareFoot
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val is58 = config.paperWidthDots == 384
                    FilterChip(
                        selected = is58,
                        onClick = { viewModel.applyProfile(PrinterProfile.MM58) },
                        label = { Text("58mm (384 Dots)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = !is58,
                        onClick = { viewModel.applyProfile(PrinterProfile.MM80) },
                        label = { Text("80mm (576 Dots)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.characterPerLine.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::updateCharsPerLine) },
                        label = { Text("Char/Line") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    OutlinedTextField(
                        value = config.paperWidthDots.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::updatePaperDots) },
                        label = { Text("Paper Dots") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.leftMargin.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::updateLeftMargin) },
                        label = { Text("Left Margin") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                    OutlinedTextField(
                        value = config.rightMargin.toString(),
                        onValueChange = { it.toIntOrNull()?.let(viewModel::updateRightMargin) },
                        label = { Text("Right Margin") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Center Margin", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = config.autoCenter, onCheckedChange = viewModel::updateAutoCenter)
                }
            }

            // ── 3. DENSITY & ENCODING ──────────────────────────────────────────
            SectionCard(
                title = "Output Density & Charset",
                subtitle = "Pengaturan pemanasan head dan simbol karakter",
                icon = Icons.Rounded.Tune
            ) {
                Text("Print Quality / Density:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PrintQuality.Light to "Light",
                        PrintQuality.Default to "Default",
                        PrintQuality.Dark to "Dark"
                    ).forEach { (q, label) ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { viewModel.setPrintQuality(q) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text("Charset Encoding:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrinterCharset.entries.forEach { cs ->
                        FilterChip(
                            selected = charset == cs,
                            onClick = { viewModel.setCharset(cs) },
                            label = { Text(cs.value) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }

            // ── 4. LIBRARY FEATURE SHOWCASE ────────────────────────────────────
            SectionCard(
                title = "Library Feature Showcase",
                subtitle = "Uji coba fungsionalitas dan kapabilitas lengkap KmpPrinter",
                icon = Icons.Rounded.Bolt
            ) {
                // Feature Buttons Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureButton(
                        label = "Enterprise POS Struk",
                        icon = Icons.Rounded.Receipt,
                        enabled = !isPrinting,
                        onClick = viewModel::printExpertReceipt,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureButton(
                        label = "Barcode & QR Suite",
                        icon = Icons.Rounded.QrCode2,
                        enabled = !isPrinting,
                        onClick = viewModel::printBarcodeSuite,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureButton(
                        label = "Page Mode (XY Coord)",
                        icon = Icons.Rounded.Grid4x4,
                        enabled = !isPrinting,
                        onClick = viewModel::printPageModeDemo,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureButton(
                        label = "Kalibrasi Dot Grid",
                        icon = Icons.Rounded.SquareFoot,
                        enabled = !isPrinting,
                        onClick = viewModel::printCalibrationPage,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureButton(
                        label = "Buka Laci Kasir",
                        icon = Icons.Rounded.MeetingRoom,
                        enabled = !isPrinting,
                        onClick = viewModel::openCashDrawer,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureButton(
                        label = "Buzzer Beep Alarm",
                        icon = Icons.Rounded.NotificationsActive,
                        enabled = !isPrinting,
                        onClick = viewModel::beep,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Stress Test Button
                OutlinedButton(
                    onClick = viewModel::runStressTest,
                    enabled = !isPrinting,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("10x Concurrency Mutex Stress Test")
                }
            }

            // ── 5. VIRTUAL RECEIPT & HEX INSPECTOR ──────────────────────────────
            SectionCard(
                title = "Virtual Receipt & Bytecode Inspector",
                subtitle = "Simulasi visual hasil cetak & analisis byte biner ESC/POS",
                icon = Icons.Rounded.Code,
                trailingAction = {
                    FilledTonalButton(
                        onClick = viewModel::buildRawHex,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Dump Hex")
                    }
                }
            ) {
                ThermalReceiptView(
                    preview = previewResult,
                    modifier = Modifier.fillMaxWidth()
                )

                HexDumpViewer(
                    hexString = rawHex,
                    totalBytes = rawBytes,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FeatureButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
