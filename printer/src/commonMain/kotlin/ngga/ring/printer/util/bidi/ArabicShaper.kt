package ngga.ring.printer.util.bidi

/**
 * Pure Kotlin Arabic Text Shaper and RTL Reorderer for ESC/POS Thermal Printers.
 * Converts Unicode Arabic characters to their contextual joined forms (isolated, initial, medial, final)
 * and reverses RTL runs so thermal printers (which print LTR by default) output legible Arabic text.
 */
object ArabicShaper {

    private data class ArabicChar(
        val isolated: Char,
        val final: Char,
        val initial: Char,
        val medial: Char
    )

    private val CHAR_MAP = mapOf(
        '\u0621' to ArabicChar('\uFE80', '\uFE80', '\uFE80', '\uFE80'), // Hamza
        '\u0622' to ArabicChar('\uFE81', '\uFE82', '\uFE81', '\uFE82'), // Alef with Madda
        '\u0623' to ArabicChar('\uFE83', '\uFE84', '\uFE83', '\uFE84'), // Alef with Hamza above
        '\u0624' to ArabicChar('\uFE85', '\uFE86', '\uFE85', '\uFE86'), // Waw with Hamza
        '\u0625' to ArabicChar('\uFE87', '\uFE88', '\uFE87', '\uFE88'), // Alef with Hamza below
        '\u0626' to ArabicChar('\uFE89', '\uFE8A', '\uFE8B', '\uFE8C'), // Yeh with Hamza
        '\u0627' to ArabicChar('\uFE8D', '\uFE8E', '\uFE8D', '\uFE8E'), // Alef
        '\u0628' to ArabicChar('\uFE8F', '\uFE90', '\uFE91', '\uFE92'), // Beh
        '\u0629' to ArabicChar('\uFE93', '\uFE94', '\uFE93', '\uFE94'), // Teh Marbuta
        '\u062A' to ArabicChar('\uFE95', '\uFE96', '\uFE97', '\uFE98'), // Teh
        '\u062B' to ArabicChar('\uFE99', '\uFE9A', '\uFE9B', '\uFE9C'), // Theh
        '\u062C' to ArabicChar('\uFE9D', '\uFE9E', '\uFE9F', '\uFEA0'), // Jeem
        '\u062D' to ArabicChar('\uFEA1', '\uFEA2', '\uFEA3', '\uFEA4'), // Hah
        '\u062E' to ArabicChar('\uFEA5', '\uFEA6', '\uFEA7', '\uFEA8'), // Khah
        '\u062F' to ArabicChar('\uFEA9', '\uFEAA', '\uFEA9', '\uFEAA'), // Dal
        '\u0630' to ArabicChar('\uFEAB', '\uFEAC', '\uFEAB', '\uFEAC'), // Thal
        '\u0631' to ArabicChar('\uFEAD', '\uFEAE', '\uFEAD', '\uFEAE'), // Reh
        '\u0632' to ArabicChar('\uFEAF', '\uFEB0', '\uFEAF', '\uFEB0'), // Zain
        '\u0633' to ArabicChar('\uFEB1', '\uFEB2', '\uFEB3', '\uFEB4'), // Seen
        '\u0634' to ArabicChar('\uFEB5', '\uFEB6', '\uFEB7', '\uFEB8'), // Sheen
        '\u0635' to ArabicChar('\uFEB9', '\uFEBA', '\uFEBB', '\uFEBC'), // Sad
        '\u0636' to ArabicChar('\uFEBD', '\uFEBE', '\uFEBF', '\uFEC0'), // Dad
        '\u0637' to ArabicChar('\uFEC1', '\uFEC2', '\uFEC3', '\uFEC4'), // Tah
        '\u0638' to ArabicChar('\uFEC5', '\uFEC6', '\uFEC7', '\uFEC8'), // Zah
        '\u0639' to ArabicChar('\uFEC9', '\uFECA', '\uFECB', '\uFECC'), // Ain
        '\u063A' to ArabicChar('\uFECD', '\uFECE', '\uFECF', '\uFED0'), // Ghain
        '\u0641' to ArabicChar('\uFED1', '\uFED2', '\uFED3', '\uFED4'), // Feh
        '\u0642' to ArabicChar('\uFED5', '\uFED6', '\uFED7', '\uFED8'), // Qaf
        '\u0643' to ArabicChar('\uFED9', '\uFEDA', '\uFEDB', '\uFEDC'), // Kaf
        '\u0644' to ArabicChar('\uFEDD', '\uFEDE', '\uFEDF', '\uFEE0'), // Lam
        '\u0645' to ArabicChar('\uFEE1', '\uFEE2', '\uFEE3', '\uFEE4'), // Meem
        '\u0646' to ArabicChar('\uFEE5', '\uFEE6', '\uFEE7', '\uFEE8'), // Noon
        '\u0647' to ArabicChar('\uFEE9', '\uFEEA', '\uFEEB', '\uFEEC'), // Heh
        '\u0648' to ArabicChar('\uFEED', '\uFEEE', '\uFEED', '\uFEEE'), // Waw
        '\u0649' to ArabicChar('\uFEEF', '\uFEF0', '\uFBE8', '\uFBE9'), // Alef Maksura
        '\u064A' to ArabicChar('\uFEF1', '\uFEF2', '\uFEF3', '\uFEF4'), // Yeh
        '\u067E' to ArabicChar('\uFB56', '\uFB57', '\uFB58', '\uFB59'), // Peh (Persian/Urdu)
        '\u0686' to ArabicChar('\uFB7A', '\uFB7B', '\uFB7C', '\uFB7D'), // Tcheh
        '\u0698' to ArabicChar('\uFB8A', '\uFB8B', '\uFB8A', '\uFB8B'), // Jeh
        '\u06AF' to ArabicChar('\uFB92', '\uFB93', '\uFB94', '\uFB95'), // Gaf
    )

    private val NON_CONNECTING_AFTER = setOf(
        '\u0621', '\u0622', '\u0623', '\u0624', '\u0625', '\u0627',
        '\u062F', '\u0630', '\u0631', '\u0632', '\u0648', '\u0649',
        '\u0688', '\u068C', '\u0698'
    )

    fun isArabicChar(c: Char): Boolean = c in '\u0600'..'\u06FF' || c in '\uFB50'..'\uFDFF' || c in '\uFE70'..'\uFEFF'

    fun hasArabic(text: String): Boolean = text.any { isArabicChar(it) }

    /**
     * Shapes Arabic letters into connected glyph forms.
     */
    fun shape(text: String): String {
        if (!hasArabic(text)) return text

        val chars = text.toCharArray()
        val result = StringBuilder()

        for (i in chars.indices) {
            val c = chars[i]
            val arabic = CHAR_MAP[c]
            if (arabic == null) {
                result.append(c)
                continue
            }

            val prevConnects = i > 0 && isArabicChar(chars[i - 1]) && chars[i - 1] !in NON_CONNECTING_AFTER
            val nextConnects = i < chars.size - 1 && isArabicChar(chars[i + 1]) && c !in NON_CONNECTING_AFTER

            val shaped = when {
                prevConnects && nextConnects -> arabic.medial
                prevConnects -> arabic.final
                nextConnects -> arabic.initial
                else -> arabic.isolated
            }
            result.append(shaped)
        }

        return result.toString()
    }

    /**
     * Shapes and reverses Arabic text runs for printing on LTR thermal receipt printers.
     */
    fun processForThermalPrinter(text: String): String {
        if (!hasArabic(text)) return text

        val shaped = shape(text)
        // Reverse words/runs of Arabic characters while preserving English/digits
        val words = shaped.split(" ")
        val reversedWords = words.map { word ->
            if (hasArabic(word)) word.reversed() else word
        }
        return reversedWords.reversed().joinToString(" ")
    }
}
