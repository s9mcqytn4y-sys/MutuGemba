
package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
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
import org.jetbrains.skia.Image as SkiaImage

private val DocumentWidth = 1140.dp
private val DocumentMinHeight = 760.dp
private val HeaderRowHeight = 34.dp
private val SubHeaderRowHeight = 34.dp
private val PartSectionHeaderHeight = 30.dp
private val BodyRowHeight = 46.dp
private val SubtotalRowHeight = 32.dp
private val TotalRowHeight = 34.dp
private val SketchColumnWidth = 132.dp
private val PartNumberColumnWidth = 148.dp
private val ProblemItemColumnWidth = 246.dp
private val DayColumnWidth = 40.dp
private val TotalColumnWidth = 88.dp
private val SectionDividerWidth = 2.dp
private val SubtotalHighlight = BrandBlue.copy(alpha = 0.06f)
private const val PREVIEW_PART_LIMIT = 8
private const val FULL_DOCUMENT_PAGE_PART_LIMIT = 14

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
    var lineSelectionLockedByUser by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<MonthlyReportUiState>(MonthlyReportUiState.Loading) }
    var manualHolidays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var documentMode by remember { mutableStateOf(MonthlyDocumentMode.PREVIEW) }
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

    LaunchedEffect(lines, dailySummaries, month, lineSelectionLockedByUser) {
        if (lineSelectionLockedByUser || lines.isEmpty()) return@LaunchedEffect
        val preferredLineId =
            dailySummaries
                .asSequence()
                .filter { YearMonth.from(it.date) == month }
                .groupBy { it.lineId }
                .maxByOrNull { (_, rows) -> rows.size }
                ?.key
        if (preferredLineId != null && preferredLineId != selectedLineId) {
            selectedLineId = preferredLineId
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
            onLineSelected = {
                lineSelectionLockedByUser = true
                selectedLineId = it
            },
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
                    placeholder = "Cari part number, UNIQ, atau jenis NG",
                    helperText = "Filter reaktif untuk membantu audit bulanan lebih cepat.",
                ),
            value = searchQuery,
            onValueChange = { searchQuery = it.take(80) },
            singleLine = true,
        )
        MonthlyDocumentModeSwitch(
            mode = documentMode,
            onModeChange = { documentMode = it },
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
                    documentMode = documentMode,
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
    val filename = "MonthlyReport-${document.header.lineCode.name}-${document.header.month}.pdf"
    val outputPath = exportDir.resolve(filename)
    // Avoid writing over a file that may still be open by the PDF viewer.
    Files.deleteIfExists(outputPath)
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
    documentMode: MonthlyDocumentMode,
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
                        documentMode = documentMode,
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
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun MonthlyReportTable(
    document: MonthlyReportDocument,
    manualHolidays: Set<LocalDate>,
    summaryByDate: Map<LocalDate, DailyChecksheetSummary>,
    searchQuery: String,
    documentMode: MonthlyDocumentMode,
) {
    val keyword = searchQuery.trim().lowercase()
    val normalizedKeyword = DefectNameSanitizer.canonicalKey(searchQuery).lowercase()
    val filteredRows =
        remember(document.rows, keyword, normalizedKeyword) {
            document.rows.filter { row ->
                if (keyword.isBlank()) {
                    true
                } else {
                    row.partNumber.lowercase().contains(keyword) ||
                        row.uniqCode.lowercase().contains(keyword) ||
                        row.problemItems.any {
                            val normalizedItem = DefectNameSanitizer.canonicalKey(it).lowercase()
                            it.lowercase().contains(keyword) || normalizedItem.contains(normalizedKeyword)
                        }
                }
            }
        }
    if (filteredRows.isEmpty()) {
        MonthlyReportEmpty()
        return
    }
    val groupedRows =
        remember(filteredRows) {
            filteredRows
                .groupBy { it.partId }
                .toList()
                .sortedBy { (_, rows) -> rows.firstOrNull()?.partNumber ?: "" }
                .map { (_, rows) ->
                    rows.sortedBy { row -> row.problemItems.firstOrNull().orEmpty() }
                }
        }
    var currentPage by remember(documentMode, groupedRows.size) { mutableStateOf(0) }
    val pageCount =
        remember(groupedRows, documentMode) {
            if (groupedRows.isEmpty()) {
                1
            } else if (documentMode == MonthlyDocumentMode.PREVIEW) {
                1
            } else {
                ((groupedRows.size - 1) / FULL_DOCUMENT_PAGE_PART_LIMIT) + 1
            }
        }
    LaunchedEffect(pageCount) {
        if (currentPage >= pageCount) {
            currentPage = (pageCount - 1).coerceAtLeast(0)
        }
    }
    val visibleGroupedRows =
        remember(groupedRows, documentMode, currentPage) {
            if (documentMode == MonthlyDocumentMode.PREVIEW) {
                groupedRows.take(PREVIEW_PART_LIMIT)
            } else {
                groupedRows
                    .drop(currentPage * FULL_DOCUMENT_PAGE_PART_LIMIT)
                    .take(FULL_DOCUMENT_PAGE_PART_LIMIT)
            }
        }
    val visibleRows = remember(visibleGroupedRows) { visibleGroupedRows.flatten() }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val sketchCache = remember { mutableStateMapOf<String, androidx.compose.ui.graphics.ImageBitmap?>() }
    val days = document.days
    val dayStyles =
        remember(days, summaryByDate, manualHolidays) {
            days.associateWith { day ->
                val summary = summaryByDate[day]
                val hasInput = summary != null
                val isHoliday = isHoliday(day, manualHolidays)
                DayCellStyle.from(hasInput = hasInput, isHoliday = isHoliday)
            }
        }
    LaunchedEffect(visibleGroupedRows) {
        val paths =
            visibleGroupedRows
                .mapNotNull { rows -> rows.firstOrNull()?.sketchPath?.takeIf { it.isNotBlank() } }
                .distinct()
        val missingPaths = paths.filterNot { sketchCache.containsKey(it) }
        if (missingPaths.isEmpty()) return@LaunchedEffect
        val loaded =
            withContext(Dispatchers.IO) {
                missingPaths.associateWith { sketchPath -> loadSketchBitmap(sketchPath) }
            }
        sketchCache.putAll(loaded)
    }
    val filteredDayTotals =
        remember(visibleRows, days) {
            days.mapIndexed { index, _ ->
                visibleRows.sumOf { it.dayValues.getOrNull(index) ?: 0 }
            }
        }
    val filteredGrandTotal = remember(visibleRows) { visibleRows.sumOf { it.totalDefect } }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (days.size > 7) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppBadge(
                        text =
                            if (documentMode == MonthlyDocumentMode.PREVIEW) {
                                "Pratinjau ${visibleGroupedRows.size}/${groupedRows.size} part"
                            } else {
                                "Dokumen penuh ${visibleGroupedRows.size} part/halaman"
                            },
                        backgroundColor = NeutralLight,
                        contentColor = NeutralTextMuted,
                    )
                    if (documentMode == MonthlyDocumentMode.FULL && pageCount > 1) {
                        AppBadge(
                            text = "Halaman ${currentPage + 1}/$pageCount",
                            backgroundColor = BrandBlue.copy(alpha = 0.1f),
                            contentColor = BrandBlue,
                        )
                    }
                    PagerNavButton(
                        enabled = scrollState.value > 0,
                        icon = AppIcons.ChevronLeft,
                        label = "Geser kiri",
                        onClick = {
                            scope.launch {
                                val next = (scrollState.value - 220).coerceAtLeast(0)
                                scrollState.animateScrollTo(next)
                            }
                        },
                    )
                    PagerNavButton(
                        enabled = scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue,
                        icon = AppIcons.ChevronRight,
                        label = "Geser kanan",
                        iconOnRight = true,
                        onClick = {
                            scope.launch {
                                val next = (scrollState.value + 220).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(next)
                            }
                        },
                    )
                    if (documentMode == MonthlyDocumentMode.FULL && pageCount > 1) {
                        PagerNavButton(
                            enabled = currentPage > 0,
                            icon = AppIcons.ChevronLeft,
                            label = "Halaman sebelumnya",
                            onClick = {
                                currentPage = (currentPage - 1).coerceAtLeast(0)
                                scope.launch { scrollState.scrollTo(0) }
                            },
                        )
                        PagerNavButton(
                            enabled = currentPage + 1 < pageCount,
                            icon = AppIcons.ChevronRight,
                            label = "Halaman berikutnya",
                            iconOnRight = true,
                            onClick = {
                                currentPage = (currentPage + 1).coerceAtMost(pageCount - 1)
                                scope.launch { scrollState.scrollTo(0) }
                            },
                        )
                    }
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val leftColumnsWidth = SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth
            val dayViewportWidth =
                (maxWidth - leftColumnsWidth - TotalColumnWidth - SectionDividerWidth).coerceAtLeast(180.dp)
            val rightSectionWidth = dayViewportWidth + TotalColumnWidth

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.width(leftColumnsWidth)) {
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
                VerticalSectionDivider(height = HeaderRowHeight + SubHeaderRowHeight)
                Column(modifier = Modifier.width(rightSectionWidth)) {
                    Row {
                        Box(modifier = Modifier.width(dayViewportWidth).horizontalScroll(scrollState)) {
                            Row {
                                TableHeaderCell(
                                    text = AppStrings.ReportsMonthly.TableDates,
                                    width = DayColumnWidth * days.size,
                                    height = HeaderRowHeight,
                                )
                            }
                        }
                        TableHeaderCell(
                            text = AppStrings.ReportsMonthly.TableTotalNg,
                            width = TotalColumnWidth,
                            height = HeaderRowHeight + SubHeaderRowHeight,
                        )
                    }
                    Row {
                        Box(modifier = Modifier.width(dayViewportWidth).horizontalScroll(scrollState)) {
                            Row {
                                days.forEach { day ->
                                    DayHeaderCell(
                                        day = day,
                                        style = dayStyles.getValue(day),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            visibleGroupedRows.forEachIndexed { groupIndex, rowsForPart ->
                val partSample = rowsForPart.first()
                val groupHeight = BodyRowHeight * rowsForPart.size
                val partDayTotals =
                    days.mapIndexed { index, _ ->
                        rowsForPart.sumOf { row -> row.dayValues.getOrNull(index) ?: 0 }
                    }
                val partTotal = rowsForPart.sumOf { it.totalDefect }
                val rowBackground = if (groupIndex % 2 == 0) NeutralSurface else NeutralLight
                val sketchBitmap =
                    partSample.sketchPath
                        ?.takeIf { it.isNotBlank() }
                        ?.let { sketchPath -> sketchCache[sketchPath] }

                Row(modifier = Modifier.fillMaxWidth()) {
                    TablePartSectionHeader(
                        text =
                            "Part ${partSample.uniqCode} - ${partSample.partNumber} " +
                                "(${rowsForPart.size} jenis NG)",
                        width = leftColumnsWidth + SectionDividerWidth + rightSectionWidth,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.width(leftColumnsWidth),
                    ) {
                        SketchCell(
                            sketchPath = partSample.sketchPath,
                            bitmap = sketchBitmap,
                            width = SketchColumnWidth,
                            height = groupHeight,
                            backgroundColor = rowBackground,
                            showPlaceholder = true,
                        )
                        TableBodyCell(
                            text = formatPartNumber(partSample.partNumber, partSample.uniqCode),
                            width = PartNumberColumnWidth,
                            height = groupHeight,
                            backgroundColor = rowBackground,
                            maxLines = 2,
                        )
                        Column {
                            rowsForPart.forEach { row ->
                                Row {
                                    TableBodyCell(
                                        text = formatProblemItems(row.problemItems),
                                        width = ProblemItemColumnWidth,
                                        height = BodyRowHeight,
                                        backgroundColor = rowBackground,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    VerticalSectionDivider(height = groupHeight)
                    Column(modifier = Modifier.width(rightSectionWidth)) {
                        rowsForPart.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.width(dayViewportWidth).horizontalScroll(scrollState)) {
                                    Row {
                                        row.dayValues.forEachIndexed { index, value ->
                                            val style = dayStyles.getValue(days[index])
                                            TableBodyCell(
                                                text = value.toString(),
                                                width = DayColumnWidth,
                                                height = BodyRowHeight,
                                                backgroundColor = style.bodyBackground,
                                                alignCenter = true,
                                                textColor = style.bodyTextColor,
                                            )
                                        }
                                    }
                                }
                                TableBodyCell(
                                    text = row.totalDefect.toString(),
                                    width = TotalColumnWidth,
                                    height = BodyRowHeight,
                                    backgroundColor = rowBackground,
                                    alignCenter = true,
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.width(leftColumnsWidth)) {
                        TableSubtotalCell(
                            text = "",
                            width = SketchColumnWidth + PartNumberColumnWidth,
                            height = SubtotalRowHeight,
                            backgroundColor = SubtotalHighlight,
                        )
                        TableSubtotalCell(
                            text = "${AppStrings.ReportsMonthly.TableSubtotal} ${partSample.uniqCode}",
                            width = ProblemItemColumnWidth,
                            height = SubtotalRowHeight,
                            backgroundColor = SubtotalHighlight,
                        )
                    }
                    VerticalSectionDivider(height = SubtotalRowHeight)
                    Row(modifier = Modifier.width(rightSectionWidth)) {
                        Box(modifier = Modifier.width(dayViewportWidth).horizontalScroll(scrollState)) {
                            Row {
                                partDayTotals.forEachIndexed { index, value ->
                                    val style = dayStyles.getValue(days[index])
                                    TableSubtotalCell(
                                        text = value.toString(),
                                        width = DayColumnWidth,
                                        height = SubtotalRowHeight,
                                        alignCenter = true,
                                        backgroundColor = style.subtotalBackground,
                                        textColor = style.bodyTextColor,
                                    )
                                }
                            }
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
                PartDivider(thickness = 3.dp, color = BrandBlue.copy(alpha = 0.28f))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.width(leftColumnsWidth)) {
                    TableFooterCell(
                        text = AppStrings.ReportsMonthly.TableGrandTotal,
                        width = leftColumnsWidth,
                        height = TotalRowHeight,
                    )
                }
                VerticalSectionDivider(height = TotalRowHeight)
                Row(modifier = Modifier.width(rightSectionWidth)) {
                    Box(modifier = Modifier.width(dayViewportWidth).horizontalScroll(scrollState)) {
                        Row {
                            filteredDayTotals.forEachIndexed { index, value ->
                                val style = dayStyles.getValue(days[index])
                                TableFooterCell(
                                    text = value.toString(),
                                    width = DayColumnWidth,
                                    height = TotalRowHeight,
                                    alignCenter = true,
                                    backgroundColor = style.footerBackground,
                                    textColor = style.bodyTextColor,
                                )
                            }
                        }
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
}

@Composable
private fun DayHeaderCell(
    day: LocalDate,
    style: DayCellStyle,
) {
    Box(
        modifier =
            Modifier
                .width(DayColumnWidth)
                .height(SubHeaderRowHeight)
                .border(1.dp, NeutralBorder)
                .background(style.headerBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                color = style.headerTextColor,
            )
            if (style.hasInput) {
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .background(style.dotColor, shape = CircleShape),
                )
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
private fun TablePartSectionHeader(
    text: String,
    width: Dp,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(PartSectionHeaderHeight)
                .border(1.dp, NeutralBorder)
                .background(BrandBlue.copy(alpha = 0.08f))
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        contentAlignment = Alignment.CenterStart,
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
private fun RowScope.TableBodyCell(
    text: String,
    width: Dp,
    height: Dp,
    backgroundColor: Color = NeutralSurface,
    alignCenter: Boolean = false,
    maxLines: Int = 1,
    textColor: Color = NeutralText,
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
            style = MaterialTheme.typography.body2,
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

@Composable
private fun MonthlyDocumentModeSwitch(
    mode: MonthlyDocumentMode,
    onModeChange: (MonthlyDocumentMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = NeutralSurface,
        border = BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadge(
                text = "Mode Tampilan",
                backgroundColor = NeutralLight,
                contentColor = NeutralTextMuted,
            )
            LineChip(
                label = "Pratinjau Cepat",
                selected = mode == MonthlyDocumentMode.PREVIEW,
                onClick = { onModeChange(MonthlyDocumentMode.PREVIEW) },
            )
            LineChip(
                label = "Dokumen Penuh",
                selected = mode == MonthlyDocumentMode.FULL,
                onClick = { onModeChange(MonthlyDocumentMode.FULL) },
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Gunakan Dokumen Penuh untuk pratinjau cetak A4 landscape per halaman.",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

private enum class MonthlyDocumentMode {
    PREVIEW,
    FULL,
}

private data class DayCellStyle(
    val hasInput: Boolean,
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
    sketchPath: String?,
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    width: Dp,
    height: Dp,
    backgroundColor: Color,
    showPlaceholder: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(backgroundColor)
                .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = AppStrings.Inspection.PartImageDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(height - 6.dp),
            )
        } else if (!sketchPath.isNullOrBlank()) {
            SkeletonBlock(
                width = width - 12.dp,
                height = (height - 10.dp).coerceAtLeast(28.dp),
                color = NeutralBorder.copy(alpha = 0.35f),
            )
        } else if (showPlaceholder) {
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
private fun PartDivider(
    thickness: Dp = 1.dp,
    color: Color = NeutralBorder.copy(alpha = 0.9f),
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(thickness)
                .background(color),
    )
}

@Composable
private fun VerticalSectionDivider(height: Dp) {
    Box(
        modifier =
            Modifier
                .width(SectionDividerWidth)
                .height(height)
                .background(NeutralBorder.copy(alpha = 0.95f)),
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
            LegendChip(label = "Sub-total", color = BrandBlue.copy(alpha = 0.4f))
            LegendChip(label = "Total", color = BrandBlue)
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

private fun formatProblemItems(items: List<String>): String {
    if (items.isEmpty()) return AppStrings.Common.Placeholder
    val normalized =
        items
            .flatMap { DefectNameSanitizer.expandProblemItems(it) }
            .ifEmpty { items.map(DefectNameSanitizer::canonicalKey) }
            .filter { it.isNotBlank() }
            .distinct()
    return normalized.joinToString(separator = " / ")
}

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
