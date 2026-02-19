package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted

@Composable
fun AppTextField(
    spec: FieldSpec,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(spec.label) },
            placeholder = { spec.placeholder?.let { Text(it) } },
            isError = spec.isError,
            enabled = spec.enabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (isFocused) NeutralLight else NeutralSurface,
                        androidx.compose.material.MaterialTheme.shapes.small,
                    ).onFocusChanged { isFocused = it.isFocused },
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            colors = defaultTextFieldColors(),
        )
        spec.helperText?.let {
            Text(text = it, color = NeutralTextMuted)
        }
    }
}

@Composable
fun AppNumberField(
    spec: FieldSpec,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = false,
) {
    AppTextField(
        spec = spec,
        value = value,
        onValueChange = { input -> onValueChange(filterNumberInput(input, allowDecimal)) },
        modifier = modifier,
        singleLine = true,
    )
}

@Composable
fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = AppStrings.Common.Zero,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val displayValue = value
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { input -> onValueChange(filterNumberInput(input, false)) },
            placeholder = {
                if (displayValue.isBlank()) {
                    Text(
                        text = placeholder,
                        style = androidx.compose.material.MaterialTheme.typography.body1,
                        color = NeutralTextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            singleLine = true,
            enabled = enabled,
            textStyle =
                TextStyle(
                    textAlign = TextAlign.Center,
                    color = NeutralText,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(
                        if (isFocused) NeutralLight else NeutralSurface,
                        androidx.compose.material.MaterialTheme.shapes.small,
                    ),
            interactionSource = interactionSource,
            colors = defaultTextFieldColors(),
        )
    }
}

private fun filterNumberInput(
    input: String,
    allowDecimal: Boolean,
): String {
    val normalized = input.replace(',', '.')
    val builder = StringBuilder()
    var hasDecimal = false
    normalized.forEach { ch ->
        when {
            ch.isDigit() -> builder.append(ch)
            allowDecimal && ch == '.' && !hasDecimal -> {
                builder.append(ch)
                hasDecimal = true
            }
        }
    }
    return builder.toString()
}

@Composable
private fun defaultTextFieldColors() =
    TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = BrandBlue,
        focusedLabelColor = BrandBlue,
        cursorColor = BrandBlue,
        unfocusedBorderColor = NeutralBorder,
        disabledBorderColor = NeutralBorder,
    )
