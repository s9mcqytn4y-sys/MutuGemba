package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
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
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.ResetDataUseCase
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.time.LocalDate

@Composable
fun HomeScreen(
    recentRecords: List<InspectionRecord>,
    lines: List<Line>,
    resetData: ResetDataUseCase,
    onNavigateToInspection: () -> Unit,
    onRefreshData: () -> Unit,
) {
    val today = LocalDate.now()
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
            title = "Riwayat Input Checksheet Harian",
            subtitle = "Ringkasan batch inspeksi yang tersimpan.",
        )
        RecentInspectionBatches(records = recentRecords)

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
                MetricCard(title = "Batch Input", value = totalToday.toString(), modifier = Modifier.weight(1f))
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
                    text = "${status.recordCount} batch input",
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

@Composable
private fun RecentInspectionBatches(records: List<InspectionRecord>) {
    if (records.isEmpty()) {
        Text(
            text = "Belum ada data inspeksi.",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )
        return
    }

    val grouped =
        records
            .groupBy { it.createdAt }
            .entries
            .sortedByDescending { entry -> DateTimeFormats.parseLocalDateTime(entry.key) }
            .take(6)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        grouped.forEachIndexed { index, entry ->
            val first = entry.value.first()
            val parts =
                entry.value.map { it.partNumber }.distinct()
            RecentBatchCard(
                title = "Checksheet • ${first.lineName}",
                subtitle = "${first.shiftName} • ${DateTimeFormats.formatTimestampWithZone(first.createdAt)}",
                parts = parts,
                badgeColor = StatusInfo,
                legend = "Rekap input checksheet harian.",
            )
            if (index < grouped.lastIndex) {
                Divider(color = NeutralBorder, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun RecentBatchCard(
    title: String,
    subtitle: String,
    parts: List<String>,
    badgeColor: androidx.compose.ui.graphics.Color,
    legend: String,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                AppBadge(
                    text = title.substringBefore("•").trim(),
                    backgroundColor = badgeColor,
                    contentColor = NeutralSurface,
                )
                Text(text = title, style = MaterialTheme.typography.body1, color = NeutralText)
            }
            Text(text = subtitle, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
            Text(text = legend, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            if (parts.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    parts.take(6).forEach { partNumber ->
                        AppBadge(
                            text = partNumber,
                            backgroundColor = NeutralLight,
                            contentColor = NeutralText,
                        )
                    }
                }
                if (parts.size > 6) {
                    Text(
                        text = "+${parts.size - 6} part NG lainnya",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
            } else {
                Text(
                    text = "Tidak ada part NG di batch ini.",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
            }
        }
    }
}
