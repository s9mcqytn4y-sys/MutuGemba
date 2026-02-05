package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted

@Composable
fun AppDropdown(
    label: String,
    options: List<DropdownOption>,
    selectedOption: DropdownOption?,
    onSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih",
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedOption?.label ?: "",
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            ) {
                Column {
                    Text(option.label)
                    option.helper?.let {
                        Text(it, color = NeutralTextMuted)
                    }
                }
            }
        }
    }
}
