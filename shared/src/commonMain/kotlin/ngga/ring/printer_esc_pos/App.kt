package ngga.ring.printer_esc_pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.model.DiscoveredPrinter
import ngga.ring.printer.model.DiscoveryConfig
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.model.PrinterConnectionType
import ngga.ring.printer.model.PrintStatus

private data class ConnectionOption(
    val type: String,
    val title: String,
    val description: String
)

private val connectionOptions = listOf(
    ConnectionOption("NETWORK", "Network TCP", "Use printer IP address and port 9100."),
    ConnectionOption("USB", "USB", "Scan USB/serial printer devices."),
    ConnectionOption("BLUETOOTH", "Bluetooth Classic", "Use OS-paired SPP/serial printer."),
    ConnectionOption("BLUETOOTH_LE", "BLE", "Scan BLE printers where supported."),
    ConnectionOption("SERIAL", "Serial", "Use a known COM/rfcomm/tty port."),
    ConnectionOption("VIRTUAL", "Virtual", "Run without physical hardware.")
)

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PrinterWizardApp()
        }
    }
}

@Composable
private fun PrinterWizardApp() {
    val scope = rememberCoroutineScope()
    val printer = remember { KmpPrinter() }

    var step by remember { mutableIntStateOf(0) }
    var type by remember { mutableStateOf("NETWORK") }
    var name by remember { mutableStateOf("Receipt Printer") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9100") }
    var baudRate by remember { mutableStateOf("9600") }
    var charsPerLine by remember { mutableStateOf("32") }
    var paperDots by remember { mutableStateOf("384") }
    var bleServiceUuid by remember { mutableStateOf("0000ff00-0000-1000-8000-00805f9b34fb") }
    var bleCharacteristicUuid by remember { mutableStateOf("0000ff01-0000-1000-8000-00805f9b34fb") }
    var bleBridgeCommand by remember { mutableStateOf("") }
    var bleAutoDiscover by remember { mutableStateOf(true) }
    var bleHandshake by remember { mutableStateOf(true) }
    var bluetoothClassicAutoBind by remember { mutableStateOf(true) }
    var bluetoothClassicRfcommDevice by remember { mutableStateOf("/dev/rfcomm0") }
    var devices by remember { mutableStateOf<List<DiscoveredPrinter>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<DiscoveredPrinter?>(null) }
    var log by remember { mutableStateOf("Choose a connection type to start.") }
    var scanning by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    fun config(): PrinterConfig = PrinterConfig(
        name = name.ifBlank { "Receipt Printer" },
        connectionType = type,
        address = address.ifBlank { null },
        port = port.toIntOrNull() ?: 9100,
        characterPerLine = charsPerLine.toIntOrNull() ?: 32,
        paperWidthDots = paperDots.toIntOrNull() ?: 384,
        baudRate = baudRate.toIntOrNull() ?: 9600,
        bleServiceUuid = bleServiceUuid,
        bleWriteCharacteristicUuid = bleCharacteristicUuid,
        bleAutoDiscover = bleAutoDiscover,
        bleHandshakeEnabled = bleHandshake,
        bleBridgeCommand = bleBridgeCommand.ifBlank { null },
        bluetoothClassicAutoBind = bluetoothClassicAutoBind,
        bluetoothClassicRfcommDevice = bluetoothClassicRfcommDevice
    )

    fun select(device: DiscoveredPrinter) {
        selectedDevice = device
        type = device.connectionType
        name = device.name
        address = device.address
        port = device.port.toString()
        log = "Selected ${device.name}."
    }

    fun scan() {
        scanJob?.cancel()
        scanJob = scope.launch {
            scanning = true
            selectedDevice = null
            devices = emptyList()
            log = "Scanning $type..."
            printer.checkAndRequestPermissions(type) { granted ->
                if (!granted) {
                    log = "Permission denied for $type."
                    scanning = false
                    return@checkAndRequestPermissions
                }
                scanJob = scope.launch {
                    val emitted = withTimeoutOrNull(4500) {
                        printer.discovery(type, DiscoveryConfig(showVirtualDevices = false)) { message ->
                            log = message
                        }.collect { found ->
                            devices = found
                            log = if (found.isEmpty()) {
                                "No real device found. Check OS pairing, cable, network, or enter address manually."
                            } else {
                                "Found ${found.size} device(s). Select one to continue."
                            }
                        }
                        true
                    }
                    scanning = false
                    if (emitted == null && devices.isEmpty()) {
                        log = "No real device found. Check OS pairing, cable, network, or enter address manually."
                    }
                }
            }
        }
    }

    fun printTest() {
        scope.launch {
            val cfg = config()
            if (cfg.connectionType != "VIRTUAL" && cfg.address.isNullOrBlank()) {
                log = "Address is required before printing."
                step = 1
                return@launch
            }
            log = "Preparing print job..."
            printer.print(cfg) {
                initialize()
                alignCenter()
                bold(true)
                line("KMP PRINTER")
                bold(false)
                line("Test Receipt")
                divider()
                alignLeft()
                segmentedLine("Type", cfg.connectionType)
                segmentedLine("Name", cfg.name)
                segmentedLine("Address", cfg.address ?: "-")
                segmentedLine("Port", cfg.port.toString())
                segmentedLine("Baud", cfg.baudRate.toString())
                feed(3)
                cut()
            }.collect { status ->
                log = when (status) {
                    PrintStatus.Idle -> "Idle"
                    PrintStatus.Processing -> "Processing"
                    PrintStatus.Connecting -> "Connecting to printer"
                    PrintStatus.Sending -> "Sending ESC/POS data"
                    PrintStatus.Success -> "Print success"
                    is PrintStatus.Error -> "Print failed: ${status.message}"
                }
            }
        }
    }

    fun testConnection() {
        scope.launch {
            log = when (val status = printer.testConnection(config())) {
                PrintStatus.Success -> "Connection test success."
                is PrintStatus.Error -> "Connection test failed: ${status.message}"
                else -> "Connection test: $status"
            }
        }
    }

    fun runDiagnostics() {
        val cfg = config()
        val report = printer.platformReport()
        log = when (cfg.connectionType) {
            PrinterConnectionType.USB -> {
                val usb = printer.diagnoseUsb(cfg)
                buildString {
                    append("USB diagnostic: ${usb.failureReason}. ${usb.message}")
                    if (usb.suggestedFix.isNotBlank()) append(" Fix: ${usb.suggestedFix}")
                    if (usb.udevRule != null) append(" Udev: ${usb.udevRule}")
                }
            }
            PrinterConnectionType.BLUETOOTH_LE -> {
                val ble = printer.diagnoseBle(cfg)
                "BLE diagnostic: ${ble.failureReason}. ${ble.message} Fix: ${ble.suggestedFix}"
            }
            PrinterConnectionType.BLUETOOTH,
            PrinterConnectionType.SERIAL -> {
                val serial = printer.diagnoseSerial(cfg)
                buildString {
                    append("${cfg.connectionType} diagnostic: ${serial.failureReason}. ${serial.message}")
                    if (serial.suggestedFix.isNotBlank()) append(" Fix: ${serial.suggestedFix}")
                    if (serial.ports.isNotEmpty()) {
                        append(" Ports: ")
                        append(serial.ports.joinToString { "${it.address}(${it.confidence}%)" })
                    }
                }
            }
            else -> {
                val capability = report.capabilityFor(cfg.connectionType)
                "Platform ${report.platformName}/${report.osName}. ${cfg.connectionType}: supported=${capability?.isSupported}, native=${capability?.isNative}. ${printer.troubleshootingHint(cfg.connectionType)}"
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 840.dp
        val outerModifier = Modifier.fillMaxSize().padding(if (compact) 12.dp else 20.dp)
        val content: @Composable (Modifier) -> Unit = { modifier ->
            WizardContent(
                modifier = modifier,
                step = step,
                compact = compact,
                type = type,
                name = name,
                address = address,
                port = port,
                baudRate = baudRate,
                charsPerLine = charsPerLine,
                paperDots = paperDots,
                devices = devices,
                selectedDevice = selectedDevice,
                scanning = scanning,
                advancedOpen = advancedOpen,
                onConnectionSelected = {
                    scanJob?.cancel()
                    scanning = false
                    type = it
                    devices = emptyList()
                    selectedDevice = null
                    log = "Connection type set to $it."
                },
                onName = { name = it },
                onAddress = { address = it },
                onPort = { port = it },
                onBaudRate = { baudRate = it },
                bleServiceUuid = bleServiceUuid,
                bleCharacteristicUuid = bleCharacteristicUuid,
                bleBridgeCommand = bleBridgeCommand,
                bleAutoDiscover = bleAutoDiscover,
                bleHandshake = bleHandshake,
                bluetoothClassicAutoBind = bluetoothClassicAutoBind,
                bluetoothClassicRfcommDevice = bluetoothClassicRfcommDevice,
                onBleServiceUuid = { bleServiceUuid = it },
                onBleCharacteristicUuid = { bleCharacteristicUuid = it },
                onBleBridgeCommand = { bleBridgeCommand = it },
                onBleAutoDiscover = { bleAutoDiscover = it },
                onBleHandshake = { bleHandshake = it },
                onBluetoothClassicAutoBind = { bluetoothClassicAutoBind = it },
                onBluetoothClassicRfcommDevice = { bluetoothClassicRfcommDevice = it },
                onScan = { scan() },
                onSelect = { select(it) },
                onAdvancedToggle = { advancedOpen = !advancedOpen },
                onCharsPerLine = { charsPerLine = it },
                onPaperDots = { paperDots = it },
                onPrint = { printTest() },
                onTestConnection = { testConnection() },
                onDiagnostics = { runDiagnostics() },
                canContinue = when (step) {
                    0 -> type.isNotBlank()
                    1 -> type == "VIRTUAL" || address.isNotBlank() || selectedDevice != null
                    else -> true
                },
                onBack = { if (step > 0) step-- },
                onNext = { if (step < 2) step++ }
            )
        }

        if (compact) {
            Column(
                modifier = outerModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WizardSidebar(step = step, log = log, compact = true, modifier = Modifier.fillMaxWidth())
                content(Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(
                modifier = outerModifier,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                WizardSidebar(step = step, log = log, compact = false, modifier = Modifier.weight(0.8f).fillMaxSize())
                content(Modifier.weight(1.7f).fillMaxSize())
            }
        }
    }
}

@Composable
private fun WizardContent(
    modifier: Modifier,
    step: Int,
    compact: Boolean,
    type: String,
    name: String,
    address: String,
    port: String,
    baudRate: String,
    charsPerLine: String,
    paperDots: String,
    devices: List<DiscoveredPrinter>,
    selectedDevice: DiscoveredPrinter?,
    scanning: Boolean,
    advancedOpen: Boolean,
    onConnectionSelected: (String) -> Unit,
    onName: (String) -> Unit,
    onAddress: (String) -> Unit,
    onPort: (String) -> Unit,
    onBaudRate: (String) -> Unit,
    bleServiceUuid: String,
    bleCharacteristicUuid: String,
    bleBridgeCommand: String,
    bleAutoDiscover: Boolean,
    bleHandshake: Boolean,
    bluetoothClassicAutoBind: Boolean,
    bluetoothClassicRfcommDevice: String,
    onBleServiceUuid: (String) -> Unit,
    onBleCharacteristicUuid: (String) -> Unit,
    onBleBridgeCommand: (String) -> Unit,
    onBleAutoDiscover: (Boolean) -> Unit,
    onBleHandshake: (Boolean) -> Unit,
    onBluetoothClassicAutoBind: (Boolean) -> Unit,
    onBluetoothClassicRfcommDevice: (String) -> Unit,
    onScan: () -> Unit,
    onSelect: (DiscoveredPrinter) -> Unit,
    onAdvancedToggle: () -> Unit,
    onCharsPerLine: (String) -> Unit,
    onPaperDots: (String) -> Unit,
    onPrint: () -> Unit,
    onTestConnection: () -> Unit,
    onDiagnostics: () -> Unit,
    canContinue: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (step) {
                    0 -> ConnectionStep(selected = type, compact = compact, onSelected = onConnectionSelected)
                    1 -> DeviceStep(
                        type = type,
                        name = name,
                        address = address,
                        port = port,
                        baudRate = baudRate,
                        devices = devices,
                        selectedDevice = selectedDevice,
                        scanning = scanning,
                        compact = compact,
                        onName = onName,
                        onAddress = onAddress,
                        onPort = onPort,
                        onBaudRate = onBaudRate,
                        bleServiceUuid = bleServiceUuid,
                        bleCharacteristicUuid = bleCharacteristicUuid,
                        bleBridgeCommand = bleBridgeCommand,
                        bleAutoDiscover = bleAutoDiscover,
                        bleHandshake = bleHandshake,
                        bluetoothClassicAutoBind = bluetoothClassicAutoBind,
                        bluetoothClassicRfcommDevice = bluetoothClassicRfcommDevice,
                        onBleServiceUuid = onBleServiceUuid,
                        onBleCharacteristicUuid = onBleCharacteristicUuid,
                        onBleBridgeCommand = onBleBridgeCommand,
                        onBleAutoDiscover = onBleAutoDiscover,
                        onBleHandshake = onBleHandshake,
                        onBluetoothClassicAutoBind = onBluetoothClassicAutoBind,
                        onBluetoothClassicRfcommDevice = onBluetoothClassicRfcommDevice,
                        onScan = onScan,
                        onSelect = onSelect
                    )
                    else -> PrintStep(
                        config = PrinterConfig(
                            name = name.ifBlank { "Receipt Printer" },
                            connectionType = type,
                            address = address.ifBlank { null },
                            port = port.toIntOrNull() ?: 9100,
                            characterPerLine = charsPerLine.toIntOrNull() ?: 32,
                            paperWidthDots = paperDots.toIntOrNull() ?: 384,
                            baudRate = baudRate.toIntOrNull() ?: 9600,
                            bleServiceUuid = bleServiceUuid,
                            bleWriteCharacteristicUuid = bleCharacteristicUuid,
                            bleAutoDiscover = bleAutoDiscover,
                            bleHandshakeEnabled = bleHandshake,
                            bleBridgeCommand = bleBridgeCommand.ifBlank { null },
                            bluetoothClassicAutoBind = bluetoothClassicAutoBind,
                            bluetoothClassicRfcommDevice = bluetoothClassicRfcommDevice
                        ),
                        advancedOpen = advancedOpen,
                        charsPerLine = charsPerLine,
                        paperDots = paperDots,
                        onAdvancedToggle = onAdvancedToggle,
                        onCharsPerLine = onCharsPerLine,
                        onPaperDots = onPaperDots,
                        onPrint = onPrint,
                        onTestConnection = onTestConnection,
                        onDiagnostics = onDiagnostics
                    )
                }
            }
            NavigationBar(step = step, canContinue = canContinue, onBack = onBack, onNext = onNext)
        }
    }
}

@Composable
private fun WizardSidebar(step: Int, log: String, compact: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp)
        ) {
            Text("KmpPrinter Sample", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!compact) {
                Text("A simple flow for selecting a real printer and sending a test receipt.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StepBadge(0, "Connection", step, Modifier.weight(1f))
                StepBadge(1, "Device", step, Modifier.weight(1f))
                StepBadge(2, "Print", step, Modifier.weight(1f))
            }
            Text("Status", fontWeight = FontWeight.SemiBold)
            Text(log, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StepBadge(index: Int, label: String, current: Int, modifier: Modifier = Modifier) {
    val active = index == current
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${index + 1}. $label", fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ConnectionStep(selected: String, compact: Boolean, onSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
    ) {
        Text("Choose Connection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Pick the transport first. The next step will only show inputs relevant to this choice.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            connectionOptions.forEach { option ->
                Card(
                    onClick = { onSelected(option.type) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected == option.type) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(option.title, fontWeight = FontWeight.Bold)
                        Text(option.type, color = MaterialTheme.colorScheme.primary)
                        Text(option.description)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStep(
    type: String,
    name: String,
    address: String,
    port: String,
    baudRate: String,
    devices: List<DiscoveredPrinter>,
    selectedDevice: DiscoveredPrinter?,
    scanning: Boolean,
    compact: Boolean,
    onName: (String) -> Unit,
    onAddress: (String) -> Unit,
    onPort: (String) -> Unit,
    onBaudRate: (String) -> Unit,
    bleServiceUuid: String,
    bleCharacteristicUuid: String,
    bleBridgeCommand: String,
    bleAutoDiscover: Boolean,
    bleHandshake: Boolean,
    bluetoothClassicAutoBind: Boolean,
    bluetoothClassicRfcommDevice: String,
    onBleServiceUuid: (String) -> Unit,
    onBleCharacteristicUuid: (String) -> Unit,
    onBleBridgeCommand: (String) -> Unit,
    onBleAutoDiscover: (Boolean) -> Unit,
    onBleHandshake: (Boolean) -> Unit,
    onBluetoothClassicAutoBind: (Boolean) -> Unit,
    onBluetoothClassicRfcommDevice: (String) -> Unit,
    onScan: () -> Unit,
    onSelect: (DiscoveredPrinter) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier.fillMaxSize().then(if (compact) Modifier.verticalScroll(scrollState) else Modifier),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Select Device", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        when (type) {
            "NETWORK" -> {
                Text("Enter the printer IP address. Most ESC/POS network printers use TCP port 9100.")
                OutlinedTextField(name, onName, label = { Text("Printer Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, onAddress, label = { Text("IP Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(port, onPort, label = { Text("TCP Port") }, modifier = Modifier.fillMaxWidth())
            }
            "VIRTUAL" -> {
                Text("Virtual mode does not need a physical printer.")
                OutlinedTextField(name, onName, label = { Text("Printer Name") }, modifier = Modifier.fillMaxWidth())
            }
            else -> {
                Text(
                    if (type == "BLUETOOTH") {
                        "Bluetooth Classic on JVM appears as an OS serial port after pairing."
                    } else {
                        "Scan real devices, then select one from the list."
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onScan, enabled = !scanning) {
                        Text(if (scanning) "Scanning" else "Scan Real Devices")
                    }
                }
                DeviceList(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    onSelect = onSelect,
                    modifier = if (compact) Modifier.height(220.dp) else Modifier.weight(1f)
                )
                OutlinedTextField(name, onName, label = { Text("Printer Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, onAddress, label = { Text("Manual Address / Port") }, modifier = Modifier.fillMaxWidth())
                if (type == "BLUETOOTH" || type == "SERIAL" || type == "USB") {
                    OutlinedTextField(baudRate, onBaudRate, label = { Text("Baud Rate") }, modifier = Modifier.fillMaxWidth())
                }
                if (type == "BLUETOOTH") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = bluetoothClassicAutoBind,
                            onClick = { onBluetoothClassicAutoBind(!bluetoothClassicAutoBind) },
                            label = { Text("Linux rfcomm Auto Bind") }
                        )
                    }
                    OutlinedTextField(
                        bluetoothClassicRfcommDevice,
                        onBluetoothClassicRfcommDevice,
                        label = { Text("Linux rfcomm Device") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (type == "BLUETOOTH_LE") {
                    Text("BLE Settings", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = bleAutoDiscover,
                            onClick = { onBleAutoDiscover(!bleAutoDiscover) },
                            label = { Text("Auto Discover") }
                        )
                        FilterChip(
                            selected = bleHandshake,
                            onClick = { onBleHandshake(!bleHandshake) },
                            label = { Text("Handshake") }
                        )
                    }
                    OutlinedTextField(bleServiceUuid, onBleServiceUuid, label = { Text("Service UUID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bleCharacteristicUuid, onBleCharacteristicUuid, label = { Text("Write Characteristic UUID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bleBridgeCommand, onBleBridgeCommand, label = { Text("Windows/macOS Bridge Command") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<DiscoveredPrinter>,
    selectedDevice: DiscoveredPrinter?,
    onSelect: (DiscoveredPrinter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        if (devices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No device listed yet")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { device ->
                    val selected = selectedDevice?.address == device.address
                    Card(
                        onClick = { onSelect(device) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text("${device.connectionType} - ${device.address}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintStep(
    config: PrinterConfig,
    advancedOpen: Boolean,
    charsPerLine: String,
    paperDots: String,
    onAdvancedToggle: () -> Unit,
    onCharsPerLine: (String) -> Unit,
    onPaperDots: (String) -> Unit,
    onPrint: () -> Unit,
    onTestConnection: () -> Unit,
    onDiagnostics: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Review and Print", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SummaryRow("Type", config.connectionType)
        SummaryRow("Name", config.name)
        SummaryRow("Address", config.address ?: "-")
        SummaryRow("Port", config.port.toString())
        SummaryRow("Baud", config.baudRate.toString())

        OutlinedButton(onClick = onAdvancedToggle) {
            Text(if (advancedOpen) "Hide Paper Settings" else "Paper Settings")
        }
        if (advancedOpen) {
            OutlinedTextField(charsPerLine, onCharsPerLine, label = { Text("Characters Per Line") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(paperDots, onPaperDots, label = { Text("Paper Width Dots") }, modifier = Modifier.fillMaxWidth())
        }

        Button(onClick = onPrint, modifier = Modifier.fillMaxWidth()) {
            Text("Print Test Receipt")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onTestConnection, modifier = Modifier.weight(1f)) {
                Text("Test Connection")
            }
            OutlinedButton(onClick = onDiagnostics, modifier = Modifier.weight(1f)) {
                Text("Diagnostics")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

@Composable
private fun NavigationBar(step: Int, canContinue: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(onClick = onBack, enabled = step > 0) {
            Text("Back")
        }
        if (step < 2) {
            Button(onClick = onNext, enabled = canContinue) {
                Text("Continue")
            }
        }
    }
}
