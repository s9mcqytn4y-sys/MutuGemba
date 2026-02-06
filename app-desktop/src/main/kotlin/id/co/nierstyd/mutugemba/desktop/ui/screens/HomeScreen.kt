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
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
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
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppRadioGroup
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.DropdownOption
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.DailyChecksheetDetail
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    recentRecords: List<InspectionRecord>,
    lines: List<Line>,
    dailySummaries: List<DailyChecksheetSummary>,
    resetData: ResetDataUseCase,
    onNavigateToInspection: () -> Unit,
    onRefreshData: () -> Unit,
    loadDailyDetail: (Long, LocalDate) -> DailyChecksheetDetail?,
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
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
    val lastInputLabel =
        recordsToday
            .maxByOrNull { DateTimeFormats.parseLocalDateTime(it.createdAt) ?: java.time.LocalDateTime.MIN }
            ?.let { DateTimeFormats.formatTimestampWithZone(it.createdAt) }
    var showLineDetail by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }
    var selectedDay by remember { mutableStateOf(today) }
    var selectedLineId by remember(lines) { mutableStateOf(lines.firstOrNull()?.id) }
    var detailState by remember { mutableStateOf<HistoryDetailState>(HistoryDetailState.Loading) }
    var isHistoryLoading by remember { mutableStateOf(true) }

    val dailySummariesByDate =
        remember(dailySummaries) {
            dailySummaries.groupBy { it.date }
        }

    LaunchedEffect(dailySummaries) {
        isHistoryLoading = true
        kotlinx.coroutines.delay(250)
        isHistoryLoading = false
    }

    LaunchedEffect(selectedDay, selectedLineId, dailySummaries) {
        detailState = HistoryDetailState.Loading
        kotlinx.coroutines.delay(200)
        val lineId = selectedLineId
        detailState =
            if (lineId == null) {
                HistoryDetailState.Empty
            } else {
                val detail = loadDailyDetail(lineId, selectedDay)
                if (detail == null) HistoryDetailState.Empty else HistoryDetailState.Loaded(detail)
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Beranda",
            subtitle = "Dashboard hasil input checksheet harian.",
        )

        HeroSummaryCard(
            dateLabel = DateTimeFormats.formatDate(today),
            totalToday = totalToday,
            totalParts = totalPartsToday,
            activeLines = activeLinesToday,
            totalCheck = totalCheckToday,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionTitle(
                    title = "Status Line Hari Ini",
                    subtitle = "Klik untuk melihat detail part per line.",
                )
                DailyLineStatusCard(
                    lines = lines,
                    recordsToday = recordsToday,
                    expanded = showLineDetail,
                    onToggleExpanded = { showLineDetail = !showLineDetail },
                )
            }
            Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DailyChecksheetSummaryCard(
                    totalParts = totalPartsToday,
                    totalLines = lines.size,
                    activeLines = activeLinesToday,
                    totalCheck = totalCheckToday,
                    lastInputLabel = lastInputLabel,
                )
                ActionCard(
                    onNavigateToInspection = onNavigateToInspection,
                    onReset = { showResetDialog = true },
                )
                feedback?.let { StatusBanner(feedback = it) }
            }
        }

        SectionTitle(
            title = "Riwayat Checksheet Bulan Ini",
            subtitle = "Pilih tanggal untuk melihat rincian checksheet harian.",
        )
        MonthlyHistorySection(
            month = currentMonth,
            today = today,
            lines = lines,
            summariesByDate = dailySummariesByDate,
            selectedDay = selectedDay,
            onDaySelected = { selectedDay = it },
            selectedLineId = selectedLineId,
            onLineSelected = { selectedLineId = it },
            detailState = detailState,
            isLoading = isHistoryLoading,
        )

        Text(
            text = "Status: Offline - Lokal",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
            modifier = Modifier.padding(top = Spacing.sm),
        )
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                MetricCard(title = "Dokumen Harian", value = totalToday.toString(), modifier = Modifier.weight(1f))
                MetricCard(title = "Part Tercatat", value = totalParts.toString(), modifier = Modifier.weight(1f))
                MetricCard(title = "Line Aktif", value = activeLines.toString(), modifier = Modifier.weight(1f))
                MetricCard(title = "Total Periksa", value = totalCheck.toString(), modifier = Modifier.weight(1f))
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
private fun MiniSummaryIcon() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier =
                Modifier
                    .width(6.dp)
                    .height(12.dp)
                    .background(StatusInfo, MaterialTheme.shapes.small),
        )
        Box(
            modifier =
                Modifier
                    .width(6.dp)
                    .height(18.dp)
                    .background(StatusSuccess, MaterialTheme.shapes.small),
        )
        Box(
            modifier =
                Modifier
                    .width(6.dp)
                    .height(8.dp)
                    .background(StatusError, MaterialTheme.shapes.small),
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
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = "Aksi Cepat", style = MaterialTheme.typography.subtitle1)
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
private fun MonthlyHistorySection(
    month: YearMonth,
    today: LocalDate,
    lines: List<Line>,
    summariesByDate: Map<LocalDate, List<DailyChecksheetSummary>>,
    selectedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    selectedLineId: Long?,
    onLineSelected: (Long?) -> Unit,
    detailState: HistoryDetailState,
    isLoading: Boolean,
) {
    val maxDay = today.dayOfMonth
    val days = (1..maxDay).map { day -> month.atDay(day) }
    val lineOptions = lines.map { DropdownOption(it.id, it.name) }
    val activeLineId = selectedLineId ?: lineOptions.firstOrNull()?.id
    val selectedPage =
        ((selectedDay.dayOfMonth - 1) / 7).coerceAtLeast(0)
    var pageIndex by remember { mutableStateOf(selectedPage) }

    LaunchedEffect(selectedDay) {
        pageIndex = ((selectedDay.dayOfMonth - 1) / 7).coerceAtLeast(0)
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(text = "Kalender Checksheet", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text =
                            "Bulan ${DateTimeFormats.formatMonth(month)} • " +
                                "Hari setelah ${today.dayOfMonth} tidak ditampilkan.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Text(
                    text = "Read-only",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }

            if (lineOptions.isNotEmpty()) {
                AppRadioGroup(
                    label = "Pilih Line (Riwayat)",
                    options = lineOptions,
                    selectedId = activeLineId,
                    onSelected = { option -> onLineSelected(option.id) },
                    helperText = "Riwayat hanya menampilkan data yang sudah tersimpan.",
                    maxHeight = 140.dp,
                )
            } else {
                Text(
                    text = "Belum ada line terdaftar.",
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }

            DayPager(
                days = days,
                pageIndex = pageIndex,
                onPageChange = { pageIndex = it },
                selectedDay = selectedDay,
                onDaySelected = onDaySelected,
                summariesByDate = summariesByDate,
                lines = lines,
            )

            StatusBanner(
                feedback =
                    UserFeedback(
                        FeedbackType.INFO,
                        "Data historis hanya dapat dilihat. Checksheet hari sebelumnya tidak bisa diubah.",
                    ),
            )

            when {
                isLoading -> HistoryDetailSkeleton()
                detailState is HistoryDetailState.Loaded -> HistoryDetailCard(detail = detailState.detail)
                else -> EmptyHistoryState()
            }
        }
    }
}

@Composable
private fun DayPager(
    days: List<LocalDate>,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    selectedDay: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    summariesByDate: Map<LocalDate, List<DailyChecksheetSummary>>,
    lines: List<Line>,
) {
    val daysPerPage = 7
    val totalPages = ((days.size - 1) / daysPerPage).coerceAtLeast(0)
    val startIndex = pageIndex * daysPerPage
    val pageDays = days.drop(startIndex).take(daysPerPage)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                text = "←",
                onClick = { onPageChange((pageIndex - 1).coerceAtLeast(0)) },
                enabled = pageIndex > 0,
            )
            Text(
                text = "Halaman ${pageIndex + 1} / ${totalPages + 1}",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            SecondaryButton(
                text = "→",
                onClick = { onPageChange((pageIndex + 1).coerceAtMost(totalPages)) },
                enabled = pageIndex < totalPages,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            pageDays.forEach { day ->
                DayChip(
                    date = day,
                    selected = day == selectedDay,
                    summaries = summariesByDate[day].orEmpty(),
                    lines = lines,
                    onClick = { onDaySelected(day) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.DayChip(
    date: LocalDate,
    selected: Boolean,
    summaries: List<DailyChecksheetSummary>,
    lines: List<Line>,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colors.primary else NeutralBorder
    val lineMap = summaries.associateBy { it.lineId }
    Surface(
        modifier = Modifier.weight(1f).clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        color = if (selected) NeutralLight else NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.subtitle1)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                lines.forEach { line ->
                    val hasInput = lineMap.containsKey(line.id)
                    Box(
                        modifier =
                            Modifier
                                .width(10.dp)
                                .height(6.dp)
                                .background(
                                    if (hasInput) StatusSuccess else NeutralBorder,
                                    MaterialTheme.shapes.small,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailCard(detail: DailyChecksheetDetail) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(text = "Dokumen Checksheet Harian", style = MaterialTheme.typography.subtitle1)
            DailyDocumentHeader(detail)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SummaryStat(title = "Total Periksa", value = detail.totalCheck.toString())
                SummaryStat(title = "Total NG", value = detail.totalDefect.toString())
                SummaryStat(title = "Total OK", value = detail.totalOk.toString())
            }
            DocumentEntryTable(detail)
            Text(
                text = "Footer: Tanda tangan QC/Leader (draft).",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton(text = "Cetak (Segera)", onClick = {}, enabled = false)
                SecondaryButton(text = "Ekspor PDF (Segera)", onClick = {}, enabled = false)
            }
        }
    }
}

@Composable
private fun DailyDocumentHeader(detail: DailyChecksheetDetail) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = NeutralLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
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
                Box(
                    modifier =
                        Modifier
                            .width(56.dp)
                            .height(32.dp)
                            .background(NeutralBorder, MaterialTheme.shapes.small),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PT. Primaraya Graha Nusantara", style = MaterialTheme.typography.subtitle1)
                    Text(text = "Quality Assurance Dept.", style = MaterialTheme.typography.caption)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "No. Dokumen", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    Text(text = detail.docNumber, style = MaterialTheme.typography.body2)
                }
            }
            Divider(color = NeutralBorder)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(text = "Judul Dokumen", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    Text(text = "Checksheet Harian NG", style = MaterialTheme.typography.body2)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = Alignment.End) {
                    Text(text = "Tanggal", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    Text(
                        text = DateTimeFormats.formatDate(detail.date),
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(text = "Line", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    Text(text = detail.lineName, style = MaterialTheme.typography.body2)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(text = "Shift", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    Text(text = detail.shiftName, style = MaterialTheme.typography.body2)
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), horizontalAlignment = Alignment.End) {
                    Text(text = "PIC", style = MaterialTheme.typography.caption, color = NeutralTextMuted)
                    Text(text = detail.picName, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@Composable
private fun DocumentEntryTable(detail: DailyChecksheetDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(NeutralLight),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TableCell(text = "Part", weight = 2f)
            TableCell(text = "Total Periksa", weight = 1f, alignCenter = true)
            TableCell(text = "Total NG", weight = 1f, alignCenter = true)
            TableCell(text = "Total OK", weight = 1f, alignCenter = true)
        }
        detail.entries.forEach { entry ->
            val totalOk = (entry.totalCheck - entry.totalDefect).coerceAtLeast(0)
            Row(modifier = Modifier.fillMaxWidth()) {
                TableCell(text = "${entry.partNumber} • ${entry.partName}", weight = 2f)
                TableCell(text = entry.totalCheck.toString(), weight = 1f, alignCenter = true)
                TableCell(text = entry.totalDefect.toString(), weight = 1f, alignCenter = true)
                TableCell(text = totalOk.toString(), weight = 1f, alignCenter = true)
            }
        }
    }
}

@Composable
private fun HistoryDetailSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SkeletonBlock(width = 180.dp, height = 14.dp)
        SkeletonBlock(width = 280.dp, height = 10.dp)
        SkeletonBlock(width = 260.dp, height = 10.dp)
        SkeletonBlock(width = 320.dp, height = 120.dp)
    }
}

@Composable
private fun EmptyHistoryState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = NeutralLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = "Belum ada checksheet di tanggal ini.", style = MaterialTheme.typography.body2)
            Text(
                text = "Input hanya tersedia untuk hari ini. Riwayat sebelumnya bersifat final.",
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    alignCenter: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .weight(weight)
                .border(1.dp, NeutralBorder)
                .background(NeutralSurface)
                .padding(Spacing.sm),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        Text(text, color = NeutralText)
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
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
            Text(text = title, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
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

private sealed class HistoryDetailState {
    data object Loading : HistoryDetailState()

    data object Empty : HistoryDetailState()

    data class Loaded(
        val detail: DailyChecksheetDetail,
    ) : HistoryDetailState()
}

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
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = "Status Input Hari Ini", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = "Ringkasan checksheet per line (${DateTimeFormats.formatDate(LocalDate.now())}).",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                Text(
                    text = if (expanded) "▲ Detail" else "▼ Detail",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary,
                )
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
    val label = if (hasInput) "Sudah Input" else "Belum Input"
    val color = if (hasInput) StatusSuccess else StatusError
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = status.line.name, style = MaterialTheme.typography.body2, color = NeutralText)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                if (hasInput) {
                    Text(
                        text = "Input terakhir ${status.lastInputTime ?: "-"}",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .width(80.dp)
                        .height(6.dp)
                        .background(NeutralBorder, MaterialTheme.shapes.small),
            ) {
                if (status.ratio > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(status.ratio.coerceIn(0f, 1f))
                                .background(StatusInfo),
                    )
                }
            }
            AppBadge(
                text = label,
                backgroundColor = color,
                contentColor = NeutralSurface,
            )
        }
    }
}
