package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.InfoCard
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.usecase.UserFeedback

@Composable
fun LoginScreen(
    feedback: UserFeedback?,
    onLogin: (String, String) -> Unit,
) {
    var nameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = AppStrings.Login.Title,
            subtitle = AppStrings.Login.Subtitle,
        )

        InfoCard(title = AppStrings.Login.CardTitle) {
            AppTextField(
                spec =
                    FieldSpec(
                        label = AppStrings.Login.NameLabel,
                        placeholder = AppStrings.Login.NamePlaceholder,
                    ),
                value = nameInput,
                onValueChange = { nameInput = it },
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            AppTextField(
                spec =
                    FieldSpec(
                        label = AppStrings.Login.PasswordLabel,
                        placeholder = AppStrings.Login.PasswordPlaceholder,
                    ),
                value = passwordInput,
                onValueChange = { passwordInput = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            PrimaryButton(
                text = AppStrings.Login.Action,
                onClick = { onLogin(nameInput, passwordInput) },
                enabled = nameInput.isNotBlank() && passwordInput.isNotBlank(),
            )
        }

        feedback?.let { StatusBanner(feedback = it) }

        Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
            Text(
                text = AppStrings.Login.Tip,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}
