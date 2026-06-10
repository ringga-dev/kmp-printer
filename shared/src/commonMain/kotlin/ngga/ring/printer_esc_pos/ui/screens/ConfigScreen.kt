package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import ngga.ring.printer_esc_pos.ui.components.GlassCard
import ngga.ring.printer_esc_pos.ui.components.SectionHeader
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

import ngga.ring.printer.model.PrinterCharset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: PrinterViewModel,
    onLaunchCalibration: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val showVirtual by viewModel.showVirtual.collectAsState()
    val scrollState = rememberScrollState()
    
    var charsetExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Hardware calibration & localization",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Discovery Options
        SectionHeader("Discovery Options", "Configure how devices are found")
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Demo Mode", fontWeight = FontWeight.Bold)
                    Text("Show simulated printers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = showVirtual,
                    onCheckedChange = { viewModel.toggleVirtual(it) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Paper Configuration
        SectionHeader("Paper Configuration", "Define physical output limits")
        Spacer(Modifier.height(16.dp))

        // Calibration Button
        Surface(
            onClick = onLaunchCalibration,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Calibration Wizard", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Resolve alignment & margin issues automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(Modifier.height(24.dp))
        
        // Presets
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(58, 80).forEach { width ->
                val isSelected = config.paperWidth == width
                ElevatedCard(
                    onClick = {
                        viewModel.updateConfig(config.copy(
                            paperWidth = width,
                            characterPerLine = if (width == 80) 48 else 31,
                            paperWidthDots = if (width == 80) 576 else 384
                        ))
                    },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "${width}mm",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Custom Calibration
        Text(
            "Manual / Custom Configuration",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = if (config.paperWidth == 0) "" else config.paperWidth.toString(),
                        onValueChange = { 
                            val value = it.toIntOrNull() ?: 0
                            viewModel.updateConfig(config.copy(paperWidth = value)) 
                        },
                        label = { Text("Width (mm)") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        placeholder = { Text("e.g. 72") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = config.characterPerLine.toString(),
                        onValueChange = { viewModel.updateConfig(config.copy(characterPerLine = it.toIntOrNull() ?: 0)) },
                        label = { Text("Chars/Line") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                OutlinedTextField(
                    value = config.paperWidthDots.toString(),
                    onValueChange = { viewModel.updateConfig(config.copy(paperWidthDots = it.toIntOrNull() ?: 0)) },
                    label = { Text("Max Print Area (Dots)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = { Text("Set to 0 for auto-calculation based on width") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = config.leftMargin.toString(),
                    onValueChange = { viewModel.updateConfig(config.copy(leftMargin = it.toIntOrNull() ?: 0)) },
                    label = { Text("Left Margin (Dots)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = { Text("Adjust this if print is offset to the left or right") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Center", fontWeight = FontWeight.Bold)
                        Text("Balances left and right margins automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.autoCenter,
                        onCheckedChange = { viewModel.updateConfig(config.copy(autoCenter = it)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Internationalization
        SectionHeader("Localization", "Charset & Encoding settings")
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = charsetExpanded,
                    onExpandedChange = { charsetExpanded = !charsetExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = config.charsetName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Library Charset") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = charsetExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = charsetExpanded,
                        onDismissRequest = { charsetExpanded = false }
                    ) {
                        PrinterCharset.entries.forEach { charset ->
                            DropdownMenuItem(
                                text = { Text(charset.value) },
                                onClick = {
                                    viewModel.updateConfig(config.copy(charsetName = charset.value))
                                    charsetExpanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = config.escPosCodePage.toInt().toString(16).uppercase(),
                    onValueChange = { 
                        val newPage = it.toIntOrNull(16)?.toByte() ?: 0
                        viewModel.updateConfig(config.copy(escPosCodePage = newPage))
                    },
                    label = { Text("ESC/POS Code Page (Hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    prefix = { Text("0x") }
                )
            }
        }
        
        Spacer(Modifier.height(100.dp)) // Extra space for navigation bar
    }
}
