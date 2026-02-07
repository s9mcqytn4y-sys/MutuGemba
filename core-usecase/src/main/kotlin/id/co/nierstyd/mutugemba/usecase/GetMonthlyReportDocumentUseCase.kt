package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocumentNumber
import id.co.nierstyd.mutugemba.domain.MonthlyReportHeader
import id.co.nierstyd.mutugemba.domain.MonthlyReportRow
import id.co.nierstyd.mutugemba.domain.MonthlyReportTotals
import java.time.YearMonth

private const val MULTIPLE_PIC_LABEL = "PIC Lebih dari 1"

class GetMonthlyReportDocumentUseCase(
    private val inspectionRepository: InspectionRepository,
    private val masterDataRepository: MasterDataRepository,
) {
    fun execute(
        lineId: Long,
        month: YearMonth,
    ): MonthlyReportDocument {
        val line =
            masterDataRepository
                .getLines()
                .firstOrNull { it.id == lineId }
                ?: error("Line tidak ditemukan.")

        val parts = inspectionRepository.getMonthlyParts(lineId, month)
        val dayDefects = inspectionRepository.getMonthlyPartDayDefects(lineId, month)
        val defectTotals = inspectionRepository.getMonthlyPartDefectTotals(lineId, month)

        val defectTotalsByType =
            defectTotals
                .groupBy { it.defectTypeId }
                .mapValues { (_, items) -> items.sumOf { it.totalDefect } }

        val defectTypes =
            masterDataRepository
                .getDefectTypes()
                .filter { defectTotalsByType.containsKey(it.id) }
                .sortedWith(compareByDescending<DefectType> { defectTotalsByType[it.id] ?: 0 }.thenBy { it.name })

        val partDayMap =
            dayDefects
                .groupBy { it.partId }
                .mapValues { (_, items) -> items.associateBy({ it.date }, { it.totalDefect }) }

        val partDefectMap =
            defectTotals
                .groupBy { it.partId }
                .mapValues { (_, items) -> items.associateBy({ it.defectTypeId }, { it.totalDefect }) }

        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }

        val rows =
            parts
                .sortedBy { it.partNumber }
                .map { part ->
                    val dayValues = days.map { date -> partDayMap[part.id]?.get(date) ?: 0 }
                    val perDefectTotals =
                        defectTypes.map { defect -> partDefectMap[part.id]?.get(defect.id) ?: 0 }
                    val totalDefect = dayValues.sum()
                    val problemItems =
                        defectTypes
                            .zip(perDefectTotals)
                            .filter { (_, total) -> total > 0 }
                            .map { (defect, _) -> defect.name }
                    MonthlyReportRow(
                        partId = part.id,
                        partNumber = part.partNumber,
                        uniqCode = part.uniqCode,
                        problemItems = problemItems,
                        sketchPath = part.picturePath,
                        dayValues = dayValues,
                        defectTotals = perDefectTotals,
                        totalDefect = totalDefect,
                    )
                }

        val dayTotals =
            days.mapIndexed { index, _ ->
                rows.sumOf { it.dayValues.getOrNull(index) ?: 0 }
            }

        val defectTotalsByColumn =
            defectTypes.mapIndexed { index, _ ->
                rows.sumOf { it.defectTotals.getOrNull(index) ?: 0 }
            }

        val totalDefect = defectTotalsByColumn.sum()

        val picName = resolvePicName(lineId, month)

        val header =
            MonthlyReportHeader(
                lineId = line.id,
                lineName = line.name,
                lineCode = line.code,
                month = month,
                documentNumber = MonthlyReportDocumentNumber.generate(line.code, month),
                picName = picName,
            )

        return MonthlyReportDocument(
            header = header,
            days = days,
            defectTypes = defectTypes,
            rows = rows,
            totals =
                MonthlyReportTotals(
                    dayTotals = dayTotals,
                    defectTotals = defectTotalsByColumn,
                    totalDefect = totalDefect,
                ),
        )
    }

    private fun resolvePicName(
        lineId: Long,
        month: YearMonth,
    ): String {
        val summaries =
            inspectionRepository
                .getDailyChecksheetSummaries(month)
                .filter { it.lineId == lineId }

        val picNames =
            summaries
                .map { it.picName.trim() }
                .filter { it.isNotBlank() }

        val distinct = picNames.distinct()
        return when {
            distinct.isEmpty() -> "-"
            distinct.size == 1 -> distinct.first()
            else -> MULTIPLE_PIC_LABEL
        }
    }
}
