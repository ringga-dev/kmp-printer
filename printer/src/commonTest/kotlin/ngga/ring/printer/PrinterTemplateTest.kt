package ngga.ring.printer

import ngga.ring.printer.model.BarcodeType
import ngga.ring.printer.model.QRCodeLevel
import ngga.ring.printer.util.escpos.ESCPosCommandBuilder
import ngga.ring.printer.util.escpos.ESCPosConfig
import ngga.ring.printer.util.escpos.TextAlignment
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.template.ReceiptTemplate
import ngga.ring.printer.util.template.TemplateElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PrinterTemplateTest {

    @Test
    fun testRenderTableWithHeadersAndRows() {
        val template = ReceiptTemplate(
            elements = listOf(
                TemplateElement.Table(
                    headers = listOf("Item", "Qty", "Price"),
                    weights = listOf(2, 1, 1),
                    rows = listOf(
                        listOf("Apple", "1", "10.00"),
                        listOf("Banana", "2", "20.00")
                    ),
                    alignments = listOf(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT),
                    autoWrap = false
                )
            )
        )
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32))
        template.render(builder)

        val previewTexts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        assertTrue(previewTexts.size >= 3, "Expected at least 3 text lines (header + 2 rows)")
        assertTrue(previewTexts.any { it.text.contains("Item") && it.text.contains("Qty") && it.text.contains("Price") })
        assertTrue(previewTexts.any { it.text.contains("Apple") && it.text.contains("1") && it.text.contains("10.00") })
        assertTrue(previewTexts.any { it.text.contains("Banana") && it.text.contains("2") && it.text.contains("20.00") })
    }

    @Test
    fun testRenderExplicitCutDoesNotDoubleCut() {
        // Template WITH explicit Cut element
        val templateWithCut = ReceiptTemplate(
            elements = listOf(
                TemplateElement.Text("Order Receipt"),
                TemplateElement.Feed(2),
                TemplateElement.Cut(full = true)
            )
        )
        val builderWithCut = ESCPosCommandBuilder()
        templateWithCut.render(builderWithCut)
        val cutsWithCut = builderWithCut.buildPreview()
            .filterIsInstance<PreviewBlock.SystemCommand>()
            .filter { it.label == "CUT" }
        assertEquals(1, cutsWithCut.size, "Should only have 1 cut command when explicit Cut is present")

        // Template WITHOUT Cut element (should issue default cut at the end)
        val templateWithoutCut = ReceiptTemplate(
            elements = listOf(
                TemplateElement.Text("Order Receipt")
            )
        )
        val builderWithoutCut = ESCPosCommandBuilder()
        templateWithoutCut.render(builderWithoutCut)
        val cutsWithoutCut = builderWithoutCut.buildPreview()
            .filterIsInstance<PreviewBlock.SystemCommand>()
            .filter { it.label == "CUT" }
        assertEquals(1, cutsWithoutCut.size, "Should issue default 1 cut command when no Cut element is provided")
    }

    @Test
    fun testVariableInjectionInTable() {
        val template = ReceiptTemplate(
            elements = listOf(
                TemplateElement.Table(
                    headers = listOf("Item", "Price"),
                    weights = listOf(1, 1),
                    rows = listOf(
                        listOf("{{item}}", "{{price}}")
                    ),
                    alignments = listOf(TextAlignment.LEFT, TextAlignment.RIGHT)
                )
            )
        )
        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32))
        template.render(
            builder,
            variables = mapOf("item" to "Coffee", "price" to "$3.50")
        )

        val previewTexts = builder.buildPreview().filterIsInstance<PreviewBlock.Text>()
        val rowText = previewTexts.firstOrNull { it.text.contains("Coffee") }
        assertNotNull(rowText, "Rendered table row should contain injected 'Coffee'")
        assertTrue(rowText.text.contains("$3.50"), "Rendered table row should contain injected '$3.50'")
        assertFalse(rowText.text.contains("{{item}}"), "Placeholder {{item}} should be replaced")
        assertFalse(rowText.text.contains("{{price}}"), "Placeholder {{price}} should be replaced")
    }

    @Test
    fun testFromMap() {
        val map = mapOf(
            "elements" to listOf(
                mapOf(
                    "type" to "text",
                    "content" to "Store Header",
                    "alignment" to "center",
                    "isBold" to true,
                    "height" to 2,
                    "width" to 2
                ),
                mapOf(
                    "type" to "keyValue",
                    "label" to "Order",
                    "value" to "#999",
                    "bold" to true
                ),
                mapOf(
                    "type" to "divider",
                    "char" to "="
                ),
                mapOf(
                    "type" to "table",
                    "headers" to listOf("Product", "Qty", "Total"),
                    "weights" to listOf(2, 1, 1),
                    "rows" to listOf(
                        listOf("Pizza", "1", "12.00"),
                        listOf("Soda", "2", "4.00")
                    ),
                    "alignments" to listOf("left", "center", "right"),
                    "autoWrap" to true
                ),
                mapOf(
                    "type" to "space",
                    "lines" to 2
                ),
                mapOf(
                    "type" to "barcode",
                    "data" to "BARCODE123",
                    "barcodeType" to "CODE39"
                ),
                mapOf(
                    "type" to "qrcode",
                    "data" to "https://kmp-printer.dev",
                    "size" to 5,
                    "level" to "H"
                ),
                mapOf(
                    "type" to "feed",
                    "lines" to 3
                ),
                mapOf(
                    "type" to "cut",
                    "full" to true
                )
            )
        )

        val template = ReceiptTemplate.fromMap(map)
        assertEquals(9, template.elements.size)

        val text = template.elements[0] as TemplateElement.Text
        assertEquals("Store Header", text.content)
        assertEquals(TextAlignment.CENTER, text.alignment)
        assertTrue(text.isBold)
        assertEquals(2, text.height)
        assertEquals(2, text.width)

        val kv = template.elements[1] as TemplateElement.KeyValue
        assertEquals("Order", kv.label)
        assertEquals("#999", kv.value)
        assertTrue(kv.isBold)

        val divider = template.elements[2] as TemplateElement.Divider
        assertEquals('=', divider.char)

        val table = template.elements[3] as TemplateElement.Table
        assertEquals(listOf("Product", "Qty", "Total"), table.headers)
        assertEquals(listOf(2, 1, 1), table.weights)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Pizza", "1", "12.00"), table.rows[0])
        assertEquals(listOf(TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT), table.alignments)
        assertTrue(table.autoWrap)

        val space = template.elements[4] as TemplateElement.Space
        assertEquals(2, space.lines)

        val barcode = template.elements[5] as TemplateElement.Barcode
        assertEquals("BARCODE123", barcode.data)
        assertEquals(BarcodeType.CODE39, barcode.type)

        val qrcode = template.elements[6] as TemplateElement.QRCode
        assertEquals("https://kmp-printer.dev", qrcode.data)
        assertEquals(5, qrcode.size)
        assertEquals(QRCodeLevel.H, qrcode.level)

        val feed = template.elements[7] as TemplateElement.Feed
        assertEquals(3, feed.lines)

        val cut = template.elements[8] as TemplateElement.Cut
        assertTrue(cut.full)
    }

    @Test
    fun testFromJson() {
        val json = """
        {
            "elements": [
                {
                    "type": "text",
                    "content": "Receipt: {{receipt_no}}",
                    "alignment": "center",
                    "bold": true
                },
                {
                    "type": "divider",
                    "char": "-"
                },
                {
                    "type": "table",
                    "headers": ["Desc", "Amt"],
                    "weights": [2, 1],
                    "rows": [
                        ["{{item}}", "{{price}}"]
                    ],
                    "alignments": ["left", "right"],
                    "autoWrap": false
                },
                {
                    "type": "key_value",
                    "label": "Total",
                    "value": "{{total}}",
                    "isBold": true
                },
                {
                    "type": "barcode",
                    "data": "1234567890",
                    "barcodeType": "CODE128",
                    "alignment": "center"
                },
                {
                    "type": "qrcode",
                    "data": "https://example.com/receipt/{{receipt_no}}",
                    "size": 6,
                    "level": "M",
                    "alignment": "center"
                },
                {
                    "type": "feed",
                    "lines": 3
                },
                {
                    "type": "cut",
                    "full": true
                }
            ]
        }
        """.trimIndent()

        val template = ReceiptTemplate.fromJson(json)
        assertEquals(8, template.elements.size)

        val builder = ESCPosCommandBuilder(ESCPosConfig(charsPerLine = 32))
        template.render(
            builder,
            mapOf(
                "receipt_no" to "TX-1001",
                "item" to "Burger",
                "price" to "8.50",
                "total" to "$8.50"
            )
        )

        val preview = builder.buildPreview()
        val texts = preview.filterIsInstance<PreviewBlock.Text>()
        assertTrue(texts.any { it.text.contains("Receipt: TX-1001") })
        assertTrue(texts.any { it.text.contains("Burger") && it.text.contains("8.50") })

        val keyValues = preview.filterIsInstance<PreviewBlock.KeyValue>()
        assertTrue(keyValues.any { it.key == "Total" && it.value == "$8.50" })

        val cuts = preview.filterIsInstance<PreviewBlock.SystemCommand>().filter { it.label == "CUT" }
        assertEquals(1, cuts.size)
    }

    @Test
    fun testJsonStringEscapesAndEdgeCases() {
        val json = """
        {
            "elements": [
                {
                    "type": "text",
                    "content": "Line 1\nLine 2\tTabbed \"Quoted\" \\ Backslash \u0041"
                }
            ]
        }
        """.trimIndent()

        val template = ReceiptTemplate.fromJson(json)
        assertEquals(1, template.elements.size)
        val text = template.elements[0] as TemplateElement.Text
        assertEquals("Line 1\nLine 2\tTabbed \"Quoted\" \\ Backslash A", text.content)
    }

    @Test
    fun testJsonRootArray() {
        val json = """
        [
            { "type": "text", "content": "Simple line" },
            { "type": "cut", "full": false }
        ]
        """.trimIndent()

        val template = ReceiptTemplate.fromJson(json)
        assertEquals(2, template.elements.size)
        assertTrue(template.elements[0] is TemplateElement.Text)
        val cut = template.elements[1] as TemplateElement.Cut
        assertFalse(cut.full)
    }
}
