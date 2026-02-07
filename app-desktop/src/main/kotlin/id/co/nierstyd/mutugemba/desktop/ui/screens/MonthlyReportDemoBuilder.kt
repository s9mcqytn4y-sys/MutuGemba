package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.data.SampleData
import id.co.nierstyd.mutugemba.domain.DefectSeverity
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocumentNumber
import id.co.nierstyd.mutugemba.domain.MonthlyReportHeader
import id.co.nierstyd.mutugemba.domain.MonthlyReportRow
import id.co.nierstyd.mutugemba.domain.MonthlyReportTotals
import java.time.YearMonth

private const val MULTIPLE_PIC_LABEL = "Multiple PIC"

fun buildDemoMonthlyReportDocument(
    pack: SampleData.DemoPack,
    lineId: Long,
    month: YearMonth,
): MonthlyReportDocument? {
    val line = pack.lines.firstOrNull { it.id == lineId } ?: pack.lines.firstOrNull() ?: return null
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val entries =
        pack.dailyDetails
            .filterKeys { (storedLineId, date) -> storedLineId == lineId && YearMonth.from(date) == month }
            .values
            .flatMap { it.entries }

    if (entries.isEmpty()) {
        return null
    }

    val parts =
        entries
            .groupBy { it.partNumber }
            .mapValues { (_, items) -> items.first() }

    val partIds =
        parts.keys
            .sorted()
            .mapIndexed { index, partNumber -> partNumber to (index + 1L) }
            .toMap()
    val defectType =
        DefectType(
            id = 1L,
            code = "NG",
            name = "NG",
            category = "General",
            severity = DefectSeverity.NORMAL,
        )

    val rows =
        parts.values
            .sortedBy { it.partNumber }
            .map { entry ->
                val dayValues =
                    days.map { date ->
                        entries
                            .filter { it.partNumber == entry.partNumber && it.date == date }
                            .sumOf { it.totalDefect }
                    }
                val totalDefect = dayValues.sum()
                MonthlyReportRow(
                    partId = partIds[entry.partNumber] ?: 0L,
                    partNumber = entry.partNumber,
                    uniqCode = entry.uniqCode,
                    problemItems = if (totalDefect > 0) listOf("NG") else emptyList(),
                    sketchPath = null,
                    dayValues = dayValues,
                    defectTotals = listOf(totalDefect),
                    totalDefect = totalDefect,
                )
            }

    val dayTotals =
        days.mapIndexed { index, _ ->
            rows.sumOf { it.dayValues.getOrNull(index) ?: 0 }
        }

    val defectTotals =
        listOf(rows.sumOf { it.totalDefect })

    val picNames =
        pack.dailyDetails
            .filterKeys { (storedLineId, date) -> storedLineId == lineId && YearMonth.from(date) == month }
            .values
            .map { it.picName.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    val picName =
        when {
            picNames.isEmpty() -> "-"
            picNames.size == 1 -> picNames.first()
            else -> MULTIPLE_PIC_LABEL
        }

    return MonthlyReportDocument(
        header =
            MonthlyReportHeader(
                lineId = line.id,
                lineName = line.name,
                lineCode = line.code,
                month = month,
                documentNumber = MonthlyReportDocumentNumber.generate(line.code, month),
                picName = picName,
            ),
        days = days,
        defectTypes = listOf(defectType),
        rows = rows,
        totals =
            MonthlyReportTotals(
                dayTotals = dayTotals,
                defectTotals = defectTotals,
                totalDefect = defectTotals.sum(),
            ),
    )
}
