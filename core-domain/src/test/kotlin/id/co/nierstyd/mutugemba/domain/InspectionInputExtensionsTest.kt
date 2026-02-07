package id.co.nierstyd.mutugemba.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InspectionInputExtensionsTest {
    @Test
    fun `normalized fills createdAt and picName`() {
        val now = LocalDateTime.of(2026, 2, 7, 9, 15)
        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 10,
                defectTypeId = 1,
                defectQuantity = 1,
                defects = emptyList(),
                picName = "",
                createdAt = " ",
            )

        val normalized = input.normalized(now)

        assertEquals(
            now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            normalized.createdAt,
        )
        assertEquals(InspectionInputDefaults.DEFAULT_PIC_NAME, normalized.picName)
    }

    @Test
    fun `resolveDefectEntries prefers detailed defects`() {
        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 10,
                defectTypeId = 2,
                defectQuantity = 3,
                defects = listOf(InspectionDefectEntry(defectTypeId = 9, quantity = 2)),
                createdAt = "2026-02-07T09:00:00",
            )

        val resolved = input.resolveDefectEntries()

        assertEquals(1, resolved.size)
        assertEquals(9, resolved.first().defectTypeId)
        assertEquals(2, resolved.first().quantity)
    }

    @Test
    fun `createdDateOrToday returns today when invalid`() {
        val today = LocalDate.of(2026, 2, 7)
        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = null,
                defectTypeId = null,
                defectQuantity = null,
                defects = emptyList(),
                createdAt = "invalid-date",
            )

        val result = input.createdDateOrToday(today)

        assertEquals(today, result)
    }

    @Test
    fun `resolveDefectEntries falls back to single defect`() {
        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 10,
                defectTypeId = 5,
                defectQuantity = 4,
                defects = emptyList(),
                createdAt = "2026-02-07T09:00:00",
            )

        val resolved = input.resolveDefectEntries()

        assertEquals(1, resolved.size)
        assertEquals(5, resolved.first().defectTypeId)
        assertEquals(4, resolved.first().quantity)
    }

    @Test
    fun `resolveDefectEntries returns empty when none`() {
        val input =
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = 1,
                shiftId = 1,
                partId = 1,
                totalCheck = 10,
                defectTypeId = null,
                defectQuantity = null,
                defects = emptyList(),
                createdAt = "2026-02-07T09:00:00",
            )

        val resolved = input.resolveDefectEntries()

        assertTrue(resolved.isEmpty())
    }
}
