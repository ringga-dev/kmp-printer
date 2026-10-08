package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.PrinterDevice
import ngga.ring.printer.model.PrinterDevices
import ngga.ring.printer.ui.components.*
import ngga.ring.printer.ui.theme.ReceiptPaperTheme
import ngga.ring.printer.util.ConnectionState
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.ESCPosConfig
import ngga.ring.printer.util.preview.PreviewResult

/**
 * Interactive Sample & Showcase Screen for `kmp_printer-ui` Compose Multiplatform components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterUiShowcaseScreen(
    onNavigateBack: () -> Unit
) {
    val printer = remember { KmpPrinter() }
    var showScannerSheet by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(ReceiptPaperTheme.ClassicThermal) }
    var paperWidth by remember { mutableStateOf(58.dp) }
    var selectedDevice by remember { mutableStateOf<PrinterDevice?>(null) }
    var simulatedConnectionState by remember { 
        mutableStateOf<ConnectionState>(ConnectionState.Connected("Epson TM-T88VI", "00:11:22:33:44:55")) 
    }

    val demoPreviewResult = remember(paperWidth) {
        val is80mm = paperWidth > 60.dp
        val chars = if (is80mm) 48 else 32
        val dots = if (is80mm) 560 else 384
        val config = ESCPosConfig(charsPerLine = chars, paperWidthDots = dots)
        val builder = ESCPosCommandBuilder(config).initialize()
        
        builder.alignCenter().bold(true).line("☕ CAFE ANTIGRAVITY").bold(false)
            .line("Jl. Sudirman 108, Jakarta")
            .line("Order #20261008-01")
            .divider('-')
            .alignLeft()
            .segmentedLine("1x Americano Hot", "Rp 25.000")
            .segmentedLine("2x Croissant Butter", "Rp 44.000")
            .divider('-')
            .segmentedLine("Subtotal", "Rp 69.000")
            .segmentedLine("PPN (10%)", "Rp 6.900")
            .bold(true).segmentedLine("TOTAL", "Rp 75.900").bold(false)
            .divider('-')
            .alignCenter()
            .line("Terima Kasih Atas Kunjungan Anda!")
            .line("WiFi: Antigravity-Guest")
            .feed(1)
            .qrCode("https://github.com/ringga-dev", size = 4)
            .feed(2)
        
        val bytes = builder.build()
        PreviewResult.from(builder.buildPreview(), config, bytes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎨 kmp_printer-ui Showcase") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showScannerSheet = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Scan Printers")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Quick Actions & Status
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Printer Status Badge & Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Current State:", style = MaterialTheme.typography.bodyMedium)
                        PrinterStatusBadge(connectionState = simulatedConnectionState)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                simulatedConnectionState = when (simulatedConnectionState) {
                                    is ConnectionState.Connected -> ConnectionState.Connecting
                                    ConnectionState.Connecting -> ConnectionState.Disconnected
                                    ConnectionState.Disconnected -> ConnectionState.Error("Paper Out / Cover Open")
                                    is ConnectionState.Error -> ConnectionState.Connected("Epson TM-T88VI", "00:11:22:33:44:55")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cycle State", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showScannerSheet = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Scanner", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 2: Device Card Showcase
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2. Adaptive Printer Device Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val demoDevice = selectedDevice ?: PrinterDevices.bluetooth(
                        name = "Epson TM-T88VI (Paired)",
                        macAddress = "00:11:22:33:44:55"
                    )

                    PrinterDeviceCard(
                        device = demoDevice,
                        isSelected = true,
                        onSelect = { showScannerSheet = true },
                        onTestClick = {}
                    )
                }
            }

            // Section 3: Live Thermal Receipt Preview
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "3. Thermal Receipt Preview (Live Canvas)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Theme selector chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedTheme == ReceiptPaperTheme.ClassicThermal,
                            onClick = { selectedTheme = ReceiptPaperTheme.ClassicThermal },
                            label = { Text("Classic Ivory", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedTheme == ReceiptPaperTheme.FreshWhite,
                            onClick = { selectedTheme = ReceiptPaperTheme.FreshWhite },
                            label = { Text("Crisp White", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedTheme == ReceiptPaperTheme.DarkTerminal,
                            onClick = { selectedTheme = ReceiptPaperTheme.DarkTerminal },
                            label = { Text("Dark Terminal", fontSize = 11.sp) }
                        )
                    }

                    // Width selector chips (58mm vs 80mm)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = paperWidth == 58.dp,
                            onClick = { paperWidth = 58.dp },
                            label = { Text("58mm Paper") }
                        )
                        FilterChip(
                            selected = paperWidth == 80.dp,
                            onClick = { paperWidth = 80.dp },
                            label = { Text("80mm Paper") }
                        )
                    }

                    // Authentic Receipt Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThermalReceiptPreview(
                            previewResult = demoPreviewResult,
                            paperTheme = selectedTheme
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
                selectedDevice = device
                simulatedConnectionState = ConnectionState.Connected(device.name, device.address)
                showScannerSheet = false
            }
        )
    }
}
