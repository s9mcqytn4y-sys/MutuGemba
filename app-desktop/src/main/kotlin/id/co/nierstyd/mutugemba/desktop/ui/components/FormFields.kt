package id.co.nierstyd.mutugemba.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import id.co.nierstyd.mutugemba.desktop.ui.theme.BrandBlue
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
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
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(spec.label) },
            placeholder = { spec.placeholder?.let { Text(it) } },
            isError = spec.isError,
            enabled = spec.enabled,
            modifier = Modifier.fillMaxWidth(),
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
    )
}

@Composable
fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onValueChange(filterNumberInput(input, false)) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            textStyle = TextStyle(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
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
