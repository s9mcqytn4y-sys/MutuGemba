package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSeverity
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
import id.co.nierstyd.mutugemba.domain.MonthlyPartDayDefect
import id.co.nierstyd.mutugemba.domain.MonthlyPartDefectTotal
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyReportUseCasesTest {
    @Test
    fun `aggregate monthly report totals`() {
        val month = YearMonth.of(2026, 2)
        val line =
            Line(
                id = 1L,
                code = LineCode.PRESS,
                name = "Press",
            )
        val parts =
            listOf(
                Part(
                    id = 1L,
                    partNumber = "PN-1001",
                    model = "M1",
                    name = "Part A",
                    uniqCode = "U1",
                    material = "Steel",
                    picturePath = null,
                    lineCode = line.code,
                ),
                Part(
                    id = 2L,
                    partNumber = "PN-1002",
                    model = "M2",
                    name = "Part B",
                    uniqCode = "U2",
                    material = "Steel",
                    picturePath = null,
                    lineCode = line.code,
                ),
            )
        val defectTypes =
            listOf(
                DefectType(1L, "D1", "Scratch", "Surface", DefectSeverity.NORMAL),
                DefectType(2L, "D2", "Crack", "Structure", DefectSeverity.KRITIS),
            )

        val day1 = month.atDay(1)
        val day2 = month.atDay(2)
        val partDayDefects =
            listOf(
                MonthlyPartDayDefect(1L, day1, 3),
                MonthlyPartDayDefect(1L, day2, 1),
                MonthlyPartDayDefect(2L, day1, 2),
            )
        val partDefectTotals =
            listOf(
                MonthlyPartDefectTotal(1L, 1L, 3),
                MonthlyPartDefectTotal(1L, 2L, 1),
                MonthlyPartDefectTotal(2L, 1L, 2),
            )
        val summaries =
            listOf(
                DailyChecksheetSummary(
                    checksheetId = 1L,
                    docNumber = "DOC-1",
                    lineId = line.id,
                    lineName = line.name,
                    shiftName = "Shift 1",
                    date = day1,
                    picName = "Rina",
                    totalParts = 2,
                    totalCheck = 100,
                    totalDefect = 5,
                    lastInputAt = null,
                ),
            )

        val inspectionRepository =
            MonthlyReportFakeInspectionRepository(
                monthlyParts = parts,
                monthlyDayDefects = partDayDefects,
                monthlyDefectTotals = partDefectTotals,
                summaries = summaries,
            )
        val masterRepository = FakeMasterRepository(lines = listOf(line), defectTypes = defectTypes)

        val useCase = GetMonthlyReportDocumentUseCase(inspectionRepository, masterRepository)
        val document = useCase.execute(line.id, month)

        assertNotNull(document)
        assertEquals(2, document.rows.size)
        assertEquals(5, document.totals.dayTotals[0])
        assertEquals(1, document.totals.dayTotals[1])
        assertEquals(listOf(5, 1), document.totals.defectTotals)
        assertEquals(6, document.totals.totalDefect)
        assertEquals("Rina", document.header.picName)
    }

    @Test
    fun `use multiple pic label when more than one`() {
        val month = YearMonth.of(2026, 2)
        val line = Line(id = 1L, code = LineCode.PRESS, name = "Press")
        val inspectionRepository =
            MonthlyReportFakeInspectionRepository(
                monthlyParts = emptyList(),
                monthlyDayDefects = emptyList(),
                monthlyDefectTotals = emptyList(),
                summaries =
                    listOf(
                        DailyChecksheetSummary(
                            checksheetId = 1L,
                            docNumber = "DOC-1",
                            lineId = line.id,
                            lineName = line.name,
                            shiftName = "Shift 1",
                            date = month.atDay(1),
                            picName = "Rina",
                            totalParts = 1,
                            totalCheck = 50,
                            totalDefect = 2,
                            lastInputAt = null,
                        ),
                        DailyChecksheetSummary(
                            checksheetId = 2L,
                            docNumber = "DOC-2",
                            lineId = line.id,
                            lineName = line.name,
                            shiftName = "Shift 1",
                            date = month.atDay(2),
                            picName = "Dewi",
                            totalParts = 1,
                            totalCheck = 40,
                            totalDefect = 1,
                            lastInputAt = null,
                        ),
                    ),
            )
        val masterRepository = FakeMasterRepository(lines = listOf(line), defectTypes = emptyList())
        val useCase = GetMonthlyReportDocumentUseCase(inspectionRepository, masterRepository)

        val document = useCase.execute(line.id, month)

        assertEquals("PIC Lebih dari 1", document.header.picName)
    }
}

private class MonthlyReportFakeInspectionRepository(
    private val monthlyParts: List<Part>,
    private val monthlyDayDefects: List<MonthlyPartDayDefect>,
    private val monthlyDefectTotals: List<MonthlyPartDefectTotal>,
    private val summaries: List<DailyChecksheetSummary>,
) : InspectionRepository {
    override fun insert(input: InspectionInput): InspectionRecord = error("Not required")

    override fun getRecent(limit: Long): List<InspectionRecord> = emptyList()

    override fun hasInspectionOnDate(
        lineId: Long,
        partId: Long,
        date: LocalDate,
    ): Boolean = false

    override fun getChecksheetEntriesForDate(
        lineId: Long,
        date: LocalDate,
    ): List<ChecksheetEntry> = emptyList()

    override fun getChecksheetEntriesForMonth(
        lineId: Long,
        month: YearMonth,
    ): List<ChecksheetEntry> = emptyList()

    override fun getDailyChecksheetSummaries(month: YearMonth): List<DailyChecksheetSummary> = summaries

    override fun getDailyChecksheetDetail(
        lineId: Long,
        date: LocalDate,
    ): DailyChecksheetDetail? = null

    override fun getMonthlyDefectSummary(month: YearMonth): List<DefectSummary> = emptyList()

    override fun getMonthlyPartDayDefects(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDayDefect> = monthlyDayDefects

    override fun getMonthlyPartDefectTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectTotal> = monthlyDefectTotals

    override fun getMonthlyParts(
        lineId: Long,
        month: YearMonth,
    ): List<Part> = monthlyParts
}

private class FakeMasterRepository(
    private val lines: List<Line>,
    private val defectTypes: List<DefectType>,
) : MasterDataRepository {
    override fun getLines(): List<Line> = lines

    override fun getShifts(): List<Shift> = emptyList()

    override fun getParts(): List<Part> = emptyList()

    override fun getDefectTypes(): List<DefectType> = defectTypes

    override fun upsertDefectType(
        name: String,
        lineCode: LineCode,
    ): DefectType =
        DefectType(
            id = 999L,
            code = name.uppercase(),
            name = name,
            category = "CUSTOM",
            severity = DefectSeverity.NORMAL,
        )
}
