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
import id.co.nierstyd.mutugemba.domain.MonthlyPartDefectDayTotal
import id.co.nierstyd.mutugemba.domain.MonthlyPartDefectTotal
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyReportUseCasesTest {
    @Test
    fun `aggregate monthly report totals`() {
        val fixture = buildAggregateFixture()
        val inspectionRepository =
            MonthlyReportFakeInspectionRepository(
                monthlyParts = fixture.parts,
                monthlyDayDefects = fixture.partDayDefects,
                monthlyDefectTotals = fixture.partDefectTotals,
                monthlyDefectDayTotals = fixture.defectDayTotals,
                summaries = fixture.summaries,
            )
        val masterRepository =
            FakeMasterRepository(lines = listOf(fixture.line), defectTypes = fixture.defectTypes)

        val useCase = GetMonthlyReportDocumentUseCase(inspectionRepository, masterRepository)
        val document = useCase.execute(fixture.line.id, fixture.month)

        assertNotNull(document)
        assertEquals(3, document.rows.size)
        assertEquals(5, document.totals.dayTotals[0])
        assertEquals(1, document.totals.dayTotals[1])
        assertEquals(listOf(5, 1), document.totals.defectTotals)
        assertEquals(6, document.totals.totalDefect)
        val totalsByLabel =
            document.rows
                .groupBy { it.problemItems.firstOrNull() }
                .mapValues { (_, rows) -> rows.sumOf { it.totalDefect } }
        assertEquals(5, totalsByLabel["SCRATCH"])
        assertEquals(1, totalsByLabel["CRACK"])
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
                monthlyDefectDayTotals = emptyList(),
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

    @Test
    fun `normalize problem item label per monthly row`() {
        val month = YearMonth.of(2026, 2)
        val line = Line(id = 1L, code = LineCode.PRESS, name = "Press")
        val parts =
            listOf(
                Part(
                    id = 1L,
                    partNumber = "PN-2001",
                    model = "M1",
                    name = "Part X",
                    uniqCode = "UX",
                    material = "Material",
                    picturePath = null,
                    lineCode = line.code,
                ),
            )
        val defectTypes =
            listOf(
                DefectType(1L, "D1", "(CB9) OVERCUTTING J", "Surface", DefectSeverity.NORMAL),
                DefectType(2L, "D2", "SPUNBOUND TERLIPAT, SPUNBOND HARDEN", "Surface", DefectSeverity.NORMAL),
            )
        val defectTotals =
            listOf(
                MonthlyPartDefectTotal(1L, 1L, 7),
                MonthlyPartDefectTotal(1L, 2L, 4),
            )
        val defectDayTotals =
            listOf(
                MonthlyPartDefectDayTotal(1L, 1L, month.atDay(3), 7),
                MonthlyPartDefectDayTotal(1L, 2L, month.atDay(4), 4),
            )

        val inspectionRepository =
            MonthlyReportFakeInspectionRepository(
                monthlyParts = parts,
                monthlyDayDefects = emptyList(),
                monthlyDefectTotals = defectTotals,
                monthlyDefectDayTotals = defectDayTotals,
                summaries = emptyList(),
            )
        val masterRepository = FakeMasterRepository(lines = listOf(line), defectTypes = defectTypes)
        val document = GetMonthlyReportDocumentUseCase(inspectionRepository, masterRepository).execute(line.id, month)

        assertEquals(2, document.rows.size)
        val labels = document.rows.map { it.problemItems.firstOrNull().orEmpty() }
        assertTrue(labels.contains("OVERCUTTING"))
        assertTrue(labels.any { it.contains("SPUNBOND TERLIPAT") })
    }

    @Test
    fun `build monthly table rows per part and problem item with stable totals`() {
        val month = YearMonth.of(2026, 2)
        val line = Line(id = 1L, code = LineCode.PRESS, name = "Press")
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
        val defectTotals =
            listOf(
                MonthlyPartDefectTotal(1L, 1L, 4),
                MonthlyPartDefectTotal(1L, 2L, 2),
                MonthlyPartDefectTotal(2L, 1L, 3),
            )
        val defectDayTotals =
            listOf(
                MonthlyPartDefectDayTotal(1L, 1L, month.atDay(1), 3),
                MonthlyPartDefectDayTotal(1L, 1L, month.atDay(2), 1),
                MonthlyPartDefectDayTotal(1L, 2L, month.atDay(1), 2),
                MonthlyPartDefectDayTotal(2L, 1L, month.atDay(1), 2),
                MonthlyPartDefectDayTotal(2L, 1L, month.atDay(2), 1),
            )
        val repository =
            MonthlyReportFakeInspectionRepository(
                monthlyParts = parts,
                monthlyDayDefects = emptyList(),
                monthlyDefectTotals = defectTotals,
                monthlyDefectDayTotals = defectDayTotals,
                summaries = emptyList(),
            )
        val masterRepository = FakeMasterRepository(lines = listOf(line), defectTypes = defectTypes)

        val document = GetMonthlyReportDocumentUseCase(repository, masterRepository).execute(line.id, month)
        val partOneScratchRow =
            document.rows.first { row ->
                row.partId == 1L && row.problemItems.firstOrNull() == "SCRATCH"
            }

        assertEquals(month.lengthOfMonth(), document.days.size)
        assertEquals(3, document.rows.size)
        assertEquals(4, partOneScratchRow.dayValues[0] + partOneScratchRow.dayValues[1])
        assertEquals(listOf(7, 2), document.totals.dayTotals.take(2))
        assertEquals(listOf(7, 2), document.totals.defectTotals)
        assertEquals(9, document.totals.totalDefect)
    }

    @Test
    fun `merge duplicate normalized problem item labels per part`() {
        val month = YearMonth.of(2026, 2)
        val line = Line(id = 1L, code = LineCode.PRESS, name = "Press")
        val parts =
            listOf(
                Part(
                    id = 1L,
                    partNumber = "PN-3001",
                    model = "M3",
                    name = "Part Merge",
                    uniqCode = "UM1",
                    material = "Fabric",
                    picturePath = null,
                    lineCode = line.code,
                ),
            )
        val defectTypes =
            listOf(
                DefectType(1L, "D1", "(AA1) SEWING MIRING", "Surface", DefectSeverity.NORMAL),
                DefectType(2L, "D2", "SEWING MIRING", "Surface", DefectSeverity.NORMAL),
            )
        val defectTotals =
            listOf(
                MonthlyPartDefectTotal(1L, 1L, 3),
                MonthlyPartDefectTotal(1L, 2L, 5),
            )
        val defectDayTotals =
            listOf(
                MonthlyPartDefectDayTotal(1L, 1L, month.atDay(1), 1),
                MonthlyPartDefectDayTotal(1L, 1L, month.atDay(2), 2),
                MonthlyPartDefectDayTotal(1L, 2L, month.atDay(1), 4),
                MonthlyPartDefectDayTotal(1L, 2L, month.atDay(3), 1),
            )
        val repository =
            MonthlyReportFakeInspectionRepository(
                monthlyParts = parts,
                monthlyDayDefects = emptyList(),
                monthlyDefectTotals = defectTotals,
                monthlyDefectDayTotals = defectDayTotals,
                summaries = emptyList(),
            )
        val masterRepository = FakeMasterRepository(lines = listOf(line), defectTypes = defectTypes)

        val document = GetMonthlyReportDocumentUseCase(repository, masterRepository).execute(line.id, month)

        assertEquals(1, document.rows.size)
        assertEquals(listOf("SEWING MIRING"), document.rows.first().problemItems)
        assertEquals(8, document.rows.first().totalDefect)
        assertEquals(5, document.rows.first().dayValues[0])
        assertEquals(2, document.rows.first().dayValues[1])
        assertEquals(1, document.rows.first().dayValues[2])
        assertEquals(listOf(5, 3), document.rows.first().defectTotals)
        assertEquals(8, document.totals.totalDefect)
    }

    private fun buildAggregateFixture(): AggregateFixture {
        val month = YearMonth.of(2026, 2)
        val line = Line(id = 1L, code = LineCode.PRESS, name = "Press")
        val parts =
            listOf(
                Part(1L, "PN-1001", "M1", "Part A", "U1", "Steel", null, line.code),
                Part(2L, "PN-1002", "M2", "Part B", "U2", "Steel", null, line.code),
            )
        val defectTypes =
            listOf(
                DefectType(1L, "D1", "Scratch", "Surface", DefectSeverity.NORMAL),
                DefectType(2L, "D2", "Crack", "Structure", DefectSeverity.KRITIS),
            )
        val day1 = month.atDay(1)
        val day2 = month.atDay(2)
        return AggregateFixture(
            month = month,
            line = line,
            parts = parts,
            defectTypes = defectTypes,
            partDayDefects =
                listOf(
                    MonthlyPartDayDefect(1L, day1, 3),
                    MonthlyPartDayDefect(1L, day2, 1),
                    MonthlyPartDayDefect(2L, day1, 2),
                ),
            defectDayTotals =
                listOf(
                    MonthlyPartDefectDayTotal(1L, 1L, day1, 2),
                    MonthlyPartDefectDayTotal(1L, 1L, day2, 1),
                    MonthlyPartDefectDayTotal(1L, 2L, day1, 1),
                    MonthlyPartDefectDayTotal(2L, 1L, day1, 2),
                ),
            partDefectTotals =
                listOf(
                    MonthlyPartDefectTotal(1L, 1L, 3),
                    MonthlyPartDefectTotal(1L, 2L, 1),
                    MonthlyPartDefectTotal(2L, 1L, 2),
                ),
            summaries =
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
                ),
        )
    }
}

private data class AggregateFixture(
    val month: YearMonth,
    val line: Line,
    val parts: List<Part>,
    val defectTypes: List<DefectType>,
    val partDayDefects: List<MonthlyPartDayDefect>,
    val defectDayTotals: List<MonthlyPartDefectDayTotal>,
    val partDefectTotals: List<MonthlyPartDefectTotal>,
    val summaries: List<DailyChecksheetSummary>,
)

private class MonthlyReportFakeInspectionRepository(
    private val monthlyParts: List<Part>,
    private val monthlyDayDefects: List<MonthlyPartDayDefect>,
    private val monthlyDefectTotals: List<MonthlyPartDefectTotal>,
    private val monthlyDefectDayTotals: List<MonthlyPartDefectDayTotal>,
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

    override fun getMonthlyPartDefectDayTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectDayTotal> = monthlyDefectDayTotals

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
