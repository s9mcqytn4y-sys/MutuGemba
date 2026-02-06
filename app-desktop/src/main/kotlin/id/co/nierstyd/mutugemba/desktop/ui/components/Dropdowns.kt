package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
fun AppDropdown(
    label: String,
    options: List<DropdownOption>,
    selectedOption: DropdownOption?,
    onSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pilih",
    enabled: Boolean = true,
    helperText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val canOpen = enabled && options.isNotEmpty()

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOption?.label ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                trailingIcon = {
                    Text(text = if (expanded) "^" else "v", color = NeutralTextMuted)
                },
                modifier =
                    Modifier.fillMaxWidth(),
                colors = dropdownColors(),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            enabled = canOpen,
                            indication = null,
                            interactionSource = interactionSource,
                        ) { expanded = !expanded },
            )

            DropdownMenu(
                expanded = expanded && canOpen,
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
                                Text(it, color = NeutralTextMuted, modifier = Modifier.padding(top = Spacing.xs))
                            }
                        }
                    }
                }
            }
        }
        helperText?.let {
            Text(text = it, color = NeutralTextMuted)
        }
    }
}

@Composable
private fun dropdownColors() =
    TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = BrandBlue,
        focusedLabelColor = BrandBlue,
        cursorColor = BrandBlue,
        unfocusedBorderColor = NeutralBorder,
        disabledBorderColor = NeutralBorder,
    )
