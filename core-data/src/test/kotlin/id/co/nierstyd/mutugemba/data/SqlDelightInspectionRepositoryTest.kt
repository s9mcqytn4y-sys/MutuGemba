package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.data.db.InMemoryDatabase
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class SqlDelightInspectionRepositoryTest {
    @Test
    fun `daily and monthly reports consume saved inspection inputs`() {
        val db = InMemoryDatabase(Files.createTempFile("mutugemba-repo-test", ".db"))
        val repository = SqlDelightInspectionRepository(db)
        val line = db.lines.first()
        val part = db.parts.first { it.lineCode == line.code }
        val defect = db.defectTypes.first()
        val createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val today = LocalDate.now()
        val month = YearMonth.from(today)

        repository.insert(
            InspectionInput(
                kind = InspectionKind.DEFECT,
                lineId = line.id,
                shiftId = db.shifts.first().id,
                partId = part.id,
                totalCheck = 20,
                defectTypeId = null,
                defectQuantity = null,
                defects = listOf(InspectionDefectEntry(defectTypeId = defect.id, quantity = 3)),
                createdAt = createdAt,
            ),
        )

        val dailyDetail = repository.getDailyChecksheetDetail(line.id, today)
        assertNotNull(dailyDetail)
        assertEquals(20, dailyDetail?.totalCheck)
        assertEquals(3, dailyDetail?.totalDefect)
        assertEquals(17, dailyDetail?.totalOk)

        val dailySummaries = repository.getDailyChecksheetSummaries(month)
        val summary = dailySummaries.firstOrNull { it.lineId == line.id && it.date == today }
        assertNotNull(summary)
        assertEquals(20, summary?.totalCheck)
        assertEquals(3, summary?.totalDefect)

        val monthlyDefectTotals = repository.getMonthlyPartDefectTotals(line.id, month)
        assertEquals(1, monthlyDefectTotals.size)
        assertEquals(part.id, monthlyDefectTotals.first().partId)
        assertEquals(defect.id, monthlyDefectTotals.first().defectTypeId)
        assertEquals(3, monthlyDefectTotals.first().totalDefect)
    }
}
