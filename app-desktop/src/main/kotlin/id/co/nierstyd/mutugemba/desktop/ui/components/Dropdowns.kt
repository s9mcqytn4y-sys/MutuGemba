package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing

@Composable
@Suppress("LongParameterList")
fun AppDropdown(
    label: String,
    options: List<DropdownOption>,
    selectedOption: DropdownOption?,
    onSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = AppStrings.Common.Select,
    enabled: Boolean = true,
    helperText: String? = null,
    isError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val canOpen = enabled && options.isNotEmpty()
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded && canOpen) 180f else 0f)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedOption?.label ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                isError = isError,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                trailingIcon = {
                    DropdownTrailingIcon(
                        enabled = enabled,
                        arrowRotation = arrowRotation,
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size
                        },
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

            AppDropdownMenu(
                expanded = expanded && canOpen,
                textFieldSize = textFieldSize,
                options = options,
                onDismiss = { expanded = false },
                onSelect = {
                    onSelected(it)
                    expanded = false
                },
            )
        }
        helperText?.let {
            Text(text = it, color = if (isError) MaterialTheme.colors.error else NeutralTextMuted)
        }
    }
}

@Composable
private fun DropdownTrailingIcon(
    enabled: Boolean,
    arrowRotation: Float,
) {
    Surface(
        color =
            if (enabled) {
                MaterialTheme.colors.primary.copy(alpha = 0.12f)
            } else {
                NeutralBorder.copy(alpha = 0.35f)
            },
        shape = MaterialTheme.shapes.small,
        elevation = 0.dp,
    ) {
        Icon(
            imageVector = AppIcons.ExpandMore,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colors.primary else NeutralTextMuted,
            modifier =
                Modifier
                    .size(22.dp)
                    .padding(2.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
        )
    }
}

@Composable
private fun AppDropdownMenu(
    expanded: Boolean,
    textFieldSize: IntSize,
    options: List<DropdownOption>,
    onDismiss: () -> Unit,
    onSelect: (DropdownOption) -> Unit,
) {
    val density = LocalDensity.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier =
            Modifier
                .padding(top = Spacing.xs)
                .let {
                    if (textFieldSize.width > 0) {
                        with(density) { it.width(textFieldSize.toSize().width.toDp()) }
                    } else {
                        it
                    }
                },
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                onClick = { onSelect(option) },
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

@Composable
private fun dropdownColors() =
    TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = BrandBlue,
        focusedLabelColor = BrandBlue,
        cursorColor = BrandBlue,
        unfocusedBorderColor = NeutralBorder,
        disabledBorderColor = NeutralBorder,
    )
