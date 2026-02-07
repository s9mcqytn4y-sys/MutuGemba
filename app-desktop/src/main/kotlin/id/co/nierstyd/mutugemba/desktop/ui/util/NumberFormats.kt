package id.co.nierstyd.mutugemba.desktop.ui.util

object NumberFormats {
    fun formatPercent(value: Double): String =
        if (value <= 0.0) {
            "-"
        } else {
            "${"%.1f".format(value * 100)}%"
        }

    fun formatPercentNoDecimal(value: Double): String =
        if (value <= 0.0) {
            "-"
        } else {
            "${"%.0f".format(value * 100)}%"
        }

    fun formatNumberNoDecimal(value: Double): String =
        if (value <= 0.0) {
            "-"
        } else {
            "%.0f".format(value)
        }
}
