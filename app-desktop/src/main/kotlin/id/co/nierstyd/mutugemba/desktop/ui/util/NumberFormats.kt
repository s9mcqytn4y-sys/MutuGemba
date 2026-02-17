package id.co.nierstyd.mutugemba.desktop.ui.util

object NumberFormats {
    fun formatPercent(value: Double): String =
        if (!value.isFinite()) {
            "0.00%"
        } else {
            "${"%.2f".format((value * 100).coerceAtLeast(0.0))}%"
        }

    fun formatPercentNoDecimal(value: Double): String =
        if (!value.isFinite()) {
            "0%"
        } else {
            "${"%.0f".format((value * 100).coerceAtLeast(0.0))}%"
        }

    fun formatNumberNoDecimal(value: Double): String =
        if (!value.isFinite()) {
            "0"
        } else {
            "%.0f".format(value.coerceAtLeast(0.0))
        }
}
