package ngga.ring.printer.util.template

import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.model.BarcodeType
import ngga.ring.printer.model.QRCodeLevel

/**
 * A declarative representation of a receipt element.
 */
sealed class TemplateElement {
    data class Text(
        val content: String,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val isBold: Boolean = false,
        val height: Int = 1,
        val width: Int = 1
    ) : TemplateElement()

    data class KeyValue(
        val label: String,
        val value: String,
        val isBold: Boolean = false
    ) : TemplateElement()

    data class Divider(val char: Char = '-') : TemplateElement()
    
    data class Space(val lines: Int = 1) : TemplateElement()

    data class Image(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        val alignment: TextAlignment = TextAlignment.CENTER
    ) : TemplateElement()

    data class Barcode(
        val data: String,
        val type: BarcodeType = BarcodeType.CODE128,
        val alignment: TextAlignment = TextAlignment.CENTER
    ) : TemplateElement()

    data class QRCode(
        val data: String,
        val size: Int = 8,
        val level: QRCodeLevel = QRCodeLevel.L,
        val alignment: TextAlignment = TextAlignment.CENTER
    ) : TemplateElement()

    data class Table(
        val headers: List<String> = emptyList(),
        val weights: List<Int> = emptyList(),
        val rows: List<List<String>> = emptyList(),
        val alignments: List<TextAlignment> = emptyList(),
        val autoWrap: Boolean = false
    ) : TemplateElement()

    data class Cut(val full: Boolean = true) : TemplateElement()

    data class Feed(val lines: Int = 1) : TemplateElement()
}

/**
 * A full receipt template.
 */
data class ReceiptTemplate(
    val elements: List<TemplateElement>
) {
    /**
     * Renders the template into ESC/POS commands using the builder.
     * @param variables Map of placeholder to real value (e.g., "total" -> "$100")
     */
    fun render(builder: ESCPosCommandBuilder, variables: Map<String, String> = emptyMap()) {
        builder.initialize()
        
        elements.forEach { element ->
            when (element) {
                is TemplateElement.Text -> {
                    val finalContent = injectVariables(element.content, variables)
                    builder.withAlignment(element.alignment) {
                        withTextSize(element.width, element.height) {
                            bold(element.isBold)
                            line(finalContent)
                        }
                    }
                }
                is TemplateElement.KeyValue -> {
                    val label = injectVariables(element.label, variables)
                    val value = injectVariables(element.value, variables)
                    builder.bold(element.isBold)
                    builder.segmentedLine(label, value)
                    builder.bold(false)
                }
                is TemplateElement.Divider -> {
                    builder.divider(element.char)
                }
                is TemplateElement.Space -> {
                    builder.feed(element.lines)
                }
                is TemplateElement.Image -> {
                    builder.image(element.bytes, element.width, element.height, element.alignment == TextAlignment.CENTER)
                }
                is TemplateElement.Barcode -> {
                    val data = injectVariables(element.data, variables)
                    builder.barcode(data, type = element.type, center = element.alignment == TextAlignment.CENTER)
                }
                is TemplateElement.QRCode -> {
                    val data = injectVariables(element.data, variables)
                    builder.qrCode(data, size = element.size, level = element.level, center = element.alignment == TextAlignment.CENTER)
                }
                is TemplateElement.Table -> {
                    if (element.headers.isNotEmpty()) {
                        val headers = element.headers.map { injectVariables(it, variables) }
                        builder.tableRow(headers, element.weights, element.alignments, element.autoWrap)
                    }
                    element.rows.forEach { row ->
                        val processedRow = row.map { injectVariables(it, variables) }
                        builder.tableRow(processedRow, element.weights, element.alignments, element.autoWrap)
                    }
                }
                is TemplateElement.Cut -> {
                    builder.cut(element.full)
                }
                is TemplateElement.Feed -> {
                    builder.feed(element.lines)
                }
            }
        }
        
        if (elements.none { it is TemplateElement.Cut }) {
            builder.feed(3)
            builder.cut()
        }
    }

    private fun injectVariables(text: String, variables: Map<String, String>): String {
        var result = text
        variables.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }

    companion object {
        /**
         * Parses a map structure containing "elements" (a List of Map<String, Any?>).
         */
        fun fromMap(data: Map<String, Any?>): ReceiptTemplate {
            val rawElements = (data["elements"] as? List<*>)
                ?: (data["elements"] as? Array<*>)?.toList()
                ?: emptyList<Any?>()
            val elements = rawElements.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                val elMap = item as? Map<String, Any?>
                    ?: (item as? Map<*, *>)?.mapKeys { it.key.toString() }
                    ?: return@mapNotNull null
                parseElement(elMap)
            }
            return ReceiptTemplate(elements)
        }

        /**
         * Parses a JSON string representing a receipt template into a [ReceiptTemplate].
         */
        fun fromJson(json: String): ReceiptTemplate {
            val trimmed = json.trim()
            if (trimmed.isEmpty()) return ReceiptTemplate(emptyList())
            val parsed = SimpleJsonParser(trimmed).parse()
            return when (parsed) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    fromMap(parsed as Map<String, Any?>)
                }
                is List<*> -> {
                    fromMap(mapOf("elements" to parsed))
                }
                else -> throw IllegalArgumentException("Root JSON value must be an object or array, was: $parsed")
            }
        }

        private fun parseElement(elMap: Map<String, Any?>): TemplateElement? {
            val type = (elMap["type"] ?: elMap["element"] ?: elMap["elementType"])
                ?.toString()?.lowercase()?.trim() ?: return null

            return when (type) {
                "text" -> {
                    val content = elMap["content"]?.toString() ?: ""
                    val alignment = parseAlignment(elMap["alignment"], default = TextAlignment.LEFT)
                    val isBold = parseBoolean(elMap["isBold"] ?: elMap["bold"], default = false)
                    val height = parseNumber(elMap["height"])?.toInt() ?: 1
                    val width = parseNumber(elMap["width"])?.toInt() ?: 1
                    TemplateElement.Text(
                        content = content,
                        alignment = alignment,
                        isBold = isBold,
                        height = height,
                        width = width
                    )
                }
                "key_value", "keyvalue", "key-value" -> {
                    val label = (elMap["label"] ?: elMap["key"])?.toString() ?: ""
                    val value = elMap["value"]?.toString() ?: ""
                    val isBold = parseBoolean(elMap["isBold"] ?: elMap["bold"], default = false)
                    TemplateElement.KeyValue(
                        label = label,
                        value = value,
                        isBold = isBold
                    )
                }
                "divider" -> {
                    val char = when (val c = elMap["char"]) {
                        is Char -> c
                        is String -> c.firstOrNull() ?: '-'
                        else -> '-'
                    }
                    TemplateElement.Divider(char = char)
                }
                "space" -> {
                    val lines = parseNumber(elMap["lines"])?.toInt() ?: 1
                    TemplateElement.Space(lines = lines)
                }
                "barcode" -> {
                    val data = elMap["data"]?.toString() ?: ""
                    val typeCandidate = (elMap["barcodeType"] ?: elMap["barcode_type"] ?: elMap["format"])?.toString()
                        ?: if (!elMap["type"]?.toString().equals("barcode", ignoreCase = true)) elMap["type"]?.toString() else null
                    val barcodeType = BarcodeType.entries.find { it.name.equals(typeCandidate, ignoreCase = true) }
                        ?: BarcodeType.CODE128
                    val alignment = parseAlignment(elMap["alignment"], default = TextAlignment.CENTER)
                    TemplateElement.Barcode(
                        data = data,
                        type = barcodeType,
                        alignment = alignment
                    )
                }
                "qrcode", "qr_code", "qr-code" -> {
                    val data = elMap["data"]?.toString() ?: ""
                    val size = parseNumber(elMap["size"])?.toInt() ?: 8
                    val levelStr = elMap["level"]?.toString()
                    val level = QRCodeLevel.entries.find { it.name.equals(levelStr, ignoreCase = true) }
                        ?: QRCodeLevel.L
                    val alignment = parseAlignment(elMap["alignment"], default = TextAlignment.CENTER)
                    TemplateElement.QRCode(
                        data = data,
                        size = size,
                        level = level,
                        alignment = alignment
                    )
                }
                "table" -> {
                    val headers = (elMap["headers"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val weights = (elMap["weights"] as? List<*>)?.mapNotNull { parseNumber(it)?.toInt() } ?: emptyList()
                    val rawRows = elMap["rows"] as? List<*>
                    val rows = rawRows?.mapNotNull { rowItem ->
                        (rowItem as? List<*>)?.map { it?.toString() ?: "" }
                    } ?: emptyList()
                    val rawAligns = elMap["alignments"] as? List<*>
                    val alignments = rawAligns?.mapNotNull { alignItem ->
                        when (alignItem) {
                            is TextAlignment -> alignItem
                            is String -> when (alignItem.lowercase().trim()) {
                                "left" -> TextAlignment.LEFT
                                "center" -> TextAlignment.CENTER
                                "right" -> TextAlignment.RIGHT
                                else -> null
                            }
                            else -> null
                        }
                    } ?: emptyList()
                    val autoWrap = parseBoolean(elMap["autoWrap"] ?: elMap["autowrap"], default = false)
                    TemplateElement.Table(
                        headers = headers,
                        weights = weights,
                        rows = rows,
                        alignments = alignments,
                        autoWrap = autoWrap
                    )
                }
                "cut" -> {
                    val full = parseBoolean(elMap["full"], default = true)
                    TemplateElement.Cut(full = full)
                }
                "feed" -> {
                    val lines = parseNumber(elMap["lines"])?.toInt() ?: 1
                    TemplateElement.Feed(lines = lines)
                }
                "image" -> {
                    val bytes = when (val b = elMap["bytes"]) {
                        is ByteArray -> b
                        is List<*> -> b.mapNotNull { (it as? Number)?.toByte() }.toByteArray()
                        else -> ByteArray(0)
                    }
                    val width = parseNumber(elMap["width"])?.toInt() ?: 0
                    val height = parseNumber(elMap["height"])?.toInt() ?: 0
                    val alignment = parseAlignment(elMap["alignment"], default = TextAlignment.CENTER)
                    TemplateElement.Image(
                        bytes = bytes,
                        width = width,
                        height = height,
                        alignment = alignment
                    )
                }
                else -> null
            }
        }

        private fun parseAlignment(value: Any?, default: TextAlignment): TextAlignment {
            if (value is TextAlignment) return value
            return when (value?.toString()?.lowercase()?.trim()) {
                "left" -> TextAlignment.LEFT
                "center" -> TextAlignment.CENTER
                "right" -> TextAlignment.RIGHT
                else -> default
            }
        }

        private fun parseBoolean(value: Any?, default: Boolean): Boolean {
            if (value == null) return default
            if (value is Boolean) return value
            val s = value.toString().lowercase().trim()
            return when (s) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> default
            }
        }

        private fun parseNumber(value: Any?): Number? {
            return when (value) {
                is Number -> value
                is String -> value.toIntOrNull() ?: value.toLongOrNull() ?: value.toDoubleOrNull()
                else -> null
            }
        }
    }
}

/**
 * Lightweight, zero-dependency JSON parser for receipt template deserialization.
 */
private class SimpleJsonParser(private val src: String) {
    private var pos = 0

    fun parse(): Any? {
        skipWhitespace()
        val result = parseValue()
        skipWhitespace()
        if (pos < src.length) {
            throw IllegalArgumentException("Unexpected character '${src[pos]}' after root element at position $pos")
        }
        return result
    }

    private fun skipWhitespace() {
        while (pos < src.length && src[pos].isWhitespace()) {
            pos++
        }
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= src.length) throw IllegalArgumentException("Unexpected end of JSON input at position $pos")
        return when (val ch = src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected character '$ch' at position $pos")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        match('{')
        val map = mutableMapOf<String, Any?>()
        skipWhitespace()
        if (pos < src.length && src[pos] == '}') {
            pos++
            return map
        }
        while (true) {
            skipWhitespace()
            if (pos >= src.length || src[pos] != '"') {
                throw IllegalArgumentException("Expected string key in object at position $pos")
            }
            val key = parseString()
            skipWhitespace()
            match(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            if (pos >= src.length) {
                throw IllegalArgumentException("Unterminated object at position $pos")
            }
            when (src[pos]) {
                ',' -> {
                    pos++
                    skipWhitespace()
                    // Allow optional trailing comma
                    if (pos < src.length && src[pos] == '}') {
                        pos++
                        break
                    }
                }
                '}' -> {
                    pos++
                    break
                }
                else -> throw IllegalArgumentException("Expected ',' or '}' in object at position $pos but found '${src[pos]}'")
            }
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        match('[')
        val list = mutableListOf<Any?>()
        skipWhitespace()
        if (pos < src.length && src[pos] == ']') {
            pos++
            return list
        }
        while (true) {
            val value = parseValue()
            list.add(value)
            skipWhitespace()
            if (pos >= src.length) {
                throw IllegalArgumentException("Unterminated array at position $pos")
            }
            when (src[pos]) {
                ',' -> {
                    pos++
                    skipWhitespace()
                    // Allow optional trailing comma
                    if (pos < src.length && src[pos] == ']') {
                        pos++
                        break
                    }
                }
                ']' -> {
                    pos++
                    break
                }
                else -> throw IllegalArgumentException("Expected ',' or ']' in array at position $pos but found '${src[pos]}'")
            }
        }
        return list
    }

    private fun parseString(): String {
        match('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            val ch = src[pos++]
            if (ch == '"') {
                return sb.toString()
            }
            if (ch == '\\') {
                if (pos >= src.length) throw IllegalArgumentException("Unterminated escape sequence at end of string")
                when (val esc = src[pos++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (pos + 4 > src.length) throw IllegalArgumentException("Invalid unicode escape at position $pos")
                        val hex = src.substring(pos, pos + 4)
                        pos += 4
                        sb.append(hex.toInt(16).toChar())
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated string literal")
    }

    private fun parseBoolean(): Boolean {
        if (src.startsWith("true", pos)) {
            pos += 4
            return true
        }
        if (src.startsWith("false", pos)) {
            pos += 5
            return false
        }
        throw IllegalArgumentException("Expected boolean at position $pos")
    }

    private fun parseNull(): Any? {
        if (src.startsWith("null", pos)) {
            pos += 4
            return null
        }
        throw IllegalArgumentException("Expected null at position $pos")
    }

    private fun parseNumber(): Number {
        val start = pos
        if (pos < src.length && src[pos] == '-') {
            pos++
        }
        while (pos < src.length && src[pos].isDigit()) {
            pos++
        }
        var isFloating = false
        if (pos < src.length && src[pos] == '.') {
            isFloating = true
            pos++
            while (pos < src.length && src[pos].isDigit()) {
                pos++
            }
        }
        if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
            isFloating = true
            pos++
            if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) {
                pos++
            }
            while (pos < src.length && src[pos].isDigit()) {
                pos++
            }
        }
        val numStr = src.substring(start, pos)
        if (numStr.isEmpty() || numStr == "-") {
            throw IllegalArgumentException("Invalid number at position $start")
        }
        return if (isFloating) {
            numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid float number '$numStr' at $start")
        } else {
            numStr.toLongOrNull()?.let { l ->
                if (l in Int.MIN_VALUE..Int.MAX_VALUE) l.toInt() else l
            } ?: numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid integer number '$numStr' at $start")
        }
    }

    private fun match(expected: Char) {
        if (pos >= src.length || src[pos] != expected) {
            val found = if (pos < src.length) "'${src[pos]}'" else "EOF"
            throw IllegalArgumentException("Expected '$expected' at position $pos but found $found")
        }
        pos++
    }
}
