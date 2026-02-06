package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun AppRadioGroup(
    label: String,
    options: List<DropdownOption>,
    selectedId: Long?,
    onSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    maxHeight: Dp? = null,
) {
    val scrollState = rememberScrollState()
    val listModifier =
        if (maxHeight != null) {
            Modifier.heightIn(max = maxHeight).verticalScroll(scrollState)
        } else {
            Modifier
        }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = label, style = MaterialTheme.typography.body1)
        Column(modifier = listModifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            options.forEach { option ->
                AppRadioOption(
                    option = option,
                    selected = option.id == selectedId,
                    onSelected = { onSelected(option) },
                )
            }
        }
        helperText?.let {
            Text(text = it, style = MaterialTheme.typography.body2, color = NeutralTextMuted)
        }
    }
}

@Composable
private fun AppRadioOption(
    option: DropdownOption,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clickable(onClick = onSelected)
                .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        RadioButton(selected = selected, onClick = onSelected)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = option.label, style = MaterialTheme.typography.body1)
            option.helper?.let {
                Text(text = it, style = MaterialTheme.typography.caption, color = NeutralTextMuted)
            }
        }
    }
}
