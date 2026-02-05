package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun HomeScreen() {
    var statusText by remember { mutableStateOf("Halo! MutuGemba siap.") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Beranda",
            subtitle = "Ringkasan akses cepat untuk operator dan inspector.",
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.body1,
        )
        PrimaryButton(
            text = "Tes Berhasil",
            onClick = { statusText = "Berhasil ✅" },
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Pilih menu di kiri untuk mulai bekerja.",
            style = MaterialTheme.typography.body2,
        )
    }
}
