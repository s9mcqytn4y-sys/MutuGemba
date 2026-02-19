package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppAccordionIndicator
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.FeedbackHost
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.LineComparisonCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.LineComparisonItemUi
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyInsightCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyParetoCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyTotalsUi
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyTrendCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.TopProblemItemCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.TopProblemItemUi
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.buildLineColors
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.desktop.ui.util.NumberFormats
import id.co.nierstyd.mutugemba.domain.ChecksheetEntry
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    recentRecords: List<InspectionRecord>,
    lines: List<Line>,
    dailySummaries: List<DailyChecksheetSummary>,
    loadDailyDetail: (Long, LocalDate) -> id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail?,
    loadMonthlyDefectSummary: (YearMonth) -> List<DefectSummary>,
    loadMonthlyEntries: (Long, YearMonth) -> List<ChecksheetEntry>,
    resetData: ResetDataUseCase,
    onNavigateToInspection: () -> Unit,
    onRefreshData: () -> Unit,
) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val recordsToday =
        recentRecords.filter { record ->
            DateTimeFormats
                .parseLocalDate(record.createdAt)
                ?.isEqual(today)
                ?: false
        }
    val totalToday = recordsToday.size
    val totalPartsToday = recordsToday.map { it.partNumber }.distinct().size
    val activeLinesToday = recordsToday.map { it.lineName }.distinct().size
    val totalCheckToday = recordsToday.sumOf { it.totalCheck ?: 0 }
    val totalLines = lines.size
    val lastInputLabel =
        recordsToday
            .maxByOrNull { DateTimeFormats.parseLocalDateTime(it.createdAt) ?: java.time.LocalDateTime.MIN }
            ?.let { DateTimeFormats.formatTimestampWithZone(it.createdAt) }
    var showLineDetail by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null, neverEqualPolicy()) }

    val summariesToday = dailySummaries.filter { it.date == today }
    val totalDefectToday = summariesToday.sumOf { it.totalDefect }
    val totalCheckFromSummary = summariesToday.sumOf { it.totalCheck }
    val ratioToday =
        if (totalCheckFromSummary >
            0
        ) {
            totalDefectToday.toDouble() / totalCheckFromSummary.toDouble()
        } else {
            0.0
        }
    val mostActiveLine =
        recordsToday.groupBy { it.lineName }.maxByOrNull { it.value.size }?.key
    val mostCheckedPart =
        recordsToday
            .groupBy { it.partNumber }
            .maxByOrNull { it.value.size }
            ?.value
            ?.firstOrNull()

    val lineColors = remember(lines) { buildLineColors(lines) }
    var paretoLineId by remember { mutableStateOf<Long?>(null) }
    var trendLineId by remember { mutableStateOf<Long?>(null) }
    var insightLineId by remember { mutableStateOf<Long?>(null) }
    var analysisDefects by remember { mutableStateOf<List<DefectSummary>>(emptyList()) }
    var analysisLoading by remember { mutableStateOf(false) }
    var monthlyDefects by remember { mutableStateOf(emptyList<DefectSummary>()) }
    var topProblemLineId by remember { mutableStateOf<Long?>(null) }
    var topProblemItem by remember { mutableStateOf<TopProblemItemUi?>(null) }
    var topProblemLoading by remember { mutableStateOf(false) }

    LaunchedEffect(month, dailySummaries) {
        monthlyDefects = aggregateDefectSummaries(loadMonthlyDefectSummary(month))
    }

    LaunchedEffect(month, paretoLineId) {
        if (paretoLineId == null) {
            analysisDefects = monthlyDefects
            return@LaunchedEffect
        }
        analysisLoading = true
        val lineId = paretoLineId ?: return@LaunchedEffect
        val totals =
            withContext(Dispatchers.IO) {
                val summaries = mutableListOf<DefectSummary>()
                (1..month.lengthOfMonth()).map { month.atDay(it) }.forEach { date ->
                    val detail = loadDailyDetail(lineId, date)
                    summaries += detail?.defectSummaries.orEmpty()
                }
                aggregateDefectSummaries(summaries)
            }
        analysisDefects = totals
        analysisLoading = false
    }

    LaunchedEffect(month, topProblemLineId, lines) {
        topProblemLoading = true
        val selectedLineId = topProblemLineId
        val entries =
            withContext(Dispatchers.IO) {
                if (selectedLineId == null) {
                    lines.flatMap { line -> loadMonthlyEntries(line.id, month) }
                } else {
                    loadMonthlyEntries(selectedLineId, month)
                }
            }
        topProblemItem =
            entries
                .groupBy { it.partNumber }
                .map { (_, items) ->
                    val totalDefect = items.sumOf { it.totalDefect }
                    val totalCheck = items.sumOf { it.totalCheck }
                    val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
                    val sample = items.first()
                    TopProblemItemUi(
                        partNumber = sample.partNumber,
                        partName = sample.partName,
                        totalDefect = totalDefect,
                        totalCheck = totalCheck,
                        ratio = ratio,
                    )
                }.maxWithOrNull(
                    compareBy<TopProblemItemUi> { it.totalDefect }.thenBy { it.ratio },
                )
        topProblemLoading = false
    }

    val monthlyTotals =
        remember(dailySummaries, month, insightLineId) {
            val summariesForMonth =
                dailySummaries.filter {
                    YearMonth.from(it.date) == month && (insightLineId == null || it.lineId == insightLineId)
                }
            val totalDocs = summariesForMonth.size
            val totalCheck = summariesForMonth.sumOf { it.totalCheck }
            val totalDefect = summariesForMonth.sumOf { it.totalDefect }
            val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
            val daysWithInput = summariesForMonth.map { it.date }.distinct().size
            val daysInMonth = month.lengthOfMonth()
            val totalParts = summariesForMonth.sumOf { it.totalParts }
            val avgDefectPerDay = if (daysWithInput > 0) totalDefect.toDouble() / daysWithInput.toDouble() else 0.0
            MonthlyTotalsUi(
                totalDocs = totalDocs,
                totalCheck = totalCheck,
                totalDefect = totalDefect,
                ratio = ratio,
                daysWithInput = daysWithInput,
                daysInMonth = daysInMonth,
                totalParts = totalParts,
                avgDefectPerDay = avgDefectPerDay,
            )
        }

    val lineComparison =
        remember(dailySummaries, month, lines) {
            lines.map { line ->
                val summaries =
                    dailySummaries.filter {
                        YearMonth.from(it.date) == month && it.lineId == line.id
                    }
                val totalDefect = summaries.sumOf { it.totalDefect }
                val totalCheck = summaries.sumOf { it.totalCheck }
                val ratio = if (totalCheck > 0) totalDefect.toDouble() / totalCheck.toDouble() else 0.0
                LineComparisonItemUi(
                    lineName = line.name,
                    totalDefect = totalDefect,
                    ratio = ratio,
                    color = lineColors[line.id] ?: StatusInfo,
                )
            }
        }

    val listState = rememberLazyListState()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportMaxHeight = if (maxHeight != Dp.Infinity) maxHeight else 900.dp
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = viewportMaxHeight),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            state = listState,
        ) {
            item {
                SectionHeader(
                    title = AppStrings.Home.Title,
                    subtitle = AppStrings.Home.Subtitle,
                )
            }
            item {
                HeroSummaryCard(
                    dateLabel = DateTimeFormats.formatDate(today),
                    totalToday = totalToday,
                    totalParts = totalPartsToday,
                    activeLines = activeLinesToday,
                    totalCheck = totalCheckToday,
                    totalLines = totalLines,
                    lastInputLabel = lastInputLabel,
                    totalDefect = totalDefectToday,
                    ratioDefect = ratioToday,
                )
            }
            item {
                DashboardSectionHeader(
                    title = AppStrings.Home.AnalyticsTitle,
                    subtitle = AppStrings.Home.AnalyticsSubtitle,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        MonthlyParetoCard(
                            month = month,
                            defectSummaries = analysisDefects,
                            accentColor = lineColors[paretoLineId] ?: StatusInfo,
                            loading = analysisLoading,
                            lines = lines,
                            selectedLineId = paretoLineId,
                            onSelectedLine = { paretoLineId = it },
                        )
                        MonthlyTrendCard(
                            month = month,
                            dailySummaries = dailySummaries.filter { YearMonth.from(it.date) == month },
                            lineColors = lineColors,
                            lines = lines,
                            selectedLineId = trendLineId,
                            onSelectedLine = { trendLineId = it },
                        )
                    }
                    Column(modifier = Modifier.weight(0.65f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        MonthlyInsightCard(
                            month = month,
                            totals = monthlyTotals,
                            lines = lines,
                            selectedLineId = insightLineId,
                            onSelectedLine = { insightLineId = it },
                        )
                        TopProblemItemCard(
                            month = month,
                            lines = lines,
                            selectedLineId = topProblemLineId,
                            onSelectedLine = { topProblemLineId = it },
                            item = topProblemItem,
                            loading = topProblemLoading,
                        )
                        LineComparisonCard(items = lineComparison)
                    }
                }
            }
            item {
                DashboardSectionHeader(
                    title = AppStrings.Home.OpsTitle,
                    subtitle = AppStrings.Home.OpsSubtitle,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        DailyChecksheetSummaryCard(
                            totalParts = totalPartsToday,
                            totalLines = lines.size,
                            activeLines = activeLinesToday,
                            totalCheck = totalCheckToday,
                            lastInputLabel = lastInputLabel,
                        )
                        DailyLineStatusCard(
                            lines = lines,
                            recordsToday = recordsToday,
                            expanded = showLineDetail,
                            onToggleExpanded = { showLineDetail = !showLineDetail },
                        )
                    }
                    Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        QcActivityCard(
                            totalInput = totalToday,
                            mostActiveLine = mostActiveLine,
                            mostCheckedPart = mostCheckedPart?.partName,
                            mostCheckedPartNumber = mostCheckedPart?.partNumber,
                            lastInputLabel = lastInputLabel,
                        )
                        ActionCard(
                            onNavigateToInspection = onNavigateToInspection,
                            onReset = { showResetDialog = true },
                        )
                        TpsFlowSnapshotCard(
                            totalInput = totalToday,
                            totalNg = totalDefectToday,
                            totalCheck = totalCheckFromSummary,
                            activeLines = activeLinesToday,
                        )
                        FeedbackHost(feedback = feedback, onDismiss = { feedback = null })
                    }
                }
            }
            item {
                Text(
                    text = AppStrings.App.StatusOffline,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }

    ConfirmDialog(
        open = showResetDialog,
        title = AppStrings.Home.ResetDialogTitle,
        message = AppStrings.Home.ResetDialogMessage,
        confirmText = AppStrings.Actions.Reset,
        dismissText = AppStrings.Actions.Cancel,
        onConfirm = {
            showResetDialog = false
            val success = resetData.execute()
            feedback =
                if (success) {
                    onRefreshData()
                    UserFeedback(FeedbackType.SUCCESS, AppStrings.Home.ResetSuccess)
                } else {
                    UserFeedback(FeedbackType.ERROR, AppStrings.Home.ResetFailed)
                }
        },
        onDismiss = { showResetDialog = false },
    )
}

@Composable
private fun TpsFlowSnapshotCard(
    totalInput: Int,
    totalNg: Int,
    totalCheck: Int,
    activeLines: Int,
) {
    val detectStatus = if (totalNg > 0) "DETECT: $totalNg NG" else "DETECT: Stabil"
    val containStatus = if (totalInput > 0) "CONTAIN: $activeLines line aktif" else "CONTAIN: Belum ada input"
    val closeRate =
        if (totalCheck > 0) {
            (((totalCheck - totalNg).coerceAtLeast(0).toDouble() / totalCheck.toDouble()) * 100.0).toInt()
        } else {
            0
        }
    val closeStatus = "CLOSE: $closeRate% OK"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text("Alur TPS Hari Ini", style = MaterialTheme.typography.subtitle1, color = NeutralText)
            Text(
                "Pantau siklus DETECT -> CONTAIN -> CLOSE untuk operator.",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Text(detectStatus, style = MaterialTheme.typography.body2, color = NeutralText)
            Text(containStatus, style = MaterialTheme.typography.body2, color = NeutralText)
            Text(closeStatus, style = MaterialTheme.typography.body2, color = NeutralText)
        }
    }
}

@Composable
private fun HeroSummaryCard(
    dateLabel: String,
    totalToday: Int,
    totalParts: Int,
    activeLines: Int,
    totalCheck: Int,
    totalLines: Int,
    lastInputLabel: String?,
    totalDefect: Int,
    ratioDefect: Double,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = AppStrings.Home.SummaryTitle, style = MaterialTheme.typography.subtitle1)
                Text(
                    text = AppStrings.Home.summaryDateLabel(dateLabel),
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
                Text(
                    text = AppStrings.Common.lastInput(lastInputLabel),
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = AppStrings.Home.MetricChecksheet,
                    value = totalToday.toString(),
                    icon = AppIcons.Assignment,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = AppStrings.Home.MetricTotalNg,
                    value = totalDefect.toString(),
                    icon = AppIcons.ErrorOutline,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = AppStrings.Home.MetricNgRatio,
                    value = NumberFormats.formatPercent(ratioDefect),
                    icon = AppIcons.InsertChart,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = AppStrings.Home.MetricParts,
                    value = totalParts.toString(),
                    icon = AppIcons.Inventory,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = AppStrings.Home.MetricLineCoverage,
                    value = AppStrings.Home.lineRecorded(activeLines, totalLines),
                    icon = AppIcons.Apartment,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = AppStrings.Home.MetricTotalCheck,
                    value = totalCheck.toString(),
                    icon = AppIcons.FactCheck,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DailyChecksheetSummaryCard(
    totalParts: Int,
    totalLines: Int,
    activeLines: Int,
    totalCheck: Int,
    lastInputLabel: String?,
) {
    val lineRatio =
        if (totalLines > 0) {
            activeLines.toFloat() / totalLines.toFloat()
        } else {
            0f
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniSummaryIcon()
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(text = AppStrings.Home.DailySummaryTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = AppStrings.Home.DailySummarySubtitle,
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(title = AppStrings.Common.PartRecorded, value = totalParts.toString())
                SummaryStat(title = AppStrings.Common.TotalCheck, value = totalCheck.toString())
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = AppStrings.Home.DailyLineCoverage,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(NeutralBorder, MaterialTheme.shapes.small),
                ) {
                    if (lineRatio > 0f) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(lineRatio)
                                    .fillMaxHeight()
                                    .background(StatusSuccess),
                        )
                    }
                }
                Text(
                    text = AppStrings.Common.lineCoverage(activeLines, totalLines),
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            Text(
                text = AppStrings.Common.lastInput(lastInputLabel),
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun QcActivityCard(
    totalInput: Int,
    mostActiveLine: String?,
    mostCheckedPart: String?,
    mostCheckedPartNumber: String?,
    lastInputLabel: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = AppStrings.Home.ActivityTitle, style = MaterialTheme.typography.subtitle1)
            Text(
                text = AppStrings.Home.ActivitySubtitle,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(title = AppStrings.Common.Input, value = totalInput.toString())
                SummaryStat(
                    title = AppStrings.Home.ActivityMostActive,
                    value =
                        mostActiveLine ?: AppStrings.Common.Placeholder,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = AppStrings.Home.ActivityMostPart,
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text =
                        listOfNotNull(mostCheckedPartNumber, mostCheckedPart)
                            .joinToString(" • ")
                            .ifBlank { AppStrings.Common.Placeholder },
                    style = MaterialTheme.typography.body2,
                )
            }
            Text(
                text = AppStrings.Common.lastInput(lastInputLabel),
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun MiniSummaryIcon() {
    Surface(
        color = StatusInfo.copy(alpha = 0.12f),
        shape = CircleShape,
        elevation = 0.dp,
    ) {
        Icon(
            imageVector = AppIcons.InsertChart,
            contentDescription = null,
            tint = StatusInfo,
            modifier = Modifier.padding(8.dp).size(16.dp),
        )
    }
}

@Composable
private fun ActionCard(
    onNavigateToInspection: () -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.primary.copy(alpha = 0.04f),
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colors.primary.copy(alpha = 0.2f),
            ),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(text = AppStrings.Home.ActionTitle, style = MaterialTheme.typography.subtitle1)
            }
            Text(
                text = AppStrings.Home.ActionSubtitle,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            PrimaryButton(
                text = AppStrings.Actions.StartInspection,
                onClick = onNavigateToInspection,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = AppStrings.Actions.ResetInput,
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
                    elevation = 0.dp,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(6.dp).size(14.dp),
                    )
                }
                Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            }
            Text(text = value, style = MaterialTheme.typography.subtitle1, color = NeutralText)
        }
    }
}

@Composable
private fun RowScope.SummaryStat(
    title: String,
    value: String,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
        Text(text = value, style = MaterialTheme.typography.subtitle1)
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.subtitle1)
        Text(text = subtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NeutralBorder.copy(alpha = 0.6f)),
        )
    }
}

private data class LineDailyStatus(
    val line: Line,
    val recordCount: Int,
    val partsRecorded: Int,
    val ratio: Float,
    val lastInputTime: String?,
)

@Composable
private fun DailyLineStatusCard(
    lines: List<Line>,
    recordsToday: List<InspectionRecord>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val totalRecords = recordsToday.size.coerceAtLeast(1)
    val status =
        lines.map { line ->
            val lineRecords = recordsToday.filter { it.lineName == line.name }
            val recordCount = lineRecords.size
            val partsRecorded = lineRecords.map { it.partNumber }.distinct().size
            val lastInputTime =
                lineRecords
                    .maxByOrNull { DateTimeFormats.parseLocalDateTime(it.createdAt) ?: java.time.LocalDateTime.MIN }
                    ?.let { DateTimeFormats.formatTime(it.createdAt) }
            LineDailyStatus(
                line = line,
                recordCount = recordCount,
                partsRecorded = partsRecorded,
                ratio = recordCount.toFloat() / totalRecords.toFloat(),
                lastInputTime = lastInputTime,
            )
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() },
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
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
                Column {
                    Text(text = AppStrings.Home.LineStatusTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = AppStrings.Home.lineStatusSubtitle(DateTimeFormats.formatDate(LocalDate.now())),
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppStrings.Common.Detail,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                    AppAccordionIndicator(
                        expanded = expanded,
                        accent = MaterialTheme.colors.primary,
                        size = 20.dp,
                    )
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    status.forEach { item ->
                        LineStatusRow(status = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun LineStatusRow(status: LineDailyStatus) {
    val hasInput = status.recordCount > 0
    val label =
        when {
            !hasInput -> AppStrings.Home.LineStatusNotInput
            status.lastInputTime != null -> AppStrings.Home.lineStatusLastInput(status.lastInputTime)
            else -> AppStrings.Home.LineStatusDone
        }
    val color = if (hasInput) StatusSuccess else StatusError
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = status.line.name, style = MaterialTheme.typography.body2, color = NeutralText)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AppStrings.Home.partsRecorded(status.partsRecorded),
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = AppStrings.Home.docsRecorded(status.recordCount),
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            AppBadge(
                text = label,
                backgroundColor = color,
                contentColor = NeutralSurface,
            )
        }
    }
}

private fun aggregateDefectSummaries(source: List<DefectSummary>): List<DefectSummary> {
    val grouped = linkedMapOf<String, DefectSummary>()
    source.forEach { summary ->
        val name = DefectNameSanitizer.normalizeDisplay(summary.defectName)
        val key = DefectNameSanitizer.canonicalKey(name)
        if (key.isBlank() || !DefectNameSanitizer.isMeaningfulItem(name)) return@forEach
        val existing = grouped[key]
        if (existing == null) {
            grouped[key] = summary.copy(defectName = name)
        } else {
            grouped[key] =
                existing.copy(
                    totalQuantity = existing.totalQuantity + summary.totalQuantity,
                )
        }
    }
    return grouped.values.sortedByDescending { it.totalQuantity }
}
