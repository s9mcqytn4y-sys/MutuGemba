package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.data.db.InMemoryDatabase
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

class SqlDelightInspectionRepositoryAggregationTest {
    @Test
    fun dailyEntries_aggregatesDuplicateRecordsForSamePartAndDate() {
        val fixture = buildFixture()
        val date = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 10,
            defectQty = 2,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 8,
            defectQty = 1,
        )

        val entries = fixture.repository.getChecksheetEntriesForDate(fixture.pressLineId, date)

        assertEquals(1, entries.size)
        assertEquals(18, entries.single().totalCheck)
        assertEquals(3, entries.single().totalDefect)
        assertEquals(15, entries.single().totalCheck - entries.single().totalDefect)
    }

    @Test
    fun dailyEntries_keepsDifferentPartsAsSeparateRows() {
        val fixture = buildFixture()
        val date = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 9,
            defectQty = 2,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partBId,
            date = date,
            totalCheck = 7,
            defectQty = 1,
        )

        val entries = fixture.repository.getChecksheetEntriesForDate(fixture.pressLineId, date)

        assertEquals(2, entries.size)
        assertEquals(16, entries.sumOf { it.totalCheck })
        assertEquals(3, entries.sumOf { it.totalDefect })
    }

    @Test
    fun dailyEntries_ignoresRecordsFromOtherLines() {
        val fixture = buildFixture()
        val date = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 10,
            defectQty = 3,
        )
        fixture.insert(
            lineId = fixture.sewingLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 12,
            defectQty = 4,
        )

        val entries = fixture.repository.getChecksheetEntriesForDate(fixture.pressLineId, date)

        assertEquals(1, entries.size)
        assertEquals(10, entries.single().totalCheck)
        assertEquals(3, entries.single().totalDefect)
    }

    @Test
    fun dailyEntries_ignoresRecordsFromOtherDates() {
        val fixture = buildFixture()
        val requestedDate = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = LocalDate.of(2026, 2, 16),
            totalCheck = 10,
            defectQty = 1,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = requestedDate,
            totalCheck = 8,
            defectQty = 2,
        )

        val entries = fixture.repository.getChecksheetEntriesForDate(fixture.pressLineId, requestedDate)

        assertEquals(1, entries.size)
        assertEquals(8, entries.single().totalCheck)
        assertEquals(2, entries.single().totalDefect)
    }

    @Test
    fun dailyDetail_totalConsistentWithFormula() {
        val fixture = buildFixture()
        val date = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 15,
            defectQty = 3,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 5,
            defectQty = 1,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partBId,
            date = date,
            totalCheck = 12,
            defectQty = 2,
        )

        val detail = fixture.repository.getDailyChecksheetDetail(fixture.pressLineId, date)

        requireNotNull(detail)
        assertEquals(32, detail.totalCheck)
        assertEquals(6, detail.totalDefect)
        assertEquals(26, detail.totalOk)
        assertEquals(detail.totalCheck, detail.totalOk + detail.totalDefect)
    }

    @Test
    fun dailyDetail_returnsNullWhenNoData() {
        val fixture = buildFixture()

        val detail = fixture.repository.getDailyChecksheetDetail(fixture.pressLineId, LocalDate.of(2026, 2, 17))

        assertNull(detail)
    }

    @Test
    fun dailyDetail_clampsTotalOkWhenNgExceedsCheck() {
        val fixture = buildFixture()
        val date = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 1,
            defectQty = 5,
        )

        val detail = fixture.repository.getDailyChecksheetDetail(fixture.pressLineId, date)

        requireNotNull(detail)
        assertEquals(1, detail.totalCheck)
        assertEquals(5, detail.totalDefect)
        assertEquals(0, detail.totalOk)
    }

    @Test
    fun monthlyEntries_aggregatePerPartPerDate() {
        val fixture = buildFixture()
        val day1 = LocalDate.of(2026, 2, 3)
        val day2 = LocalDate.of(2026, 2, 4)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = day1,
            totalCheck = 10,
            defectQty = 2,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = day1,
            totalCheck = 5,
            defectQty = 1,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = day2,
            totalCheck = 8,
            defectQty = 2,
        )

        val entries = fixture.repository.getChecksheetEntriesForMonth(fixture.pressLineId, YearMonth.of(2026, 2))

        assertEquals(2, entries.size)
        val byDate = entries.associateBy { it.date }
        assertEquals(15, byDate.getValue(day1).totalCheck)
        assertEquals(3, byDate.getValue(day1).totalDefect)
        assertEquals(8, byDate.getValue(day2).totalCheck)
        assertEquals(2, byDate.getValue(day2).totalDefect)
    }

    @Test
    fun dailySummaries_countDistinctPartsOnly() {
        val fixture = buildFixture()
        val date = LocalDate.of(2026, 2, 17)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 10,
            defectQty = 2,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = date,
            totalCheck = 11,
            defectQty = 1,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partBId,
            date = date,
            totalCheck = 7,
            defectQty = 1,
        )

        val summary =
            fixture.repository
                .getDailyChecksheetSummaries(YearMonth.of(2026, 2))
                .single { it.lineId == fixture.pressLineId && it.date == date }

        assertEquals(2, summary.totalParts)
        assertEquals(28, summary.totalCheck)
        assertEquals(4, summary.totalDefect)
    }

    @Test
    fun dailySummaries_splitByLineAndDate() {
        val fixture = buildFixture()
        val day1 = LocalDate.of(2026, 2, 17)
        val day2 = LocalDate.of(2026, 2, 18)
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partAId,
            date = day1,
            totalCheck = 12,
            defectQty = 2,
        )
        fixture.insert(
            lineId = fixture.sewingLineId,
            partId = fixture.partAId,
            date = day1,
            totalCheck = 9,
            defectQty = 1,
        )
        fixture.insert(
            lineId = fixture.pressLineId,
            partId = fixture.partBId,
            date = day2,
            totalCheck = 8,
            defectQty = 3,
        )

        val summaries = fixture.repository.getDailyChecksheetSummaries(YearMonth.of(2026, 2))

        assertEquals(3, summaries.size)
        val pressDay1 = summaries.single { it.lineId == fixture.pressLineId && it.date == day1 }
        val sewingDay1 = summaries.single { it.lineId == fixture.sewingLineId && it.date == day1 }
        val pressDay2 = summaries.single { it.lineId == fixture.pressLineId && it.date == day2 }
        assertEquals(12, pressDay1.totalCheck)
        assertEquals(2, pressDay1.totalDefect)
        assertEquals(9, sewingDay1.totalCheck)
        assertEquals(1, sewingDay1.totalDefect)
        assertEquals(8, pressDay2.totalCheck)
        assertEquals(3, pressDay2.totalDefect)
    }

    private fun buildFixture(): Fixture {
        val db = InMemoryDatabase(Files.createTempFile("mutugemba-inspection-repo", ".db"))
        val repository = SqlDelightInspectionRepository(db)
        val partIds = db.parts.take(2).map { it.id }
        return Fixture(
            repository = repository,
            pressLineId = db.lines.first().id,
            sewingLineId = db.lines.last().id,
            partAId = partIds.first(),
            partBId = partIds.last(),
        )
    }

    private data class Fixture(
        val repository: SqlDelightInspectionRepository,
        val pressLineId: Long,
        val sewingLineId: Long,
        val partAId: Long,
        val partBId: Long,
    ) {
        fun insert(
            lineId: Long,
            partId: Long,
            date: LocalDate,
            totalCheck: Int,
            defectQty: Int,
            shiftId: Long = 1L,
        ) {
            repository.insert(
                InspectionInput(
                    kind = InspectionKind.DEFECT,
                    lineId = lineId,
                    shiftId = shiftId,
                    partId = partId,
                    totalCheck = totalCheck,
                    defectTypeId = null,
                    defectQuantity = null,
                    defects = listOf(InspectionDefectEntry(defectTypeId = 1L, quantity = defectQty)),
                    createdAt =
                        LocalDateTime
                            .of(date.year, date.monthValue, date.dayOfMonth, 8, 0)
                            .format(DATE_FORMATTER),
                ),
            )
        }
    }
}
