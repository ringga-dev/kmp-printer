package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ngga.ring.printer.model.PrintQuality
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.model.PrinterCharset
import ngga.ring.printer.model.PrinterProfile
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

private val connectionTypes = listOf("NETWORK", "BLUETOOTH", "USB", "BLE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupPrinterScreen(viewModel: PrinterViewModel) {
    val config by viewModel.config.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val isPrinting by viewModel.isPrinting.collectAsState()
    val quality by viewModel.printQuality.collectAsState()
    val charset by viewModel.charset.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Printer Setup", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Koneksi ──────────────────────────────────────────────
            SectionCard(title = "Koneksi") {
                ChipRow(
                    options = connectionTypes,
                    selected = config.connectionType,
                    onSelect = viewModel::setConnectionType
                )
                OutlinedTextField(
                    value = config.address ?: "",
                    onValueChange = viewModel::updateAddress,
                    label = { Text("Address (IP / MAC / VID:PID)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(value = config.port.toString(), label = "Port",
                        onChange = { it.toIntOrNull()?.let(viewModel::updatePort) },
                        modifier = Modifier.weight(1f))
                    NumberField(value = config.baudRate.toString(), label = "Baud",
                        onChange = { it.toIntOrNull()?.let(viewModel::updateBaudRate) },
                        modifier = Modifier.weight(1f))
                }

                // ── Device scan & select ──
                val devices by viewModel.discoveredPrinters.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val log by viewModel.discoveryLog.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cari Perangkat", style = MaterialTheme.typography.labelLarge)
                    if (isScanning) {
                        OutlinedButton(onClick = viewModel::cancelDiscovery) { Text("Batal") }
                    } else {
                        OutlinedButton(onClick = viewModel::startDiscovery) { Text("Scan") }
                    }
                }
                if (log.isNotBlank()) {
                    Text(log, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                devices.forEach { device ->
                    FilterChip(
                        selected = config.address == device.address,
                        onClick = { viewModel.selectPrinter(device) },
                        label = { Text("${device.name} — ${device.address}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Kertas & Margin ──────────────────────────────────────
            SectionCard(title = "Kertas & Margin") {
                ChipRow(
                    options = listOf("58mm (384)", "80mm (576)"),
                    selected = if (config.paperWidthDots == 576) "80mm (576)" else "58mm (384)",
                    onSelect = { when (it) {
                        "58mm (384)" -> viewModel.applyProfile(PrinterProfile.MM58)
                        else -> viewModel.applyProfile(PrinterProfile.MM80)
                    } }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(value = config.characterPerLine.toString(), label = "Char/Line",
                        onChange = { it.toIntOrNull()?.let(viewModel::updateCharsPerLine) },
                        modifier = Modifier.weight(1f))
                    NumberField(value = config.paperWidthDots.toString(), label = "Paper Dots",
                        onChange = { it.toIntOrNull()?.let(viewModel::updatePaperDots) },
                        modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(value = config.leftMargin.toString(), label = "Left Margin",
                        onChange = { it.toIntOrNull()?.let(viewModel::updateLeftMargin) },
                        modifier = Modifier.weight(1f))
                    NumberField(value = config.rightMargin.toString(), label = "Right Margin",
                        onChange = { it.toIntOrNull()?.let(viewModel::updateRightMargin) },
                        modifier = Modifier.weight(1f))
                }
                NumberField(
                    value = config.lineSpacing.toString(),
                    label = "Line Spacing (dots, 0 = default)",
                    onChange = { it.toIntOrNull()?.let(viewModel::updateLineSpacing) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto Center Margin", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = config.autoCenter, onCheckedChange = viewModel::updateAutoCenter)
                }
            }

            // ── Output ───────────────────────────────────────────────
            SectionCard(title = "Output") {
                Text("Print Quality", style = MaterialTheme.typography.labelLarge)
                ChipRow(
                    options = listOf("Light", "Default", "Dark"),
                    selected = when (quality) {
                        PrintQuality.Light -> "Light"
                        PrintQuality.Dark -> "Dark"
                        else -> "Default"
                    },
                    onSelect = {
                        viewModel.setPrintQuality(
                            when (it) {
                                "Light" -> PrintQuality.Light
                                "Dark" -> PrintQuality.Dark
                                else -> PrintQuality.Default
                            }
                        )
                    }
                )
                Text("Charset", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrinterCharset.entries.forEach { cs ->
                        FilterChip(
                            selected = charset == cs,
                            onClick = { viewModel.setCharset(cs) },
                            label = { Text(cs.value) }
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::printTestPage,
                enabled = !isPrinting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isPrinting) "Mencetak…" else "Test Print")
            }

            val statusText = when (val s = printStatus) {
                PrintStatus.Idle -> null
                PrintStatus.Connecting -> "Menghubungkan…"
                PrintStatus.Processing -> "Memproses…"
                PrintStatus.Sending -> "Mengirim…"
                PrintStatus.Success -> "Berhasil ✓"
                is PrintStatus.Error -> "Gagal: ${s.message}"
            }
            statusText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(opt) }
            )
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    label: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { newVal ->
            text = newVal
            onChange(newVal)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true
    )
}
