package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyInsightCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyParetoCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyTotalsUi
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.MonthlyTrendCard
import id.co.nierstyd.mutugemba.desktop.ui.components.analytics.buildLineColors
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    recentRecords: List<InspectionRecord>,
    lines: List<Line>,
    dailySummaries: List<DailyChecksheetSummary>,
    loadDailyDetail: (Long, LocalDate) -> id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail?,
    loadMonthlyDefectSummary: (YearMonth) -> List<DefectSummary>,
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
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }

    val summariesToday = dailySummaries.filter { it.date == today }
    val totalDefectToday = summariesToday.sumOf { it.totalDefect }
    val totalCheckFromSummary = summariesToday.sumOf { it.totalCheck }
    val ratioToday = if (totalCheckFromSummary > 0) totalDefectToday.toDouble() / totalCheckFromSummary.toDouble() else 0.0
    val mostActiveLine =
        recordsToday.groupBy { it.lineName }.maxByOrNull { it.value.size }?.key
    val mostCheckedPart =
        recordsToday.groupBy { it.partNumber }.maxByOrNull { it.value.size }?.value?.firstOrNull()

    val lineColors = remember(lines) { buildLineColors(lines) }
    var paretoLineId by remember { mutableStateOf<Long?>(null) }
    var trendLineId by remember { mutableStateOf<Long?>(null) }
    var insightLineId by remember { mutableStateOf<Long?>(null) }
    var analysisDefects by remember { mutableStateOf<List<DefectSummary>>(emptyList()) }
    var analysisLoading by remember { mutableStateOf(false) }
    var monthlyDefects by remember { mutableStateOf(emptyList<DefectSummary>()) }

    LaunchedEffect(month, dailySummaries) {
        monthlyDefects = loadMonthlyDefectSummary(month)
    }

    LaunchedEffect(month, paretoLineId) {
        if (paretoLineId == null) {
            analysisDefects = monthlyDefects
            return@LaunchedEffect
        }
        analysisLoading = true
        val lineId = paretoLineId ?: return@LaunchedEffect
        val totals =
            withContext(Dispatchers.Default) {
                val acc = mutableMapOf<Long, DefectSummary>()
                (1..month.lengthOfMonth()).map { month.atDay(it) }.forEach { date ->
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
        remember(dailySummaries, month, insightLineId) {
            val summariesForMonth = dailySummaries.filter { YearMonth.from(it.date) == month && (insightLineId == null || it.lineId == insightLineId) }
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

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        state = listState,
    ) {
        item {
            SectionHeader(
                title = "Beranda",
                subtitle = "Dashboard hasil input checksheet harian.",
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
            SectionTitle(
                title = "Sorotan Analitik QC",
                subtitle = "Pareto & trend NG untuk evaluasi bulanan.",
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
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
                Column(modifier = Modifier.weight(0.75f), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    MonthlyInsightCard(
                        month = month,
                        totals = monthlyTotals,
                        lines = lines,
                        selectedLineId = insightLineId,
                        onSelectedLine = { insightLineId = it },
                    )
                }
            }
        }
        item {
            SectionTitle(
                title = "Operasional Hari Ini",
                subtitle = "Status input dan aktivitas QC.",
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
                    feedback?.let { StatusBanner(feedback = it) }
                }
            }
        }
        item {
            Text(
                text = "Status: Offline - Lokal",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }

    ConfirmDialog(
        open = showResetDialog,
        title = "Reset Riwayat Input",
        message = "Semua data inspeksi akan dihapus. Lanjutkan?",
        confirmText = "Reset",
        dismissText = "Batal",
        onConfirm = {
            showResetDialog = false
            val success = resetData.execute()
            feedback =
                if (success) {
                    onRefreshData()
                    UserFeedback(FeedbackType.SUCCESS, "Riwayat input berhasil direset.")
                } else {
                    UserFeedback(FeedbackType.ERROR, "Reset riwayat gagal. Coba ulangi.")
                }
        },
        onDismiss = { showResetDialog = false },
    )
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
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "Ringkasan Checksheet Hari Ini", style = MaterialTheme.typography.subtitle1)
                Text(
                    text = "Tanggal $dateLabel • Data terakumulasi sepanjang shift.",
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
                Text(
                    text = "Input terakhir: ${lastInputLabel ?: "-"}",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Checksheet Masuk",
                    value = totalToday.toString(),
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Total NG",
                    value = totalDefect.toString(),
                    icon = Icons.Filled.ErrorOutline,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Rasio NG",
                    value = if (ratioDefect <= 0.0) "-" else "${"%.1f".format(ratioDefect * 100)}%",
                    icon = Icons.Filled.InsertChart,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Part Tercatat",
                    value = totalParts.toString(),
                    icon = Icons.Filled.Inventory2,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Line Terisi",
                    value = "$activeLines/$totalLines",
                    icon = Icons.Filled.Apartment,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Total Periksa",
                    value = totalCheck.toString(),
                    icon = Icons.AutoMirrored.Filled.FactCheck,
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
                    Text(text = "Ringkasan Harian", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = "Ringkas data input harian untuk evaluasi cepat.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(title = "Part Tercatat", value = totalParts.toString())
                SummaryStat(title = "Total Periksa", value = totalCheck.toString())
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "Cakupan Line", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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
                    text = "$activeLines dari $totalLines line tercatat hari ini.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
            Text(
                text = "Input terakhir: ${lastInputLabel ?: "-"}",
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
            Text(text = "Aktivitas QC Hari Ini", style = MaterialTheme.typography.subtitle1)
            Text(
                text = "Pantau aktivitas input QC untuk evaluasi cepat.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(title = "Input", value = totalInput.toString())
                SummaryStat(title = "Line Aktif", value = mostActiveLine ?: "-")
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(text = "Part Terbanyak", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                Text(
                    text = listOfNotNull(mostCheckedPartNumber, mostCheckedPart).joinToString(" • ").ifBlank { "-" },
                    style = MaterialTheme.typography.body2,
                )
            }
            Text(
                text = "Input terakhir: ${lastInputLabel ?: "-"}",
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
            imageVector = Icons.Filled.InsertChart,
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
                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(text = "Aksi Cepat", style = MaterialTheme.typography.subtitle1)
            }
            Text(
                text = "Mulai input inspeksi atau kosongkan data simulasi.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            PrimaryButton(
                text = "Mulai Input Inspeksi",
                onClick = onNavigateToInspection,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "Reset Riwayat Input",
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
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
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
            Text(text = value, style = MaterialTheme.typography.h6)
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
private fun SectionTitle(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.subtitle1)
        Text(text = subtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
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
                    Text(text = "Status Input Hari Ini", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = "Ringkasan checksheet per line (${DateTimeFormats.formatDate(LocalDate.now())}).",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Detail",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(16.dp),
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
            !hasInput -> "Belum Input"
            status.lastInputTime != null -> "Input Terakhir ${status.lastInputTime}"
            else -> "Sudah Input"
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
                    text = "${status.partsRecorded} part tercatat",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
                Text(
                    text = "${status.recordCount} dokumen harian",
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
