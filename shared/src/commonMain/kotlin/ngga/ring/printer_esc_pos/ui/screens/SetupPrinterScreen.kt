package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer_esc_pos.ui.components.ConnectionTypeGrid
import ngga.ring.printer_esc_pos.ui.components.DiscoveredDeviceList
import ngga.ring.printer_esc_pos.ui.components.PaperSizeSection
import ngga.ring.printer_esc_pos.ui.components.PresetOption
import ngga.ring.printer_esc_pos.ui.components.PresetSelector
import ngga.ring.printer_esc_pos.ui.components.PrinterStatusBadge
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPrinterScreen(viewModel: PrinterViewModel) {
    val config by viewModel.config.collectAsState()
    val discoveredDevices by viewModel.discoveredPrinters.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val log by viewModel.discoveryLog.collectAsState()

    var step by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Setup Printer", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        StepIndicator(current = step, modifier = Modifier.padding(horizontal = 16.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                slideInHorizontally { it * dir } + fadeIn() togetherWith
                    slideOutHorizontally { -it * dir } + fadeOut()
            },
            modifier = Modifier.weight(1f)
        ) { currentStep ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    0 -> ConnectionStep(
                        selectedType = config.connectionType,
                        onTypeSelected = { viewModel.setConnectionType(it) }
                    )
                    1 -> DeviceStep(
                        config = config,
                        devices = discoveredDevices,
                        isScanning = isScanning,
                        viewModel = viewModel
                    )
                    2 -> ReviewStep(
                        config = config,
                        log = log,
                        connectionState = connectionState,
                        printStatus = printStatus,
                        viewModel = viewModel
                    )
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    log,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                if (step < 2) {
                    Button(
                        onClick = { step++ },
                        enabled = when (step) {
                            0 -> config.connectionType.isNotBlank()
                            1 -> config.connectionType == "VIRTUAL" || config.address?.isNotBlank() == true
                            else -> true
                        }
                    ) {
                        Text("Next")
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, modifier: Modifier = Modifier) {
    val steps = listOf("Connection", "Device", "Review")
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { i, label ->
            val isActive = i == current
            val isDone = i < current
            val color = when {
                isDone -> Color(0xFF2E7D32)
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp),
                            tint = Color.White
                        )
                    } else {
                        Text(
                            "${i + 1}",
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (i < steps.lastIndex) {
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive || isDone) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            } else {
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive || isDone) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionStep(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Choose Connection Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Select how your printer connects to this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        ConnectionTypeGrid(
            selectedType = selectedType,
            onTypeSelected = onTypeSelected
        )
    }
}

@Composable
private fun DeviceStep(
    config: ngga.ring.printer.model.PrinterConfig,
    devices: List<DiscoveredPrinter>,
    isScanning: Boolean,
    viewModel: PrinterViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Configure Device",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Connection: ${config.connectionType}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = config.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Printer Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        when (config.connectionType) {
            "NETWORK" -> {
                OutlinedTextField(
                    value = config.address ?: "",
                    onValueChange = { viewModel.updateAddress(it) },
                    label = { Text("IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.port.toString(),
                    onValueChange = { viewModel.updatePort(it.toIntOrNull() ?: 9100) },
                    label = { Text("TCP Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            "VIRTUAL" -> {
                Text(
                    "Virtual mode \u2014 no physical device needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            else -> {
                    var useAuto by remember { mutableStateOf(config.address.isNullOrBlank()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = useAuto,
                            onClick = { useAuto = true },
                            label = { Text("Auto") },
                            leadingIcon = {
                                if (useAuto) Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !useAuto,
                            onClick = { useAuto = false },
                            label = { Text("Manual") },
                            leadingIcon = {
                                if (!useAuto) Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (useAuto) {
                        OutlinedButton(
                            onClick = { viewModel.startDiscovery() },
                            enabled = !isScanning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scanning...")
                            } else {
                                Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan Devices")
                            }
                        }

                        DiscoveredDeviceList(
                            devices = devices,
                            onSelect = { viewModel.selectPrinter(it) },
                            selectedAddress = config.address,
                            modifier = Modifier.height(180.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = config.address ?: "",
                            onValueChange = { viewModel.updateAddress(it) },
                            label = { Text("Device Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("Enter MAC / address manually") }
                        )
                    }

                if (config.connectionType in listOf("BLUETOOTH", "SERIAL", "USB")) {
                    OutlinedTextField(
                        value = config.baudRate.toString(),
                        onValueChange = { viewModel.updateBaudRate(it.toIntOrNull() ?: 9600) },
                        label = { Text("Baud Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (config.connectionType == "BLUETOOTH") {
                    HorizontalDivider()
                    Text("Bluetooth Options", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.bluetoothClassicAutoBind,
                            onClick = { viewModel.updateBluetoothAutoBind(!config.bluetoothClassicAutoBind) },
                            label = { Text("Auto Bind") }
                        )
                    }
                    PresetSelector(
                        label = "rfcomm Device",
                        presets = listOf(
                            PresetOption("/dev/rfcomm0", "/dev/rfcomm0"),
                            PresetOption("/dev/rfcomm1", "/dev/rfcomm1"),
                            PresetOption("/dev/ttyS0", "/dev/ttyS0"),
                            PresetOption("COM1 (Windows)", "COM1"),
                            PresetOption("COM3 (Windows)", "COM3")
                        ),
                        selectedValue = config.bluetoothClassicRfcommDevice,
                        onValueSelected = { viewModel.updateBluetoothRfcomm(it) }
                    )
                }

                if (config.connectionType == "BLUETOOTH_LE") {
                    HorizontalDivider()
                    Text("BLE Options", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = config.bleAutoDiscover,
                            onClick = { viewModel.updateBleAutoDiscover(!config.bleAutoDiscover) },
                            label = { Text("Auto Discover") }
                        )
                        FilterChip(
                            selected = config.bleHandshakeEnabled,
                            onClick = { viewModel.updateBleHandshake(!config.bleHandshakeEnabled) },
                            label = { Text("Handshake") }
                        )
                    }
                    PresetSelector(
                        label = "Service UUID",
                        presets = listOf(
                            PresetOption("Standard SPP", "0000ff00-0000-1000-8000-00805f9b34fb"),
                            PresetOption("TI SensorTag", "f000ffc0-0451-4000-b000-000000000000"),
                            PresetOption("HM-10/CC2541", "0000ffe0-0000-1000-8000-00805f9b34fb"),
                            PresetOption("Adafruit Bluefruit", "0000fe00-0000-1000-8000-00805f9b34fb"),
                            PresetOption("Printer BLE (ESC/POS)", "000018f0-0000-1000-8000-00805f9b34fb")
                        ),
                        selectedValue = config.bleServiceUuid,
                        onValueSelected = { viewModel.updateBleServiceUuid(it) }
                    )
                    PresetSelector(
                        label = "Write Characteristic UUID",
                        presets = listOf(
                            PresetOption("Standard SPP", "0000ff01-0000-1000-8000-00805f9b34fb"),
                            PresetOption("HM-10/CC2541 TX", "0000ffe1-0000-1000-8000-00805f9b34fb"),
                            PresetOption("TI SensorTag Data", "f000ffc1-0451-4000-b000-000000000000"),
                            PresetOption("Adafruit Bluefruit TX", "0000fe01-0000-1000-8000-00805f9b34fb"),
                            PresetOption("Printer BLE TX", "000018f1-0000-1000-8000-00805f9b34fb")
                        ),
                        selectedValue = config.bleWriteCharacteristicUuid,
                        onValueSelected = { viewModel.updateBleCharacteristicUuid(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    config: ngga.ring.printer.model.PrinterConfig,
    log: String,
    connectionState: ngga.ring.printer.util.ConnectionState,
    printStatus: ngga.ring.printer.model.PrintStatus,
    viewModel: PrinterViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Review & Print",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        PrinterStatusBadge(
            connectionState = connectionState,
            printStatus = printStatus,
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Configuration Summary", fontWeight = FontWeight.SemiBold)
                SummaryRow("Type", config.connectionType)
                SummaryRow("Name", config.name)
                SummaryRow("Address", config.address ?: "-")
                SummaryRow("Port", config.port.toString())
                SummaryRow("Baud", config.baudRate.toString())
                SummaryRow("Chars/Line", config.characterPerLine.toString())
                SummaryRow("Paper Dots", config.paperWidthDots.toString())
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Paper Settings", fontWeight = FontWeight.SemiBold)
                PaperSettingsFields(
                    initialChars = config.characterPerLine,
                    initialDots = config.paperWidthDots,
                    onCharsChange = { viewModel.updateCharsPerLine(it) },
                    onDotsChange = { viewModel.updatePaperDots(it) }
                )
            }
        }

        HorizontalDivider()

        Text("Actions", fontWeight = FontWeight.SemiBold)

        Button(
            onClick = { viewModel.printTestPage() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Print, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Print Test Page")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.testConnection() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.OnlinePrediction, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Test")
            }
            OutlinedButton(
                onClick = { viewModel.runDiagnostics() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.BugReport, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Diag")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.printExpertTest() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Code, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Expert")
            }
            OutlinedButton(
                onClick = { viewModel.printBarcodeSuite() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Code, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Barcode")
            }
        }
    }
}

@Composable
private fun PaperSettingsFields(
    initialChars: Int,
    initialDots: Int,
    onCharsChange: (Int) -> Unit,
    onDotsChange: (Int) -> Unit
) {
    var charsText by remember { mutableStateOf(initialChars.toString()) }
    var dotsText by remember { mutableStateOf(initialDots.toString()) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = charsText,
            onValueChange = { newVal ->
                charsText = newVal
                newVal.toIntOrNull()?.let { valid -> onCharsChange(valid) }
            },
            label = { Text("Char/Line") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = dotsText,
            onValueChange = { newVal ->
                dotsText = newVal
                newVal.toIntOrNull()?.let { valid -> onDotsChange(valid) }
            },
            label = { Text("Paper Dots") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
