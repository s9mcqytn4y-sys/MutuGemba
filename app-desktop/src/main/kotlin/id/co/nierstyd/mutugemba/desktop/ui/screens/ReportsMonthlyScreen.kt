
package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
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
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import org.jetbrains.skia.Image as SkiaImage

private val DocumentWidth = 1120.dp
private val DocumentMinHeight = 760.dp
private val HeaderRowHeight = 30.dp
private val SubHeaderRowHeight = 30.dp
private val BodyRowHeight = 36.dp
private val SubtotalRowHeight = 28.dp
private val TotalRowHeight = 30.dp
private val SketchColumnWidth = 72.dp
private val PartNumberColumnWidth = 180.dp
private val ProblemItemColumnWidth = 280.dp
private val DayColumnWidth = 30.dp
private val TotalColumnWidth = 84.dp
private const val PART_PAGE_SIZE = 8
private val SubtotalHighlight = BrandBlue.copy(alpha = 0.06f)

private sealed class MonthlyReportUiState {
    data object Loading : MonthlyReportUiState()

    data object Empty : MonthlyReportUiState()

    data class Loaded(
        val document: MonthlyReportDocument,
    ) : MonthlyReportUiState()
}

@Composable
fun ReportsMonthlyScreen(
    lines: List<Line>,
    dailySummaries: List<DailyChecksheetSummary>,
    loadMonthlyReportDocument: (Long, YearMonth) -> MonthlyReportDocument?,
    loadManualHolidays: () -> Set<LocalDate>,
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
    var state by remember { mutableStateOf<MonthlyReportUiState>(MonthlyReportUiState.Loading) }
    var manualHolidays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val summaryByDate =
        remember(dailySummaries, month, selectedLineId) {
            dailySummaries
                .filter { YearMonth.from(it.date) == month }
                .filter { it.lineId == selectedLineId }
                .associateBy { it.date }
        }

    LaunchedEffect(Unit) {
        manualHolidays = loadManualHolidays()
    }

    LaunchedEffect(lines) {
        if (selectedLineId == null && lines.isNotEmpty()) {
            selectedLineId = lines.first().id
        }
    }

    LaunchedEffect(selectedLineId, month) {
        val lineId = selectedLineId
        if (lineId == null) {
            state = MonthlyReportUiState.Empty
            return@LaunchedEffect
        }
        state = MonthlyReportUiState.Loading
        val document =
            withContext(Dispatchers.IO) {
                loadMonthlyReportDocument(lineId, month)
            }
        state = document?.let { MonthlyReportUiState.Loaded(it) } ?: MonthlyReportUiState.Empty
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = AppStrings.ReportsMonthly.Title,
            subtitle = AppStrings.ReportsMonthly.Subtitle,
        )
        Text(
            text = AppStrings.ReportsMonthly.Body,
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )

        MonthlyReportToolbar(
            month = month,
            todayMonth = todayMonth,
            lines = lines,
            selectedLineId = selectedLineId,
            onLineSelected = { selectedLineId = it },
            onMonthChange = { target -> month = if (target.isAfter(todayMonth)) todayMonth else target },
            onPrint = {
                val document = (state as? MonthlyReportUiState.Loaded)?.document ?: return@MonthlyReportToolbar
                scope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                exportMonthlyPdf(document, manualHolidays)
                            }
                        }
                    result
                        .onSuccess { path ->
                            val printed =
                                runCatching {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().print(path.toFile())
                                        true
                                    } else {
                                        false
                                    }
                                }.getOrElse { false }
                            feedback =
                                if (printed) {
                                    UserFeedback(FeedbackType.SUCCESS, AppStrings.ReportsMonthly.PrintSuccess)
                                } else {
                                    UserFeedback(FeedbackType.ERROR, AppStrings.ReportsMonthly.PrintFailed)
                                }
                        }.onFailure {
                            feedback = UserFeedback(FeedbackType.ERROR, AppStrings.ReportsMonthly.PrintFailed)
                        }
                }
            },
            onExport = {
                val document = (state as? MonthlyReportUiState.Loaded)?.document ?: return@MonthlyReportToolbar
                scope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching { exportMonthlyPdf(document, manualHolidays) }
                        }
                    feedback =
                        if (result.isSuccess) {
                            UserFeedback(FeedbackType.SUCCESS, AppStrings.ReportsMonthly.ExportSuccess)
                        } else {
                            UserFeedback(FeedbackType.ERROR, AppStrings.ReportsMonthly.ExportFailed)
                        }
                }
            },
        )
        AppTextField(
            spec =
                FieldSpec(
                    label = "Cari Part / Jenis NG",
                    placeholder = "Cari part number, UNIQ, atau item defect",
                    helperText = "Filter reaktif untuk membantu audit bulanan lebih cepat.",
                ),
            value = searchQuery,
            onValueChange = { searchQuery = it.take(80) },
            singleLine = true,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatusBanner(
                feedback =
                    UserFeedback(
                        FeedbackType.INFO,
                        "${AppStrings.ReportsMonthly.InfoSync} ${AppStrings.ReportsMonthly.InfoHoliday}",
                    ),
                dense = true,
            )
            feedback?.let {
                StatusBanner(
                    feedback = it,
                    onDismiss = { feedback = null },
                    dense = true,
                )
            }
        }

        when (val snapshot = state) {
            MonthlyReportUiState.Loading ->
                MonthlyReportSkeleton()
            MonthlyReportUiState.Empty ->
                MonthlyReportEmpty()
            is MonthlyReportUiState.Loaded ->
                MonthlyReportDocumentCard(
                    document = snapshot.document,
                    manualHolidays = manualHolidays,
                    summaryByDate = summaryByDate,
                    searchQuery = searchQuery,
                )
        }
    }
}

private fun exportMonthlyPdf(
    document: MonthlyReportDocument,
    manualHolidays: Set<LocalDate>,
): java.nio.file.Path {
    val exportDir = AppDataPaths.exportsDir()
    Files.createDirectories(exportDir)
    val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
    val filename = "MonthlyReport-${document.header.lineCode.name}-${document.header.month}-$timestamp.pdf"
    val outputPath = exportDir.resolve(filename)
    val meta =
        MonthlyReportPrintMeta(
            companyName = AppStrings.App.CompanyName,
            departmentName = AppStrings.App.DepartmentName,
            customerName = AppStrings.App.CustomerName,
            documentTitle =
                if (document.header.lineCode.name == "SEWING") {
                    AppStrings.ReportsMonthly.DocumentTitleSewing
                } else {
                    AppStrings.ReportsMonthly.DocumentTitlePress
                },
        )
    return MonthlyReportPdfExporter.export(document, meta, outputPath, manualHolidays)
}

@Composable
private fun MonthlyReportToolbar(
    month: YearMonth,
    todayMonth: YearMonth,
    lines: List<Line>,
    selectedLineId: Long?,
    onLineSelected: (Long) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onPrint: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    Text(text = DateTimeFormats.formatMonth(month), style = MaterialTheme.typography.subtitle1)
                }
                AppBadge(
                    text = AppStrings.App.ReadOnly,
                    backgroundColor = NeutralLight,
                    contentColor = NeutralText,
                )
            }
            MonthSwitcher(
                month = month,
                todayMonth = todayMonth,
                onMonthChange = onMonthChange,
            )
            if (lines.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    lines.forEach { line ->
                        LineChip(
                            label = line.name,
                            selected = selectedLineId == line.id,
                            onClick = { onLineSelected(line.id) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(
                    text = AppStrings.Actions.PrintDocument,
                    onClick = onPrint,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = AppStrings.Actions.ExportPdf,
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            PagerNavButton(
                enabled = true,
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
private fun PagerNavButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconOnRight: Boolean = false,
    onClick: () -> Unit,
) {
    val borderColor = if (enabled) NeutralBorder else NeutralBorder.copy(alpha = 0.4f)
    val contentColor = if (enabled) NeutralText else NeutralTextMuted
    Surface(
        modifier =
            modifier.then(
                if (enabled) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                },
            ),
        shape = MaterialTheme.shapes.small,
        color = NeutralSurface,
        border = BorderStroke(1.dp, borderColor),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!iconOnRight) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(text = label, style = MaterialTheme.typography.caption, color = contentColor)
            if (iconOnRight) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun LineChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) BrandBlue else NeutralSurface
    val contentColor = if (selected) NeutralSurface else NeutralText
    Surface(
        modifier =
            Modifier
                .widthIn(min = 88.dp)
                .clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        color = background,
        elevation = 0.dp,
        border = BorderStroke(1.dp, NeutralBorder),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
        )
    }
}

@Composable
private fun MonthlyReportDocumentCard(
    document: MonthlyReportDocument,
    manualHolidays: Set<LocalDate>,
    summaryByDate: Map<LocalDate, DailyChecksheetSummary>,
    searchQuery: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NeutralSurface, MaterialTheme.shapes.medium)
                        .border(1.dp, NeutralBorder, MaterialTheme.shapes.medium)
                        .padding(Spacing.md),
            ) {
                Column(
                    modifier = Modifier.widthIn(max = DocumentWidth).heightIn(min = DocumentMinHeight),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MonthlyReportHeader(document = document)
                    MonthlyReportMeta(document = document)
                    MonthlyReportTable(
                        document = document,
                        manualHolidays = manualHolidays,
                        summaryByDate = summaryByDate,
                        searchQuery = searchQuery,
                    )
                    MonthlyReportLegend()
                    MonthlyReportSignature()
                }
            }
        }
    }
}

@Composable
private fun MonthlyReportHeader(document: MonthlyReportDocument) {
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = AppStrings.App.CompanyName, style = MaterialTheme.typography.subtitle2)
                Text(
                    text = AppStrings.App.DepartmentName,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
        }
        Column(modifier = Modifier.weight(1.6f), horizontalAlignment = Alignment.CenterHorizontally) {
            val title =
                if (document.header.lineCode.name == "SEWING") {
                    AppStrings.ReportsMonthly.DocumentTitleSewing
                } else {
                    AppStrings.ReportsMonthly.DocumentTitlePress
                }
            Text(
                text = title,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                text = AppStrings.ReportsMonthly.DocumentNoLabel,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Text(text = document.header.documentNumber, style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
private fun MonthlyReportMeta(document: MonthlyReportDocument) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        MetaItem(
            label = AppStrings.ReportsMonthly.DocumentMonthLabel,
            value = DateTimeFormats.formatMonth(document.header.month),
            modifier = Modifier.weight(1f),
        )
        MetaItem(
            label = AppStrings.ReportsMonthly.DocumentCustomerLabel,
            value = AppStrings.App.CustomerName,
            modifier = Modifier.weight(1.1f),
        )
        MetaItem(
            label = AppStrings.ReportsMonthly.DocumentPicLabel,
            value = document.header.picName,
            modifier = Modifier.weight(1f),
        )
        MetaItem(
            label = AppStrings.ReportsMonthly.DocumentLineLabel,
            value = document.header.lineName,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetaItem(
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
@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun MonthlyReportTable(
    document: MonthlyReportDocument,
    manualHolidays: Set<LocalDate>,
    summaryByDate: Map<LocalDate, DailyChecksheetSummary>,
    searchQuery: String,
) {
    val filteredRows =
        document.rows.filter { row ->
            val keyword = searchQuery.trim().lowercase()
            if (keyword.isBlank()) {
                true
            } else {
                row.partNumber.lowercase().contains(keyword) ||
                    row.uniqCode.lowercase().contains(keyword) ||
                    row.problemItems.any { it.lowercase().contains(keyword) }
            }
        }
    if (filteredRows.isEmpty()) {
        MonthlyReportEmpty()
        return
    }

    val scrollState = rememberScrollState()
    val days = document.days
    var dayMarkerFilter by remember(days, summaryByDate, manualHolidays) { mutableStateOf(DayMarkerFilter.ALL) }
    var focusedDay by remember(days, summaryByDate, manualHolidays) { mutableStateOf<LocalDate?>(null) }
    val dayStyles =
        remember(days, summaryByDate, manualHolidays) {
            days.associateWith { day ->
                val summary = summaryByDate[day]
                val hasInput = summary != null
                val isHoliday = isHoliday(day, manualHolidays)
                DayCellStyle.from(hasInput = hasInput, isHoliday = isHoliday)
            }
        }
    val visibleDayEntries =
        remember(days, dayStyles, dayMarkerFilter, focusedDay) {
            val scopedDays =
                if (focusedDay != null) {
                    days.filter { it == focusedDay }
                } else {
                    when (dayMarkerFilter) {
                        DayMarkerFilter.ALL -> days
                        DayMarkerFilter.INPUT -> days.filter { dayStyles.getValue(it).hasInput }
                        DayMarkerFilter.HOLIDAY -> days.filter { dayStyles.getValue(it).isHoliday }
                        DayMarkerFilter.EMPTY ->
                            days.filter { style ->
                                !dayStyles.getValue(style).hasInput &&
                                    !dayStyles.getValue(style).isHoliday
                            }
                    }
                }
            days.mapIndexedNotNull { index, day ->
                if (day in scopedDays) index to day else null
            }
        }
    if (focusedDay != null && visibleDayEntries.none { it.second == focusedDay }) {
        focusedDay = null
    }
    val filteredDayTotals =
        visibleDayEntries.map { (index, _) ->
            filteredRows.sumOf { it.dayValues.getOrNull(index) ?: 0 }
        }
    val scopedDayView = dayMarkerFilter != DayMarkerFilter.ALL || focusedDay != null
    val filteredGrandTotal =
        if (scopedDayView) {
            filteredDayTotals.sum()
        } else {
            filteredRows.sumOf { it.totalDefect }
        }
    val groupedRows =
        filteredRows
            .groupBy { it.partId }
            .toList()
            .sortedBy { (_, rows) -> rows.firstOrNull()?.partNumber.orEmpty() }
    var currentPage by remember(searchQuery, groupedRows.size) { mutableStateOf(0) }
    var compactMode by remember(searchQuery, groupedRows.size) { mutableStateOf(true) }
    val pageCount =
        remember(groupedRows.size, compactMode) {
            if (!compactMode || groupedRows.isEmpty()) {
                1
            } else {
                ((groupedRows.size - 1) / PART_PAGE_SIZE) + 1
            }
        }
    LaunchedEffect(pageCount) {
        if (currentPage >= pageCount) {
            currentPage = (pageCount - 1).coerceAtLeast(0)
        }
    }
    val visibleGroups =
        remember(groupedRows, compactMode, currentPage) {
            if (!compactMode) {
                groupedRows
            } else {
                groupedRows
                    .drop(currentPage * PART_PAGE_SIZE)
                    .take(PART_PAGE_SIZE)
            }
        }
    val visibleRangeStart = if (groupedRows.isEmpty()) 0 else (currentPage * PART_PAGE_SIZE) + 1
    val visibleRangeEnd =
        if (groupedRows.isEmpty()) {
            0
        } else {
            (visibleRangeStart + visibleGroups.size - 1).coerceAtMost(groupedRows.size)
        }
    val sketchCache = remember { mutableStateMapOf<String, androidx.compose.ui.graphics.ImageBitmap?>() }
    LaunchedEffect(visibleGroups) {
        val sketchPaths =
            visibleGroups
                .mapNotNull { (_, rows) -> rows.firstOrNull()?.sketchPath }
                .filter { it.isNotBlank() }
                .distinct()
        sketchPaths.forEach { path ->
            if (!sketchCache.containsKey(path)) {
                sketchCache[path] = withContext(Dispatchers.IO) { loadSketchBitmap(path) }
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val summaryLabel =
                if (groupedRows.isEmpty()) {
                    "Part 0"
                } else {
                    "Part $visibleRangeStart-$visibleRangeEnd dari ${groupedRows.size}"
                }
            Text(
                text = summaryLabel,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecondaryButton(
                    text = if (compactMode) "Tampilkan Semua Part" else "Mode Ringkas Part",
                    onClick = { compactMode = !compactMode },
                )
                if (compactMode) {
                    PagerNavButton(
                        enabled = currentPage > 0,
                        icon = AppIcons.ChevronLeft,
                        label = "Part Sebelumnya",
                        onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                    )
                    Text(
                        text = "Halaman ${currentPage + 1}/$pageCount",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                    PagerNavButton(
                        enabled = currentPage + 1 < pageCount,
                        icon = AppIcons.ChevronRight,
                        label = "Part Berikutnya",
                        iconOnRight = true,
                        onClick = { currentPage = (currentPage + 1).coerceAtMost(pageCount - 1) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LineChip(label = "Semua Tanggal", selected = dayMarkerFilter == DayMarkerFilter.ALL, onClick = {
                dayMarkerFilter = DayMarkerFilter.ALL
                focusedDay = null
            })
            LineChip(label = "Terisi", selected = dayMarkerFilter == DayMarkerFilter.INPUT, onClick = {
                dayMarkerFilter = DayMarkerFilter.INPUT
                focusedDay = null
            })
            LineChip(label = "Libur", selected = dayMarkerFilter == DayMarkerFilter.HOLIDAY, onClick = {
                dayMarkerFilter = DayMarkerFilter.HOLIDAY
                focusedDay = null
            })
            LineChip(label = "Belum Input", selected = dayMarkerFilter == DayMarkerFilter.EMPTY, onClick = {
                dayMarkerFilter = DayMarkerFilter.EMPTY
                focusedDay = null
            })
            if (focusedDay != null) {
                SecondaryButton(
                    text = "Buka Semua Kolom",
                    onClick = { focusedDay = null },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.width(SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth)) {
                Row {
                    TableHeaderCell(
                        text = AppStrings.ReportsMonthly.TableSketch,
                        width = SketchColumnWidth,
                        height = HeaderRowHeight + SubHeaderRowHeight,
                    )
                    TableHeaderCell(
                        text = AppStrings.ReportsMonthly.TablePartNumber,
                        width = PartNumberColumnWidth,
                        height = HeaderRowHeight + SubHeaderRowHeight,
                    )
                    TableHeaderCell(
                        text = AppStrings.ReportsMonthly.TableProblemItem,
                        width = ProblemItemColumnWidth,
                        height = HeaderRowHeight + SubHeaderRowHeight,
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
            ) {
                Column {
                    Row {
                        TableHeaderCell(
                            text = AppStrings.ReportsMonthly.TableDates,
                            width = DayColumnWidth * visibleDayEntries.size,
                            height = HeaderRowHeight,
                        )
                        TableHeaderCell(
                            text = AppStrings.ReportsMonthly.TableTotals,
                            width = TotalColumnWidth,
                            height = HeaderRowHeight,
                        )
                    }
                    Row {
                        visibleDayEntries.forEach { (_, day) ->
                            DayHeaderCell(
                                day = day,
                                style = dayStyles.getValue(day),
                                selected = focusedDay == day,
                                onClick = {
                                    focusedDay = if (focusedDay == day) null else day
                                },
                            )
                        }
                        TableHeaderCell(
                            text = AppStrings.ReportsMonthly.TableTotalNg,
                            width = TotalColumnWidth,
                            height = SubHeaderRowHeight,
                        )
                    }
                }
            }
        }

        visibleGroups.forEachIndexed { groupIndex, (_, rowsForPart) ->
            val partSample = rowsForPart.first()
            val rowBackground = if (groupIndex % 2 == 0) NeutralSurface else NeutralLight
            val partDayTotals =
                visibleDayEntries.map { (dayIndex, _) ->
                    rowsForPart.sumOf { row -> row.dayValues.getOrElse(dayIndex) { 0 } }
                }
            val partTotal =
                if (scopedDayView) {
                    partDayTotals.sum()
                } else {
                    rowsForPart.sumOf { it.totalDefect }
                }
            rowsForPart.forEachIndexed { rowIndex, row ->
                val itemLabel = normalizeProblemItems(row.problemItems).firstOrNull().orEmpty()
                val itemDisplay = itemLabel.ifBlank { "Tanpa Keterangan" }
                val rowTotal =
                    if (scopedDayView) {
                        visibleDayEntries.sumOf { (dayIndex, _) ->
                            row.dayValues.getOrElse(dayIndex) { 0 }
                        }
                    } else {
                        row.totalDefect
                    }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.width(SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth)) {
                        if (rowIndex == 0) {
                            SketchCell(
                                bitmap = partSample.sketchPath?.let { sketchCache[it] },
                                width = SketchColumnWidth,
                                height = BodyRowHeight,
                                backgroundColor = rowBackground,
                            )
                            TableBodyCell(
                                text = formatPartNumber(partSample.partNumber, partSample.uniqCode),
                                width = PartNumberColumnWidth,
                                height = BodyRowHeight,
                                backgroundColor = rowBackground,
                                maxLines = 3,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            TableBodyCell(
                                text = "",
                                width = SketchColumnWidth,
                                height = BodyRowHeight,
                                backgroundColor = rowBackground,
                            )
                            TableBodyCell(
                                text = "",
                                width = PartNumberColumnWidth,
                                height = BodyRowHeight,
                                backgroundColor = rowBackground,
                            )
                        }
                        TableBodyCell(
                            text = itemDisplay,
                            width = ProblemItemColumnWidth,
                            height = BodyRowHeight,
                            backgroundColor = rowBackground,
                            maxLines = 2,
                        )
                    }
                    Row(modifier = Modifier.horizontalScroll(scrollState)) {
                        visibleDayEntries.forEach { (dayIndex, day) ->
                            val style = dayStyles.getValue(day)
                            val value = row.dayValues.getOrElse(dayIndex) { 0 }
                            TableBodyCell(
                                text = value.toString(),
                                width = DayColumnWidth,
                                height = BodyRowHeight,
                                backgroundColor = style.bodyBackground,
                                alignCenter = true,
                                textColor = style.bodyTextColor,
                            )
                        }
                        TableBodyCell(
                            text = rowTotal.toString(),
                            width = TotalColumnWidth,
                            height = BodyRowHeight,
                            backgroundColor = rowBackground,
                            alignCenter = true,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.width(SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth)) {
                    TableSubtotalCell(
                        text = "",
                        width = SketchColumnWidth + PartNumberColumnWidth,
                        height = SubtotalRowHeight,
                        backgroundColor = SubtotalHighlight,
                    )
                    TableSubtotalCell(
                        text = AppStrings.ReportsMonthly.TableSubtotal,
                        width = ProblemItemColumnWidth,
                        height = SubtotalRowHeight,
                        backgroundColor = SubtotalHighlight,
                    )
                }
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    partDayTotals.forEachIndexed { dayIndex, value ->
                        val style = dayStyles.getValue(visibleDayEntries[dayIndex].second)
                        TableSubtotalCell(
                            text = value.toString(),
                            width = DayColumnWidth,
                            height = SubtotalRowHeight,
                            alignCenter = true,
                            backgroundColor = style.subtotalBackground,
                            textColor = style.bodyTextColor,
                        )
                    }
                    TableSubtotalCell(
                        text = partTotal.toString(),
                        width = TotalColumnWidth,
                        height = SubtotalRowHeight,
                        alignCenter = true,
                        backgroundColor = SubtotalHighlight,
                    )
                }
            }
            PartDivider()
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.width(SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth)) {
                TableFooterCell(
                    text = AppStrings.ReportsMonthly.TableGrandTotal,
                    width = SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth,
                    height = TotalRowHeight,
                )
            }
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                filteredDayTotals.forEachIndexed { index, value ->
                    val style = dayStyles.getValue(visibleDayEntries[index].second)
                    TableFooterCell(
                        text = value.toString(),
                        width = DayColumnWidth,
                        height = TotalRowHeight,
                        alignCenter = true,
                        backgroundColor = style.footerBackground,
                        textColor = style.bodyTextColor,
                    )
                }
                TableFooterCell(
                    text = filteredGrandTotal.toString(),
                    width = TotalColumnWidth,
                    height = TotalRowHeight,
                    alignCenter = true,
                    backgroundColor = BrandBlue.copy(alpha = 0.12f),
                    textColor = BrandBlue,
                )
            }
        }
    }
}

@Composable
private fun DayHeaderCell(
    day: LocalDate,
    style: DayCellStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(DayColumnWidth)
                .height(SubHeaderRowHeight)
                .border(1.dp, NeutralBorder)
                .background(if (selected) BrandBlue.copy(alpha = 0.18f) else style.headerBackground)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = style.headerTextColor,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (style.hasInput) {
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(style.dotColor, shape = CircleShape),
                    )
                }
                if (style.isHoliday) {
                    Box(
                        modifier =
                            Modifier
                                .width(8.dp)
                                .height(4.dp)
                                .background(StatusWarning, shape = MaterialTheme.shapes.small),
                    )
                }
                if (!style.hasInput && !style.isHoliday) {
                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .background(NeutralTextMuted.copy(alpha = 0.35f), shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(
    text: String,
    width: Dp,
    height: Dp,
    backgroundColor: Color = headerBackground(),
    textColor: Color = NeutralText,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun RowScope.TableBodyCell(
    text: String,
    width: Dp,
    height: Dp,
    backgroundColor: Color = NeutralSurface,
    alignCenter: Boolean = false,
    maxLines: Int = 1,
    textColor: Color = NeutralText,
    fontWeight: FontWeight? = null,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .heightIn(min = height)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2.copy(fontWeight = fontWeight ?: FontWeight.Normal),
            color = textColor,
            maxLines = maxLines,
        )
    }
}

@Composable
private fun RowScope.TableSubtotalCell(
    text: String,
    width: Dp,
    height: Dp,
    alignCenter: Boolean = false,
    backgroundColor: Color = NeutralLight,
    textColor: Color = NeutralText,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.caption, color = textColor)
    }
}

@Composable
private fun RowScope.TableFooterCell(
    text: String,
    width: Dp,
    height: Dp,
    alignCenter: Boolean = false,
    backgroundColor: Color = NeutralLight,
    textColor: Color = NeutralText,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.body2, color = textColor)
    }
}

private data class DayCellStyle(
    val hasInput: Boolean,
    val isHoliday: Boolean,
    val headerBackground: Color,
    val headerTextColor: Color,
    val bodyBackground: Color,
    val footerBackground: Color,
    val subtotalBackground: Color,
    val bodyTextColor: Color,
    val dotColor: Color,
) {
    companion object {
        fun from(
            hasInput: Boolean,
            isHoliday: Boolean,
        ): DayCellStyle {
            val kind =
                when {
                    hasInput -> DayCellKind.INPUT
                    isHoliday -> DayCellKind.HOLIDAY
                    else -> DayCellKind.EMPTY
                }
            val base = dayTextColor(kind)
            return DayCellStyle(
                hasInput = hasInput,
                isHoliday = isHoliday,
                headerBackground = dayHeaderBackground(kind),
                headerTextColor = base,
                bodyBackground = dayBodyBackground(kind),
                footerBackground = dayFooterBackground(kind),
                subtotalBackground = daySubtotalBackground(kind),
                bodyTextColor = if (kind == DayCellKind.EMPTY) NeutralText else base,
                dotColor = if (kind == DayCellKind.INPUT) StatusSuccess else base,
            )
        }
    }
}

private enum class DayCellKind {
    INPUT,
    HOLIDAY,
    EMPTY,
}

private enum class DayMarkerFilter {
    ALL,
    INPUT,
    HOLIDAY,
    EMPTY,
}

private fun dayTextColor(kind: DayCellKind): Color =
    when (kind) {
        DayCellKind.INPUT -> StatusSuccess
        DayCellKind.HOLIDAY -> StatusWarning
        DayCellKind.EMPTY -> NeutralTextMuted
    }

private fun dayHeaderBackground(kind: DayCellKind): Color =
    when (kind) {
        DayCellKind.INPUT -> StatusSuccess.copy(alpha = 0.14f)
        DayCellKind.HOLIDAY -> StatusWarning.copy(alpha = 0.14f)
        DayCellKind.EMPTY -> NeutralLight
    }

private fun dayBodyBackground(kind: DayCellKind): Color =
    when (kind) {
        DayCellKind.INPUT -> StatusSuccess.copy(alpha = 0.06f)
        DayCellKind.HOLIDAY -> StatusWarning.copy(alpha = 0.08f)
        DayCellKind.EMPTY -> NeutralSurface
    }

private fun daySubtotalBackground(kind: DayCellKind): Color =
    when (kind) {
        DayCellKind.INPUT -> StatusSuccess.copy(alpha = 0.09f)
        DayCellKind.HOLIDAY -> StatusWarning.copy(alpha = 0.1f)
        DayCellKind.EMPTY -> NeutralLight
    }

private fun dayFooterBackground(kind: DayCellKind): Color =
    when (kind) {
        DayCellKind.INPUT -> StatusSuccess.copy(alpha = 0.1f)
        DayCellKind.HOLIDAY -> StatusWarning.copy(alpha = 0.12f)
        DayCellKind.EMPTY -> NeutralLight
    }

@Composable
private fun SketchCell(
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    width: Dp,
    height: Dp,
    backgroundColor: Color,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height - 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = AppStrings.Inspection.PartImageDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(height - 8.dp),
                )
            }
        } else {
            Text(
                text = AppStrings.ReportsMonthly.TableSketch,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PartDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NeutralBorder.copy(alpha = 0.9f)),
    )
}

@Composable
private fun MonthlyReportLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendChip(label = "Terisi", color = StatusSuccess)
            LegendChip(label = "Libur/Weekend", color = StatusWarning)
            LegendChip(label = "Belum Input", color = NeutralTextMuted)
            LegendChip(label = "Grand Total", color = BrandBlue)
            LegendChip(label = "Sub-total", color = BrandBlue.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun MonthlyReportSignature() {
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
private fun LegendChip(
    label: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = NeutralTextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.SignatureCell(label: String) {
    Surface(
        modifier = Modifier.weight(1f),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, NeutralBorder),
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
private fun LogoMark() {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        color = NeutralLight,
        border = BorderStroke(1.dp, NeutralBorder),
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
private fun MonthlyReportEmpty() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = AppStrings.ReportsMonthly.EmptyTitle, style = MaterialTheme.typography.subtitle1)
            Text(
                text = AppStrings.ReportsMonthly.EmptySubtitle,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun MonthlyReportSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(220.dp))
        }
    }
}

@Composable
private fun headerBackground(): Color = MaterialTheme.colors.primary.copy(alpha = 0.08f)

private fun isHoliday(
    date: LocalDate,
    manualHolidays: Set<LocalDate>,
): Boolean {
    val isWeekend = date.dayOfWeek.value >= 6
    return isWeekend || manualHolidays.contains(date)
}

private fun formatPartNumber(
    partNumber: String,
    uniqCode: String,
): String = "$partNumber ($uniqCode)"

private fun normalizeProblemItems(items: List<String>): List<String> =
    items
        .flatMap { DefectNameSanitizer.expandProblemItems(it) }
        .ifEmpty { items.map(DefectNameSanitizer::normalizeDisplay) }
        .filter(DefectNameSanitizer::isMeaningfulItem)
        .distinct()
        .map(DefectNameSanitizer::normalizeDisplay)

private fun loadSketchBitmap(path: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (path.isNullOrBlank()) return null
    val candidate = resolveSketchFile(path) ?: return null
    return runCatching {
        SkiaImage.makeFromEncoded(candidate.readBytes()).toComposeImageBitmap()
    }.getOrNull()
}

private fun resolveSketchFile(path: String): File? {
    val direct = File(path)
    if (direct.exists()) return direct
    if (direct.isAbsolute) return null
    val attachmentCandidate = AppDataPaths.attachmentsDir().resolve(path).toFile()
    if (attachmentCandidate.exists()) return attachmentCandidate
    val dataCandidate = AppDataPaths.dataDir().resolve(path).toFile()
    if (dataCandidate.exists()) return dataCandidate
    return null
}
