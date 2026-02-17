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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    @Test
    fun `execute with seed keeps simulation deterministic`() {
        val firstRepository = RecordingInspectionRepository()
        val secondRepository = RecordingInspectionRepository()
        val masterData =
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
                        Shift(
                            id = 2L,
                            code = "S2",
                            name = "Shift 2",
                            startTime = "17:00",
                            endTime = "01:00",
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
            )

        val firstUseCase =
            GenerateHighVolumeSimulationUseCase(
                inspectionRepository = firstRepository,
                masterDataRepository = masterData,
            )
        val secondUseCase =
            GenerateHighVolumeSimulationUseCase(
                inspectionRepository = secondRepository,
                masterDataRepository = masterData,
            )

        val firstInserted = firstUseCase.execute(days = 2, density = 2, seed = 42L)
        val secondInserted = secondUseCase.execute(days = 2, density = 2, seed = 42L)

        assertNotEquals(0, firstInserted)
        assertEquals(firstInserted, secondInserted)
        assertEquals(
            firstRepository.insertedInputs.first().totalCheck,
            secondRepository.insertedInputs.first().totalCheck,
        )
        val firstDefectTotal =
            firstRepository
                .insertedInputs
                .first()
                .defects
                .sumOf { it.quantity }
        val secondDefectTotal =
            secondRepository
                .insertedInputs
                .first()
                .defects
                .sumOf { it.quantity }
        assertEquals(
            firstDefectTotal,
            secondDefectTotal,
        )
    }

    @Test
    fun `fallback defect uses same material recommendation only`() {
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
                                    uniqCode = "U-1",
                                    material = "FELT",
                                    picturePath = null,
                                    lineCode = LineCode.PRESS,
                                    recommendedDefectCodes = listOf("DF-FELT"),
                                ),
                                Part(
                                    id = 12L,
                                    partNumber = "PN-002",
                                    model = "M2",
                                    name = "Part B",
                                    uniqCode = "U-2",
                                    material = "FELT",
                                    picturePath = null,
                                    lineCode = LineCode.PRESS,
                                    recommendedDefectCodes = emptyList(),
                                ),
                                Part(
                                    id = 13L,
                                    partNumber = "PN-003",
                                    model = "M3",
                                    name = "Part C",
                                    uniqCode = "U-3",
                                    material = "PVC",
                                    picturePath = null,
                                    lineCode = LineCode.PRESS,
                                    recommendedDefectCodes = listOf("DF-PVC"),
                                ),
                            ),
                        defects =
                            listOf(
                                DefectType(
                                    id = 101L,
                                    code = "DF-FELT",
                                    name = "Scratch Felt",
                                    category = "ITEM_DEFECT",
                                    severity = DefectSeverity.NORMAL,
                                    lineCode = LineCode.PRESS,
                                ),
                                DefectType(
                                    id = 102L,
                                    code = "DF-PVC",
                                    name = "Bubble PVC",
                                    category = "ITEM_DEFECT",
                                    severity = DefectSeverity.NORMAL,
                                    lineCode = LineCode.PRESS,
                                ),
                            ),
                    ),
            )

        useCase.execute(days = 1, density = 1, seed = 99L)

        val feltPartInputs = inspectionRepository.insertedInputs.filter { it.partId == 12L }
        assertFalse(feltPartInputs.isEmpty(), "Part tanpa rekomendasi tetap harus dapat defect fallback.")
        feltPartInputs.forEach { input ->
            val pickedDefectIds = input.defects.map { it.defectTypeId }.toSet()
            assertEquals(setOf(101L), pickedDefectIds, "Fallback defect harus relevan dengan material FELT.")
        }
    }

    @Test
    fun `simulation keeps total check above defect and ratio in realistic bound`() {
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
                                    uniqCode = "U-1",
                                    material = "FELT",
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

        useCase.execute(days = 4, density = 2, seed = 77L)

        inspectionRepository.insertedInputs.forEach { input ->
            val totalDefect = input.defects.sumOf { it.quantity }
            val totalCheck = input.totalCheck ?: 0
            assertTrue(totalCheck > totalDefect)
            val ratio = totalDefect.toDouble() / totalCheck.toDouble()
            assertTrue(ratio in 0.01..0.2, "Rasio NG simulasi harus realistis (1%-20%).")
        }
    }

    @Test
    fun `simulation still populates using line fallback when recommendations are empty`() {
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
                                    uniqCode = "U-1",
                                    material = "UNKNOWN-MAT",
                                    picturePath = null,
                                    lineCode = LineCode.PRESS,
                                    recommendedDefectCodes = emptyList(),
                                ),
                            ),
                        defects =
                            listOf(
                                DefectType(
                                    id = 101L,
                                    code = "D-101",
                                    name = "SEWING MIRING",
                                    category = "ITEM_DEFECT",
                                    severity = DefectSeverity.NORMAL,
                                    lineCode = LineCode.PRESS,
                                ),
                            ),
                    ),
            )

        val inserted = useCase.execute(days = 1, density = 1, seed = 123L)

        assertTrue(inserted > 0)
        assertTrue(inspectionRepository.insertedInputs.isNotEmpty())
        val pickedDefectIds =
            inspectionRepository.insertedInputs
                .flatMap { it.defects }
                .map { it.defectTypeId }
                .toSet()
        assertEquals(setOf(101L), pickedDefectIds)
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
