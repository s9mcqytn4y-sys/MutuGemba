package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate

data class DailyChecksheetSummary(
    val checksheetId: Long,
    val docNumber: String,
    val lineId: Long,
    val lineName: String,
    val shiftName: String,
    val date: LocalDate,
    val picName: String,
    val totalParts: Int,
    val totalCheck: Int,
    val totalDefect: Int,
    val lastInputAt: String?,
)

data class DailyChecksheetDetail(
    val checksheetId: Long,
    val docNumber: String,
    val lineId: Long,
    val lineName: String,
    val shiftName: String,
    val date: LocalDate,
    val picName: String,
    val totalCheck: Int,
    val totalDefect: Int,
    val totalOk: Int,
    val lastInputAt: String?,
    val entries: List<ChecksheetEntry>,
)
