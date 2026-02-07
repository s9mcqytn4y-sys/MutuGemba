package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.ChecksheetDocumentNumber
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSeverity
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import java.time.LocalDate

object SampleData {
    data class DemoPack(
        val lines: List<Line>,
        val inspectionRecords: List<InspectionRecord>,
        val dailySummaries: List<DailyChecksheetSummary>,
        val dailyDetails: Map<Pair<Long, LocalDate>, DailyChecksheetDetail>,
        val monthlyDefectSummary: List<DefectSummary>,
    )

    fun buildDemoPack(today: LocalDate = LocalDate.now()): DemoPack {
        val linePress =
            Line(
                id = 1,
                code = LineCode.PRESS,
                name = "Press",
            )
        val lineSewing =
            Line(
                id = 2,
                code = LineCode.SEWING,
                name = "Sewing",
            )
        val lines = listOf(linePress, lineSewing)

        val createdAt = today.atTime(8, 30)
        val inspectionRecords =
            listOf(
                InspectionRecord(
                    id = 1,
                    kind = InspectionKind.DEFECT,
                    lineName = linePress.name,
                    shiftName = "Shift 1 (08:00-17:00 WIB)",
                    partName = "Housing Assy",
                    partNumber = "PN-1001",
                    totalCheck = 120,
                    createdAt = createdAt.toString(),
                ),
                InspectionRecord(
                    id = 2,
                    kind = InspectionKind.DEFECT,
                    lineName = linePress.name,
                    shiftName = "Shift 1 (08:00-17:00 WIB)",
                    partName = "Bracket Base",
                    partNumber = "PN-1002",
                    totalCheck = 90,
                    createdAt = createdAt.plusMinutes(25).toString(),
                ),
                InspectionRecord(
                    id = 3,
                    kind = InspectionKind.DEFECT,
                    lineName = lineSewing.name,
                    shiftName = "Shift 1 (08:00-17:00 WIB)",
                    partName = "Strap Holder",
                    partNumber = "PN-2001",
                    totalCheck = 110,
                    createdAt = createdAt.plusMinutes(55).toString(),
                ),
            )

        val dailySummaries =
            listOf(
                DailyChecksheetSummary(
                    checksheetId = 1,
                    docNumber = ChecksheetDocumentNumber.generate(linePress.code.name, today),
                    lineId = linePress.id,
                    lineName = linePress.name,
                    shiftName = "Shift 1",
                    date = today,
                    picName = "Admin QC",
                    totalParts = 2,
                    totalCheck = 210,
                    totalDefect = 7,
                    lastInputAt = createdAt.plusMinutes(25).toString(),
                ),
                DailyChecksheetSummary(
                    checksheetId = 2,
                    docNumber = ChecksheetDocumentNumber.generate(lineSewing.code.name, today),
                    lineId = lineSewing.id,
                    lineName = lineSewing.name,
                    shiftName = "Shift 1",
                    date = today,
                    picName = "Admin QC",
                    totalParts = 1,
                    totalCheck = 110,
                    totalDefect = 3,
                    lastInputAt = createdAt.plusMinutes(55).toString(),
                ),
            )

        val entriesPress =
            listOf(
                ChecksheetEntry(
                    inspectionId = 1,
                    date = today,
                    partNumber = "PN-1001",
                    uniqCode = "HG-A-001",
                    partName = "Housing Assy",
                    material = "Aluminium",
                    totalCheck = 120,
                    totalDefect = 4,
                ),
                ChecksheetEntry(
                    inspectionId = 2,
                    date = today,
                    partNumber = "PN-1002",
                    uniqCode = "BR-B-002",
                    partName = "Bracket Base",
                    material = "Steel",
                    totalCheck = 90,
                    totalDefect = 3,
                ),
            )
        val entriesSewing =
            listOf(
                ChecksheetEntry(
                    inspectionId = 3,
                    date = today,
                    partNumber = "PN-2001",
                    uniqCode = "SH-A-005",
                    partName = "Strap Holder",
                    material = "Fabric",
                    totalCheck = 110,
                    totalDefect = 3,
                ),
            )

        val defectsPress =
            listOf(
                DefectSummary(
                    defectTypeId = 1,
                    defectName = "Goresan",
                    category = "Permukaan",
                    severity = DefectSeverity.NORMAL,
                    totalQuantity = 4,
                ),
                DefectSummary(
                    defectTypeId = 2,
                    defectName = "Retak",
                    category = "Struktur",
                    severity = DefectSeverity.KRITIS,
                    totalQuantity = 3,
                ),
            )
        val defectsSewing =
            listOf(
                DefectSummary(
                    defectTypeId = 7,
                    defectName = "Jahitan Loncat",
                    category = "Jahitan",
                    severity = DefectSeverity.NORMAL,
                    totalQuantity = 3,
                ),
            )

        val dailyDetails =
            mapOf(
                linePress.id to today to
                    DailyChecksheetDetail(
                        checksheetId = 1,
                        docNumber = ChecksheetDocumentNumber.generate(linePress.code.name, today),
                        lineId = linePress.id,
                        lineName = linePress.name,
                        shiftName = "Shift 1",
                        date = today,
                        picName = "Admin QC",
                        totalCheck = entriesPress.sumOf { it.totalCheck },
                        totalDefect = entriesPress.sumOf { it.totalDefect },
                        totalOk = entriesPress.sumOf { it.totalCheck - it.totalDefect },
                        lastInputAt = createdAt.plusMinutes(25).toString(),
                        entries = entriesPress,
                        defectSummaries = defectsPress,
                    ),
                lineSewing.id to today to
                    DailyChecksheetDetail(
                        checksheetId = 2,
                        docNumber = ChecksheetDocumentNumber.generate(lineSewing.code.name, today),
                        lineId = lineSewing.id,
                        lineName = lineSewing.name,
                        shiftName = "Shift 1",
                        date = today,
                        picName = "Admin QC",
                        totalCheck = entriesSewing.sumOf { it.totalCheck },
                        totalDefect = entriesSewing.sumOf { it.totalDefect },
                        totalOk = entriesSewing.sumOf { it.totalCheck - it.totalDefect },
                        lastInputAt = createdAt.plusMinutes(55).toString(),
                        entries = entriesSewing,
                        defectSummaries = defectsSewing,
                    ),
            )

        val monthlyDefectSummary =
            listOf(
                defectsPress[0].copy(totalQuantity = 4),
                defectsPress[1].copy(totalQuantity = 3),
                defectsSewing[0].copy(totalQuantity = 3),
            )

        return DemoPack(
            lines = lines,
            inspectionRecords = inspectionRecords,
            dailySummaries = dailySummaries,
            dailyDetails = dailyDetails,
            monthlyDefectSummary = monthlyDefectSummary,
        )
    }
}
