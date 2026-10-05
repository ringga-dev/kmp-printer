package ngga.ring.printer.util.preview

import ngga.ring.printer.util.escpos.ESCPosConfig
import ngga.ring.printer.util.escpos.TextAlignment

data class PreviewResult(
    val blocks: List<PreviewBlock>,
    val virtualLines: List<VirtualLine> = emptyList(),
    val paperWidthDots: Int = 384,
    val charsPerLine: Int = 32,
    val leftMargin: Int = 0,
    val totalBytes: Int = 0,
    val asciiArt: String = "",
    val style: StyleInfo = StyleInfo()
) {
    data class StyleInfo(
        val hasBold: Boolean = false,
        val hasUnderline: Boolean = false,
        val hasInverted: Boolean = false,
        val hasDoubleWidth: Boolean = false,
        val hasDoubleHeight: Boolean = false,
        val alignmentUsed: Set<TextAlignment> = emptySet(),
        val blockCounts: Map<String, Int> = emptyMap()
    )

    val paperWidthMm: Int get() = paperWidthDots * 10 / 384

    companion object {
        val EMPTY = PreviewResult(emptyList())

        fun from(blocks: List<PreviewBlock>, config: ESCPosConfig, bytes: ByteArray): PreviewResult {
            val virtualLines = ESCPosVirtualRenderer.render(bytes, config.charsPerLine)
            val asciiArt = ESCPosVirtualRenderer.toAsciiArt(virtualLines, config.charsPerLine)
            return buildResult(blocks, virtualLines, asciiArt, config, bytes.size)
        }

        fun from(blocks: List<PreviewBlock>, config: ESCPosConfig): PreviewResult {
            val virtualLines = blocks.toVirtualLines(config.charsPerLine)
            val asciiArt = ESCPosVirtualRenderer.toAsciiArt(virtualLines, config.charsPerLine)
            return buildResult(blocks, virtualLines, asciiArt, config, 0)
        }

        private fun buildResult(
            blocks: List<PreviewBlock>,
            virtualLines: List<VirtualLine>,
            asciiArt: String,
            config: ESCPosConfig,
            totalBytes: Int
        ): PreviewResult {
            val alignmentSet = mutableSetOf<TextAlignment>()
            var hasB = false; var hasU = false; var hasI = false
            var hasW = false; var hasH = false
            val counts = mutableMapOf<String, Int>()

            blocks.forEach { block ->
                val typeName = block::class.simpleName ?: "Unknown"
                counts[typeName] = (counts[typeName] ?: 0) + 1

                if (block is PreviewBlock.Text) {
                    alignmentSet.add(block.alignment)
                    if (block.isBold) hasB = true
                    if (block.isUnderline) hasU = true
                    if (block.isInverted) hasI = true
                    if (block.widthMultiplier > 1) hasW = true
                    if (block.heightMultiplier > 1) hasH = true
                }
            }

            return PreviewResult(
                blocks = blocks,
                virtualLines = virtualLines,
                paperWidthDots = config.paperWidthDots,
                charsPerLine = config.charsPerLine,
                leftMargin = config.leftMargin,
                totalBytes = totalBytes,
                asciiArt = asciiArt,
                style = StyleInfo(
                    hasBold = hasB,
                    hasUnderline = hasU,
                    hasInverted = hasI,
                    hasDoubleWidth = hasW,
                    hasDoubleHeight = hasH,
                    alignmentUsed = alignmentSet,
                    blockCounts = counts
                )
            )
        }
    }
}

internal fun List<PreviewBlock>.toVirtualLines(charsPerLine: Int): List<VirtualLine> {
    return map { block ->
        when (block) {
            is PreviewBlock.Text -> VirtualLine(
                content = block.text,
                type = LineType.TEXT,
                alignment = block.alignment,
                isBold = block.isBold,
                isUnderline = block.isUnderline,
                isInverted = block.isInverted,
                widthMultiplier = block.widthMultiplier,
                heightMultiplier = block.heightMultiplier
            )
            is PreviewBlock.KeyValue -> VirtualLine(
                content = "${block.key}: ${block.value}",
                type = LineType.TEXT,
                alignment = TextAlignment.LEFT,
                isBold = block.isBold,
                isInverted = block.isInverted
            )
            is PreviewBlock.Divider -> VirtualLine(
                content = block.char.toString().repeat(charsPerLine.coerceAtLeast(1)),
                type = LineType.DIVIDER
            )
            is PreviewBlock.Space -> VirtualLine(content = "", type = LineType.SPACE)
            is PreviewBlock.Barcode -> VirtualLine(
                content = block.content,
                type = LineType.BARCODE,
                alignment = block.alignment
            )
            is PreviewBlock.QRCode -> VirtualLine(
                content = block.content,
                type = LineType.QR_CODE,
                alignment = block.alignment
            )
            is PreviewBlock.Image -> VirtualLine(
                content = "[IMAGE ${block.width}x${block.height}]",
                type = LineType.IMAGE,
                alignment = block.alignment
            )
            is PreviewBlock.SystemCommand -> VirtualLine(
                content = "[${block.label}] ${block.detail}",
                type = LineType.SYSTEM_COMMAND
            )
        }
    }
}
