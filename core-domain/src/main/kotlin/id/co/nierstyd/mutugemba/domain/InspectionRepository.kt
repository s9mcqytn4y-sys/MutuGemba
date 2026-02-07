package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate
import java.time.YearMonth

interface InspectionRepository {
    fun insert(input: InspectionInput): InspectionRecord

    fun getRecent(limit: Long): List<InspectionRecord>

    fun hasInspectionOnDate(
        lineId: Long,
        partId: Long,
        date: LocalDate,
    ): Boolean

    fun getChecksheetEntriesForDate(
        lineId: Long,
        date: LocalDate,
    ): List<ChecksheetEntry>

    fun getChecksheetEntriesForMonth(
        lineId: Long,
        month: YearMonth,
    ): List<ChecksheetEntry>

    fun getDailyChecksheetSummaries(month: YearMonth): List<DailyChecksheetSummary>

    fun getDailyChecksheetDetail(
        lineId: Long,
        date: LocalDate,
    ): DailyChecksheetDetail?

    fun getMonthlyDefectSummary(month: YearMonth): List<DefectSummary>

    fun getMonthlyPartDayDefects(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDayDefect>

    fun getMonthlyPartDefectTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectTotal>

    fun getMonthlyParts(
        lineId: Long,
        month: YearMonth,
    ): List<Part>
}
