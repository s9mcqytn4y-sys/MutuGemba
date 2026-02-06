package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InspectionUseCasesTest {
    @Test
    fun `reject duplicate daily input for non-admin`() {
        val repository = FakeInspectionRepository(duplicatePartIds = setOf(1L))
        val useCase = CreateInspectionRecordUseCase(repository)
        val defectEntries = listOf(InspectionDefectEntry(defectTypeId = 1, quantity = 1))

        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 10,
                defectTypeId = null,
                defectQuantity = null,
                defects = defectEntries,
                createdAt = "2026-02-05T08:00:00",
            )

        val result = useCase.execute(input, actorRole = UserRole.USER)
        assertNull(result.record)
        assertEquals(FeedbackType.ERROR, result.feedback.type)
        assertNull(repository.lastInserted)
    }

    @Test
    fun `allow duplicate daily input for admin`() {
        val repository = FakeInspectionRepository(duplicatePartIds = setOf(1L))
        val useCase = CreateInspectionRecordUseCase(repository)
        val defectEntries = listOf(InspectionDefectEntry(defectTypeId = 1, quantity = 1))

        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 10,
                defectTypeId = null,
                defectQuantity = null,
                defects = defectEntries,
                createdAt = "2026-02-05T08:00:00",
            )

        val result = useCase.execute(input, actorRole = UserRole.ADMIN)
        assertNotNull(result.record)
        assertNotNull(repository.lastInserted)
    }

    @Test
    fun `reject total check below total defect`() {
        val repository = FakeInspectionRepository()
        val useCase = CreateInspectionRecordUseCase(repository)
        val defectEntries = listOf(InspectionDefectEntry(defectTypeId = 1, quantity = 3))

        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 2,
                defectTypeId = null,
                defectQuantity = null,
                defects = defectEntries,
                createdAt = "2026-02-05T08:00:00",
            )

        val result = useCase.execute(input, actorRole = UserRole.USER)
        assertNull(result.record)
        assertEquals(FeedbackType.ERROR, result.feedback.type)
        assertNull(repository.lastInserted)
    }

    @Test
    fun `batch save returns warning when some part fails`() {
        val repository = FakeInspectionRepository(duplicatePartIds = setOf(2L))
        val singleUseCase = CreateInspectionRecordUseCase(repository)
        val batchUseCase = CreateBatchInspectionRecordsUseCase(singleUseCase)

        val inputs =
            listOf(
                InspectionInput(
                    kind = InspectionKind.DEFECT,
                    lineId = 1,
                    shiftId = 1,
                    partId = 1,
                    totalCheck = 10,
                    defectTypeId = null,
                    defectQuantity = null,
                    defects = listOf(InspectionDefectEntry(defectTypeId = 1, quantity = 1)),
                    createdAt = "2026-02-05T08:00:00",
                ),
                InspectionInput(
                    kind = InspectionKind.DEFECT,
                    lineId = 1,
                    shiftId = 1,
                    partId = 2,
                    totalCheck = 10,
                    defectTypeId = null,
                    defectQuantity = null,
                    defects = listOf(InspectionDefectEntry(defectTypeId = 1, quantity = 1)),
                    createdAt = "2026-02-05T08:00:00",
                ),
            )

        val result = batchUseCase.execute(inputs)

        assertEquals(FeedbackType.WARNING, result.feedback.type)
        assertEquals(1, result.failedParts.size)
        assertEquals(1, result.savedRecords.size)
    }
}

private class FakeInspectionRepository(
    private val duplicatePartIds: Set<Long> = emptySet(),
) : InspectionRepository {
    var lastInserted: InspectionInput? = null

    override fun insert(input: InspectionInput): InspectionRecord {
        lastInserted = input
        return InspectionRecord(
            id = 1,
            kind = input.kind,
            lineName = "Press",
            shiftName = "Shift 1",
            partName = "Part",
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
    ): Boolean = duplicatePartIds.contains(partId)

    override fun getChecksheetEntriesForDate(
        lineId: Long,
        date: LocalDate,
    ): List<id.co.nierstyd.mutugemba.domain.ChecksheetEntry> = emptyList()

    override fun getChecksheetEntriesForMonth(
        lineId: Long,
        month: java.time.YearMonth,
    ): List<id.co.nierstyd.mutugemba.domain.ChecksheetEntry> = emptyList()
}
