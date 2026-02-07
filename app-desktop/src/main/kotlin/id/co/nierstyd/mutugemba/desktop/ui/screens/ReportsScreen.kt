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
import id.co.nierstyd.mutugemba.analytics.paretoCounts
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppRadioGroup
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
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

private data class MonthlyTotals(
    val totalDocs: Int,
    val totalCheck: Int,
    val totalDefect: Int,
    val ratio: Double,
    val daysWithInput: Int,
    val daysInMonth: Int,
)

@Composable
fun ReportsScreen(
    lines: List<Line>,
    dailySummaries: List<DailyChecksheetSummary>,
    loadDailyDetail: (Long, LocalDate) -> DailyChecksheetDetail?,
    loadMonthlyDefectSummary: (YearMonth) -> List<DefectSummary>,
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
    var monthlyDefects by remember { mutableStateOf(emptyList<DefectSummary>()) }

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

    LaunchedEffect(month, dailySummaries) {
        monthlyDefects = loadMonthlyDefectSummary(month)
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

    var analysisLineId by remember { mutableStateOf<Long?>(null) }
    var analysisDefects by remember { mutableStateOf<List<DefectSummary>>(emptyList()) }
    var analysisLoading by remember { mutableStateOf(false) }

    LaunchedEffect(month, analysisLineId, availableDates) {
        if (analysisLineId == null) {
            analysisDefects = monthlyDefects
            return@LaunchedEffect
        }
        analysisLoading = true
        val lineId = analysisLineId ?: return@LaunchedEffect
        val totals =
            withContext(Dispatchers.Default) {
                val acc = mutableMapOf<Long, DefectSummary>()
                availableDates.forEach { date ->
                    val detail = loadDailyDetail(lineId, date)
                    detail?.defectSummaries?.forEach { summary ->
                        val existing = acc[summary.defectTypeId]
                        val total = (existing?.totalQuantity ?: 0) + summary.totalQuantity
                        acc[summary.defectTypeId] = summary.copy(totalQuantity = total)
                    }
                }
                acc.values.sortedByDescending { it.totalQuantity }
            }
        analysisDefects = totals
        analysisLoading = false
    }
    val monthlyTotals =
        remember(dailySummaries, month) {
            val summariesForMonth = dailySummaries.filter { YearMonth.from(it.date) == month }
            val totalDocs = summariesForMonth.size
            val totalCheck = summariesForMonth.sumOf { it.totalCheck }
            val totalDefect = summariesForMonth.sumOf { it.totalDefect }
            val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
            val daysWithInput = summariesForMonth.map { it.date }.distinct().size
            val daysInMonth = month.lengthOfMonth()
            MonthlyTotals(
                totalDocs = totalDocs,
                totalCheck = totalCheck,
                totalDefect = totalDefect,
                ratio = ratio,
                daysWithInput = daysWithInput,
                daysInMonth = daysInMonth,
            )
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Laporan Checksheet Harian",
            subtitle = "Ringkasan dokumen harian dan statistik bulanan untuk evaluasi QC.",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1.35f),
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
                )
                SectionDivider()

                DailyDocumentSection(
                    detailState = detailState,
                    selectedDate = selectedDate,
                )
            }

            Column(
                modifier = Modifier.weight(0.85f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                AnalysisFilterCard(
                    lines = lines,
                    selectedLineId = analysisLineId,
                    onSelected = { analysisLineId = it },
                )
                MonthlyInsightCard(
                    month = month,
                    totals = monthlyTotals,
                )
                SectionDivider()
                MonthlyParetoCard(
                    month = month,
                    defectSummaries = analysisDefects,
                    accentColor = lineColors[analysisLineId] ?: StatusInfo,
                    loading = analysisLoading,
                )
                MonthlyTrendCard(
                    month = month,
                    dailySummaries = dailySummaries.filter { YearMonth.from(it.date) == month },
                    lineColors = lineColors,
                    lines = lines,
                    selectedLineId = analysisLineId,
                )
            }
        }
    }
}

private fun buildLineColors(lines: List<Line>): Map<Long, androidx.compose.ui.graphics.Color> {
    val palette =
        listOf(
            BrandBlue,
            StatusSuccess,
            StatusWarning,
            StatusInfo,
        )
    return lines.mapIndexed { index, line -> line.id to palette[index % palette.size] }.toMap()
}

private fun buildAvailableDates(month: YearMonth): List<LocalDate> {
    return (1..month.lengthOfMonth()).map { month.atDay(it) }
}

private fun weekStartFor(date: LocalDate): LocalDate {
    val offset = (date.dayOfWeek.value + 6) % 7
    return date.minusDays(offset.toLong())
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
                text = "Gunakan pager atau kalender untuk memeriksa tanggal yang sudah diinput. Klik tanggal untuk melihat dokumen.",
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

            Divider(color = NeutralBorder, thickness = 1.dp)

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
            )

            HistoryLegend()
            if (lines.isNotEmpty()) {
                LineLegend(lines = lines, lineColors = lineColors)
            }

            StatusBanner(
                feedback =
                    id.co.nierstyd.mutugemba.usecase.UserFeedback(
                        id.co.nierstyd.mutugemba.usecase.FeedbackType.INFO,
                        "Data checksheet harian bersifat final. QC hanya dapat melihat riwayat hingga hari ini dan tidak dapat mengubah data sebelumnya.",
                    ),
            )
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
    val totalWeekend = pageDates.count { it.dayOfWeek.value >= 6 }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PagerNavButton(
                enabled = currentPage > 0,
                icon = Icons.Filled.ChevronLeft,
                label = "Sebelumnya",
                onClick = { onPageChange(currentPage - 1) },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Halaman ${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            PagerNavButton(
                enabled = currentPage < totalPages - 1,
                icon = Icons.Filled.ChevronRight,
                label = "Berikutnya",
                iconOnRight = true,
                onClick = { onPageChange(currentPage + 1) },
            )
        }
        Divider(color = NeutralBorder, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    DateButton(
                        date = date,
                        summary = summaryByDate[date],
                        selected = date == selectedDate,
                        isWeekend = isWeekend,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadge(
                text = "Terisi $totalFilled/$totalEnabled",
                backgroundColor = StatusSuccess.copy(alpha = 0.12f),
                contentColor = StatusSuccess,
            )
            AppBadge(
                text = "Libur $totalWeekend",
                backgroundColor = StatusWarning.copy(alpha = 0.12f),
                contentColor = StatusWarning,
            )
        }
    }
}

@Composable
private fun PagerNavButton(
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    iconOnRight: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (enabled) NeutralSurface else NeutralLight
    val content = if (enabled) NeutralText else NeutralTextMuted
    Surface(
        modifier = Modifier.height(36.dp).clickable(enabled = enabled) { onClick() },
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
            Text(text = label, style = MaterialTheme.typography.caption, color = content)
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
            isWeekend -> StatusWarning.copy(alpha = 0.08f)
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
                        color = (if (isWeekend) StatusWarning else NeutralText).copy(alpha = alpha),
                    )
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
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = StatusSuccess,
            modifier = Modifier.size(14.dp),
        )
        Text(text = "Sudah Input", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Icon(
            imageVector = Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = NeutralBorder,
            modifier = Modifier.size(14.dp),
        )
        Text(text = "Belum Input", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Spacer(modifier = Modifier.width(Spacing.sm))
        Icon(
            imageVector = Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = StatusWarning,
            modifier = Modifier.size(14.dp),
        )
        Text(text = "Akhir Pekan/Libur", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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

@Composable
private fun MonthlyInsightCard(
    month: YearMonth,
    totals: MonthlyTotals,
) {
    val dayRatio =
        if (totals.daysInMonth > 0) {
            totals.daysWithInput.toFloat() / totals.daysInMonth.toFloat()
        } else {
            0f
        }
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
            Text(text = "Ringkasan Bulan Ini", style = MaterialTheme.typography.subtitle1)
            Text(
                text = "${DateTimeFormats.formatMonth(month)} • Ringkasan checksheet harian.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = "Hari Terisi",
                    value = "${totals.daysWithInput}/${totals.daysInMonth}",
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = "Dokumen",
                    value = totals.totalDocs.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = "Total Periksa",
                    value = totals.totalCheck.toString(),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = "Rasio NG",
                    value = formatPercent(totals.ratio),
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "Cakupan hari input", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(NeutralLight, MaterialTheme.shapes.small),
                ) {
                    if (dayRatio > 0f) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(dayRatio)
                                    .height(8.dp)
                                    .background(StatusSuccess, MaterialTheme.shapes.small),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisFilterCard(
    lines: List<Line>,
    selectedLineId: Long?,
    onSelected: (Long?) -> Unit,
) {
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
            Text(text = "Filter Analisis NG", style = MaterialTheme.typography.subtitle1)
            Text(
                text = "Pilih line untuk analisis Pareto dan trend.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ToggleChip(
                    label = "Semua Line",
                    selected = selectedLineId == null,
                    onClick = { onSelected(null) },
                )
                lines.forEach { line ->
                    ToggleChip(
                        label = line.name,
                        selected = selectedLineId == line.id,
                        onClick = { onSelected(line.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
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
            Text(text = value, style = MaterialTheme.typography.subtitle1, color = NeutralText)
        }
    }
}

@Composable
private fun MonthlyParetoCard(
    month: YearMonth,
    defectSummaries: List<DefectSummary>,
    accentColor: androidx.compose.ui.graphics.Color,
    loading: Boolean,
) {
    val paretoItems =
        remember(defectSummaries) {
            if (defectSummaries.isEmpty()) {
                emptyList()
            } else {
                val expanded =
                    defectSummaries.flatMap { summary ->
                        List(summary.totalQuantity.coerceAtMost(200)) { summary.defectName }
                    }
                paretoCounts(expanded).take(6)
            }
        }

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
            Text(text = "Pareto NG Bulan Ini", style = MaterialTheme.typography.subtitle1)
            Text(
                text = "Kontribusi NG terbesar & kumulatif (${DateTimeFormats.formatMonth(month)}).",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            if (loading) {
                SkeletonBlock(width = 240.dp, height = 18.dp)
                SkeletonBlock(width = 320.dp, height = 8.dp)
                SkeletonBlock(width = 280.dp, height = 8.dp)
            } else if (paretoItems.isEmpty()) {
                Text(text = "Belum ada data NG bulan ini.", style = MaterialTheme.typography.body2)
            } else {
                val maxValue = paretoItems.maxOfOrNull { it.second } ?: 1
                val total = paretoItems.sumOf { it.second }.coerceAtLeast(1)
                var cumulative = 0
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ParetoPieChart(
                        items = paretoItems,
                        accentColor = accentColor,
                        modifier = Modifier.size(140.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        paretoItems.forEach { (label, count) ->
                            cumulative += count
                            val cumulativeRatio = cumulative.toDouble() / total.toDouble()
                            val ratio = count.toDouble() / total.toDouble()
                            ParetoRow(
                                label = label,
                                value = count,
                                maxValue = maxValue,
                                ratio = ratio,
                                cumulativeRatio = cumulativeRatio,
                                accentColor = accentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParetoPieChart(
    items: List<Pair<String, Int>>,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val total = items.sumOf { it.second }.coerceAtLeast(1)
    val palette =
        listOf(
            accentColor,
            accentColor.copy(alpha = 0.85f),
            StatusSuccess,
            StatusWarning,
            StatusInfo,
            NeutralBorder,
        )
    Canvas(modifier = modifier) {
        var startAngle = -90f
        items.forEachIndexed { index, item ->
            val sweep = (item.second.toFloat() / total.toFloat()) * 360f
            drawArc(
                color = palette[index % palette.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun ParetoRow(
    label: String,
    value: Int,
    maxValue: Int,
    ratio: Double,
    cumulativeRatio: Double,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.body2)
            Text(
                text = "${value} (${formatPercent(ratio)})",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(NeutralLight, MaterialTheme.shapes.small),
        ) {
            val ratio = value.toFloat() / maxValue.toFloat()
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(ratio)
                        .height(8.dp)
                        .background(accentColor, MaterialTheme.shapes.small),
            )
        }
        Text(
            text = "Kumulatif ${formatPercent(cumulativeRatio)}",
            style = MaterialTheme.typography.caption,
            color = NeutralTextMuted,
        )
    }
}

@Composable
private fun MonthlyTrendCard(
    month: YearMonth,
    dailySummaries: List<DailyChecksheetSummary>,
    lineColors: Map<Long, androidx.compose.ui.graphics.Color>,
    lines: List<Line>,
    selectedLineId: Long?,
) {
    val seriesByLine =
        remember(dailySummaries, month, lines) {
            val daysInMonth = month.lengthOfMonth()
            val grouped = dailySummaries.groupBy { it.lineId }
            lines.associate { line ->
                val totalsByDate =
                    grouped[line.id].orEmpty()
                        .groupBy { it.date }
                        .mapValues { (_, items) -> items.sumOf { it.totalDefect } }
                line.id to (1..daysInMonth).map { day ->
                    totalsByDate[month.atDay(day)] ?: 0
                }
            }
        }
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
            Text(text = "Trend NG Harian", style = MaterialTheme.typography.subtitle1)
            Text(
                text = "Pergerakan NG harian untuk monitoring stabilitas proses.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            TrendChart(
                seriesByLine = seriesByLine,
                lineColors = lineColors,
                lines = lines,
                selectedLineId = selectedLineId,
            )
            Text(
                text = "Sumbu Y: jumlah NG per hari.",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun TrendChart(
    seriesByLine: Map<Long, List<Int>>,
    lineColors: Map<Long, androidx.compose.ui.graphics.Color>,
    lines: List<Line>,
    selectedLineId: Long?,
) {
    val allValues = seriesByLine.values.flatten()
    val maxValue = allValues.maxOrNull()?.coerceAtLeast(1) ?: 1
    val activeLines =
        if (selectedLineId == null) {
            lines
        } else {
            lines.filter { it.id == selectedLineId }
        }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            activeLines.forEach { line ->
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
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(NeutralLight, MaterialTheme.shapes.small),
        ) {
            seriesByLine.forEach { (lineId, values) ->
                if (selectedLineId != null && lineId != selectedLineId) return@forEach
                if (values.isEmpty()) return@forEach
                val color = lineColors[lineId] ?: StatusInfo
                val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                val chartHeight = size.height - 12f
                val path = Path()
                values.forEachIndexed { index, value ->
                    val ratio = value.toFloat() / maxValue.toFloat()
                    val x = stepX * index
                    val y = chartHeight - (chartHeight * ratio) + 6f
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
                values.forEachIndexed { index, value ->
                    val ratio = value.toFloat() / maxValue.toFloat()
                    val x = stepX * index
                    val y = chartHeight - (chartHeight * ratio) + 6f
                    drawCircle(
                        color = color,
                        radius = 4f,
                        center = Offset(x, y),
                    )
                }
            }
        }
    }
}

private fun formatPercent(value: Double): String =
    if (value <= 0.0) {
        "-"
    } else {
        "${"%.1f".format(value * 100)}%"
    }
