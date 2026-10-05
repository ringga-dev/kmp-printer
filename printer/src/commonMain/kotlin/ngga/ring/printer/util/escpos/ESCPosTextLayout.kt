package ngga.ring.printer.util.escpos

/**
 * Helper utilities for formatting plain text for ESC/POS printers.
 * Works entirely with pure Kotlin String manipulation to remain KMP-safe.
 */
object ESCPosTextLayout {

    // A safety margin ensures the printer hardware buffer never hits 100% capacity in one line,
    // which prevents automatic (and redundant) line feeds from the firmware.
    private const val SAFETY_MARGIN = 1

    /**
     * Produces a single-line text segment with a left label and a right value.
     * Handles wrapping/truncation gracefully to ensure the right value is ALWAYS visible.
     */
    fun segmentedText(
        left: String,
        right: String,
        maxCharsPerLine: Int
    ): String {
        val safeMax = (maxCharsPerLine - SAFETY_MARGIN).coerceAtLeast(1)
        
        // Right part always gets a space prefix for separation if there's room
        val rightPart = if (right.length < safeMax) " $right" else right.take(safeMax)
        val maxLeft = (safeMax - rightPart.length).coerceAtLeast(0)
        
        val trimmedLeft = if (left.length > maxLeft) {
            if (maxLeft > 3) left.take(maxLeft - 3) + "..."
            else left.take(maxLeft)
        } else {
            left
        }

        // Calculate exact spaces needed to fill the gap
        val spacesNeeded = (safeMax - trimmedLeft.length - right.length).coerceAtLeast(0)

        return buildString {
            append(trimmedLeft)
            repeat(spacesNeeded) { append(' ') }
            append(right.take(safeMax - trimmedLeft.length - spacesNeeded).ifEmpty { right })
        }
    }

    /**
     * Centers a text string into the given line width using spaces.
     * Long text is normalized with [wrapText] first so content is never
     * silently truncated; each wrapped line is centered with balanced padding.
     */
    fun centeredText(
        text: String,
        maxCharsPerLine: Int
    ): String {
        val safeMax = (maxCharsPerLine - SAFETY_MARGIN).coerceAtLeast(1)
        val lines = wrapText(text, safeMax)

        return lines.joinToString("\n") { line ->
            val totalPadding = (safeMax - line.length).coerceAtLeast(0)
            val leftPad = totalPadding / 2
            val rightPad = totalPadding - leftPad
            " ".repeat(leftPad) + line + " ".repeat(rightPad)
        }
    }

    /**
     * Word-wrap a single string into lines of at most [maxWidth] chars.
     * - Extra whitespace is normalized to a single space.
     * - Words longer than maxWidth are hard-split.
     * - Always returns at least one element.
     */
    fun wrapText(text: String, maxWidth: Int): List<String> {
        val width = maxWidth.coerceAtLeast(1)
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        var current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder()
            }
        }

        for (word in normalized.split(' ')) {
            var remaining = word
            // Hard-split words longer than the max width
            while (remaining.length > width) {
                flush()
                lines.add(remaining.take(width))
                remaining = remaining.drop(width)
            }
            if (current.isEmpty()) {
                current.append(remaining)
            } else if (current.length + 1 + remaining.length <= width) {
                current.append(' ').append(remaining)
            } else {
                flush()
                current.append(remaining)
            }
        }
        flush()
        if (lines.isEmpty()) lines.add("")
        return lines
    }

    /**
     * Centers text and splits into multiple lines if needed (wrapping).
     */
    fun centerText(maxCharsPerLine: Int, text: String, maxLine: Int = 5): String {
        if (text.isBlank()) return ""
        val safeMax = (maxCharsPerLine - SAFETY_MARGIN).coerceAtLeast(1)

        val chunks = wrapText(text, safeMax)
        val finalLines = if (chunks.size > maxLine) {
            chunks.take(maxLine - 1) + (chunks[maxLine - 1].take((safeMax - 3).coerceAtLeast(0)) + "...")
        } else {
            chunks
        }

        return finalLines.joinToString("\n") { line ->
            centeredText(line.trim(), maxCharsPerLine) // centeredText will apply safety again
        }
    }

    /**
     * Produces a multi-column table row using weights.
     */
    fun tableRow(
        columns: List<String>,
        weights: List<Int>,
        maxCharsPerLine: Int
    ): String {
        if (columns.isEmpty() || weights.isEmpty() || columns.size != weights.size) return ""
        
        val safeMax = (maxCharsPerLine - SAFETY_MARGIN).coerceAtLeast(1)
        val totalWeight = weights.sum().toDouble()
        val charsPerWeight = safeMax / totalWeight
        
        return buildString {
            columns.zip(weights).forEachIndexed { index, (text, weight) ->
                val colWidth = if (index == columns.lastIndex) {
                    safeMax - length
                } else {
                    (weight * charsPerWeight).toInt()
                }
                
                val content = if (text.length > colWidth) {
                    text.take((colWidth - 1).coerceAtLeast(0)) + " "
                } else {
                    val alignment = if (index == 0) "left" else if (index == columns.lastIndex) "right" else "center"
                    when (alignment) {
                        "left" -> text.padEnd(colWidth)
                        "right" -> text.padStart(colWidth)
                        else -> centeredText(text, colWidth + SAFETY_MARGIN) // pass back full width since safety is here
                    }
                }
                append(content.take(colWidth))
            }
        }
    }
}
