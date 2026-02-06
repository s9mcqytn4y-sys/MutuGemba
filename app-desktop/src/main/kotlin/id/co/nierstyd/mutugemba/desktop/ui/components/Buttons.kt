package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.Sizing

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(Sizing.controlHeightLarge),
        colors =
            ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary,
                disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.4f),
                disabledContentColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f),
            ),
    ) {
        val textColor = if (enabled) NeutralSurface else NeutralSurface.copy(alpha = 0.8f)
        Text(text, color = textColor)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(Sizing.controlHeightLarge),
    ) {
        Text(text)
    }
}
