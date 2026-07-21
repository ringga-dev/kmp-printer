package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.model.PrinterConfig
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintPreviewScreen(viewModel: PrinterViewModel) {
    val previewBlocks by viewModel.previewBlocks.collectAsState()
    val config by viewModel.config.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Print Preview", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (previewBlocks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No preview available. Configure a printer first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            previewBlocks.forEach { block ->
                                PreviewBlockItem(block, config)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Paper: ${config.paperWidthDots} dots (${config.paperWidthDots * 10 / 384}mm)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBlockItem(block: PreviewBlock, config: PrinterConfig) {
    when (block) {
        is PreviewBlock.Text -> {
            Text(
                text = block.text,
                color = Color.Black,
                fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                textAlign = when (block.alignment) {
                    TextAlignment.LEFT -> TextAlign.Start
                    TextAlignment.CENTER -> TextAlign.Center
                    TextAlignment.RIGHT -> TextAlign.End
                },
                fontSize = (if (block.widthMultiplier > 1 || block.heightMultiplier > 1) 14 else 11).sp,
                modifier = when (block.alignment) {
                    TextAlignment.LEFT -> Modifier.fillMaxWidth()
                    TextAlignment.CENTER -> Modifier.fillMaxWidth()
                    TextAlignment.RIGHT -> Modifier.fillMaxWidth()
                }.padding(vertical = 1.dp)
            )
        }
        is PreviewBlock.KeyValue -> {
            Text(
                text = "${block.key}: ${block.value}",
                color = Color.Black,
                fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
            )
        }
        is PreviewBlock.Divider -> {
            HorizontalDivider(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        is PreviewBlock.Space -> {
            Spacer(Modifier.height(8.dp))
        }
        is PreviewBlock.Barcode -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                BarcodePlaceholder(block.content)
            }
        }
        is PreviewBlock.QRCode -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                QRPlaceholder(block.content)
            }
        }
        is PreviewBlock.Image -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    "[Image ${block.width}x${block.height}]",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun BarcodePlaceholder(content: String) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(40.dp)
    ) {
        val barCount = content.length * 3 + 10
        val barWidth = size.width / barCount
        for (i in 0 until barCount) {
            if (i % 3 != 0) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(x = i * barWidth, y = 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height)
                )
            }
        }
    }
}

@Composable
private fun QRPlaceholder(content: String) {
    val size = 60.dp
    Canvas(
        modifier = Modifier
            .width(size)
            .height(size)
    ) {
        val moduleSize = size.toPx() / 11
        val pattern = listOf(
            1,1,1,1,1,1,1,0,0,0,0,
            1,0,0,0,0,0,1,0,0,0,0,
            1,0,1,1,1,0,1,0,0,0,0,
            1,0,1,1,1,0,1,0,0,0,0,
            1,0,1,1,1,0,1,0,0,0,0,
            1,0,0,0,0,0,1,0,0,0,0,
            1,1,1,1,1,1,1,0,0,0,0
        )
        pattern.forEachIndexed { idx, v ->
            if (v == 1) {
                val row = idx / 11
                val col = idx % 11
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(col * moduleSize, row * moduleSize),
                    size = androidx.compose.ui.geometry.Size(moduleSize, moduleSize)
                )
            }
        }
    }
}
