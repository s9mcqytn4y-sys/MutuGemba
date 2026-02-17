package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
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

        val partDefectMap =
            defectTotals
                .groupBy { it.partId }
                .mapValues { (_, items) -> items.associateBy({ it.defectTypeId }, { it.totalDefect }) }
        val partDefectDayMap =
            defectDayTotals
                .groupBy { it.partId to it.defectTypeId }
                .mapValues { (_, items) -> items.associateBy({ it.date }, { it.totalDefect }) }

        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val rows = buildRows(parts, defectTypes, partDefectMap, partDefectDayMap, days)

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

    private fun problemItemLabel(defect: DefectType): String =
        DefectNameSanitizer.normalizeDisplay(defect.name).ifBlank { defect.name.trim() }

    private fun buildRows(
        parts: List<id.co.nierstyd.mutugemba.domain.Part>,
        defectTypes: List<DefectType>,
        partDefectMap: Map<Long, Map<Long, Int>>,
        partDefectDayMap: Map<Pair<Long, Long>, Map<java.time.LocalDate, Int>>,
        days: List<java.time.LocalDate>,
    ): List<MonthlyReportRow> {
        val problemLabelByDefectId = defectTypes.associate { it.id to problemItemLabel(it) }
        val defectTypeOrder = defectTypes.map { it.id }
        return parts
            .sortedBy { it.partNumber }
            .flatMap { part ->
                val partDefectTotals = partDefectMap[part.id].orEmpty().filterValues { total -> total > 0 }
                partDefectTotals.entries
                    .groupBy { (defectTypeId, _) -> problemLabelByDefectId[defectTypeId].orEmpty() }
                    .entries
                    .sortedBy { it.key }
                    .map { (problemLabel, groupedDefects) ->
                        val groupedDefectTypeIds = groupedDefects.map { it.key }.toSet()
                        val totalDefect = groupedDefects.sumOf { it.value }
                        val dayValues =
                            days.map { date ->
                                groupedDefectTypeIds.sumOf { defectTypeId ->
                                    partDefectDayMap[part.id to defectTypeId]?.get(date) ?: 0
                                }
                            }
                        val perDefectTotals =
                            defectTypeOrder.map { defectTypeId ->
                                groupedDefects
                                    .filter { it.key == defectTypeId }
                                    .sumOf { it.value }
                            }
                        MonthlyReportRow(
                            partId = part.id,
                            partNumber = part.partNumber,
                            uniqCode = part.uniqCode,
                            problemItems = listOf(problemLabel),
                            sketchPath = part.picturePath,
                            dayValues = dayValues,
                            defectTotals = perDefectTotals,
                            totalDefect = totalDefect,
                        )
                    }
            }
    }
}
