package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChecksheetDocumentNumber {
    private val periodFormatter = DateTimeFormatter.ofPattern("yyyyMM", Locale("id", "ID"))
    private val dayFormatter = DateTimeFormatter.ofPattern("dd", Locale("id", "ID"))

    fun generate(
        lineCode: String,
        date: LocalDate,
    ): String {
        val lineToken = lineCode.uppercase().take(3).padEnd(3, 'X')
        return "MG-QC-$lineToken-${date.format(periodFormatter)}-${date.format(dayFormatter)}"
    }
}
