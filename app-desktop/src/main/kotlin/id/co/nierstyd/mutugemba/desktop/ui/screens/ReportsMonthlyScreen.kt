package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun ReportsMonthlyScreen() {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
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
    }
}
