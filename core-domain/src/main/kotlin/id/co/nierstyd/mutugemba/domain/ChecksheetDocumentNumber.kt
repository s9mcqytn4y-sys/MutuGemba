package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChecksheetDocumentNumber {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale("id", "ID"))

    fun generate(
        lineCode: String,
        date: LocalDate,
    ): String = "PGN-QC-CHK-${lineCode.uppercase()}-${date.format(dateFormatter)}"
}
