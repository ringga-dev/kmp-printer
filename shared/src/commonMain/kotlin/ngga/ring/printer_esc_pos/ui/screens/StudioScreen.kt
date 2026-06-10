package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer_esc_pos.util.rememberImagePicker
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel
import org.jetbrains.compose.resources.*

@Composable
fun StudioScreen(viewModel: PrinterViewModel) {
    val config by viewModel.config.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val previewBlocks by viewModel.previewBlocks.collectAsState()
    val discoveryLog by viewModel.discoveryLog.collectAsState()
    
    val dithering by viewModel.imagingDithering.collectAsState()
    val contrast by viewModel.imagingContrast.collectAsState()
    val brightness by viewModel.imagingBrightness.collectAsState()
    
    val receiptScrollState = rememberScrollState()
    val mainScrollState = rememberScrollState()
    val isReady = config.address?.isNotEmpty() == true || config.connectionType == "VIRTUAL"

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(mainScrollState)
                .padding(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Studio",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        "ENTERPRISE PRO EDITION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (isReady) Color(0xFF4CAF50) else Color(0xFFFF5252))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isReady) "HARDWARE READY" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Main Content Layout (Responsive Column)
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                
                // Hardware & Hardening Profile
                HardwareProfileCard(config, connectionState.toString())

                // Imaging Lab
                ImagingLabCard(
                    viewModel = viewModel,
                    dithering = dithering,
                    contrast = contrast,
                    brightness = brightness
                )

                // The Preview Ticket
                ReceiptPreviewSection(
                    config = config,
                    previewBlocks = previewBlocks,
                    scrollState = receiptScrollState,
                    isProcessing = printStatus !is PrintStatus.Idle
                )

                // Expert Sandbox
                ExpertSandboxCard(
                    isReady = isReady,
                    onPageMode = { viewModel.printPageModeDemo() },
                    onBarcode = { viewModel.printBarcodeSuite() },
                    onExpertReceipt = { viewModel.printExpertReceipt() }
                )

                // Reliability Console (Stress Test)
                ReliabilityConsoleCard(
                    log = discoveryLog,
                    onStressTest = { viewModel.runStressTest() },
                    onBasicTest = { viewModel.printExpertTest() },
                    enabled = isReady
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }

        // Overlay Dialogs
        if (printStatus !is PrintStatus.Idle) {
            StatusDialog(status = printStatus, onDismiss = { viewModel.resetPrintStatus() })
        }
    }
}

@Composable
fun HardwareProfileCard(config: ngga.ring.printer.model.PrinterConfig, platformState: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Dns, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("HARDWARE PROFILE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoChip(Icons.Default.Print, "${config.paperWidth}mm")
                Spacer(Modifier.width(8.dp))
                InfoChip(Icons.Default.Bolt, "HARDENED")
                Spacer(Modifier.width(8.dp))
                InfoChip(Icons.Default.Shuffle, "CHUNKED")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Address: ${config.address ?: "None"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ImagingLabCard(
    viewModel: PrinterViewModel,
    dithering: String,
    contrast: Int,
    brightness: Int
) {
    val logoPreview by viewModel.logoPreview.collectAsState()
    val imagePicker = rememberImagePicker { image, preview ->
        viewModel.setLogo(image, preview)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhotoFilter, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("IMAGING LAB", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { imagePicker() }) {
                    Text(if (logoPreview == null) "IMPORT IMAGE" else "REPLACE")
                }
            }

            if (logoPreview != null) {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Minimized Preview
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = logoPreview!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    // Controls
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DITHERING ENGINE", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AlgorithmChip("FLOYD", dithering == "FLOYD_STEINBERG") { viewModel.updateImaging(dithering = "FLOYD_STEINBERG") }
                            AlgorithmChip("ATKINSON", dithering == "ATKINSON") { viewModel.updateImaging(dithering = "ATKINSON") }
                            AlgorithmChip("TRESH", dithering == "THRESHOLD") { viewModel.updateImaging(dithering = "THRESHOLD") }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                // Adjustment Sliders
                AdjustmentSlider("CONTRAST", contrast, -100f..100f) { viewModel.updateImaging(contrast = it.toInt()) }
                AdjustmentSlider("BRIGHTNESS", brightness, -100f..100f) { viewModel.updateImaging(brightness = it.toInt()) }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).clickable { imagePicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.outline)
                        Text("No Image Selected", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun AlgorithmChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp) },
        shape = MaterialTheme.shapes.small
    )
}

@Composable
fun AdjustmentSlider(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
fun ReceiptPreviewSection(
    config: ngga.ring.printer.model.PrinterConfig,
    previewBlocks: List<PreviewBlock>,
    scrollState: ScrollState,
    isProcessing: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("VIRTUAL OUTPUT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        
        val previewWidth = if (config.paperWidth == 58) 250.dp else 320.dp
        
        ElevatedCard(
            modifier = Modifier
                .width(previewWidth)
                .height(400.dp)
                .blur(if (isProcessing) 8.dp else 0.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            // Rendering logic
            val totalDots = (config.paperWidth - 10) * 8 
            val dotsPerChar = totalDots.toDouble() / config.characterPerLine.toDouble()
            val effectiveWidth = if (config.autoCenter) (totalDots - (2 * config.leftMargin)).coerceAtLeast(1).toDouble() else totalDots.toDouble()
            val effectiveChars = if (config.autoCenter) (effectiveWidth / dotsPerChar).toInt().coerceAtLeast(1) else config.characterPerLine
            val actualTextWidthDots = effectiveChars.toDouble() * dotsPerChar
            val centeringPaddingDots = if (config.autoCenter) ((effectiveWidth - actualTextWidthDots) / 2.0).toInt().coerceAtLeast(0) else 0
            val totalLeftMarginDots = config.leftMargin + centeringPaddingDots
            val marginRatio = totalLeftMarginDots.toFloat() / totalDots.coerceAtLeast(384).toFloat()
            val marginPadding = (previewWidth.value * marginRatio).dp
            val contentWidth = if (config.autoCenter) previewWidth - (marginPadding * 2) else previewWidth - marginPadding

            Box(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
                Column(
                    modifier = Modifier
                        .padding(start = marginPadding)
                        .width(contentWidth)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    previewBlocks.forEach { block -> RenderPreviewBlock(block) }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun ExpertSandboxCard(
    isReady: Boolean,
    onPageMode: () -> Unit,
    onBarcode: () -> Unit,
    onExpertReceipt: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("EXPERT SANDBOX", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SandboxButton(Icons.Default.Layers, "PAGE MODE", Modifier.weight(1f), isReady, onPageMode)
                SandboxButton(Icons.Default.QrCode2, "BARCODES", Modifier.weight(1f), isReady, onBarcode)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onExpertReceipt,
                enabled = isReady,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.ReceiptLong, null)
                Spacer(Modifier.width(12.dp))
                Text("PRINT EXPERT RECEIPT")
            }
        }
    }
}

@Composable
fun SandboxButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ReliabilityConsoleCard(
    log: String,
    onStressTest: () -> Unit,
    onBasicTest: () -> Unit,
    enabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("RELIABILITY CONSOLE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            
            // Log Area
            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = Color.Black,
                shape = MaterialTheme.shapes.small
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = log,
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBasicTest,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("BASIC TEST")
                }
                Button(
                    onClick = onStressTest,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("STRESS TEST")
                }
            }
        }
    }
}

@Composable
private fun RenderPreviewBlock(block: PreviewBlock) {
    val monoFont = FontFamily.Monospace
    
    when (block) {
        is PreviewBlock.Text -> {
            Text(
                text = block.text,
                color = if (block.isInverted) Color.White else Color.Black,
                fontWeight = if (block.isBold) FontWeight.Black else FontWeight.Normal,
                fontSize = 11.sp * block.heightMultiplier,
                fontFamily = monoFont,
                textDecoration = if (block.isUnderline) TextDecoration.Underline else TextDecoration.None,
                textAlign = when (block.alignment) {
                    TextAlignment.CENTER -> TextAlign.Center
                    TextAlignment.RIGHT -> TextAlign.Right
                    else -> TextAlign.Left
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind { if (block.isInverted) drawRect(color = Color.Black) }
                    .padding(vertical = (2.dp * block.heightMultiplier))
                    .graphicsLayer {
                        scaleX = block.widthMultiplier.toFloat() / block.heightMultiplier.toFloat()
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            )
        }
        is PreviewBlock.KeyValue -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).drawBehind { if (block.isInverted) drawRect(color = Color.Black) },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(block.key, color = if (block.isInverted) Color.White else Color.Gray, fontSize = 10.sp, fontFamily = monoFont, modifier = Modifier.weight(1f))
                Text(block.value, color = if (block.isInverted) Color.White else Color.Black, fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp, fontFamily = monoFont, textAlign = TextAlign.Right)
            }
        }
        is PreviewBlock.Divider -> {
            Text(text = block.char.toString().repeat(64), color = Color.LightGray, maxLines = 1, fontFamily = monoFont, letterSpacing = 2.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        is PreviewBlock.Barcode -> {
            Column(horizontalAlignment = when (block.alignment) { TextAlignment.CENTER -> Alignment.CenterHorizontally; TextAlignment.RIGHT -> Alignment.End; else -> Alignment.Start }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Icon(Icons.Default.ViewWeek, null, Modifier.size(width = 120.dp, height = 40.dp), Color.Black)
                Text(block.content, color = Color.Gray, fontSize = 9.sp, fontFamily = monoFont)
            }
        }
        is PreviewBlock.QRCode -> {
            Column(horizontalAlignment = when (block.alignment) { TextAlignment.CENTER -> Alignment.CenterHorizontally; TextAlignment.RIGHT -> Alignment.End; else -> Alignment.Start }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Icon(Icons.Default.QrCode2, null, Modifier.size(80.dp), Color.Black)
                Text("SCAN TO VERIFY", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = monoFont)
            }
        }
        is PreviewBlock.Image -> {
            Column(horizontalAlignment = when (block.alignment) { TextAlignment.CENTER -> Alignment.CenterHorizontally; TextAlignment.RIGHT -> Alignment.End; else -> Alignment.Start }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                val bitmap = block.previewData as? ImageBitmap
                if (bitmap != null) {
                    androidx.compose.foundation.Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.width((block.width / 2.5).dp).height((block.height / 2.5).dp))
                } else {
                    Box(modifier = Modifier.size(width = (block.width / 2.5).dp, height = (block.height / 2.5).dp).drawBehind { drawRect(Color.LightGray.copy(alpha = 0.2f)) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, null, Modifier.size(20.dp), Color.Gray)
                            Text("${block.width}x${block.height}", fontSize = 8.sp, color = Color.Gray, fontFamily = monoFont)
                        }
                    }
                }
            }
        }
        PreviewBlock.Space -> { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun StatusDialog(status: PrintStatus, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when(status) {
                PrintStatus.Processing, PrintStatus.Connecting, PrintStatus.Sending -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(text = when(status) { PrintStatus.Processing -> "Preparing engine..."; PrintStatus.Connecting -> "Connecting to hardware..."; else -> "Sending stream data..." }, textAlign = TextAlign.Center)
                }
                PrintStatus.Success -> {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(64.dp), tint = Color(0xFF4CAF50))
                    Text("PRINT SUCCESS", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
                    Button(onClick = onDismiss, Modifier.padding(top = 24.dp).fillMaxWidth()) { Text("BACK") }
                }
                is PrintStatus.Error -> {
                    Icon(Icons.Default.Error, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Text("PRINT FAILED", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
                    Text(status.message, fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onDismiss, Modifier.padding(top = 24.dp).fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("RETRY") }
                }
                else -> {}
            }
        }
    }
}
