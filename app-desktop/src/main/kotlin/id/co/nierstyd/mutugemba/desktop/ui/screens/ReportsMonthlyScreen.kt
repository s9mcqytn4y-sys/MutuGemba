
package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.MonthlyReportDocument
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.awt.Desktop
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DocumentWidth = 1120.dp
private val DocumentMinHeight = 760.dp
private val HeaderRowHeight = 32.dp
private val SubHeaderRowHeight = 32.dp
private val BodyRowHeight = 32.dp
private val SubtotalRowHeight = 26.dp
private val TotalRowHeight = 30.dp
private val SketchColumnWidth = 56.dp
private val PartNumberColumnWidth = 130.dp
private val ProblemItemColumnWidth = 170.dp
private val DayColumnWidth = 32.dp
private val DefectColumnWidth = 90.dp
private val TotalColumnWidth = 80.dp

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
    val scope = rememberCoroutineScope()

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
                    result.onSuccess { path ->
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
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
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
                Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(16.dp))
            }
            Text(text = label, style = MaterialTheme.typography.caption, color = contentColor)
            if (iconOnRight) {
                Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(16.dp))
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
                    MonthlyReportTable(document = document, manualHolidays = manualHolidays)
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
            Text(text = AppStrings.ReportsMonthly.DocumentNoLabel, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
private fun MonthlyReportTable(
    document: MonthlyReportDocument,
    manualHolidays: Set<LocalDate>,
) {
    if (document.rows.isEmpty()) {
        MonthlyReportEmpty()
        return
    }

    val scrollState = rememberScrollState()
    val days = document.days
    val defectTypes = document.defectTypes

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
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
                            width = DayColumnWidth * days.size,
                            height = HeaderRowHeight,
                        )
                        TableHeaderCell(
                            text = AppStrings.ReportsMonthly.TableTotals,
                            width = DefectColumnWidth * defectTypes.size + TotalColumnWidth,
                            height = HeaderRowHeight,
                        )
                    }
                    Row {
                        days.forEach { day ->
                            val isHoliday = isHoliday(day, manualHolidays)
                            TableHeaderCell(
                                text = day.dayOfMonth.toString(),
                                width = DayColumnWidth,
                                height = SubHeaderRowHeight,
                                backgroundColor = if (isHoliday) NeutralLight else headerBackground(),
                            )
                        }
                        defectTypes.forEach { defect ->
                            TableHeaderCell(
                                text = defect.name,
                                width = DefectColumnWidth,
                                height = SubHeaderRowHeight,
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

        document.rows.forEachIndexed { index, row ->
            val rowBackground = if (index % 2 == 0) NeutralSurface else NeutralLight
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.width(SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth)) {
                    TableBodyCell(
                        text = "-",
                        width = SketchColumnWidth,
                        height = BodyRowHeight,
                        backgroundColor = rowBackground,
                        alignCenter = true,
                    )
                    TableBodyCell(
                        text = row.partNumber,
                        width = PartNumberColumnWidth,
                        height = BodyRowHeight,
                        backgroundColor = rowBackground,
                    )
                    TableBodyCell(
                        text = row.problemItem,
                        width = ProblemItemColumnWidth,
                        height = BodyRowHeight,
                        backgroundColor = rowBackground,
                    )
                }
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    row.dayValues.forEach { value ->
                        TableBodyCell(
                            text = value.toString(),
                            width = DayColumnWidth,
                            height = BodyRowHeight,
                            backgroundColor = rowBackground,
                            alignCenter = true,
                        )
                    }
                    row.defectTotals.forEach { value ->
                        TableBodyCell(
                            text = value.toString(),
                            width = DefectColumnWidth,
                            height = BodyRowHeight,
                            backgroundColor = rowBackground,
                            alignCenter = true,
                        )
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

            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.width(SketchColumnWidth + PartNumberColumnWidth + ProblemItemColumnWidth)) {
                    TableSubtotalCell(
                        text = "",
                        width = SketchColumnWidth + PartNumberColumnWidth,
                        height = SubtotalRowHeight,
                    )
                    TableSubtotalCell(
                        text = AppStrings.ReportsMonthly.TableSubtotal,
                        width = ProblemItemColumnWidth,
                        height = SubtotalRowHeight,
                    )
                }
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    row.dayValues.forEach { value ->
                        TableSubtotalCell(
                            text = value.toString(),
                            width = DayColumnWidth,
                            height = SubtotalRowHeight,
                            alignCenter = true,
                        )
                    }
                    row.defectTotals.forEach { value ->
                        TableSubtotalCell(
                            text = value.toString(),
                            width = DefectColumnWidth,
                            height = SubtotalRowHeight,
                            alignCenter = true,
                        )
                    }
                    TableSubtotalCell(
                        text = row.totalDefect.toString(),
                        width = TotalColumnWidth,
                        height = SubtotalRowHeight,
                        alignCenter = true,
                    )
                }
            }
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
                document.totals.dayTotals.forEach { value ->
                    TableFooterCell(
                        text = value.toString(),
                        width = DayColumnWidth,
                        height = TotalRowHeight,
                        alignCenter = true,
                    )
                }
                document.totals.defectTotals.forEach { value ->
                    TableFooterCell(
                        text = value.toString(),
                        width = DefectColumnWidth,
                        height = TotalRowHeight,
                        alignCenter = true,
                    )
                }
                TableFooterCell(
                    text = document.totals.totalDefect.toString(),
                    width = TotalColumnWidth,
                    height = TotalRowHeight,
                    alignCenter = true,
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
            color = NeutralText,
            maxLines = 1,
            textAlign = TextAlign.Center,
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
        Text(text = text, style = MaterialTheme.typography.body2, color = NeutralText, maxLines = 1)
    }
}

@Composable
private fun RowScope.TableSubtotalCell(
    text: String,
    width: Dp,
    height: Dp,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.caption, color = NeutralText)
    }
}

@Composable
private fun RowScope.TableFooterCell(
    text: String,
    width: Dp,
    height: Dp,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .border(1.dp, NeutralBorder)
                .background(NeutralLight)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.body2, color = NeutralText)
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
            Text(text = AppStrings.Reports.DocumentSignatureName, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
        Box(contentAlignment = Alignment.Center) {
            Text(text = AppStrings.App.Logo, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
            Text(text = AppStrings.ReportsMonthly.EmptySubtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
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
