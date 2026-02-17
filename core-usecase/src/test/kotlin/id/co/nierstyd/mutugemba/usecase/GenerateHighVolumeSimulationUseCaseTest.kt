package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSeverity
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class GenerateHighVolumeSimulationUseCaseTest {
    @Test
    fun `execute returns zero when master data is incomplete`() {
        val useCase =
            GenerateHighVolumeSimulationUseCase(
                inspectionRepository = RecordingInspectionRepository(),
                masterDataRepository = FakeMasterDataRepository(),
            )

        val inserted = useCase.execute(days = 3, density = 2)

        assertEquals(0, inserted)
    }

    @Test
    fun `execute supports small part list and inserts records`() {
        val inspectionRepository = RecordingInspectionRepository()
        val useCase =
            GenerateHighVolumeSimulationUseCase(
                inspectionRepository = inspectionRepository,
                masterDataRepository =
                    FakeMasterDataRepository(
                        lines = listOf(Line(id = 1L, code = LineCode.PRESS, name = "Press")),
                        shifts =
                            listOf(
                                Shift(
                                    id = 1L,
                                    code = "S1",
                                    name = "Shift 1",
                                    startTime = "08:00",
                                    endTime = "17:00",
                                ),
                            ),
                        parts =
                            listOf(
                                Part(
                                    id = 11L,
                                    partNumber = "PN-001",
                                    model = "M1",
                                    name = "Part A",
                                    uniqCode = "UNIQ-001",
                                    material = "Material A",
                                    picturePath = null,
                                    lineCode = LineCode.PRESS,
                                    recommendedDefectCodes = listOf("DF-A"),
                                ),
                            ),
                        defects =
                            listOf(
                                DefectType(
                                    id = 101L,
                                    code = "DF-A",
                                    name = "Scratch",
                                    category = "ITEM_DEFECT",
                                    severity = DefectSeverity.NORMAL,
                                    lineCode = LineCode.PRESS,
                                ),
                            ),
                    ),
            )

        val inserted = useCase.execute(days = 1, density = 1)

        assertEquals(1, inserted)
        assertEquals(1, inspectionRepository.insertedInputs.size)
        assertTrue(
            inspectionRepository
                .insertedInputs
                .first()
                .defects
                .isNotEmpty(),
        )
    }
}

private class RecordingInspectionRepository : InspectionRepository {
    val insertedInputs = mutableListOf<InspectionInput>()

    override fun insert(input: InspectionInput): InspectionRecord {
        insertedInputs += input
        return InspectionRecord(
            id = insertedInputs.size.toLong(),
            kind = InspectionKind.DEFECT,
            lineName = "Press",
            shiftName = "Shift 1",
            partName = "Part A",
            partNumber = "PN-001",
            totalCheck = input.totalCheck,
            createdAt = input.createdAt,
        )
    }

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

    override fun getDailyChecksheetSummaries(month: YearMonth): List<DailyChecksheetSummary> = emptyList()

    override fun getDailyChecksheetDetail(
        lineId: Long,
        date: LocalDate,
    ): DailyChecksheetDetail? = null

    override fun getMonthlyDefectSummary(month: YearMonth): List<DefectSummary> = emptyList()

    override fun getMonthlyPartDayDefects(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDayDefect> = emptyList()

    override fun getMonthlyPartDefectTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectTotal> = emptyList()

    override fun getMonthlyPartDefectDayTotals(
        lineId: Long,
        month: YearMonth,
    ): List<MonthlyPartDefectDayTotal> = emptyList()

    override fun getMonthlyParts(
        lineId: Long,
        month: YearMonth,
    ): List<Part> = emptyList()
}

private class FakeMasterDataRepository(
    private val lines: List<Line> = emptyList(),
    private val shifts: List<Shift> = emptyList(),
    private val parts: List<Part> = emptyList(),
    private val defects: List<DefectType> = emptyList(),
) : MasterDataRepository {
    override fun getLines(): List<Line> = lines

    override fun getShifts(): List<Shift> = shifts

    override fun getParts(): List<Part> = parts

    override fun getDefectTypes(): List<DefectType> = defects

    override fun upsertDefectType(
        name: String,
        lineCode: LineCode,
    ): DefectType = throw UnsupportedOperationException("Tidak dipakai pada test ini.")
}
