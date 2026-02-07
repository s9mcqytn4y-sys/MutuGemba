package id.co.nierstyd.mutugemba.desktop.ui.components.analytics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.Icons
import androidx.compose.material.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.analytics.paretoCounts
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandRed
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.desktop.ui.util.DateTimeFormats
import id.co.nierstyd.mutugemba.domain.DailyChecksheetSummary
import id.co.nierstyd.mutugemba.domain.DefectSummary
import id.co.nierstyd.mutugemba.domain.Line
import java.time.YearMonth

data class MonthlyTotalsUi(
    val totalDocs: Int,
    val totalCheck: Int,
    val totalDefect: Int,
    val ratio: Double,
    val daysWithInput: Int,
    val daysInMonth: Int,
    val totalParts: Int,
    val avgDefectPerDay: Double,
)

fun buildLineColors(lines: List<Line>): Map<Long, androidx.compose.ui.graphics.Color> {
    val palette =
        listOf(
            BrandBlue,
            StatusSuccess,
            StatusWarning,
            StatusInfo,
        )
    return lines.mapIndexed { index, line -> line.id to palette[index % palette.size] }.toMap()
}

private fun paretoPalette(): List<androidx.compose.ui.graphics.Color> =
    listOf(
        StatusError,
        StatusWarning,
        StatusSuccess,
        StatusInfo,
        BrandBlue,
        BrandRed,
    )

@Composable
private fun LineFilterChips(
    lines: List<Line>,
    selectedLineId: Long?,
    onSelectedLine: (Long?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AnalyticsToggleChip(
            label = "Semua",
            selected = selectedLineId == null,
            onClick = { onSelectedLine(null) },
        )
        lines.forEach { line ->
            AnalyticsToggleChip(
                label = line.name,
                selected = selectedLineId == line.id,
                onClick = { onSelectedLine(line.id) },
            )
        }
    }
}

@Composable
private fun AnalyticsToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) BrandBlue else NeutralSurface
    val contentColor = if (selected) NeutralSurface else NeutralText
    Surface(
        modifier = Modifier
            .clickable { onClick() },
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
fun MonthlyInsightCard(
    month: YearMonth,
    totals: MonthlyTotalsUi,
    lines: List<Line>,
    selectedLineId: Long?,
    onSelectedLine: (Long?) -> Unit,
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Ringkasan Bulan Ini", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = "${DateTimeFormats.formatMonth(month)} - Ringkasan checksheet harian.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                LineFilterChips(lines = lines, selectedLineId = selectedLineId, onSelectedLine = onSelectedLine)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = "Dokumen",
                    value = totals.totalDocs.toString(),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = "Hari Terisi",
                    value = "${totals.daysWithInput}/${totals.daysInMonth}",
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
                    title = "Total NG",
                    value = totals.totalDefect.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = "Hari Kosong",
                    value = (totals.daysInMonth - totals.daysWithInput).toString(),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = "NG/Dokumen",
                    value = if (totals.totalDocs > 0) "%.1f".format(totals.totalDefect.toDouble() / totals.totalDocs.toDouble()) else "-",
                    modifier = Modifier.weight(1f),
                )
            }
Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = "Rasio NG",
                    value = formatPercent(totals.ratio),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = "NG per Hari",
                    value = if (totals.avgDefectPerDay <= 0.0) "-" else "%.1f".format(totals.avgDefectPerDay),
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = "Part tercatat: ${totals.totalParts}",
                    style = MaterialTheme.typography.caption,
                    color = NeutralTextMuted,
                )
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
fun MonthlyParetoCard(
    month: YearMonth,
    defectSummaries: List<DefectSummary>,
    accentColor: androidx.compose.ui.graphics.Color,
    loading: Boolean,
    lines: List<Line>,
    selectedLineId: Long?,
    onSelectedLine: (Long?) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = "Pareto NG Bulan Ini", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = "Kontribusi NG terbesar & kumulatif (${DateTimeFormats.formatMonth(month)}).",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                LineFilterChips(lines = lines, selectedLineId = selectedLineId, onSelectedLine = onSelectedLine)
            }
            if (loading) {
                SkeletonBlock(width = 240.dp, height = 18.dp)
                SkeletonBlock(width = 320.dp, height = 8.dp)
                SkeletonBlock(width = 280.dp, height = 8.dp)
            } else if (paretoItems.isEmpty()) {
                Text(text = "Belum ada data NG bulan ini.", style = MaterialTheme.typography.body2)
            } else {
                val maxValue = paretoItems.maxOfOrNull { it.second } ?: 1
                val total = paretoItems.sumOf { it.second }.coerceAtLeast(1)
                val palette = paretoPalette()
                var cumulative = 0
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "Top 3 NG Dominan",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        paretoItems.take(3).forEachIndexed { index, (label, count) ->
                            val color = palette[index % palette.size]
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = NeutralLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, color),
                                elevation = 0.dp,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
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
                                        text = "$label ($count)",
                                        style = MaterialTheme.typography.caption,
                                        color = NeutralTextMuted,
                                    )
                                }
                            }
                        }
                    }
                }
Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ParetoPieChart(
                        items = paretoItems,
                        palette = palette,
                        modifier = Modifier.size(140.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        paretoItems.forEachIndexed { index, (label, count) ->
                            cumulative += count
                            val cumulativeRatio = cumulative.toDouble() / total.toDouble()
                            val ratio = count.toDouble() / total.toDouble()
                            ParetoRow(
                                label = label,
                                value = count,
                                maxValue = maxValue,
                                ratio = ratio,
                                cumulativeRatio = cumulativeRatio,
                                accentColor = palette[index % palette.size],
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
    palette: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier,
) {
    val total = items.sumOf { it.second }.coerceAtLeast(1)
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
fun MonthlyTrendCard(
    month: YearMonth,
    dailySummaries: List<DailyChecksheetSummary>,
    lineColors: Map<Long, androidx.compose.ui.graphics.Color>,
    lines: List<Line>,
    selectedLineId: Long?,
    onSelectedLine: (Long?) -> Unit,
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Trend NG Harian", style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = "Pergerakan NG harian untuk monitoring stabilitas proses.",
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                LineFilterChips(lines = lines, selectedLineId = selectedLineId, onSelectedLine = onSelectedLine)
            }
            TrendChart(
                seriesByLine = seriesByLine,
                lineColors = lineColors,
                lines = lines,
                selectedLineId = selectedLineId,
            )
            Text(
                text = "Tanggal di bawah grafik menunjukkan hari ke-1 s/d akhir bulan. Titik = NG harian.",
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
    val dayCount = seriesByLine.values.firstOrNull()?.size ?: 0
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
        if (dayCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                (1..dayCount).forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                    }
                }
            }
        }
    }
}


fun formatPercent(value: Double): String =
    if (value <= 0.0) {
        "-"
    } else {
        "${"%.1f".format(value * 100)}%"
    }
