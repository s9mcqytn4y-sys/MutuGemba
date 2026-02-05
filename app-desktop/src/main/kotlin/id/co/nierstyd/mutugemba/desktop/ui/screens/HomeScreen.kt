package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.domain.InspectionRecord

@Composable
fun HomeScreen(
    recentRecords: List<InspectionRecord>,
    onNavigateToInspection: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Beranda",
            subtitle = "Ringkasan aktivitas QC TPS harian.",
        )
        PrimaryButton(
            text = "Mulai Input Inspeksi",
            onClick = onNavigateToInspection,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Riwayat Inspeksi Terbaru",
            style = MaterialTheme.typography.subtitle1,
        )
        if (recentRecords.isEmpty()) {
            Text(
                text = "Belum ada data. Silakan input inspeksi terlebih dahulu.",
                style = MaterialTheme.typography.body2,
            )
        } else {
            recentRecords.take(5).forEach { record ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                ) {
                    Text(
                        text = "${record.type} - ${record.lineName}",
                        style = MaterialTheme.typography.body1,
                    )
                    Text(
                        text = record.createdAt,
                        style = MaterialTheme.typography.body2,
                        color = NeutralTextMuted,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Status: Offline - Lokal",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
}
