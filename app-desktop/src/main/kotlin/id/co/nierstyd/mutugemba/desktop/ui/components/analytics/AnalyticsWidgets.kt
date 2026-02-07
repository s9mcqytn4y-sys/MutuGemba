package id.co.nierstyd.mutugemba.desktop.ui.components.analytics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.draw.shadow
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
import id.co.nierstyd.mutugemba.desktop.ui.components.SkeletonBlock
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
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
import id.co.nierstyd.mutugemba.desktop.ui.util.NumberFormats
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

data class TopProblemItemUi(
    val partNumber: String,
    val partName: String,
    val totalDefect: Int,
)

data class LineComparisonItemUi(
    val lineName: String,
    val totalDefect: Int,
    val ratio: Double,
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
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        AnalyticsToggleChip(
            label = AppStrings.Analytics.All,
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
        modifier =
            Modifier
                .sizeIn(minWidth = 88.dp, minHeight = 32.dp)
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
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
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
                    Text(text = AppStrings.Analytics.InsightTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = AppStrings.Analytics.monthLabel(DateTimeFormats.formatMonth(month)),
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                LineFilterChips(lines = lines, selectedLineId = selectedLineId, onSelectedLine = onSelectedLine)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = AppStrings.Analytics.Documents,
                    value = totals.totalDocs.toString(),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = AppStrings.Analytics.DaysFilled,
                    value = "${totals.daysWithInput}/${totals.daysInMonth}",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = AppStrings.Common.TotalCheck,
                    value = totals.totalCheck.toString(),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = AppStrings.Common.TotalNg,
                    value = totals.totalDefect.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = AppStrings.Analytics.EmptyDays,
                    value = (totals.daysInMonth - totals.daysWithInput).toString(),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = AppStrings.Analytics.NgPerDoc,
                    value =
                        if (totals.totalDocs > 0) {
                            NumberFormats.formatNumberNoDecimal(
                                totals.totalDefect.toDouble() / totals.totalDocs.toDouble(),
                            )
                        } else {
                            AppStrings.Common.Placeholder
                        },
                    modifier = Modifier.weight(1f),
                )
            }
Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MonthlyMetricCard(
                    title = AppStrings.Common.NgRatio,
                    value = NumberFormats.formatPercentNoDecimal(totals.ratio),
                    modifier = Modifier.weight(1f),
                )
                MonthlyMetricCard(
                    title = AppStrings.Analytics.NgPerDay,
                    value =
                        if (totals.avgDefectPerDay <= 0.0) {
                            AppStrings.Common.Placeholder
                        } else {
                            NumberFormats.formatNumberNoDecimal(totals.avgDefectPerDay)
                        },
                    modifier = Modifier.weight(1f),
                )
            }
            val coveragePercent = NumberFormats.formatPercentNoDecimal(dayRatio.toDouble())
            val coverageColor =
                when {
                    dayRatio >= 0.8f -> StatusSuccess
                    dayRatio >= 0.5f -> StatusWarning
                    else -> StatusError
                }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppStrings.Analytics.partRecorded(totals.totalParts),
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = coverageColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, coverageColor.copy(alpha = 0.4f)),
                        elevation = 0.dp,
                    ) {
                        Text(
                            text = coveragePercent,
                            style = MaterialTheme.typography.caption,
                            color = coverageColor,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp),
                        )
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(NeutralLight, MaterialTheme.shapes.small),
                ) {
                    if (dayRatio > 0f) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(dayRatio)
                                    .height(6.dp)
                                    .background(coverageColor, MaterialTheme.shapes.small),
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
            defectSummaries
                .filter { it.totalQuantity > 0 }
                .map { it.defectName to it.totalQuantity }
                .sortedWith(
                    compareByDescending<Pair<String, Int>> { it.second }
                        .thenBy { it.first },
                )
                .take(6)
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
                    Text(text = AppStrings.Analytics.ParetoTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = AppStrings.Analytics.paretoSubtitle(DateTimeFormats.formatMonth(month)),
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
                Text(text = AppStrings.Analytics.NoData, style = MaterialTheme.typography.body2)
            } else {
                val maxValue = paretoItems.maxOfOrNull { it.second } ?: 1
                val total = paretoItems.sumOf { it.second }.coerceAtLeast(1)
                val palette = paretoPalette()
                var cumulative = 0
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = AppStrings.Analytics.TopDominant,
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
                                        text = AppStrings.Analytics.topDominantItem(label, count),
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
                text = AppStrings.Analytics.valueWithPercent(value, NumberFormats.formatPercent(ratio)),
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
            text = AppStrings.Analytics.cumulativeLabel(NumberFormats.formatPercent(cumulativeRatio)),
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
                    Text(text = AppStrings.Analytics.TrendTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = AppStrings.Analytics.TrendSubtitle,
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
                text = AppStrings.Analytics.TrendFootnote,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
fun TopProblemItemCard(
    month: YearMonth,
    lines: List<Line>,
    selectedLineId: Long?,
    onSelectedLine: (Long?) -> Unit,
    item: TopProblemItemUi?,
    loading: Boolean,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = AppStrings.Analytics.TopProblemTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        text = AppStrings.Analytics.TopProblemSubtitle,
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
                LineFilterChips(lines = lines, selectedLineId = selectedLineId, onSelectedLine = onSelectedLine)
            }
            Text(
                text = AppStrings.Analytics.monthLabel(DateTimeFormats.formatMonth(month)),
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            if (loading) {
                SkeletonBlock(width = 220.dp, height = 18.dp)
                SkeletonBlock(width = 160.dp, height = 10.dp)
            } else if (item == null) {
                Text(text = AppStrings.Analytics.NoData, style = MaterialTheme.typography.body2)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "${item.partNumber} • ${item.partName}",
                        style = MaterialTheme.typography.subtitle1,
                        color = NeutralText,
                    )
                    Text(
                        text = "NG ${item.totalDefect}",
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
            }
        }
    }
}

@Composable
fun LineComparisonCard(
    items: List<LineComparisonItemUi>,
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
            Column {
                Text(text = AppStrings.Analytics.LineCompareTitle, style = MaterialTheme.typography.subtitle1)
                Text(
                    text = AppStrings.Analytics.LineCompareSubtitle,
                    style = MaterialTheme.typography.body2,
                    color = NeutralTextMuted,
                )
            }
            if (items.isEmpty()) {
                Text(text = AppStrings.Analytics.NoData, style = MaterialTheme.typography.body2)
            } else {
                val maxNg = (items.maxOfOrNull { it.totalDefect } ?: 1).coerceAtLeast(1)
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = item.lineName, style = MaterialTheme.typography.body2, color = NeutralText)
                            Text(
                                text = "${item.totalDefect} • ${NumberFormats.formatPercentNoDecimal(item.ratio)}",
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
                            val ratio = item.totalDefect.toFloat() / maxNg.toFloat()
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(ratio)
                                        .height(8.dp)
                                        .background(BrandBlue, MaterialTheme.shapes.small),
                            )
                        }
                    }
                }
            }
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


