package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun ReportsScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Laporan",
            subtitle = "Dasar visual untuk Pareto dan trend NG.",
        )
        ReportPlaceholderCard(
            title = "Pareto NG",
            description = "Menampilkan kontribusi jenis NG terbesar.",
        )
        ReportPlaceholderCard(
            title = "Trend NG",
            description = "Tren NG harian/mingguan untuk pemantauan stabilitas.",
        )
    }
}

@Composable
private fun ReportPlaceholderCard(
    title: String,
    description: String,
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
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            Text(text = description, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                listOf(0.6f, 0.4f, 0.3f, 0.2f, 0.1f).forEach { heightRatio ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp * heightRatio)
                                .background(NeutralLight, MaterialTheme.shapes.small),
                    )
                }
            }
        }
    }
}
