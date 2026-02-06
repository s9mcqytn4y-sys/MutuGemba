package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import java.time.LocalDate
import java.time.YearMonth

class GetDailyChecksheetEntriesUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(
        lineId: Long,
        date: LocalDate = LocalDate.now(),
    ): List<ChecksheetEntry> = repository.getChecksheetEntriesForDate(lineId, date)
}

class GetMonthlyChecksheetEntriesUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(
        lineId: Long,
        month: YearMonth = YearMonth.now(),
    ): List<ChecksheetEntry> = repository.getChecksheetEntriesForMonth(lineId, month)
}

class GetMonthlyDailyChecksheetSummariesUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(month: YearMonth = YearMonth.now()): List<DailyChecksheetSummary> =
        repository.getDailyChecksheetSummaries(month)
}

class GetDailyChecksheetDetailUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(
        lineId: Long,
        date: LocalDate,
    ): DailyChecksheetDetail? = repository.getDailyChecksheetDetail(lineId, date)
}
