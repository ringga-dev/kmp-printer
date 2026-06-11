package ngga.ring.printer.model

data class HeatConfig(
    val dots: Int,
    val time: Int,
    val interval: Int
) {
    companion object {
        val DEFAULT = HeatConfig(dots = 7, time = 80, interval = 2)
        val DARK = HeatConfig(dots = 11, time = 120, interval = 40)
    }
}

data class PrintQuality(
    val density: Int,
    val heatConfig: HeatConfig? = null
) {
    companion object {
        val Default = PrintQuality(density = 8, heatConfig = HeatConfig.DEFAULT)
        val Light = PrintQuality(density = 4, heatConfig = HeatConfig(dots = 5, time = 60, interval = 2))
        val Dark = PrintQuality(density = 15, heatConfig = HeatConfig.DARK)
    }
}

