package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppRadioGroup
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.buildLineColors
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.resources.classpathPainterResource
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.desktop.ui.util.NumberFormats
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.Line
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.ceil

private const val HISTORY_PAGE_SIZE = 7
private val DocumentMaxWidth = 820.dp
private val DocumentMinHeight = 1120.dp

private enum class DocumentViewMode {
    PREVIEW,
    FULL,
}

private sealed class HistoryDetailState {
    data object Loading : HistoryDetailState()

    data object Empty : HistoryDetailState()

    data class Loaded(
        val detail: DailyChecksheetDetail,
    ) : HistoryDetailState()
}

private data class DocumentTotals(
    val totalCheck: Int,
    val totalDefect: Int,
    val totalOk: Int,
    val ratio: Double,
)

private fun deriveDocumentTotals(detail: DailyChecksheetDetail): DocumentTotals =
    deriveDocumentTotals(entries = detail.entries, fallback = detail)

private fun deriveDocumentTotals(
    entries: List<ChecksheetEntry>,
    fallback: DailyChecksheetDetail,
): DocumentTotals {
    if (entries.isEmpty()) {
        val ratio =
            if (fallback.totalCheck > 0) {
                fallback.totalDefect.toDouble() / fallback.totalCheck.toDouble()
            } else {
                0.0
            }
        return DocumentTotals(
            totalCheck = fallback.totalCheck,
            totalDefect = fallback.totalDefect,
            totalOk = fallback.totalOk,
            ratio = ratio,
        )
    }
    val totalCheck = entries.sumOf { it.totalCheck }
    val totalDefect = entries.sumOf { it.totalDefect }
    val totalOk = (totalCheck - totalDefect).coerceAtLeast(0)
    val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
    return DocumentTotals(
        totalCheck = totalCheck,
        totalDefect = totalDefect,
        totalOk = totalOk,
        ratio = ratio,
    )
}

@Composable
fun ReportsScreen(
    lines: List<Line>,
    dailySummaries: List<DailyChecksheetSummary>,
    loadDailyDetail: (Long, LocalDate) -> DailyChecksheetDetail?,
    loadManualHolidays: () -> Set<LocalDate>,
    saveManualHolidays: (Set<LocalDate>) -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }
    val today = now.toLocalDate()
    val todayMonth = YearMonth.from(today)
    var month by remember { mutableStateOf(todayMonth) }
    var selectedLineId by remember(lines) { mutableStateOf(lines.firstOrNull()?.id) }
    var selectedDate by remember { mutableStateOf(today) }
    val maxStoredDate =
        remember(dailySummaries, month) {
            dailySummaries
                .map { it.date }
                .filter { YearMonth.from(it) == month }
                .maxOrNull()
        }
    val availableDates = remember(month) { buildAvailableDates(month) }
    var pageIndex by remember { mutableStateOf(0) }
    var detailState by remember { mutableStateOf<HistoryDetailState>(HistoryDetailState.Empty) }
    var manualHolidays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var showHistoryInfo by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        manualHolidays = loadManualHolidays()
    }

    LaunchedEffect(lines) {
        if (selectedLineId == null && lines.isNotEmpty()) {
            selectedLineId = lines.first().id
        }
    }

    LaunchedEffect(availableDates, today, month, maxStoredDate) {
        if (availableDates.isNotEmpty()) {
            val targetDate =
                when {
                    month == todayMonth -> maxStoredDate ?: today
                    maxStoredDate != null -> maxStoredDate
                    else -> availableDates.last()
                }
            val targetIndex = availableDates.indexOf(targetDate).coerceAtLeast(0)
            pageIndex = targetIndex / HISTORY_PAGE_SIZE
            if (selectedDate !in availableDates) {
                selectedDate = targetDate
            }
        }
    }

    LaunchedEffect(selectedLineId, selectedDate) {
        val lineId = selectedLineId
        if (lineId == null) {
            detailState = HistoryDetailState.Empty
            return@LaunchedEffect
        }
        detailState = HistoryDetailState.Loading
        delay(180)
        val detail = loadDailyDetail(lineId, selectedDate)
        detailState = detail?.let { HistoryDetailState.Loaded(it) } ?: HistoryDetailState.Empty
    }

    val summaryByDate =
        remember(selectedLineId, dailySummaries, month) {
            dailySummaries
                .filter { YearMonth.from(it.date) == month }
                .filter { it.lineId == selectedLineId }
                .associateBy { it.date }
        }

    val lineCountsByDate =
        remember(dailySummaries, month) {
            dailySummaries
                .filter { YearMonth.from(it.date) == month }
                .groupBy { it.date }
                .mapValues { (_, items) -> items.groupBy { it.lineId }.mapValues { it.value.size } }
        }

    val lineColors =
        remember(lines) {
            buildLineColors(lines)
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = AppStrings.Reports.Title,
            subtitle = AppStrings.Reports.Subtitle,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MonthlyHistoryCard(
                month = month,
                todayMonth = todayMonth,
                lines = lines,
                selectedLineId = selectedLineId,
                onLineSelected = { selectedLineId = it },
                dates = availableDates,
                pageIndex = pageIndex,
                onPageChange = { pageIndex = it },
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDate = it
                    val targetIndex = availableDates.indexOf(it).coerceAtLeast(0)
                    pageIndex = targetIndex / HISTORY_PAGE_SIZE
                },
                summaryByDate = summaryByDate,
                lineCountsByDate = lineCountsByDate,
                lineColors = lineColors,
                today = today,
                onMonthChange = { target ->
                    month = if (target.isAfter(todayMonth)) todayMonth else target
                },
                manualHolidays = manualHolidays,
                onToggleHoliday = { date ->
                    if (!date.isAfter(today)) {
                        val updated =
                            if (manualHolidays.contains(date)) {
                                manualHolidays - date
                            } else {
                                manualHolidays + date
                            }
                        manualHolidays = updated
                        saveManualHolidays(updated)
                    }
                },
                showInfo = showHistoryInfo,
                onCloseInfo = { showHistoryInfo = false },
            )
            SectionDivider()

            DailyDocumentSection(
                detailState = detailState,
                allDates = availableDates,
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDate = it
                    val targetIndex = availableDates.indexOf(it).coerceAtLeast(0)
                    pageIndex = targetIndex / HISTORY_PAGE_SIZE
                },
            )
        }
    }
}

private fun buildAvailableDates(month: YearMonth): List<LocalDate> = (1..month.lengthOfMonth()).map { month.atDay(it) }

@Composable
private fun MonthlyHistoryCard(
    month: YearMonth,
    todayMonth: YearMonth,
    lines: List<Line>,
    selectedLineId: Long?,
    onLineSelected: (Long) -> Unit,
    dates: List<LocalDate>,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    summaryByDate: Map<LocalDate, DailyChecksheetSummary>,
    lineCountsByDate: Map<LocalDate, Map<Long, Int>>,
    lineColors: Map<Long, androidx.compose.ui.graphics.Color>,
    today: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    manualHolidays: Set<LocalDate>,
    onToggleHoliday: (LocalDate) -> Unit,
    showInfo: Boolean,
    onCloseInfo: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = AppIcons.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(text = AppStrings.Reports.HistoryTitle, style = MaterialTheme.typography.subtitle1)
                    }
                    Text(
                        text = AppStrings.Reports.historyMonthLabel(DateTimeFormats.formatMonth(month)),
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                AppBadge(
                    text = AppStrings.App.ReadOnly,
                    backgroundColor = NeutralLight,
                    contentColor = NeutralText,
                )
            }
            Divider(color = NeutralBorder, thickness = 1.dp)
            MonthSwitcher(
                month = month,
                todayMonth = todayMonth,
                onMonthChange = onMonthChange,
            )
            Text(
                text = AppStrings.Reports.HistoryHint,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )

            if (lines.isNotEmpty()) {
                AppRadioGroup(
                    label = AppStrings.Reports.LineHistoryLabel,
                    options = lines.map { DropdownOption(it.id, it.name) },
                    selectedId = selectedLineId,
                    onSelected = { onLineSelected(it.id) },
                    helperText = AppStrings.Reports.LineHistoryHint,
                )
            }

            DayPager(
                month = month,
                dates = dates,
                pageIndex = pageIndex,
                onPageChange = onPageChange,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                summaryByDate = summaryByDate,
                lineCountsByDate = lineCountsByDate,
                lineColors = lineColors,
                today = today,
                manualHolidays = manualHolidays,
                onToggleHoliday = onToggleHoliday,
            )

            HistoryLegend()
            if (lines.isNotEmpty()) {
                LineLegend(lines = lines, lineColors = lineColors)
            }

            if (showInfo) {
                StatusBanner(
                    feedback =
                        id.co.nierstyd.mutugemba.usecase.UserFeedback(
                            id.co.nierstyd.mutugemba.usecase.FeedbackType.INFO,
                            AppStrings.Reports.HistoryInfo,
                        ),
                    onDismiss = onCloseInfo,
                    dense = true,
                )
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    todayMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
) {
    val canNext = month.isBefore(todayMonth)
    val canPrev = true
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            PagerNavButton(
                enabled = canPrev,
                icon = AppIcons.ChevronLeft,
                label = AppStrings.Actions.PreviousMonth,
                onClick = { onMonthChange(month.minusMonths(1)) },
            )
            Text(
                text = DateTimeFormats.formatMonth(month),
                style = MaterialTheme.typography.subtitle2,
                color = NeutralText,
            )
            PagerNavButton(
                enabled = canNext,
                icon = AppIcons.ChevronRight,
                label = AppStrings.Actions.NextMonth,
                iconOnRight = true,
                onClick = { onMonthChange(month.plusMonths(1)) },
            )
        }
        if (month != todayMonth) {
            SecondaryButton(
                text = AppStrings.Actions.CurrentMonth,
                onClick = { onMonthChange(todayMonth) },
            )
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) BrandBlue else NeutralSurface
    val contentColor = if (selected) NeutralSurface else NeutralText
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        color = background,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun DayPager(
    month: YearMonth,
    dates: List<LocalDate>,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    summaryByDate: Map<LocalDate, DailyChecksheetSummary>,
    lineCountsByDate: Map<LocalDate, Map<Long, Int>>,
    lineColors: Map<Long, androidx.compose.ui.graphics.Color>,
    today: LocalDate,
    manualHolidays: Set<LocalDate>,
    onToggleHoliday: (LocalDate) -> Unit,
) {
    val totalPages = maxOf(1, ceil(dates.size / HISTORY_PAGE_SIZE.toDouble()).toInt())
    val currentPage = pageIndex.coerceIn(0, totalPages - 1)
    val pageStartDay = currentPage * HISTORY_PAGE_SIZE + 1
    val pageEndDay = (pageStartDay + HISTORY_PAGE_SIZE - 1).coerceAtMost(month.lengthOfMonth())
    val pageDayNumbers = (pageStartDay..pageEndDay).toList()
    val pageDates = pageDayNumbers.map { day -> month.atDay(day) }
    val rangeLabel =
        if (pageDates.isNotEmpty()) {
            val start = pageDates.first().dayOfMonth
            val end = pageDates.last().dayOfMonth
            AppStrings.Reports.monthlyRangeLabel(start, end)
        } else {
            AppStrings.Reports.MonthRangeLabel
        }
    val totalEnabled =
        pageDates.count {
            if (YearMonth.from(it) == YearMonth.from(today)) {
                !it.isAfter(today)
            } else {
                true
            }
        }
    val totalFilled = pageDates.count { summaryByDate[it] != null }
    val totalWeekend = pageDates.count { it.dayOfWeek.value >= 6 || manualHolidays.contains(it) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PagerNavButton(
                modifier = Modifier.widthIn(min = 140.dp),
                enabled = currentPage > 0,
                icon = AppIcons.ChevronLeft,
                label = AppStrings.Actions.Previous,
                onClick = { onPageChange(currentPage - 1) },
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = AppStrings.Reports.pageLabel(currentPage + 1, totalPages),
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }
            PagerNavButton(
                modifier = Modifier.widthIn(min = 140.dp),
                enabled = currentPage < totalPages - 1,
                icon = AppIcons.ChevronRight,
                label = AppStrings.Actions.Next,
                iconOnRight = true,
                onClick = { onPageChange(currentPage + 1) },
            )
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            (0 until HISTORY_PAGE_SIZE).forEach { offset ->
                val dayNumber = pageStartDay + offset
                if (dayNumber <= month.lengthOfMonth()) {
                    val date = month.atDay(dayNumber)
                    val hasInput = summaryByDate[date] != null
                    val enabled =
                        hasInput ||
                            if (YearMonth.from(date) == YearMonth.from(today)) {
                                !date.isAfter(today)
                            } else {
                                true
                            }
                    val isWeekend = date.dayOfWeek.value >= 6
                    val isHoliday = isWeekend || manualHolidays.contains(date)
                    DateButton(
                        date = date,
                        summary = summaryByDate[date],
                        selected = date == selectedDate,
                        isWeekend = isWeekend,
                        isHoliday = isHoliday,
                        enabled = enabled,
                        onClick = { if (enabled) onDateSelected(date) },
                        modifier = Modifier.weight(1f),
                        lineCounts = lineCountsByDate[date].orEmpty(),
                        lineColors = lineColors,
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadge(
                text = AppStrings.Reports.filledCount(totalFilled, totalEnabled),
                backgroundColor = StatusSuccess.copy(alpha = 0.12f),
                contentColor = StatusSuccess,
                modifier = Modifier.widthIn(min = 96.dp),
            )
            AppBadge(
                text = AppStrings.Reports.holidayCount(totalWeekend),
                backgroundColor = StatusWarning.copy(alpha = 0.12f),
                contentColor = StatusWarning,
                modifier = Modifier.widthIn(min = 96.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            val canToggleHoliday = !selectedDate.isAfter(today) && summaryByDate[selectedDate] == null
            SecondaryButton(
                text =
                    if (manualHolidays.contains(selectedDate)) {
                        AppStrings.Reports.ToggleHolidayOff
                    } else {
                        AppStrings.Reports.ToggleHoliday
                    },
                onClick = { if (canToggleHoliday) onToggleHoliday(selectedDate) },
                enabled = canToggleHoliday,
            )
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
    }
}

@Composable
private fun PagerNavButton(
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    iconOnRight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (enabled) NeutralSurface else NeutralLight
    val content = if (enabled) NeutralText else NeutralTextMuted
    Surface(
        modifier = modifier.height(36.dp).clickable(enabled = enabled) { onClick() },
        shape = MaterialTheme.shapes.small,
        color = background,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (!iconOnRight) {
                Icon(imageVector = icon, contentDescription = label, tint = content, modifier = Modifier.size(16.dp))
            }
            Text(text = label, style = MaterialTheme.typography.body2, color = content)
            if (iconOnRight) {
                Icon(imageVector = icon, contentDescription = label, tint = content, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DateButton(
    date: LocalDate,
    summary: DailyChecksheetSummary?,
    selected: Boolean,
    isWeekend: Boolean,
    isHoliday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lineCounts: Map<Long, Int> = emptyMap(),
    lineColors: Map<Long, androidx.compose.ui.graphics.Color> = emptyMap(),
) {
    val hasInput = summary != null
    val borderColor =
        when {
            selected -> BrandBlue
            hasInput -> StatusSuccess
            else -> NeutralBorder
        }
    val background =
        when {
            hasInput -> StatusSuccess.copy(alpha = 0.08f)
            isHoliday -> StatusWarning.copy(alpha = 0.08f)
            else -> NeutralSurface
        }
    val alpha = if (enabled) 1f else 0.45f
    val tooltipText =
        if (summary == null) {
            AppStrings.Reports.NoInput
        } else {
            AppStrings.Reports.ngCheckTooltip(summary.totalDefect, summary.totalCheck)
        }
    Box(modifier = modifier) {
        TooltipArea(
            tooltip = { TooltipCard(text = tooltipText) },
            delayMillis = 250,
        ) {
            Surface(
                modifier = Modifier.heightIn(min = 48.dp).clickable(enabled = enabled) { onClick() },
                shape = MaterialTheme.shapes.small,
                color = background,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                elevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.subtitle1,
                        color = (if (isHoliday) StatusWarning else NeutralText).copy(alpha = alpha),
                    )
                    if (isHoliday && summary == null) {
                        Text(
                            text = AppStrings.Reports.HolidayLabel,
                            style = MaterialTheme.typography.caption,
                            color = StatusWarning,
                        )
                    }
                    if (summary != null && summary.totalDefect > 0) {
                        Text(
                            text = AppStrings.Reports.ngCountLabel(summary.totalDefect),
                            style = MaterialTheme.typography.caption,
                            color = StatusWarning,
                        )
                    }
                    if (lineCounts.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            lineCounts.entries.sortedBy { it.key }.forEach { (lineId, count) ->
                                val color = lineColors[lineId] ?: StatusInfo
                                LineCountBadge(
                                    label = count.toString(),
                                    color = color,
                                )
                            }
                        }
                    }
                    if (hasInput) {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendPill(
            icon = AppIcons.CheckCircle,
            color = StatusSuccess,
            label = AppStrings.Reports.LegendFilled,
        )
        LegendPill(
            icon = AppIcons.RadioButtonUnchecked,
            color = NeutralBorder,
            label = AppStrings.Reports.LegendEmpty,
        )
        LegendPill(
            icon = AppIcons.RadioButtonUnchecked,
            color = StatusWarning,
            label = AppStrings.Reports.LegendWeekend,
        )
    }
    Text(
        text = AppStrings.Reports.LineCountHint,
        style = MaterialTheme.typography.caption,
        color = NeutralTextMuted,
        modifier = Modifier.padding(top = Spacing.xs),
    )
}

@Composable
private fun LineLegend(
    lines: List<Line>,
    lineColors: Map<Long, androidx.compose.ui.graphics.Color>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lines.forEach { line ->
            val color = lineColors[line.id] ?: StatusInfo
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(color, CircleShape),
                )
                Text(text = line.name, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            }
        }
    }
}

@Composable
private fun LegendPill(
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    label: String,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = NeutralLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun DailyDocumentSection(
    detailState: HistoryDetailState,
    allDates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    var viewMode by remember { mutableStateOf(DocumentViewMode.PREVIEW) }
    val selectedIndex = allDates.indexOf(selectedDate)
    val previousDate = if (selectedIndex > 0) allDates.getOrNull(selectedIndex - 1) else null
    val nextDate =
        if (selectedIndex >= 0 && selectedIndex < allDates.lastIndex) {
            allDates.getOrNull(selectedIndex + 1)
        } else {
            null
        }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = AppIcons.Description,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(text = AppStrings.Reports.DailyDocumentTitle, style = MaterialTheme.typography.subtitle1)
            AppBadge(
                text = AppStrings.App.ReadOnly,
                backgroundColor = NeutralLight,
                contentColor = NeutralTextMuted,
            )
        }
        Text(
            text = AppStrings.Reports.dailyDocumentLabel(DateTimeFormats.formatDate(selectedDate)),
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )
        DocumentViewToggle(
            viewMode = viewMode,
            onModeChange = { viewMode = it },
        )
        when (detailState) {
            HistoryDetailState.Loading -> HistoryDetailSkeleton()
            HistoryDetailState.Empty -> EmptyHistoryState(selectedDate = selectedDate)
            is HistoryDetailState.Loaded -> {
                if (viewMode == DocumentViewMode.PREVIEW) {
                    DocumentPreviewCard(
                        detail = detailState.detail,
                        onExpand = { viewMode = DocumentViewMode.FULL },
                    )
                } else {
                    DailyDocumentCard(detail = detailState.detail)
                }
                DailyDocumentBottomNavigator(
                    previousDate = previousDate,
                    nextDate = nextDate,
                    onSelectDate = onDateSelected,
                )
                DocumentActionRow()
            }
        }
    }
}

@Composable
private fun DailyDocumentBottomNavigator(
    previousDate: LocalDate?,
    nextDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PagerNavButton(
                enabled = previousDate != null,
                icon = AppIcons.ChevronLeft,
                label = AppStrings.Actions.Previous,
                onClick = { previousDate?.let(onSelectDate) },
            )
            Text(
                text = "Navigasi cepat dokumen",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            PagerNavButton(
                enabled = nextDate != null,
                icon = AppIcons.ChevronRight,
                label = AppStrings.Actions.Next,
                iconOnRight = true,
                onClick = { nextDate?.let(onSelectDate) },
            )
        }
    }
}

@Composable
private fun DocumentViewToggle(
    viewMode: DocumentViewMode,
    onModeChange: (DocumentViewMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ToggleChip(
            label = AppStrings.Reports.PreviewBeforePrint,
            selected = viewMode == DocumentViewMode.PREVIEW,
            onClick = { onModeChange(DocumentViewMode.PREVIEW) },
        )
        ToggleChip(
            label = AppStrings.Reports.DocumentFull,
            selected = viewMode == DocumentViewMode.FULL,
            onClick = { onModeChange(DocumentViewMode.FULL) },
        )
    }
}

@Composable
private fun HistoryDetailSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SkeletonBlock(width = 220.dp, height = 18.dp)
            SkeletonBlock(width = 320.dp, height = 12.dp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SkeletonBlock(width = 120.dp, height = 48.dp)
                SkeletonBlock(width = 120.dp, height = 48.dp)
                SkeletonBlock(width = 120.dp, height = 48.dp)
            }
            SkeletonBlock(width = 400.dp, height = 160.dp)
        }
    }
}

@Composable
private fun EmptyHistoryState(selectedDate: LocalDate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = AppStrings.Reports.DocumentEmptyTitle, style = MaterialTheme.typography.subtitle1)
            Text(
                text = AppStrings.Reports.documentNotFound(DateTimeFormats.formatDate(selectedDate)),
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun DailyDocumentCard(detail: DailyChecksheetDetail) {
    val totals = deriveDocumentTotals(detail)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralLight,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = DocumentMaxWidth)
                        .heightIn(min = DocumentMinHeight),
                color = NeutralSurface,
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
                elevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    DocumentHeader(detail = detail)
                    DocumentTotalsRow(totals = totals)
                    DocumentEntryTable(entries = detail.entries, totals = totals)
                    DailyStatsCard(detail = detail)
                    Spacer(modifier = Modifier.weight(1f))
                    SignatureFooter()
                }
            }
        }
    }
}

@Composable
private fun DocumentPreviewCard(
    detail: DailyChecksheetDetail,
    onExpand: () -> Unit,
) {
    val previewRows = detail.entries.take(4)
    val totals =
        deriveDocumentTotals(
            entries = previewRows,
            fallback = detail,
        )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = AppStrings.Reports.DocumentPreviewTitle, style = MaterialTheme.typography.subtitle1)
                SecondaryButton(
                    text = AppStrings.Actions.ViewFullDocument,
                    onClick = onExpand,
                )
            }
            DailyDocumentMiniHeader(detail = detail)
            DocumentTotalsRow(totals = totals)
            DocumentEntryTable(entries = previewRows, totals = totals)
            Text(
                text = AppStrings.Reports.previewCount(previewRows.size, detail.entries.size),
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun DailyDocumentMiniHeader(detail: DailyChecksheetDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = AppStrings.App.CompanyName, style = MaterialTheme.typography.subtitle2)
                Text(
                    text = AppStrings.App.DepartmentName,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AppStrings.Reports.DocumentNumberLabel,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(text = detail.docNumber, style = MaterialTheme.typography.body2)
            }
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaDate,
                value = DateTimeFormats.formatDate(detail.date),
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaLine,
                value = detail.lineName,
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaShift,
                value = detail.shiftName,
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaPic,
                value = detail.picName,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DocumentHeader(detail: DailyChecksheetDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogoMark()
                Column(
                    modifier = Modifier.heightIn(min = 40.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = AppStrings.App.CompanyName, style = MaterialTheme.typography.subtitle2)
                    Text(
                        text = AppStrings.App.DepartmentName,
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
            }
            Column(modifier = Modifier.weight(1.6f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = AppStrings.Reports.DocumentHeaderTitle,
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = AppStrings.Reports.DocumentNumberLabel,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = detail.docNumber,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.End,
                )
            }
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaDate,
                value = DateTimeFormats.formatDate(detail.date),
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaLine,
                value = detail.lineName,
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaShift,
                value = detail.shiftName,
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = AppStrings.Reports.DocumentMetaPic,
                value = detail.picName,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LogoMark() {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        color = NeutralLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            Image(
                painter = classpathPainterResource("branding/pt_prima_mark.png"),
                contentDescription = AppStrings.App.Logo,
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun DocumentMetaItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun DocumentTotalsRow(totals: DocumentTotals) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        DocumentStatCard(title = AppStrings.Reports.DocumentTableTotalCheck, value = totals.totalCheck.toString())
        DocumentStatCard(title = AppStrings.Reports.DocumentTableTotalNg, value = totals.totalDefect.toString())
        DocumentStatCard(title = AppStrings.Reports.DocumentTableTotalOk, value = totals.totalOk.toString())
        DocumentStatCard(
            title = AppStrings.Reports.DocumentTableNgRatio,
            value = NumberFormats.formatPercent(totals.ratio),
        )
    }
}

@Composable
private fun RowScope.DocumentStatCard(
    title: String,
    value: String,
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = MaterialTheme.shapes.small,
        color = NeutralLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            Text(text = value, style = MaterialTheme.typography.subtitle1)
        }
    }
}

@Composable
private fun DocumentEntryTable(
    entries: List<ChecksheetEntry>,
    totals: DocumentTotals,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DocumentHeaderCell(text = "No", weight = 0.45f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTablePartUniq, weight = 1.05f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTablePartNumber, weight = 1.2f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTablePartName, weight = 1.55f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTableTotalCheck, weight = 0.8f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTableTotalNg, weight = 0.7f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTableTotalOk, weight = 0.7f)
            DocumentHeaderCell(text = AppStrings.Reports.DocumentTableNgRatio, weight = 0.8f)
        }

        entries.forEachIndexed { index, entry ->
            val ratio = if (entry.totalCheck > 0) entry.totalDefect.toDouble() / entry.totalCheck.toDouble() else 0.0
            val highRisk = ratio >= 0.15
            val rowBackground =
                if (highRisk) {
                    MaterialTheme.colors.primary.copy(alpha = 0.08f)
                } else if (index % 2 == 0) {
                    NeutralSurface
                } else {
                    NeutralLight
                }
            Row(modifier = Modifier.fillMaxWidth()) {
                DocumentBodyCell(
                    text = (index + 1).toString(),
                    weight = 0.45f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                )
                DocumentBodyCell(text = entry.uniqCode, weight = 1.05f, backgroundColor = rowBackground)
                DocumentBodyCell(text = entry.partNumber, weight = 1.2f, backgroundColor = rowBackground, maxLines = 2)
                DocumentBodyCell(text = entry.partName, weight = 1.55f, backgroundColor = rowBackground, maxLines = 2)
                DocumentBodyCell(
                    text = entry.totalCheck.toString(),
                    weight = 0.8f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                )
                DocumentBodyCell(
                    text = entry.totalDefect.toString(),
                    weight = 0.7f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                    textColor = if (entry.totalDefect > 0) MaterialTheme.colors.primary else NeutralTextMuted,
                    fontWeight = if (entry.totalDefect > 0) FontWeight.SemiBold else FontWeight.Normal,
                )
                DocumentBodyCell(
                    text = (entry.totalCheck - entry.totalDefect).coerceAtLeast(0).toString(),
                    weight = 0.7f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                )
                DocumentBodyCell(
                    text = NumberFormats.formatPercent(ratio),
                    weight = 0.8f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                    textColor = if (highRisk) StatusError else NeutralText,
                    fontWeight = if (highRisk) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            DocumentFooterCell(text = AppStrings.Reports.DocumentTableTotal, weight = 0.45f + 1.05f + 1.2f + 1.55f)
            DocumentFooterCell(text = totals.totalCheck.toString(), weight = 0.8f, alignCenter = true)
            DocumentFooterCell(text = totals.totalDefect.toString(), weight = 0.7f, alignCenter = true)
            DocumentFooterCell(text = totals.totalOk.toString(), weight = 0.7f, alignCenter = true)
            DocumentFooterCell(text = NumberFormats.formatPercent(totals.ratio), weight = 0.8f, alignCenter = true)
        }
    }
}

@Composable
private fun RowScope.DocumentHeaderCell(
    text: String,
    weight: Float,
) {
    val headerBackground = MaterialTheme.colors.primary.copy(alpha = 0.08f)
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(headerBackground)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = NeutralText,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.DocumentBodyCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
    backgroundColor: androidx.compose.ui.graphics.Color = NeutralSurface,
    maxLines: Int = 1,
    textColor: androidx.compose.ui.graphics.Color = NeutralText,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2.copy(fontWeight = fontWeight),
            color = textColor,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
private fun RowScope.DocumentFooterCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.body2, color = NeutralText)
    }
}

@Composable
private fun DailyStatsCard(detail: DailyChecksheetDetail) {
    val totals = deriveDocumentTotals(detail)
    val entries = detail.entries
    val totalCheck = totals.totalCheck
    val totalDefect = totals.totalDefect
    val overallRatio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0

    val mostNg = entries.maxByOrNull { it.totalDefect }
    val highestRatio =
        entries
            .filter { it.totalCheck > 0 }
            .maxByOrNull { it.totalDefect.toDouble() / it.totalCheck.toDouble() }
    val topDefect = detail.defectSummaries.maxByOrNull { it.totalQuantity }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralLight,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = AppStrings.Reports.DocumentTotalsTitle, style = MaterialTheme.typography.subtitle1)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatInline(
                    label = AppStrings.Reports.DocumentTotalsPart,
                    value = mostNg?.let { "${it.partNumber} ${it.partName}" } ?: AppStrings.Common.Placeholder,
                )
                StatInline(
                    label = AppStrings.Reports.DocumentTotalsRatio,
                    value =
                        highestRatio?.let {
                            val ratio = it.totalDefect.toDouble() / it.totalCheck.toDouble()
                            "${it.partNumber} (${NumberFormats.formatPercent(ratio)})"
                        } ?: AppStrings.Common.Placeholder,
                )
                StatInline(
                    label = AppStrings.Reports.DocumentTotalsOverall,
                    value = NumberFormats.formatPercent(overallRatio),
                )
                StatInline(
                    label = AppStrings.Reports.DocumentTotalsDefect,
                    value =
                        topDefect?.let {
                            "${it.defectName} (${it.totalQuantity})"
                        } ?: AppStrings.Common.Placeholder,
                )
            }
        }
    }
}

@Composable
private fun StatInline(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.body1, color = NeutralText)
    }
}

@Composable
private fun LineCountBadge(
    label: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TooltipCard(text: String) {
    Surface(
        color = NeutralText,
        shape = MaterialTheme.shapes.small,
        elevation = 4.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = NeutralSurface,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun SectionDivider() {
    Divider(color = NeutralBorder, thickness = 1.dp)
}

@Composable
private fun StatLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.body1, color = NeutralText)
    }
}

@Composable
private fun SignatureFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SignatureCell(label = AppStrings.Reports.DocumentSignaturePrepared)
            SignatureCell(label = AppStrings.Reports.DocumentSignatureChecked)
            SignatureCell(label = AppStrings.Reports.DocumentSignatureApproved)
        }
    }
}

@Composable
private fun RowScope.SignatureCell(label: String) {
    Surface(
        modifier = Modifier.weight(1f),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            Box(modifier = Modifier.height(64.dp).fillMaxWidth().background(NeutralLight))
            Text(
                text = AppStrings.Reports.DocumentSignatureName,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun DocumentActionRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SecondaryButton(
            text = AppStrings.Actions.PrintDocument,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = AppStrings.Actions.ExportPdf,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
    }
}
