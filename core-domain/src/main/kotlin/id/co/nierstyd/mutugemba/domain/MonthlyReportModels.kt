package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyReportHeader(
    val lineId: Long,
    val lineName: String,
    val lineCode: LineCode,
    val month: YearMonth,
    val documentNumber: String,
    val picName: String,
)

data class MonthlyReportRow(
    val partId: Long,
    val partNumber: String,
    val problemItem: String,
    val sketchPath: String?,
    val dayValues: List<Int>,
    val defectTotals: List<Int>,
    val totalDefect: Int,
)

data class MonthlyReportTotals(
    val dayTotals: List<Int>,
    val defectTotals: List<Int>,
    val totalDefect: Int,
)

data class MonthlyReportDocument(
    val header: MonthlyReportHeader,
    val days: List<LocalDate>,
    val defectTypes: List<DefectType>,
    val rows: List<MonthlyReportRow>,
    val totals: MonthlyReportTotals,
)

data class MonthlyPartDayDefect(
    val partId: Long,
    val date: LocalDate,
    val totalDefect: Int,
)

data class MonthlyPartDefectTotal(
    val partId: Long,
    val defectTypeId: Long,
    val totalDefect: Int,
)

object MonthlyReportDocumentNumber {
    fun generate(
        lineCode: LineCode,
        month: YearMonth,
    ): String {
        val monthLabel = "%04d%02d".format(month.year, month.monthValue)
        return "MR-${lineCode.name}-$monthLabel"
    }
}
