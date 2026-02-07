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
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun AbnormalScreen() {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = AppStrings.Abnormal.Title,
            subtitle = AppStrings.Abnormal.Subtitle,
        )
        Text(
            text = AppStrings.Abnormal.Description,
            style = MaterialTheme.typography.body1,
        )
        PrimaryButton(
            text = AppStrings.Abnormal.CreateDummy,
            onClick = { showConfirm = true },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = AppStrings.Abnormal.Note,
            style = MaterialTheme.typography.body2,
        )
    }

    ConfirmDialog(
        open = showConfirm,
        title = AppStrings.Abnormal.DialogTitle,
        message = AppStrings.Abnormal.DialogMessage,
        confirmText = AppStrings.Actions.Create,
        dismissText = AppStrings.Actions.Cancel,
        onConfirm = { showConfirm = false },
        onDismiss = { showConfirm = false },
    )
}
