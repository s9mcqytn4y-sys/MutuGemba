package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate

data class ChecksheetEntry(
    val inspectionId: Long,
    val date: LocalDate,
    val partNumber: String,
    val partName: String,
    val totalCheck: Int,
    val totalDefect: Int,
)
