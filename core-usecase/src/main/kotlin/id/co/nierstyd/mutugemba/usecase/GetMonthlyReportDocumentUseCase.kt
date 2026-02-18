package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
import id.co.nierstyd.mutugemba.domain.MonthlyPartDefectDayTotal
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
        val defectDayTotals = inspectionRepository.getMonthlyPartDefectDayTotals(lineId, month)

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
        val partDefectDayMap = buildPartDefectDayMap(defectDayTotals)

        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }

        val rows =
            parts
                .sortedBy { it.partNumber }
                .flatMap { part ->
                    buildRowsForPart(
                        partId = part.id,
                        partNumber = part.partNumber,
                        uniqCode = part.uniqCode,
                        sketchPath = part.picturePath,
                        days = days,
                        defectTypes = defectTypes,
                        partDefectDayMap = partDefectDayMap,
                        partDayMap = partDayMap,
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

        val totalDefect = dayTotals.sum()

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

    @Suppress("LongParameterList", "ReturnCount")
    private fun buildRowsForPart(
        partId: Long,
        partNumber: String,
        uniqCode: String,
        sketchPath: String?,
        days: List<java.time.LocalDate>,
        defectTypes: List<DefectType>,
        partDefectDayMap: Map<Long, Map<Long, Map<java.time.LocalDate, Int>>>,
        partDayMap: Map<Long, Map<java.time.LocalDate, Int>>,
    ): List<MonthlyReportRow> {
        val partRows =
            defectTypes.mapNotNull { defect ->
                val dayValueMap = partDefectDayMap[partId]?.get(defect.id).orEmpty()
                val dayValues = days.map { day -> dayValueMap[day] ?: 0 }
                val total = dayValues.sum()
                if (total <= 0) {
                    null
                } else {
                    val label = canonicalProblemLabel(defect)
                    MonthlyReportRow(
                        partId = partId,
                        partNumber = partNumber,
                        uniqCode = uniqCode,
                        problemItems = listOf(label),
                        sketchPath = sketchPath,
                        dayValues = dayValues,
                        defectTotals =
                            defectTypes.map { other ->
                                if (other.id == defect.id) {
                                    total
                                } else {
                                    0
                                }
                            },
                        totalDefect = total,
                    )
                }
            }
        if (partRows.isNotEmpty()) return partRows

        val fallbackDayValues = days.map { day -> partDayMap[partId]?.get(day) ?: 0 }
        val fallbackTotal = fallbackDayValues.sum()
        if (fallbackTotal <= 0) return emptyList()
        val placeholderLabel = "ITEM DEFECT"
        return listOf(
            MonthlyReportRow(
                partId = partId,
                partNumber = partNumber,
                uniqCode = uniqCode,
                problemItems = listOf(placeholderLabel),
                sketchPath = sketchPath,
                dayValues = fallbackDayValues,
                defectTotals = defectTypes.map { 0 },
                totalDefect = fallbackTotal,
            ),
        )
    }

    private fun canonicalProblemLabel(defect: DefectType): String {
        val candidates =
            DefectNameSanitizer
                .expandProblemItems(defect.name)
                .ifEmpty {
                    listOf(DefectNameSanitizer.normalizeDisplay(defect.name))
                }.filter(DefectNameSanitizer::isMeaningfulItem)
        if (candidates.isNotEmpty()) return candidates.first()
        return DefectNameSanitizer.normalizeDisplay(defect.name).ifBlank { defect.code }
    }

    private fun buildPartDefectDayMap(
        rows: List<MonthlyPartDefectDayTotal>,
    ): Map<Long, Map<Long, Map<java.time.LocalDate, Int>>> =
        rows
            .groupBy { it.partId }
            .mapValues { (_, partRows) ->
                partRows
                    .groupBy { it.defectTypeId }
                    .mapValues { (_, defectRows) ->
                        defectRows.associate { it.date to it.totalDefect }
                    }
            }
}
