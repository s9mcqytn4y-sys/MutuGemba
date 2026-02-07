package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun ReportsMonthlyScreen() {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Laporan Bulanan",
            subtitle = "Fondasi laporan bulanan berbasis checksheet harian.",
        )
        Text(
            text = "Halaman ini akan menampilkan ringkasan bulanan, Pareto, dan trend lintas line.",
            style = MaterialTheme.typography.body2,
            color = NeutralTextMuted,
        )
    }
}
