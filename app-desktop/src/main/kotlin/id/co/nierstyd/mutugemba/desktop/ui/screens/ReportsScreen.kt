package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.formatPercent
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            title = "Laporan Checksheet Harian",
            subtitle = "Riwayat dan dokumen checksheet harian untuk evaluasi QC.",
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
                selectedDate = selectedDate,
            )
        }
    }
}

private fun buildAvailableDates(month: YearMonth): List<LocalDate> {
    return (1..month.lengthOfMonth()).map { month.atDay(it) }
}

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
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(text = "Riwayat Checksheet Bulan Ini", style = MaterialTheme.typography.subtitle1)
                    }
                    Text(
                        text = "Bulan ${DateTimeFormats.formatMonth(month)} • Riwayat bersifat read-only.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                AppBadge(
                    text = "Read-only",
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
                text = "Ganti tanggal di bar di atas untuk melihat riwayat. Tanggal masa depan tidak bisa dibuka.",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )

            if (lines.isNotEmpty()) {
                AppRadioGroup(
                    label = "Pilih Line (Riwayat)",
                    options = lines.map { DropdownOption(it.id, it.name) },
                    selectedId = selectedLineId,
                    onSelected = { onLineSelected(it.id) },
                    helperText = "Riwayat hanya menampilkan dokumen yang sudah tersimpan.",
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
                            "Data checksheet harian bersifat final. QC dapat menandai hari libur secara manual.",
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
                icon = Icons.Filled.ChevronLeft,
                label = "Bulan Sebelum",
                onClick = { onMonthChange(month.minusMonths(1)) },
            )
            Text(
                text = DateTimeFormats.formatMonth(month),
                style = MaterialTheme.typography.subtitle2,
                color = NeutralText,
            )
            PagerNavButton(
                enabled = canNext,
                icon = Icons.Filled.ChevronRight,
                label = "Bulan Berikut",
                iconOnRight = true,
                onClick = { onMonthChange(month.plusMonths(1)) },
            )
        }
        if (month != todayMonth) {
            SecondaryButton(
                text = "Bulan Ini",
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
            "Tanggal $start–$end"
        } else {
            "Tanggal"
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
                icon = Icons.Filled.ChevronLeft,
                label = "Sebelumnya",
                onClick = { onPageChange(currentPage - 1) },
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Halaman ${currentPage + 1} / $totalPages",
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
                icon = Icons.Filled.ChevronRight,
                label = "Berikutnya",
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
                text = "Terisi $totalFilled/$totalEnabled",
                backgroundColor = StatusSuccess.copy(alpha = 0.12f),
                contentColor = StatusSuccess,
                modifier = Modifier.widthIn(min = 96.dp),
            )
            AppBadge(
                text = "Libur $totalWeekend",
                backgroundColor = StatusWarning.copy(alpha = 0.12f),
                contentColor = StatusWarning,
                modifier = Modifier.widthIn(min = 96.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            val canToggleHoliday = !selectedDate.isAfter(today) && summaryByDate[selectedDate] == null
            SecondaryButton(
                text = if (manualHolidays.contains(selectedDate)) "Batalkan Libur" else "Tandai Libur",
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
            "Belum ada input"
        } else {
            "NG ${summary.totalDefect} • Periksa ${summary.totalCheck}"
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
                        text = "Libur",
                        style = MaterialTheme.typography.caption,
                        color = StatusWarning,
                    )
                }
                    if (summary != null && summary.totalDefect > 0) {
                        Text(
                            text = "NG ${summary.totalDefect}",
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
                            imageVector = Icons.Filled.CheckCircle,
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
            icon = Icons.Filled.CheckCircle,
            color = StatusSuccess,
            label = "Sudah Input",
        )
        LegendPill(
            icon = Icons.Filled.RadioButtonUnchecked,
            color = NeutralBorder,
            label = "Belum Input",
        )
        LegendPill(
            icon = Icons.Filled.RadioButtonUnchecked,
            color = StatusWarning,
            label = "Akhir Pekan/Libur",
        )
    }
    Text(
        text = "Angka kecil di tanggal menunjukkan jumlah input per line.",
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
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
    selectedDate: LocalDate,
) {
    var viewMode by remember { mutableStateOf(DocumentViewMode.PREVIEW) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(text = "Dokumen Checksheet Harian", style = MaterialTheme.typography.subtitle1)
            AppBadge(
                text = "Read-only",
                backgroundColor = NeutralLight,
                contentColor = NeutralTextMuted,
            )
        }
        Text(
            text = "Rincian dokumen untuk ${DateTimeFormats.formatDate(selectedDate)}.",
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
                DocumentActionRow()
            }
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
            label = "Preview Sebelum Cetak",
            selected = viewMode == DocumentViewMode.PREVIEW,
            onClick = { onModeChange(DocumentViewMode.PREVIEW) },
        )
        ToggleChip(
            label = "Dokumen Lengkap",
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
            Text(text = "Belum ada dokumen", style = MaterialTheme.typography.subtitle1)
            Text(
                text = "Tidak ditemukan data checksheet untuk ${DateTimeFormats.formatDate(selectedDate)}.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun DailyDocumentCard(detail: DailyChecksheetDetail) {
    val totals =
        DocumentTotals(
            totalCheck = detail.totalCheck,
            totalDefect = detail.totalDefect,
            totalOk = detail.totalOk,
            ratio = if (detail.totalCheck > 0) detail.totalDefect.toDouble() / detail.totalCheck.toDouble() else 0.0,
        )

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
    val totals =
        DocumentTotals(
            totalCheck = detail.totalCheck,
            totalDefect = detail.totalDefect,
            totalOk = detail.totalOk,
            ratio = if (detail.totalCheck > 0) detail.totalDefect.toDouble() / detail.totalCheck.toDouble() else 0.0,
        )
    val previewRows = detail.entries.take(4)
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
                Text(text = "Preview Dokumen (Ringkas)", style = MaterialTheme.typography.subtitle1)
                SecondaryButton(
                    text = "Lihat Dokumen Lengkap",
                    onClick = onExpand,
                )
            }
            DailyDocumentMiniHeader(detail = detail)
            DocumentTotalsRow(totals = totals)
            DocumentEntryTable(entries = previewRows, totals = totals)
            Text(
                text = "Menampilkan ${previewRows.size} dari ${detail.entries.size} part.",
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
                Text(text = "PT. Primaraya Graha Nusantara", style = MaterialTheme.typography.subtitle2)
                Text(
                    text = "Quality Assurance Dept.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "No. Dokumen", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                Text(text = detail.docNumber, style = MaterialTheme.typography.body2)
            }
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DocumentMetaItem(label = "Tanggal", value = DateTimeFormats.formatDate(detail.date), modifier = Modifier.weight(1f))
            DocumentMetaItem(label = "Line", value = detail.lineName, modifier = Modifier.weight(1f))
            DocumentMetaItem(label = "Shift", value = detail.shiftName, modifier = Modifier.weight(1f))
            DocumentMetaItem(label = "PIC", value = detail.picName, modifier = Modifier.weight(1f))
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
                    Text(text = "PT. Primaraya Graha Nusantara", style = MaterialTheme.typography.subtitle2)
                    Text(
                        text = "Quality Assurance Dept.",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
            }
            Column(modifier = Modifier.weight(1.6f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CHECKSHEET HARIAN",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(text = "No. Dokumen", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
                label = "Tanggal",
                value = DateTimeFormats.formatDate(detail.date),
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = "Line",
                value = detail.lineName,
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = "Shift",
                value = detail.shiftName,
                modifier = Modifier.weight(1f),
            )
            DocumentMetaItem(
                label = "PIC",
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
        Box(contentAlignment = Alignment.Center) {
            Text(text = "LOGO", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
        DocumentStatCard(title = "Total Periksa", value = totals.totalCheck.toString())
        DocumentStatCard(title = "Total NG", value = totals.totalDefect.toString())
        DocumentStatCard(title = "Total OK", value = totals.totalOk.toString())
        DocumentStatCard(title = "Rasio NG", value = formatPercent(totals.ratio))
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
            DocumentHeaderCell(text = "Part UNIQ", weight = 1.05f)
            DocumentHeaderCell(text = "Part Number", weight = 1.2f)
            DocumentHeaderCell(text = "Part Name", weight = 1.55f)
            DocumentHeaderCell(text = "Total Periksa", weight = 0.8f)
            DocumentHeaderCell(text = "Total NG", weight = 0.7f)
            DocumentHeaderCell(text = "Total OK", weight = 0.7f)
            DocumentHeaderCell(text = "Rasio NG", weight = 0.8f)
        }

        entries.forEachIndexed { index, entry ->
            val ratio = if (entry.totalCheck > 0) entry.totalDefect.toDouble() / entry.totalCheck.toDouble() else 0.0
            val rowBackground = if (index % 2 == 0) NeutralSurface else NeutralLight
            Row(modifier = Modifier.fillMaxWidth()) {
                DocumentBodyCell(text = entry.uniqCode, weight = 1.05f, backgroundColor = rowBackground)
                DocumentBodyCell(text = entry.partNumber, weight = 1.2f, backgroundColor = rowBackground)
                DocumentBodyCell(text = entry.partName, weight = 1.55f, backgroundColor = rowBackground)
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
                )
                DocumentBodyCell(
                    text = (entry.totalCheck - entry.totalDefect).coerceAtLeast(0).toString(),
                    weight = 0.7f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                )
                DocumentBodyCell(
                    text = formatPercent(ratio),
                    weight = 0.8f,
                    alignCenter = true,
                    backgroundColor = rowBackground,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            DocumentFooterCell(text = "Total", weight = 1.05f + 1.2f + 1.55f)
            DocumentFooterCell(text = totals.totalCheck.toString(), weight = 0.8f, alignCenter = true)
            DocumentFooterCell(text = totals.totalDefect.toString(), weight = 0.7f, alignCenter = true)
            DocumentFooterCell(text = totals.totalOk.toString(), weight = 0.7f, alignCenter = true)
            DocumentFooterCell(text = formatPercent(totals.ratio), weight = 0.8f, alignCenter = true)
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
            style = MaterialTheme.typography.body2,
            color = NeutralText,
            maxLines = 1,
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
    val entries = detail.entries
    val totalCheck = detail.totalCheck
    val totalDefect = detail.totalDefect
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
            Text(text = "Ringkasan Statistik Harian", style = MaterialTheme.typography.subtitle1)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatInline(
                    label = "Part dengan NG terbanyak",
                    value = mostNg?.let { "${it.partNumber} ${it.partName}" } ?: "-",
                )
                StatInline(
                    label = "Rasio NG part tertinggi",
                    value =
                        highestRatio?.let {
                            val ratio = it.totalDefect.toDouble() / it.totalCheck.toDouble()
                            "${it.partNumber} (${formatPercent(ratio)})"
                        } ?: "-",
                )
                StatInline(
                    label = "Rasio NG total hari ini",
                    value = formatPercent(overallRatio),
                )
                StatInline(
                    label = "Jenis NG terbanyak",
                    value = topDefect?.let { "${it.defectName} (${it.totalQuantity})" } ?: "-",
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
            SignatureCell(label = "Prepared")
            SignatureCell(label = "Checked")
            SignatureCell(label = "Approved")
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
            Text(text = "Nama & TTD", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
            text = "Cetak Dokumen",
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        PrimaryButton(
            text = "Ekspor PDF",
            onClick = {},
            modifier = Modifier.weight(1f),
        )
    }
}
