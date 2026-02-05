package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun SettingsScreen() {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Pengaturan",
            subtitle = "Pengaturan dasar aplikasi (dummy).",
        )
        Text(
            text = "Gunakan tombol di bawah untuk simulasi reset data dummy.",
            style = MaterialTheme.typography.body1,
        )
        PrimaryButton(
            text = "Reset Data Dummy",
            onClick = { showResetDialog = true },
        )
        Text(
            text = "Backup/restore akan tersedia pada milestone berikutnya.",
            style = MaterialTheme.typography.body2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SecondaryButton(
                text = "Backup (Nanti)",
                onClick = {},
                enabled = false,
            )
            SecondaryButton(
                text = "Restore (Nanti)",
                onClick = {},
                enabled = false,
            )
        }
    }

    ConfirmDialog(
        open = showResetDialog,
        title = "Reset Data",
        message = "Yakin ingin reset data dummy?",
        confirmText = "Reset",
        dismissText = "Batal",
        onConfirm = { showResetDialog = false },
        onDismiss = { showResetDialog = false },
    )
}
