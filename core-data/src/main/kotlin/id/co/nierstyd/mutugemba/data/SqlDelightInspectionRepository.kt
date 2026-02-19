package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.data.db.InMemoryDatabase
import id.co.nierstyd.mutugemba.data.db.StoredInspection
import id.co.nierstyd.mutugemba.domain.CacheInvalidator
import id.co.nierstyd.mutugemba.domain.ChecksheetDocumentNumber
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.MonthlyPartDayDefect
import id.co.nierstyd.mutugemba.domain.MonthlyPartDefectDayTotal
import id.co.nierstyd.mutugemba.domain.MonthlyPartDefectTotal
import id.co.nierstyd.mutugemba.domain.Part
import java.time.LocalDate
import java.time.YearMonth

class SqlDelightInspectionRepository(
    private val database: InMemoryDatabase,
) : InspectionRepository,
    CacheInvalidator {
    override fun insert(input: InspectionInput): InspectionRecord = database.insertInspection(input)

    override fun getRecent(limit: Long): List<InspectionRecord> =
        database.inspections
            .asReversed()
            .take(limit.toInt().coerceAtLeast(0))
            .map { it.record }

    override fun hasInspectionOnDate(
        lineId: Long,
        partId: Long,
        date: LocalDate,
    ): Boolean =
        database.inspections.any {
            it.input.lineId == lineId &&
                it.input.partId == partId &&
                it.createdDate == date
        }

    override fun getChecksheetEntriesForDate(
        lineId: Long,
        date: LocalDate,
    ): List<ChecksheetEntry> =
        database.inspections
            .filter { it.input.lineId == lineId && it.createdDate == date }
            .map { it.toChecksheetEntry(database.parts) }

    override fun getChecksheetEntriesForMonth(
        lineId: Long,
        month: YearMonth,
    ): List<ChecksheetEntry> =
        database.inspections
            .filter { it.input.lineId == lineId && YearMonth.from(it.createdDate) == month }
            .map { it.toChecksheetEntry(database.parts) }

    override fun getDailyChecksheetSummaries(month: YearMonth): List<DailyChecksheetSummary> =
        database.inspections
            .filter { YearMonth.from(it.createdDate) == month }
            .groupBy { it.input.lineId to it.createdDate }
            .map { (key, rows) ->
                val lineId = key.first
                val date = key.second
                val line = database.lines.firstOrNull { it.id == lineId }
                val totalDefect = rows.sumOf { it.defects.sumOf(InspectionDefectEntry::totalQuantity) }
                val totalCheck = rows.sumOf { it.input.totalCheck ?: 0 }
                DailyChecksheetSummary(
                    checksheetId = rows.first().record.id,
                    docNumber = ChecksheetDocumentNumber.generate(line?.code?.name ?: "PRESS", date),
                    lineId = lineId,
                    lineName = line?.name ?: "-",
                    shiftName = "Shift 1",
                    date = date,
                    picName = rows.first().input.picName,
                    totalParts = rows.map { it.input.partId }.distinct().size,
                    totalCheck = totalCheck,
                    totalDefect = totalDefect,
                    lastInputAt = rows.maxByOrNull { it.record.createdAt }?.record?.createdAt,
                )
            }.sortedByDescending { it.date }

    override fun getDailyChecksheetDetail(
        lineId: Long,
        date: LocalDate,
    ): DailyChecksheetDetail? {
        val rows = database.inspections.filter { it.input.lineId == lineId && it.createdDate == date }
        if (rows.isEmpty()) return null
        val line = database.lines.firstOrNull { it.id == lineId }
        val entries = rows.map { it.toChecksheetEntry(database.parts) }
        val defectSummaries =
            rows
                .flatMap { row ->
                    row.defects.map { defect ->
                        val defectType = database.defectTypes.firstOrNull { it.id == defect.defectTypeId }
                        DefectSummary(
                            defectTypeId = defect.defectTypeId,
                            defectName = defectType?.name ?: "Defect",
                            category = defectType?.category ?: "-",
                            severity = defectType?.severity ?: database.defectTypes.first().severity,
                            totalQuantity = defect.totalQuantity,
                        )
                    }
                }.groupBy { it.defectTypeId }
                .map { (_, values) -> values.first().copy(totalQuantity = values.sumOf { it.totalQuantity }) }
                .sortedByDescending { it.totalQuantity }

        val totalCheck = entries.sumOf { it.totalCheck }
        val totalDefect = defectSummaries.sumOf { it.totalQuantity }
        return DailyChecksheetDetail(
            checksheetId = rows.first().record.id,
            docNumber = ChecksheetDocumentNumber.generate(line?.code?.name ?: "PRESS", date),
            lineId = lineId,
            lineName = line?.name ?: "-",
            shiftName = "Shift 1",
            date = date,
            picName = rows.first().input.picName,
            totalCheck = totalCheck,
            totalDefect = totalDefect,
            totalOk = (totalCheck - totalDefect).coerceAtLeast(0),
            lastInputAt = rows.maxByOrNull { it.record.createdAt }?.record?.createdAt,
            entries = entries,
            defectSummaries = defectSummaries,
        )
    }

    override fun getMonthlyDefectSummary(month: YearMonth): List<DefectSummary> =
        database.inspections
            .filter { YearMonth.from(it.createdDate) == month }
            .flatMap { row ->
                row.defects.map { defect ->
                    val defectType = database.defectTypes.firstOrNull { it.id == defect.defectTypeId }
                    DefectSummary(
                        defectTypeId = defect.defectTypeId,
                        defectName = defectType?.name ?: "Defect",
                        category = defectType?.category ?: "-",
                        severity = defectType?.severity ?: database.defectTypes.first().severity,
                        totalQuantity = defect.totalQuantity,
                    )
                }
            }.groupBy { it.defectTypeId }
            .map { (_, values) -> values.first().copy(totalQuantity = values.sumOf { it.totalQuantity }) }
            .sortedByDescending { it.totalQuantity }

    override fun getMonthlyPartDayDefects(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDayDefect> =
        database.inspections
            .filter { it.input.lineId == lineId && YearMonth.from(it.createdDate) == month }
            .groupBy { it.input.partId to it.createdDate }
            .map { (key, values) ->
                MonthlyPartDayDefect(
                    partId = key.first,
                    date = key.second,
                    totalDefect = values.sumOf { it.defects.sumOf(InspectionDefectEntry::totalQuantity) },
                )
            }

    override fun getMonthlyPartDefectTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectTotal> =
        database.inspections
            .filter { it.input.lineId == lineId && YearMonth.from(it.createdDate) == month }
            .flatMap { row -> row.defects.map { row.input.partId to it } }
            .groupBy { it.first to it.second.defectTypeId }
            .map { (key, values) ->
                MonthlyPartDefectTotal(
                    partId = key.first,
                    defectTypeId = key.second,
                    totalDefect = values.sumOf { it.second.totalQuantity },
                )
            }

    override fun getMonthlyPartDefectDayTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectDayTotal> =
        database.inspections
            .filter { it.input.lineId == lineId && YearMonth.from(it.createdDate) == month }
            .flatMap { row ->
                row.defects.map { defect ->
                    Triple(row.input.partId, defect.defectTypeId, row.createdDate) to defect.totalQuantity
                }
            }.groupBy { it.first }
            .map { (key, values) ->
                MonthlyPartDefectDayTotal(
                    partId = key.first,
                    defectTypeId = key.second,
                    date = key.third,
                    totalDefect = values.sumOf { it.second },
                )
            }

    override fun getMonthlyParts(
        lineId: Long,
        month: YearMonth,
    ): List<Part> {
        val partIds =
            database.inspections
                .filter { it.input.lineId == lineId && YearMonth.from(it.createdDate) == month }
                .map { it.input.partId }
                .distinct()
        return database.parts.filter { it.id in partIds }
    }

    override fun clearCache() {
        // Nothing to clear for in-memory fallback.
    }

    private fun StoredInspection.toChecksheetEntry(parts: List<Part>): ChecksheetEntry {
        val part = parts.firstOrNull { it.id == input.partId }
        return ChecksheetEntry(
            inspectionId = record.id,
            date = createdDate,
            partNumber = part?.partNumber ?: "-",
            uniqCode = part?.uniqCode ?: "-",
            partName = part?.name ?: "-",
            material = part?.material ?: "-",
            totalCheck = input.totalCheck ?: 0,
            totalDefect = defects.sumOf(InspectionDefectEntry::totalQuantity),
        )
    }
}
