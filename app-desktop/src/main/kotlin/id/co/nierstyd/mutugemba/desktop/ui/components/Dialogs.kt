package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun ConfirmDialog(
    open: Boolean,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            PrimaryButton(text = confirmText, onClick = onConfirm)
        },
        dismissButton = {
            SecondaryButton(text = dismissText, onClick = onDismiss)
        },
    )
}
