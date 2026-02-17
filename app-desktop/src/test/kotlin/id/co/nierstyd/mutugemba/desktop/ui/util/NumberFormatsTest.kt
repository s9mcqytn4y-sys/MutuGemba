package id.co.nierstyd.mutugemba.desktop.ui.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumberFormatsTest {
    @Test
    fun formatPercent_returnsTwoDecimalForZeroAndRoundedValues() {
        assertEquals("0.00%", NumberFormats.formatPercent(0.0))
        assertEquals("20.10%", NumberFormats.formatPercent(0.201))
        assertEquals("33.33%", NumberFormats.formatPercent(0.333333))
    }

    @Test
    fun formatPercentNoDecimal_returnsZeroForInvalidAndNegative() {
        assertEquals("0%", NumberFormats.formatPercentNoDecimal(0.0))
        assertEquals("0%", NumberFormats.formatPercentNoDecimal(Double.NaN))
        assertEquals("0%", NumberFormats.formatPercentNoDecimal(-0.5))
    }

    @Test
    fun formatNumberNoDecimal_clampsNegativeAndInvalidValues() {
        assertEquals("0", NumberFormats.formatNumberNoDecimal(0.0))
        assertEquals("0", NumberFormats.formatNumberNoDecimal(Double.POSITIVE_INFINITY))
        assertEquals("12", NumberFormats.formatNumberNoDecimal(12.4))
    }
}
