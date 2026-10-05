package ngga.ring.printer.model

data class PrinterProfile(
    val paperWidth: Int,
    val paperWidthDots: Int,
    val characterPerLine: Int,
    val leftMargin: Int = 0,
    val autoCenter: Boolean = false,
    val rightMargin: Int = 0,
    val lineSpacing: Int = 0
) {
    companion object {
        val MM58 = PrinterProfile(
            paperWidth = 58,
            paperWidthDots = 384,
            characterPerLine = 32
        )

        val MM80 = PrinterProfile(
            paperWidth = 80,
            paperWidthDots = 576,
            characterPerLine = 48
        )

        fun custom(
            paperWidth: Int,
            paperWidthDots: Int,
            characterPerLine: Int,
            leftMargin: Int = 0,
            autoCenter: Boolean = false,
            rightMargin: Int = 0,
            lineSpacing: Int = 0
        ): PrinterProfile = PrinterProfile(
            paperWidth = paperWidth,
            paperWidthDots = paperWidthDots,
            characterPerLine = characterPerLine,
            leftMargin = leftMargin,
            autoCenter = autoCenter,
            rightMargin = rightMargin,
            lineSpacing = lineSpacing
        )
    }
}

